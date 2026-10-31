<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Models\MovieModel;
use PDO;

/**
 * Movie Controller (`GET /movies`, `GET /movies/{id}`, `GET /movies/categories`)
 */
final class MovieController
{
    public static function index(PDO $pdo): never
    {
        $pg = ResponseHelper::paginationParams(20, 50);
        $category = trim((string) ($_GET['category'] ?? ''));
        $movies = MovieModel::getMovies($pdo, $pg['limit'], $pg['offset'], $category);
        ResponseHelper::success($movies);
    }

    public static function show(PDO $pdo, string $idOrSlug): never
    {
        $row = MovieModel::findByIdOrSlug($pdo, $idOrSlug);
        if (!$row) {
            ResponseHelper::error('Movie not found.', 404, 'MOVIE_NOT_FOUND');
        }
        ResponseHelper::success(MovieModel::format($row));
    }

    public static function categories(PDO $pdo): never
    {
        ResponseHelper::success(HomeController::extractCategories($pdo));
    }
}
