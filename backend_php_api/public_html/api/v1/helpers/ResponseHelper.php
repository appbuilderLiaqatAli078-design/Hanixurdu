<?php
declare(strict_types=1);

namespace HaniX\Api\Helpers;

/**
 * Centralized JSON Response & Input Validation Helper
 * Never returns HTML from any API endpoint.
 */
final class ResponseHelper
{
    public static function sendHeaders(): void
    {
        header('Content-Type: application/json; charset=utf-8');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: no-referrer');
        header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');

        $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
        if ($origin === 'https://hanixurdu.com' || $origin === 'https://www.hanixurdu.com') {
            header('Access-Control-Allow-Origin: ' . $origin);
            header('Vary: Origin');
        } else {
            header('Access-Control-Allow-Origin: https://hanixurdu.com');
        }
        header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-Client-Platform');
    }

    public static function success(mixed $data = null, string $message = 'Success', int $statusCode = 200): never
    {
        http_response_code($statusCode);
        echo json_encode([
            'success' => true,
            'message' => $message,
            'data'    => $data,
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function error(
        string $message,
        int $statusCode = 400,
        string $errorCode = 'API_ERROR',
        ?array $details = null
    ): never {
        http_response_code($statusCode);
        $payload = [
            'success'    => false,
            'message'    => $message,
            'error_code' => $errorCode,
            'data'       => null,
        ];
        if ($details !== null) {
            $payload['details'] = $details;
        }
        echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function jsonBody(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === false || trim($raw) === '') {
            return $_POST ?: [];
        }
        $decoded = json_decode($raw, true);
        if (!is_array($decoded)) {
            self::error('Invalid JSON request payload.', 400, 'INVALID_JSON');
        }
        return $decoded;
    }

    public static function paginationParams(int $defaultLimit = 20, int $maxLimit = 50): array
    {
        $page = max(1, (int) ($_GET['page'] ?? 1));
        $limit = (int) ($_GET['limit'] ?? $_GET['per_page'] ?? $defaultLimit);
        $limit = max(1, min($maxLimit, $limit));
        $offset = ($page - 1) * $limit;

        return [
            'page'   => $page,
            'limit'  => $limit,
            'offset' => $offset,
        ];
    }
}
