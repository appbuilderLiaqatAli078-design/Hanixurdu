<?php
declare(strict_types=1);

use HaniX\Api\Controllers\SearchController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && $routePath === '/search') {
    SearchController::search($pdo);
}
