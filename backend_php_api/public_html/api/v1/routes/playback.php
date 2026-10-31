<?php
declare(strict_types=1);

use HaniX\Api\Controllers\PlaybackController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'POST' && $routePath === '/playback/authorize') {
    PlaybackController::authorize($pdo);
}
if ($method === 'POST' && $routePath === '/playback/progress') {
    PlaybackController::progress($pdo);
}
if ($method === 'POST' && $routePath === '/playback/complete') {
    PlaybackController::complete($pdo);
}
