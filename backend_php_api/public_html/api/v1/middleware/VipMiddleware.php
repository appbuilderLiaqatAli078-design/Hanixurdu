<?php
declare(strict_types=1);

namespace HaniX\Api\Middleware;

use HaniX\Api\Helpers\ResponseHelper;
use PDO;
use Throwable;

/**
 * VIP & Membership Middleware (Matches `check_premium.php`, `add_premium.php`, `unlock_vip.php`, and `series.php`)
 *
 * Real `premium_users` schema from uploaded `check_premium.php` & `unlock_vip.php`:
 *   CREATE TABLE IF NOT EXISTS premium_users (
 *       id INT AUTO_INCREMENT PRIMARY KEY,
 *       email VARCHAR(255) NOT NULL UNIQUE,
 *       expiry_date DATETIME NOT NULL
 *   )
 *
 * Query in `check_premium.php`:
 *   SELECT * FROM premium_users WHERE email = ? AND expiry_date > NOW()
 *
 * Also checks `users.is_premium == 1` fallback from `series.php`.
 * Strictly TWO membership plans exist: FREE and VIP.
 */
final class VipMiddleware
{
    public static function resolveMembership(PDO $pdo, int $userId, string $verifiedServerEmail = '', ?array $userRow = null): array
    {
        $email = trim($verifiedServerEmail);

        if ($email !== '') {
            try {
                // Exact query from check_premium.php using the server-authenticated user's email
                $stmt = $pdo->prepare('
                    SELECT expiry_date
                    FROM premium_users
                    WHERE LOWER(email) = LOWER(:email)
                      AND expiry_date > NOW()
                    ORDER BY expiry_date DESC
                    LIMIT 1
                ');
                $stmt->execute([':email' => $email]);
                $row = $stmt->fetch();

                if ($row && !empty($row['expiry_date'])) {
                    $expiryTimestamp = strtotime((string) $row['expiry_date']);
                    $now = time();
                    if ($expiryTimestamp !== false && $expiryTimestamp > $now) {
                        $remainingDays = (int) ceil(($expiryTimestamp - $now) / 86400);
                        return [
                            'membership'            => 'VIP',
                            'isVip'                 => true,
                            'vipExpiry'             => date('Y-m-d H:i:s', $expiryTimestamp),
                            'vipExpiryEpochSeconds' => $expiryTimestamp,
                            'remainingDays'         => $remainingDays,
                            'planName'              => 'VIP Membership',
                        ];
                    }
                }
            } catch (Throwable $ignored) {
            }
        }

        // Secondary check from series.php: `SELECT is_premium FROM users WHERE email = ? LIMIT 1`
        if ($userRow !== null && isset($userRow['is_premium']) && (int) $userRow['is_premium'] === 1) {
            return [
                'membership'            => 'VIP',
                'isVip'                 => true,
                'vipExpiry'             => null,
                'vipExpiryEpochSeconds' => null,
                'remainingDays'         => 30,
                'planName'              => 'VIP Membership',
            ];
        }

        return [
            'membership'            => 'FREE',
            'isVip'                 => false,
            'vipExpiry'             => null,
            'vipExpiryEpochSeconds' => null,
            'remainingDays'         => 0,
            'planName'              => 'Free Plan',
        ];
    }

    public static function requireActiveVip(array $authContext): void
    {
        if (empty($authContext['membership']['isVip']) || ($authContext['membership']['membership'] ?? 'FREE') !== 'VIP') {
            ResponseHelper::error(
                'Yeh content Premium (VIP) hai. Isko dekhne ke liye VIP plan activate karein.',
                403,
                'VIP_REQUIRED',
                [
                    'payment_method'   => 'JazzCash',
                    'account_name'     => 'Muhammad Hasnain',
                    'account_number'   => '03294806457',
                    'whatsapp_support' => 'https://wa.me/923160450074',
                    'whatsapp_channel' => 'https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q',
                ]
            );
        }
    }
}
