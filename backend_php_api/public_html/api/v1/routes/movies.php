<?php
declare(strict_types=1);

use HaniX\Api\Controllers\MovieController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && $routePath === '/movies') {
    MovieController::index($pdo);
}
if ($method === 'GET' && $routePath === '/movies/categories') {
    MovieController::categories($pdo);
}
if ($method === 'GET' && preg_match('#^/movies/([^/]+)$#', $routePath, $m) && $m[1] !== 'categories') {
    MovieController::show($pdo, $m[1]);
}
