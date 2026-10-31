<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Models\MovieModel;
use HaniX\Api\Models\SeriesModel;
use PDO;

/**
 * Search Controller (`search.php`)
 *
 * Existing Database Logic Preserved:
 * - Searches `title`, `description`, and `series_name` across `movies` and `series`.
 * - Uses prepared SQL statements.
 * - Supports pagination (`page`, `limit`).
 * - Correctly calculates `has_video` and `is_premium` on every returned movie/episode.
 */
final class SearchController
{
    public static function search(PDO $pdo): never
    {
        $query = trim((string) ($_GET['q'] ?? ''));
        $pg = ResponseHelper::paginationParams(20, 50);

        if ($query === '') {
            ResponseHelper::success([
                'query'  => '',
                'page'   => $pg['page'],
                'limit'  => $pg['limit'],
                'movies' => [],
                'series' => [],
            ]);
        }

        $like = '%' . $query . '%';

        $mStmt = $pdo->prepare('
            SELECT *
            FROM movies
            WHERE title LIKE :q1
               OR description LIKE :q2
               OR series_name LIKE :q3
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        ');
        $mStmt->bindValue(':q1', $like, PDO::PARAM_STR);
        $mStmt->bindValue(':q2', $like, PDO::PARAM_STR);
        $mStmt->bindValue(':q3', $like, PDO::PARAM_STR);
        $mStmt->bindValue(':limit', $pg['limit'], PDO::PARAM_INT);
        $mStmt->bindValue(':offset', $pg['offset'], PDO::PARAM_INT);
        $mStmt->execute();
        $movies = array_map([MovieModel::class, 'format'], $mStmt->fetchAll());

        $sStmt = $pdo->prepare('
            SELECT *
            FROM series
            WHERE series_name LIKE :q1
               OR index_title LIKE :q2
               OR description LIKE :q3
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        ');
        $sStmt->bindValue(':q1', $like, PDO::PARAM_STR);
        $sStmt->bindValue(':q2', $like, PDO::PARAM_STR);
        $sStmt->bindValue(':q3', $like, PDO::PARAM_STR);
        $sStmt->bindValue(':limit', $pg['limit'], PDO::PARAM_INT);
        $sStmt->bindValue(':offset', $pg['offset'], PDO::PARAM_INT);
        $sStmt->execute();
        $series = array_map([SeriesModel::class, 'formatSeries'], $sStmt->fetchAll());

        ResponseHelper::success([
            'query'  => $query,
            'page'   => $pg['page'],
            'limit'  => $pg['limit'],
            'movies' => $movies,
            'series' => $series,
        ]);
    }
}
