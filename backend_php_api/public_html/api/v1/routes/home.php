<?php
declare(strict_types=1);

use HaniX\Api\Controllers\HomeController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && $routePath === '/home') {
    HomeController::index($pdo);
}
if ($method === 'GET' && $routePath === '/home/trending') {
    HomeController::trending($pdo);
}
if ($method === 'GET' && $routePath === '/home/banners') {
    HomeController::banners($pdo);
}
if ($method === 'GET' && $routePath === '/home/featured') {
    HomeController::featured($pdo);
}
if ($method === 'GET' && $routePath === '/home/latest') {
    HomeController::latest($pdo);
}
if ($method === 'GET' && ($routePath === '/categories' || $routePath === '/home/categories')) {
    HomeController::categories($pdo);
}
