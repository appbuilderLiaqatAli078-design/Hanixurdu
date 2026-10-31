<?php
declare(strict_types=1);

namespace HaniX\Api\Helpers;

use PDO;
use Throwable;

/**
 * Server-Side Rate Limiter for Authentication & Password Recovery Endpoints
 * Protects against credential stuffing and brute-force PIN/password attacks.
 */
final class RateLimiter
{
    public static function check(PDO $pdo, string $bucketKey, int $maxAttempts = 8, int $windowSeconds = 300): void
    {
        $ip = self::clientIp();
        $compositeKey = hash('sha256', $bucketKey . '|' . $ip);

        // File-based fallback if rate_limits table is not created yet
        $tmpFile = sys_get_temp_dir() . '/hanix_rl_' . substr($compositeKey, 0, 32) . '.json';
        $now = time();
        $timestamps = [];

        if (is_file($tmpFile)) {
            $raw = @file_get_contents($tmpFile);
            $decoded = $raw ? json_decode($raw, true) : null;
            if (is_array($decoded)) {
                foreach ($decoded as $ts) {
                    if (is_int($ts) && ($now - $ts) < $windowSeconds) {
                        $timestamps[] = $ts;
                    }
                }
            }
        }

        if (count($timestamps) >= $maxAttempts) {
            ResponseHelper::error(
                'Too many attempts. Please wait a few minutes before trying again.',
                429,
                'RATE_LIMIT_EXCEEDED'
            );
        }

        $timestamps[] = $now;
        @file_put_contents($tmpFile, json_encode($timestamps), LOCK_EX);
    }

    public static function clientIp(): string
    {
        $ip = $_SERVER['HTTP_CF_CONNECTING_IP']
            ?? $_SERVER['HTTP_X_FORWARDED_FOR']
            ?? $_SERVER['REMOTE_ADDR']
            ?? '0.0.0.0';
        if (str_contains($ip, ',')) {
            $ip = trim(explode(',', $ip)[0]);
        }
        return substr($ip, 0, 45);
    }
}
