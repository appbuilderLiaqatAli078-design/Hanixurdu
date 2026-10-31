<?php
declare(strict_types=1);

namespace HaniX\Api\Models;

use PDO;
use Throwable;

/**
 * User Model (Matches real `signup.php`, `login_process.php`, `forgot_password.php`)
 *
 * Real `users` table in `signup.php`:
 *   INSERT INTO users (email, password, secret_pin) VALUES (?, ?, ?)
 *
 * Never exposes `password` or `secret_pin` in API output.
 */
final class UserModel
{
    public static function findById(PDO $pdo, int $userId): ?array
    {
        $stmt = $pdo->prepare('SELECT * FROM users WHERE id = :id LIMIT 1');
        $stmt->execute([':id' => $userId]);
        $row = $stmt->fetch();
        return is_array($row) ? $row : null;
    }

    public static function findByEmail(PDO $pdo, string $email): ?array
    {
        $stmt = $pdo->prepare('SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1');
        $stmt->execute([':email' => trim($email)]);
        $row = $stmt->fetch();
        return is_array($row) ? $row : null;
    }

    /**
     * Preserves `password_verify($password, $row['password'])` from `login_process.php`.
     */
    public static function verifyPassword(PDO $pdo, array $userRow, string $plainPassword): bool
    {
        $storedHash = (string) ($userRow['password'] ?? '');
        if ($storedHash === '' || $plainPassword === '') {
            return false;
        }

        if (password_verify($plainPassword, $storedHash)) {
            return true;
        }

        // Legacy fallback if any older account stored plain/MD5 password
        if (hash_equals($storedHash, $plainPassword) || hash_equals($storedHash, md5($plainPassword))) {
            $upgradedHash = password_hash($plainPassword, PASSWORD_DEFAULT);
            $stmt = $pdo->prepare('UPDATE users SET password = :hash WHERE id = :id');
            $stmt->execute([':hash' => $upgradedHash, ':id' => (int) $userRow['id']]);
            return true;
        }

        return false;
    }

    /**
     * Verifies `secret_pin` from `forgot_password.php`:
     * Supports both plain-text `$secret_pin === $row['secret_pin']` (so website `forgot_password.php` keeps working)
     * and hashed `secret_pin` if upgraded.
     */
    public static function verifySecretPin(array $userRow, string $submittedPin): bool
    {
        $storedPin = trim((string) ($userRow['secret_pin'] ?? ''));
        $submitted = trim($submittedPin);
        if ($storedPin === '' || $submitted === '') {
            return false;
        }

        if (str_starts_with($storedPin, '$2y$') || str_starts_with($storedPin, '$2a$')) {
            return password_verify($submitted, $storedPin);
        }

        return hash_equals($storedPin, $submitted);
    }

    public static function sanitize(array $userRow, array $membership): array
    {
        $id = (int) ($userRow['id'] ?? 0);
        $email = (string) ($userRow['email'] ?? '');
        $emailPrefix = $email !== '' ? explode('@', $email)[0] : ('User #' . $id);
        $name = (string) ($userRow['username'] ?? $userRow['name'] ?? $emailPrefix);
        $avatar = (string) ($userRow['profile_image'] ?? $userRow['avatar'] ?? '');

        return [
            'id'                    => (string) $id,
            'user_id'               => $id,
            'name'                  => $name,
            'username'              => $name,
            'email'                 => $email,
            'profileImage'          => $avatar,
            'profile_image'         => $avatar,
            'membership'            => $membership['membership'], // Strictly 'FREE' or 'VIP'
            'vipStatus'             => (bool) $membership['isVip'],
            'is_vip'                => (bool) $membership['isVip'],
            'vipExpiry'             => $membership['vipExpiry'],
            'expiry_date'           => $membership['vipExpiry'],
            'vipExpiryEpochSeconds' => $membership['vipExpiryEpochSeconds'] ?? null,
        ];
    }
}
