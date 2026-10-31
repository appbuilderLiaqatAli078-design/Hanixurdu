<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Models\MovieModel;
use HaniX\Api\Models\SeriesModel;
use PDO;

/**
 * Series, Seasons & Episodes Controller (`series.php`, `view_series.php`, `series.php?ajax=1`)
 */
final class SeriesController
{
    public static function index(PDO $pdo): never
    {
        $pg = ResponseHelper::paginationParams(20, 50);
        $category = trim((string) ($_GET['category'] ?? ''));
        $seriesList = SeriesModel::getSeriesList($pdo, $pg['limit'], $pg['offset'], $category);
        ResponseHelper::success($seriesList);
    }

    public static function show(PDO $pdo, string $idOrName): never
    {
        $row = SeriesModel::findSeries($pdo, urldecode($idOrName));
        if (!$row) {
            ResponseHelper::error('Series not found.', 404, 'SERIES_NOT_FOUND');
        }
        $formatted = SeriesModel::formatSeries($row);
        $formatted['seasons'] = SeriesModel::getSeasonsForSeries($pdo, $row);
        ResponseHelper::success($formatted);
    }

    public static function seasons(PDO $pdo, string $idOrName): never
    {
        $row = SeriesModel::findSeries($pdo, urldecode($idOrName));
        if (!$row) {
            ResponseHelper::error('Series not found.', 404, 'SERIES_NOT_FOUND');
        }
        ResponseHelper::success(SeriesModel::getSeasonsForSeries($pdo, $row));
    }

    /**
     * Supports both:
     * - `GET /series/{id}/episodes?season={season}` (Part 2 master specification, matching `series.php?ajax=1`)
     * - `GET /seasons/{seasonId}/episodes`
     */
    public static function episodesBySeries(PDO $pdo, string $seriesIdOrName): never
    {
        $season = trim((string) ($_GET['season'] ?? $_GET['season_name'] ?? ''));
        $seriesRow = SeriesModel::findSeries($pdo, urldecode($seriesIdOrName));
        $seriesName = $seriesRow
            ? (string) ($seriesRow['series_name'] ?? $seriesRow['title'] ?? '')
            : urldecode($seriesIdOrName);

        if ($seriesName === '') {
            ResponseHelper::error('Series not found.', 404, 'SERIES_NOT_FOUND');
        }

        $episodes = SeriesModel::getEpisodesForSeriesSeason($pdo, $seriesName, $season);
        ResponseHelper::success($episodes);
    }

    public static function episodesBySeason(PDO $pdo, string $seasonIdentifier): never
    {
        $decoded = urldecode($seasonIdentifier);
        $seriesParam = trim((string) ($_GET['series'] ?? $_GET['series_name'] ?? ''));

        if ($seriesParam !== '') {
            ResponseHelper::success(SeriesModel::getEpisodesForSeriesSeason($pdo, $seriesParam, $decoded));
        }

        // Look up in `movies` by `season_name`
        $stmt = $pdo->prepare('SELECT * FROM movies WHERE season_name = :season ORDER BY id ASC');
        $stmt->execute([':season' => $decoded]);
        $rows = $stmt->fetchAll();

        if (!empty($rows)) {
            $seriesName = (string) ($rows[0]['series_name'] ?? '');
            ResponseHelper::success(SeriesModel::getEpisodesForSeriesSeason($pdo, $seriesName, $decoded));
        }

        ResponseHelper::success([]);
    }

    public static function episodeDetail(PDO $pdo, string $episodeId): never
    {
        $row = MovieModel::findByIdOrSlug($pdo, $episodeId);
        if (!$row) {
            ResponseHelper::error('Episode not found.', 404, 'EPISODE_NOT_FOUND');
        }
        $formatted = MovieModel::format($row);
        ResponseHelper::success([
            'id'                   => $formatted['id'],
            'seasonId'             => $formatted['season_name'] !== '' ? $formatted['season_name'] : 'Season 1',
            'series_name'          => $formatted['series_name'],
            'season_name'          => $formatted['season_name'],
            'title'                => $formatted['title'],
            'description'          => $formatted['description'],
            'poster'               => $formatted['poster'],
            'poster_path'          => $formatted['poster_path'],
            'isPremium'            => $formatted['isPremium'],
            'is_premium'           => $formatted['is_premium'],
            'has_video'            => $formatted['has_video'],
            'duration'             => $formatted['duration'],
            'playbackAvailability' => $formatted['has_video'] ? 'READY' : 'UNAVAILABLE',
        ]);
    }
}
