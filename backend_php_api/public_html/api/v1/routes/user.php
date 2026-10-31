<?php
declare(strict_types=1);

use HaniX\Api\Controllers\UserController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && $routePath === '/user/profile') {
    UserController::profile($pdo);
}
if ($method === 'PUT' && $routePath === '/user/profile') {
    UserController::updateProfile($pdo);
}
if ($method === 'GET' && $routePath === '/user/history') {
    UserController::history($pdo);
}
if ($method === 'DELETE' && $routePath === '/user/history') {
    UserController::clearHistory($pdo);
}
