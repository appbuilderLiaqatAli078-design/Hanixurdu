<?php
declare(strict_types=1);

namespace HaniX\Api\Middleware;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Models\UserModel;
use PDO;

/**
 * Bearer Token Authentication Middleware
 * Never trusts client-supplied user_id or email for authentication.
 */
final class AuthMiddleware
{
    public static function extractBearerToken(): ?string
    {
        $headers = [];
        if (function_exists('getallheaders')) {
            $headers = getallheaders() ?: [];
        }
        $authHeader = $headers['Authorization']
            ?? $headers['authorization']
            ?? ($_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? ''));

        if (preg_match('/^\s*Bearer\s+([A-Za-z0-9._~+\/-]+=*)\s*$/i', (string) $authHeader, $m)) {
            return $m[1];
        }
        return null;
    }

    /**
     * Requires a valid, unrevoked, non-expired Bearer access token.
     */
    public static function requireAuth(PDO $pdo): array
    {
        $rawToken = self::extractBearerToken();
        if ($rawToken === null || $rawToken === '') {
            ResponseHelper::error('Authentication required. Please log in.', 401, 'UNAUTHORIZED');
        }

        $tokenHash = hash('sha256', $rawToken);
        $stmt = $pdo->prepare('
            SELECT id, user_id, expires_at, revoked_at
            FROM api_auth_tokens
            WHERE access_token_hash = :hash
            LIMIT 1
        ');
        $stmt->execute([':hash' => $tokenHash]);
        $tokenRow = $stmt->fetch();

        if (!$tokenRow || !empty($tokenRow['revoked_at'])) {
            ResponseHelper::error('Invalid or revoked access token.', 401, 'INVALID_TOKEN');
        }

        if (strtotime((string) $tokenRow['expires_at']) <= time()) {
            ResponseHelper::error('Access token has expired.', 401, 'TOKEN_EXPIRED');
        }

        $userId = (int) $tokenRow['user_id'];
        $userRow = UserModel::findById($pdo, $userId);
        if (!$userRow) {
            ResponseHelper::error('Authenticated user account no longer exists.', 401, 'USER_NOT_FOUND');
        }

        $membership = VipMiddleware::resolveMembership($pdo, $userId, (string) ($userRow['email'] ?? ''), $userRow);

        return [
            'tokenId'    => (int) $tokenRow['id'],
            'userId'     => $userId,
            'userRow'    => $userRow,
            'membership' => $membership,
            'user'       => UserModel::sanitize($userRow, $membership),
        ];
    }

    /**
     * Optional authentication (for /home or /movies personalization).
     */
    public static function optionalAuth(PDO $pdo): ?array
    {
        $rawToken = self::extractBearerToken();
        if ($rawToken === null || $rawToken === '') {
            return null;
        }
        $tokenHash = hash('sha256', $rawToken);
        $stmt = $pdo->prepare('
            SELECT id, user_id, expires_at, revoked_at
            FROM api_auth_tokens
            WHERE access_token_hash = :hash
            LIMIT 1
        ');
        $stmt->execute([':hash' => $tokenHash]);
        $tokenRow = $stmt->fetch();
        if (!$tokenRow || !empty($tokenRow['revoked_at']) || strtotime((string) $tokenRow['expires_at']) <= time()) {
            return null;
        }
        $userId = (int) $tokenRow['user_id'];
        $userRow = UserModel::findById($pdo, $userId);
        if (!$userRow) {
            return null;
        }
        $membership = VipMiddleware::resolveMembership($pdo, $userId, (string) ($userRow['email'] ?? ''), $userRow);
        return [
            'tokenId'    => (int) $tokenRow['id'],
            'userId'     => $userId,
            'userRow'    => $userRow,
            'membership' => $membership,
            'user'       => UserModel::sanitize($userRow, $membership),
        ];
    }
}
