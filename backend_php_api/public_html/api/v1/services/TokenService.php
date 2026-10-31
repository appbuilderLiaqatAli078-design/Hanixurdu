<?php
declare(strict_types=1);

namespace HaniX\Api\Services;

use PDO;

/**
 * Token Service — Issues, validates, rotates, and revokes Bearer access & refresh tokens
 * plus short-lived password reset verification tokens.
 */
final class TokenService
{
    public static function issueTokens(PDO $pdo, int $userId, string $deviceName = 'HaniX-Urdu-Android'): array
    {
        $accessToken = bin2hex(random_bytes(32));
        $refreshToken = bin2hex(random_bytes(32));

        $accessHash = hash('sha256', $accessToken);
        $refreshHash = hash('sha256', $refreshToken);

        $accessExpiresAt = gmdate('Y-m-d H:i:s', time() + 86400);       // 24 hours
        $refreshExpiresAt = gmdate('Y-m-d H:i:s', time() + 86400 * 30); // 30 days

        $stmt = $pdo->prepare('
            INSERT INTO api_auth_tokens
                (user_id, access_token_hash, refresh_token_hash, device_name, expires_at, refresh_expires_at, created_at)
            VALUES
                (:uid, :ahash, :rhash, :device, :exp, :rexp, NOW())
        ');
        $stmt->execute([
            ':uid'    => $userId,
            ':ahash'  => $accessHash,
            ':rhash'  => $refreshHash,
            ':device' => substr($deviceName, 0, 120),
            ':exp'    => $accessExpiresAt,
            ':rexp'   => $refreshExpiresAt,
        ]);

        return [
            'access_token'  => $accessToken,
            'refresh_token' => $refreshToken,
            'accessToken'   => $accessToken,
            'refreshToken'  => $refreshToken,
            'expiresIn'     => 86400,
        ];
    }

    /**
     * Generates a short-lived HMAC-signed password reset token after `secret_pin` verification (`forgot_password.php`).
     */
    public static function createPasswordResetToken(int $userId, string $email, string $currentPasswordHash): string
    {
        $expires = time() + 600; // 10 minutes
        $secret = hash('sha256', $currentPasswordHash . '|hanix_reset_secret|' . (getenv('HANIX_APP_SECRET') ?: 'hanixurdu_v1'));
        $payload = $userId . '|' . strtolower($email) . '|' . $expires;
        $sig = hash_hmac('sha256', $payload, $secret);
        return base64_encode($payload . '|' . $sig);
    }

    public static function verifyPasswordResetToken(string $token, int $userId, string $email, string $currentPasswordHash): bool
    {
        $decoded = base64_decode($token, true);
        if ($decoded === false) {
            return false;
        }
        $parts = explode('|', $decoded);
        if (count($parts) !== 4) {
            return false;
        }
        [$tokUid, $tokEmail, $tokExp, $tokSig] = $parts;
        if ((int) $tokUid !== $userId || strtolower($tokEmail) !== strtolower($email)) {
            return false;
        }
        if ((int) $tokExp < time()) {
            return false;
        }
        $secret = hash('sha256', $currentPasswordHash . '|hanix_reset_secret|' . (getenv('HANIX_APP_SECRET') ?: 'hanixurdu_v1'));
        $expectedSig = hash_hmac('sha256', $tokUid . '|' . $tokEmail . '|' . $tokExp, $secret);
        return hash_equals($expectedSig, $tokSig);
    }
}
