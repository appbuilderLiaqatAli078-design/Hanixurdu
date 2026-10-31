<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Models\MovieModel;
use HaniX\Api\Models\SeriesModel;
use PDO;

/**
 * Home & Categories Controller (Preserves exact `index.php` queries & 5 category rails)
 *
 * Existing `index.php` Sections Preserved:
 * - Hero Coverflow (`trending`): Trending `series` + Trending standalone `movies` (`FIND_IN_SET('trending', category) > 0`, `LIMIT 15`)
 * - Category 1 (`urdu_subtitles`): "Urdu Subtitles" (16:9 landscape cards)
 * - Category 2 (`all`): "All Content"
 * - Category 3 (`coming_soon`): "Coming Soon"
 * - Category 4 (`fan_dubbing`): "Hanix Originals"
 * - Category 5 (`premium`): "Premium" (VIP Locked)
 */
final class HomeController
{
    public static function index(PDO $pdo): never
    {
        $trendingMovies = MovieModel::getTrendingMovies($pdo, 15);
        $trendingSeries = SeriesModel::getTrendingSeries($pdo, 15);
        $latestMovies = MovieModel::getMovies($pdo, 24, 0);
        $latestSeries = SeriesModel::getSeriesList($pdo, 24, 0);
        $categories = self::extractCategories($pdo);

        // Build the 5 exact category rails from index.php (merging series + standalone movies in each rail)
        $urduSubtitles = self::buildCategoryRail($pdo, 'urdu_subtitles', 24);
        $allContent = self::buildCategoryRail($pdo, 'all', 24);
        $comingSoon = self::buildCategoryRail($pdo, 'coming_soon', 24);
        $hanixOriginals = self::buildCategoryRail($pdo, 'fan_dubbing', 24);
        $premiumContent = self::buildCategoryRail($pdo, 'premium', 24);

        $featuredMovies = !empty($trendingMovies) ? $trendingMovies : array_slice($latestMovies, 0, 12);
        $featuredSeries = !empty($trendingSeries) ? $trendingSeries : array_slice($latestSeries, 0, 12);

        // Build Hero Coverflow banners just like index.php (trending series + trending standalone movies)
        $banners = [];
        foreach ($trendingSeries as $s) {
            $banners[] = [
                'id'          => (string) $s['id'],
                'title'       => $s['title'],
                'subtitle'    => $s['category'] !== '' ? $s['category'] : 'Series',
                'backdropUrl' => $s['poster'],
                'poster_path' => $s['poster'],
                'contentType' => 'series',
                'contentId'   => (string) $s['id'],
                'seriesName'  => (string) $s['series_name'],
                'customUrl'   => (string) ($s['custom_url'] ?? ''),
                'isPremium'   => (bool) ($s['isPremium'] ?? false),
                'is_premium'  => (int) ($s['is_premium'] ?? 0),
            ];
        }
        foreach ($trendingMovies as $m) {
            $banners[] = [
                'id'          => (string) $m['id'],
                'title'       => $m['title'],
                'subtitle'    => $m['category'] !== '' ? $m['category'] : 'Movie',
                'backdropUrl' => $m['poster_path'],
                'poster_path' => $m['poster_path'],
                'contentType' => 'movie',
                'contentId'   => (string) $m['id'],
                'seriesName'  => '',
                'customUrl'   => (string) ($m['custom_url'] ?? ''),
                'isPremium'   => (bool) $m['isPremium'],
                'is_premium'  => (int) $m['is_premium'],
            ];
        }

        if (empty($banners)) {
            foreach (array_slice($featuredMovies, 0, 6) as $m) {
                $banners[] = [
                    'id'          => (string) $m['id'],
                    'title'       => $m['title'],
                    'subtitle'    => $m['category'],
                    'backdropUrl' => $m['poster_path'],
                    'poster_path' => $m['poster_path'],
                    'contentType' => 'movie',
                    'contentId'   => (string) $m['id'],
                    'seriesName'  => '',
                    'customUrl'   => (string) ($m['custom_url'] ?? ''),
                    'isPremium'   => (bool) $m['isPremium'],
                    'is_premium'  => (int) $m['is_premium'],
                ];
            }
        }

        ResponseHelper::success([
            'banners'         => $banners,
            'trending_movies' => $trendingMovies,
            'trending_series' => $trendingSeries,
            'featuredMovies'  => $featuredMovies,
            'featuredSeries'  => $featuredSeries,
            'latestMovies'    => $latestMovies,
            'latestSeries'    => $latestSeries,
            'urduSubtitles'   => $urduSubtitles,
            'allContent'      => !empty($allContent) ? $allContent : $latestMovies,
            'comingSoon'      => $comingSoon,
            'hanixOriginals'  => $hanixOriginals,
            'premiumContent'  => $premiumContent,
            'categories'      => $categories,
        ]);
    }

    /**
     * Merges `series` and standalone `movies` for an `index.php` category rail (`urdu_subtitles`, `all`, `coming_soon`, `fan_dubbing`, `premium`).
     */
    private static function buildCategoryRail(PDO $pdo, string $categoryKey, int $limit = 24): array
    {
        $seriesRows = SeriesModel::getSeriesByCategory($pdo, $categoryKey, $limit);
        $movieRows = MovieModel::getMoviesByCategory($pdo, $categoryKey, $limit);

        $rail = [];
        foreach ($seriesRows as $s) {
            $rail[] = [
                'id'           => (string) $s['id'],
                'movie_id'     => (int) $s['series_id'],
                'title'        => $s['title'],
                'description'  => $s['description'],
                'poster'       => $s['poster'],
                'poster_path'  => $s['poster'],
                'category'     => $s['category'],
                'series_name'  => $s['series_name'],
                'season_name'  => '',
                'contentType'  => 'series',
                'isPremium'    => (bool) ($s['isPremium'] ?? ($categoryKey === 'premium')),
                'is_premium'   => ($s['isPremium'] ?? ($categoryKey === 'premium')) ? 1 : 0,
                'has_video'    => true,
                'duration'     => 'Series',
                'custom_url'   => $s['custom_url'],
                'views'        => 0,
            ];
        }
        foreach ($movieRows as $m) {
            $rail[] = $m;
        }
        return array_slice($rail, 0, $limit);
    }

    public static function trending(PDO $pdo): never
    {
        $trendingMovies = MovieModel::getTrendingMovies($pdo, 15);
        $trendingSeries = SeriesModel::getTrendingSeries($pdo, 15);

        ResponseHelper::success([
            'trending_movies' => $trendingMovies,
            'trending_series' => $trendingSeries,
            'featuredMovies'  => $trendingMovies,
            'featuredSeries'  => $trendingSeries,
        ]);
    }

    public static function banners(PDO $pdo): never
    {
        $movies = MovieModel::getTrendingMovies($pdo, 8);
        if (empty($movies)) {
            $movies = MovieModel::getMovies($pdo, 8, 0);
        }
        $banners = [];
        foreach ($movies as $m) {
            $banners[] = [
                'id'          => (string) $m['id'],
                'title'       => $m['title'],
                'subtitle'    => $m['category'],
                'backdropUrl' => $m['poster_path'],
                'poster_path' => $m['poster_path'],
                'contentType' => 'movie',
                'contentId'   => (string) $m['id'],
                'isPremium'   => $m['isPremium'],
                'is_premium'  => $m['is_premium'],
            ];
        }
        ResponseHelper::success($banners);
    }

    public static function featured(PDO $pdo): never
    {
        $trendingMovies = MovieModel::getTrendingMovies($pdo, 15);
        if (empty($trendingMovies)) {
            $trendingMovies = MovieModel::getMovies($pdo, 15, 0);
        }
        ResponseHelper::success($trendingMovies);
    }

    public static function latest(PDO $pdo): never
    {
        ResponseHelper::success(MovieModel::getMovies($pdo, 24, 0));
    }

    public static function categories(PDO $pdo): never
    {
        ResponseHelper::success(self::extractCategories($pdo));
    }

    public static function extractCategories(PDO $pdo): array
    {
        $officialRails = [
            'trending'       => ['id' => 'trending', 'name' => 'Trending', 'slug' => 'trending', 'itemCount' => 0],
            'urdu_subtitles' => ['id' => 'urdu_subtitles', 'name' => 'Urdu Subtitles', 'slug' => 'urdu_subtitles', 'itemCount' => 0],
            'all'            => ['id' => 'all', 'name' => 'All Content', 'slug' => 'all', 'itemCount' => 0],
            'coming_soon'    => ['id' => 'coming_soon', 'name' => 'Coming Soon', 'slug' => 'coming_soon', 'itemCount' => 0],
            'fan_dubbing'    => ['id' => 'fan_dubbing', 'name' => 'Hanix Originals', 'slug' => 'fan_dubbing', 'itemCount' => 0],
            'premium'        => ['id' => 'premium', 'name' => 'Premium', 'slug' => 'premium', 'itemCount' => 0],
        ];

        $stmt = $pdo->query("SELECT category FROM movies WHERE category IS NOT NULL AND TRIM(category) != ''");
        if ($stmt) {
            foreach ($stmt->fetchAll() as $row) {
                $parts = array_map('trim', explode(',', (string) $row['category']));
                foreach ($parts as $part) {
                    if ($part === '') {
                        continue;
                    }
                    $key = strtolower($part);
                    if (!isset($officialRails[$key])) {
                        $officialRails[$key] = [
                            'id'        => $key,
                            'name'      => $part,
                            'slug'      => preg_replace('/[^a-z0-9_]+/', '_', $key),
                            'itemCount' => 0,
                        ];
                    }
                    $officialRails[$key]['itemCount']++;
                }
            }
        }
        return array_values($officialRails);
    }
}
