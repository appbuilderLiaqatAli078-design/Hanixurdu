<?php
declare(strict_types=1);

namespace HaniX\Api\Models;

use HaniX\Api\Config\Config;
use PDO;
use Throwable;

/**
 * Movie Model (Preserves exact `movies` table queries from `index.php`, `search.php`, `run_video.php`, `edit_movie.php`)
 *
 * Existing Database Rules Preserved:
 * - Standalone movies have `(series_name IS NULL OR series_name = '' OR LOWER(series_name) = 'general')`.
 * - Trending movies filter `FIND_IN_SET('trending', category) > 0` with the General/empty series-name condition, ordered by `id DESC`.
 * - Category rails (`urdu_subtitles`, `all`, `coming_soon`, `fan_dubbing`, `premium`) filter `FIND_IN_SET(:cat, category) > 0`.
 * - Calculates `has_video` and `is_premium` (`is_premium == 1` or `category` contains `'premium'`/`'vip'`).
 * - Never exposes raw `video_path`, `video_link`, `server1_link`, `server2_link` in public listing responses.
 */
final class MovieModel
{
    public static function isPremiumRow(array $row): bool
    {
        $flag = (int) ($row['is_premium'] ?? $row['is_vip'] ?? 0);
        if ($flag === 1) {
            return true;
        }
        $category = strtolower((string) ($row['category'] ?? ''));
        return str_contains($category, 'premium') || str_contains($category, 'vip');
    }

    public static function hasVideoSource(array $row): bool
    {
        $candidates = [
            $row['video_path'] ?? '',
            $row['video_link'] ?? '',
            $row['server1_link'] ?? '',
            $row['server2_link'] ?? '',
            $row['bunny_video_id'] ?? '',
        ];
        foreach ($candidates as $source) {
            if (trim((string) $source) !== '') {
                return true;
            }
        }
        return false;
    }

    public static function format(array $row): array
    {
        $id = (int) ($row['id'] ?? 0);
        $poster = Config::assetUrl((string) ($row['poster_path'] ?? $row['index_poster'] ?? $row['poster'] ?? ''));
        $isPremium = self::isPremiumRow($row);
        $hasVideo = self::hasVideoSource($row);
        $category = (string) ($row['category'] ?? 'General');
        $seriesName = (string) ($row['series_name'] ?? '');
        $isEpisode = $seriesName !== '' && strtolower(trim($seriesName)) !== 'general';

        return [
            'id'           => (string) $id,
            'movie_id'     => $id,
            'title'        => (string) ($row['title'] ?? ''),
            'description'  => (string) ($row['description'] ?? ''),
            'poster'       => $poster,
            'poster_path'  => $poster,
            'category'     => $category,
            'series_name'  => $seriesName,
            'season_name'  => (string) ($row['season_name'] ?? ''),
            'contentType'  => $isEpisode ? 'episode' : 'movie',
            'isPremium'    => $isPremium,
            'is_premium'   => $isPremium ? 1 : 0,
            'has_video'    => $hasVideo,
            'duration'     => (string) ($row['duration'] ?? ''),
            'custom_url'   => (string) ($row['custom_url'] ?? ''),
            'views'        => (int) ($row['views'] ?? 0),
        ];
    }

    /**
     * Trending Movies query from `index.php`:
     * `SELECT * FROM movies WHERE FIND_IN_SET('trending', category) > 0 AND (series_name = 'General' OR series_name IS NULL OR series_name = '') ORDER BY id DESC LIMIT 15`
     */
    public static function getTrendingMovies(PDO $pdo, int $limit = 15): array
    {
        $stmt = $pdo->prepare("
            SELECT *
            FROM movies
            WHERE (FIND_IN_SET('trending', category) > 0 OR LOWER(category) LIKE '%trending%')
              AND (series_name IS NULL OR TRIM(series_name) = '' OR LOWER(TRIM(series_name)) = 'general')
            ORDER BY id DESC
            LIMIT :limit
        ");
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'format'], $stmt->fetchAll());
    }

    /**
     * Category rail query from `index.php`:
     * `SELECT * FROM movies WHERE FIND_IN_SET(:cat, category) > 0 AND (series_name = 'General' OR series_name IS NULL OR series_name = '') ORDER BY id DESC`
     */
    public static function getMoviesByCategory(PDO $pdo, string $category, int $limit = 30): array
    {
        $cleanCat = trim($category);
        $stmt = $pdo->prepare("
            SELECT *
            FROM movies
            WHERE (FIND_IN_SET(:cat, category) > 0 OR LOWER(category) LIKE LOWER(:likeCat))
              AND (series_name IS NULL OR TRIM(series_name) = '' OR LOWER(TRIM(series_name)) = 'general')
            ORDER BY id DESC
            LIMIT :limit
        ");
        $stmt->bindValue(':cat', $cleanCat, PDO::PARAM_STR);
        $stmt->bindValue(':likeCat', '%' . $cleanCat . '%', PDO::PARAM_STR);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'format'], $stmt->fetchAll());
    }

    /**
     * Latest standalone movies (`series_name` empty or `'General'`), ordered by `id DESC`.
     */
    public static function getMovies(PDO $pdo, int $limit = 20, int $offset = 0, string $category = ''): array
    {
        if ($category !== '' && strtolower($category) !== 'all') {
            $cleanCat = trim($category);
            $stmt = $pdo->prepare("
                SELECT *
                FROM movies
                WHERE (series_name IS NULL OR TRIM(series_name) = '' OR LOWER(TRIM(series_name)) = 'general')
                  AND (FIND_IN_SET(:cat, category) > 0 OR LOWER(category) LIKE LOWER(:likeCat))
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            ");
            $stmt->bindValue(':cat', $cleanCat, PDO::PARAM_STR);
            $stmt->bindValue(':likeCat', '%' . $cleanCat . '%', PDO::PARAM_STR);
        } else {
            $stmt = $pdo->prepare("
                SELECT *
                FROM movies
                WHERE (series_name IS NULL OR TRIM(series_name) = '' OR LOWER(TRIM(series_name)) = 'general')
                ORDER BY id DESC
                LIMIT :limit OFFSET :offset
            ");
        }
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return array_map([self::class, 'format'], $stmt->fetchAll());
    }

    /**
     * "More Videos & Recommended for You" query from `run_video.php`:
     * Groups episodes by `series_name` so only the latest entry per series or standalone movie is recommended.
     */
    public static function getRecommendedMovies(PDO $pdo, int $excludeId, int $limit = 24): array
    {
        try {
            $stmt = $pdo->prepare("
                SELECT *
                FROM movies
                WHERE id != :excludeId
                  AND id IN (
                      SELECT MAX(id)
                      FROM movies
                      GROUP BY CASE
                          WHEN series_name = 'General' OR series_name IS NULL OR TRIM(series_name) = '' THEN CAST(id AS CHAR)
                          ELSE series_name
                      END
                  )
                ORDER BY id DESC
                LIMIT :limit
            ");
            $stmt->bindValue(':excludeId', $excludeId, PDO::PARAM_INT);
            $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
            $stmt->execute();
            return array_map([self::class, 'format'], $stmt->fetchAll());
        } catch (Throwable $ignored) {
            return self::getMovies($pdo, $limit, 0);
        }
    }

    public static function findByIdOrSlug(PDO $pdo, string $idOrSlug): ?array
    {
        if (ctype_digit($idOrSlug)) {
            $stmt = $pdo->prepare('SELECT * FROM movies WHERE id = :id LIMIT 1');
            $stmt->execute([':id' => (int) $idOrSlug]);
            $row = $stmt->fetch();
            if (is_array($row)) {
                return $row;
            }
        }

        $stmt = $pdo->prepare('SELECT * FROM movies WHERE custom_url = :slug OR title = :title LIMIT 1');
        $stmt->execute([':slug' => $idOrSlug, ':title' => $idOrSlug]);
        $row = $stmt->fetch();
        return is_array($row) ? $row : null;
    }
}
