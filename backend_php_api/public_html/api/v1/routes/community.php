<?php
declare(strict_types=1);

use HaniX\Api\Controllers\CommunityController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && ($routePath === '/mind-game/questions' || $routePath === '/mind-game')) {
    CommunityController::mindGameQuestions();
}

if ($method === 'POST' && ($routePath === '/studio/audition' || $routePath === '/studio/upload')) {
    CommunityController::submitAudition($pdo);
}

if ($method === 'GET' && ($routePath === '/updates' || $routePath === '/help')) {
    CommunityController::updates();
}
