<?php
declare(strict_types=1);

use HaniX\Api\Controllers\AuthController;
use HaniX\Api\Controllers\UserController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'POST' && $routePath === '/auth/login') {
    AuthController::login($pdo);
}
if ($method === 'POST' && $routePath === '/auth/register') {
    AuthController::register($pdo);
}
if ($method === 'POST' && $routePath === '/auth/logout') {
    AuthController::logout($pdo);
}
if ($method === 'POST' && $routePath === '/auth/refresh') {
    AuthController::refresh($pdo);
}
if ($method === 'GET' && $routePath === '/auth/me') {
    UserController::profile($pdo);
}
if ($method === 'POST' && ($routePath === '/auth/forgot-password/verify' || $routePath === '/auth/forgot-password')) {
    AuthController::verifyForgotPassword($pdo);
}
if ($method === 'POST' && $routePath === '/auth/forgot-password/reset') {
    AuthController::resetForgotPassword($pdo);
}
