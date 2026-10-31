<?php
declare(strict_types=1);

use HaniX\Api\Controllers\MembershipController;

/** @var string $method */
/** @var string $routePath */
/** @var PDO $pdo */

if ($method === 'GET' && ($routePath === '/membership/status' || $routePath === '/vip/status')) {
    MembershipController::status($pdo);
}
if ($method === 'GET' && ($routePath === '/membership/plans' || $routePath === '/vip/plans')) {
    MembershipController::plans();
}
