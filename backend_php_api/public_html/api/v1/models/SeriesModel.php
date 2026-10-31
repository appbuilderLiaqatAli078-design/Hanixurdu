<?php
declare(strict_types=1);

namespace HaniX\Api\Models;

use HaniX\Api\Config\Config;
use PDO;
use Throwable;

/**
 * Series Model (Preserves exact `series`, `series_seasons`, and `movies` episode queries from `index.php`, `series.php`, `view_series.php`)
 *
 * Existing Database Logic Preserved:
 * - Trending series: Read `series`, filter `FIND_IN_SET('trending', category) > 0`, order by `id DESC`,
 *   preserve poster fallback using `index_poster` and `poster_path`.
 * - Category rails (`urdu_subtitles`, `all`, `coming_soon`, `fan_dubbing`, `premium`): `FIND_IN_SET(:cat, category) > 0`.
 * - Seasons: Read distinct `season_name` from `movies` where `series_name = :series_name`, merged with `series_seasons`.
 * - Episodes (`series.php?ajax=1`): Read episodes from `movies` using `series_name` and `season_name`, ordered by `id ASC`.
 */
final class SeriesModel
{
    public static function formatSeries(array $row): array
    {
        $id = (int) ($row['id'] ?? 0);
        $seriesName = (string) ($row['series_name'] ?? $row['title'] ?? '');
        $indexTitle = (string) ($row['index_title'] ?? '');
        $displayTitle = $indexTitle !== '' ? $indexTitle : $seriesName;

        // Preserve existing poster fallback logic: index_poster -> poster_path
        $rawIndexPoster = trim((string) ($row['index_poster'] ?? ''));
        $rawPosterPath = trim((string) ($row['poster_path'] ?? ''));
        $resolvedPoster = Config::assetUrl($rawIndexPoster !== '' ? $rawIndexPoster : $rawPosterPath);
        $category = (string) ($row['category'] ?? '');
        $isPremium = str_contains(strtolower($category), 'premium') || str_contains(strtolower($category), 'vip');

        return [
            'id'           => (string) $id,
            'series_id'    => $id,
            'title'        => $displayTitle,
            'series_name'  => $seriesName,
            'index_title'  => $indexTitle,
            'description'  => (string) ($row['description'] ?? ''),
            'poster'       => $resolvedPoster,
            'poster_path'  => Config::assetUrl($rawPosterPath !== '' ? $rawPosterPath : $rawIndexPoster),
            'index_poster' => Config::assetUrl($rawIndexPoster !== '' ? $rawIndexPoster : $rawPosterPath),
            'category'     => $category,
            'trailer_link' => (string) ($row['trailer_link'] ?? ''),
            'custom_url'   => (string) ($row['custom_url'] ?? ''),
            'contentType'  => 'series',
            'isPremium'    => $isPremium,
            'is_premium'   => $isPremium ? 1 : 0,
        ];
    }

    /**
     * Trending Series query from `index.php`:
     * `SELECT * FROM series WHERE FIND_IN_SET('trending', category) > 0 ORDER BY id DESC LIMIT 15`
     */
    public static function getTrendingSeries(PDO $pdo, int $limit = 15): array
    {
        $stmt = $pdo->prepare("
            SELECT *
            FROM series
            WHERE FIND_IN_SET('trending', category) > 0 OR LOWER(category) LIKE '%trending%'
            ORDER BY id DESC
            LIMIT :limit
        ");
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'formatSeries'], $stmt->fetchAll());
    }

    /**
     * Category rail query from `index.php`:
     * `SELECT * FROM series WHERE FIND_IN_SET(:cat, category) > 0 ORDER BY id DESC`
     */
    public static function getSeriesByCategory(PDO $pdo, string $category, int $limit = 30): array
    {
        $cleanCat = trim($category);
        $stmt = $pdo->prepare("
            SELECT *
            FROM series
            WHERE FIND_IN_SET(:cat, category) > 0 OR LOWER(category) LIKE LOWER(:likeCat)
            ORDER BY id DESC
            LIMIT :limit
        ");
        $stmt->bindValue(':cat', $cleanCat, PDO::PARAM_STR);
        $stmt->bindValue(':likeCat', '%' . $cleanCat . '%', PDO::PARAM_STR);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'formatSeries'], $stmt->fetchAll());
    }

    public static function getSeriesList(PDO $pdo, int $limit = 20, int $offset = 0, string $category = ''): array
    {
        if ($category !== '' && strtolower($category) !== 'all') {
            $cleanCat = trim($category);
            $stmt = $pdo->prepare('
                SELECT * FROM series
                WHERE FIND_IN_SET(:cat, category) > 0 OR LOWER(category) LIKE LOWER(:likeCat)
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            ');
            $stmt->bindValue(':cat', $cleanCat, PDO::PARAM_STR);
            $stmt->bindValue(':likeCat', '%' . $cleanCat . '%', PDO::PARAM_STR);
        } else {
            $stmt = $pdo->prepare('
                SELECT * FROM series
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            ');
        }
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'formatSeries'], $stmt->fetchAll());
    }

    public static function findSeries(PDO $pdo, string $idOrName): ?array
    {
        if (ctype_digit($idOrName)) {
            $stmt = $pdo->prepare('SELECT * FROM series WHERE id = :id LIMIT 1');
            $stmt->execute([':id' => (int) $idOrName]);
            $row = $stmt->fetch();
            if (is_array($row)) {
                return $row;
            }
        }

        $stmt = $pdo->prepare('
            SELECT * FROM series
            WHERE series_name = :name OR custom_url = :slug OR index_title = :title
            LIMIT 1
        ');
        $stmt->execute([
            ':name'  => $idOrName,
            ':slug'  => $idOrName,
            ':title' => $idOrName,
        ]);
        $row = $stmt->fetch();
        return is_array($row) ? $row : null;
    }

    /**
     * Reads seasons from `movies` (`DISTINCT season_name`) and merges with `series_seasons` if present (`series.php`).
     */
    public static function getSeasonsForSeries(PDO $pdo, array $seriesRow): array
    {
        $seriesId = (string) ($seriesRow['id'] ?? '');
        $seriesName = (string) ($seriesRow['series_name'] ?? $seriesRow['title'] ?? '');
        $seasonNames = [];

        // 1. Read from `movies` table where episodes are stored
        if ($seriesName !== '') {
            $stmt = $pdo->prepare("
                SELECT DISTINCT season_name
                FROM movies
                WHERE series_name = :sname
                  AND season_name IS NOT NULL
                  AND TRIM(season_name) != ''
                ORDER BY id ASC
            ");
            $stmt->execute([':sname' => $seriesName]);
            foreach ($stmt->fetchAll() as $r) {
                $sn = trim((string) ($r['season_name'] ?? ''));
                if ($sn !== '' && !in_array($sn, $seasonNames, true)) {
                    $seasonNames[] = $sn;
                }
            }
        }

        // 2. Also read from `series_seasons` table if used by `edit_series.php` / `view_series.php`
        if ($seriesName !== '') {
            try {
                $sStmt = $pdo->prepare("
                    SELECT season_name
                    FROM series_seasons
                    WHERE series_name = :sname
                      AND season_name IS NOT NULL
                      AND TRIM(season_name) != ''
                    ORDER BY id ASC
                ");
                $sStmt->execute([':sname' => $seriesName]);
                foreach ($sStmt->fetchAll() as $r) {
                    $sn = trim((string) ($r['season_name'] ?? ''));
                    if ($sn !== '' && !in_array($sn, $seasonNames, true)) {
                        $seasonNames[] = $sn;
                    }
                }
            } catch (Throwable $ignored) {
            }
        }

        if (empty($seasonNames)) {
            $seasonNames[] = 'Season 1';
        }

        $seasons = [];
        foreach ($seasonNames as $index => $seasonName) {
            $seasons[] = [
                'id'          => $seasonName,
                'season_id'   => $index + 1,
                'seriesId'    => $seriesId,
                'series_name' => $seriesName,
                'seasonName'  => $seasonName,
                'season_name' => $seasonName,
            ];
        }
        return $seasons;
    }

    /**
     * Reads episodes from `movies` using `series_name` and `season_name`, preserving episode ordering by `id ASC` (`series.php?ajax=1`).
     */
    public static function getEpisodesForSeriesSeason(PDO $pdo, string $seriesName, string $seasonName = ''): array
    {
        if ($seasonName !== '') {
            $stmt = $pdo->prepare('
                SELECT *
                FROM movies
                WHERE series_name = :sname
                  AND season_name = :season
                ORDER BY id ASC
            ');
            $stmt->execute([
                ':sname'  => $seriesName,
                ':season' => $seasonName,
            ]);
        } else {
            $stmt = $pdo->prepare('
                SELECT *
                FROM movies
                WHERE series_name = :sname
                ORDER BY id ASC
            ');
            $stmt->execute([':sname' => $seriesName]);
        }

        $rows = $stmt->fetchAll();
        $episodes = [];
        foreach ($rows as $row) {
            $id = (int) ($row['id'] ?? 0);
            $poster = Config::assetUrl((string) ($row['poster_path'] ?? $row['poster'] ?? ''));
            $isPremium = MovieModel::isPremiumRow($row);
            $hasVideo = MovieModel::hasVideoSource($row);

            $episodes[] = [
                'id'                   => (string) $id,
                'episode_id'           => $id,
                'seasonId'             => (string) ($row['season_name'] ?? $seasonName ?: 'Season 1'),
                'series_name'          => (string) ($row['series_name'] ?? $seriesName),
                'season_name'          => (string) ($row['season_name'] ?? $seasonName ?: 'Season 1'),
                'title'                => (string) ($row['title'] ?? ('Episode ' . $id)),
                'description'          => (string) ($row['description'] ?? ''),
                'poster'               => $poster,
                'poster_path'          => $poster,
                'isPremium'            => $isPremium,
                'is_premium'           => $isPremium ? 1 : 0,
                'has_video'            => $hasVideo,
                'duration'             => (string) ($row['duration'] ?? ''),
                'custom_url'           => (string) ($row['custom_url'] ?? ''),
                'playbackAvailability' => $hasVideo ? 'READY' : 'UNAVAILABLE',
            ];
        }
        return $episodes;
    }
}
