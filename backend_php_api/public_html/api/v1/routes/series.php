<?php
declare(strict_types=1);

use HaniX\Api\Controllers\SeriesController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && $routePath === '/series') {
    SeriesController::index($pdo);
}
if ($method === 'GET' && preg_match('#^/series/([^/]+)/seasons$#', $routePath, $m)) {
    SeriesController::seasons($pdo, $m[1]);
}
if ($method === 'GET' && preg_match('#^/series/([^/]+)/episodes$#', $routePath, $m)) {
    SeriesController::episodesBySeries($pdo, $m[1]);
}
if ($method === 'GET' && preg_match('#^/series/([^/]+)$#', $routePath, $m)) {
    SeriesController::show($pdo, $m[1]);
}
if ($method === 'GET' && preg_match('#^/seasons/([^/]+)/episodes$#', $routePath, $m)) {
    SeriesController::episodesBySeason($pdo, $m[1]);
}
if ($method === 'GET' && preg_match('#^/episodes/([^/]+)$#', $routePath, $m)) {
    SeriesController::episodeDetail($pdo, $m[1]);
}
