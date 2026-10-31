<?php
declare(strict_types=1);

use HaniX\Api\Controllers\DownloadController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'POST' && $routePath === '/downloads/authorize') {
    DownloadController::authorize($pdo);
}
if ($method === 'GET' && $routePath === '/downloads') {
    DownloadController::index($pdo);
}
if ($method === 'DELETE' && preg_match('#^/downloads/([^/]+)$#', $routePath, $m)) {
    DownloadController::destroy($pdo, $m[1]);
}
