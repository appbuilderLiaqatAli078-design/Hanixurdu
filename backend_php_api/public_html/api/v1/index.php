<?php
declare(strict_types=1);

/**
 * HaniX Urdu — Production REST API v1 Front Controller
 * Base URL: https://hanixurdu.com/api/v1/
 *
 * Modular Architecture:
 *   config/Config.php
 *   helpers/{ResponseHelper.php, RateLimiter.php}
 *   middleware/{AuthMiddleware.php, VipMiddleware.php}
 *   models/{UserModel.php, MovieModel.php, SeriesModel.php}
 *   services/{TokenService.php, PlaybackService.php}
 *   controllers/{Auth,User,Home,Movie,Series,Search,Membership,Playback,Download,Community}Controller.php
 *   routes/{auth,user,home,movies,series,search,membership,playback,downloads,community}.php
 */

require_once __DIR__ . '/config/Config.php';
require_once __DIR__ . '/helpers/ResponseHelper.php';
require_once __DIR__ . '/helpers/RateLimiter.php';
require_once __DIR__ . '/models/UserModel.php';
require_once __DIR__ . '/models/MovieModel.php';
require_once __DIR__ . '/models/SeriesModel.php';
require_once __DIR__ . '/middleware/VipMiddleware.php';
require_once __DIR__ . '/middleware/AuthMiddleware.php';
require_once __DIR__ . '/services/TokenService.php';
require_once __DIR__ . '/services/PlaybackService.php';
require_once __DIR__ . '/controllers/AuthController.php';
require_once __DIR__ . '/controllers/UserController.php';
require_once __DIR__ . '/controllers/HomeController.php';
require_once __DIR__ . '/controllers/MovieController.php';
require_once __DIR__ . '/controllers/SeriesController.php';
require_once __DIR__ . '/controllers/SearchController.php';
require_once __DIR__ . '/controllers/MembershipController.php';
require_once __DIR__ . '/controllers/PlaybackController.php';
require_once __DIR__ . '/controllers/DownloadController.php';
require_once __DIR__ . '/controllers/CommunityController.php';

use HaniX\Api\Config\Config;
use HaniX\Api\Helpers\ResponseHelper;

ResponseHelper::sendHeaders();

$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
if ($method === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$basePrefixPos = strpos($requestUri, '/api/v1');
$routePath = $basePrefixPos !== false
    ? substr($requestUri, $basePrefixPos + strlen('/api/v1'))
    : $requestUri;
$routePath = '/' . trim($routePath, '/');
if ($routePath === '//') {
    $routePath = '/';
}

try {
    $pdo = Config::getPdo();

    // Dispatch modular route groups
    require __DIR__ . '/routes/auth.php';
    require __DIR__ . '/routes/user.php';
    require __DIR__ . '/routes/home.php';
    require __DIR__ . '/routes/movies.php';
    require __DIR__ . '/routes/series.php';
    require __DIR__ . '/routes/search.php';
    require __DIR__ . '/routes/membership.php';
    require __DIR__ . '/routes/playback.php';
    require __DIR__ . '/routes/downloads.php';
    require __DIR__ . '/routes/community.php';

    // Application Settings & Version endpoints
    if ($method === 'GET' && $routePath === '/app/settings') {
        ResponseHelper::success([
            'maintenanceMode'          => false,
            'supportEmail'             => 'hannnix88@gmail.com',
            'whatsappSupportUrl'       => 'https://wa.me/923160450074',
            'whatsappChannelUrl'       => 'https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q',
            'jazzCashAccountName'      => 'Muhammad Hasnain',
            'jazzCashAccountNumber'    => '03294806457',
            'privacyPolicyUrl'         => 'https://hanixurdu.com/help.php',
            'termsUrl'                 => 'https://hanixurdu.com/help.php',
            'bunnyStreamEnabled'       => Config::bunnyCdnHostname() !== '',
            'downloadsEnabled'         => true,
            'vipSubscriptionsEnabled'  => true,
        ]);
    }

    if ($method === 'GET' && $routePath === '/app/version') {
        ResponseHelper::success([
            'latestVersionCode' => 1,
            'latestVersionName' => '1.0.0',
            'forceUpdate'       => false,
            'changelog'         => 'HaniX Urdu Official Native Android Release (Real PHP API v1 + FREE/VIP Membership + Mind Games + Dubbing Studio).',
        ]);
    }

    ResponseHelper::error('Endpoint not found: ' . $method . ' ' . $routePath, 404, 'ROUTE_NOT_FOUND');
} catch (Throwable $e) {
    ResponseHelper::error(
        'Server error while processing the API request.',
        500,
        'INTERNAL_SERVER_ERROR'
    );
}
