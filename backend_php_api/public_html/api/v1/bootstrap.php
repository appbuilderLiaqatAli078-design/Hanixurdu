<?php
/**
 * HaniX Urdu — Security, Authentication & Membership Bootstrap (`public_html/api/v1/bootstrap.php`)
 * Enforces:
 * - FREE and VIP membership model only (checks `premium_users.expiry_date`).
 * - Expired VIP accounts immediately lose VIP privileges on the very next check.
 * - Server-side Bearer token resolution (never trusts client-supplied user IDs).
 */

declare(strict_types=1);

ini_set('display_errors', '0');
error_reporting(E_ALL);

require_once __DIR__ . '/response.php';
require_once __DIR__ . '/database.php';

/**
 * Extracts Bearer token from Authorization header.
 */
function hanix_get_bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? '';
    if (preg_match('/^Bearer\s+([A-Za-z0-9\-._~+\/]+=*)$/i', trim($header), $matches)) {
        return $matches[1];
    }
    return null;
}

/**
 * Queries `premium_users` to determine if the given user email has an active, non-expired VIP membership.
 * Returns:
 * [
 *   'membership' => 'FREE' | 'VIP',
 *   'vipStatus'  => bool,
 *   'vipExpiry'  => ?string,
 *   'vipExpiryEpochSeconds' => ?int
 * ]
 */
function hanix_resolve_membership(PDO $pdo, string $email): array
{
    try {
        $stmt = $pdo->prepare(
            'SELECT expiry_date FROM premium_users WHERE email = :email ORDER BY expiry_date DESC LIMIT 1'
        );
        $stmt->execute([':email' => $email]);
        $row = $stmt->fetch();

        if (!$row || empty($row['expiry_date'])) {
            return [
                'membership'            => 'FREE',
                'vipStatus'             => false,
                'vipExpiry'             => null,
                'vipExpiryEpochSeconds' => null,
            ];
        }

        $expiryStr = (string)$row['expiry_date'];
        $expiryEpoch = strtotime($expiryStr);
        if ($expiryEpoch !== false && strlen(trim($expiryStr)) === 10) {
            // Date-only format YYYY-MM-DD: valid until 23:59:59 UTC of that date
            $expiryEpoch += 86399;
        }

        $now = time();
        if ($expiryEpoch !== false && $expiryEpoch > $now) {
            return [
                'membership'            => 'VIP',
                'vipStatus'             => true,
                'vipExpiry'             => $expiryStr,
                'vipExpiryEpochSeconds' => $expiryEpoch,
            ];
        }
    } catch (Throwable $e) {
        error_log('hanix_resolve_membership error: ' . $e->getMessage());
    }

    return [
        'membership'            => 'FREE',
        'vipStatus'             => false,
        'vipExpiry'             => null,
        'vipExpiryEpochSeconds' => null,
    ];
}

/**
 * Resolves the authenticated user strictly from the server-side `api_auth_tokens` table.
 * Never trusts client-provided user IDs or membership flags.
 */
function hanix_require_auth(PDO $pdo): array
{
    $rawToken = hanix_get_bearer_token();
    if ($rawToken === null || $rawToken === '') {
        hanix_send_json(false, 'Authentication required', null, 401);
    }

    $tokenHash = hash('sha256', $rawToken);
    $stmt = $pdo->prepare(
        'SELECT user_email, access_expires_at, revoked
         FROM api_auth_tokens
         WHERE access_token_hash = :hash
         LIMIT 1'
    );
    $stmt->execute([':hash' => $tokenHash]);
    $tokenRow = $stmt->fetch();

    if (!$tokenRow || (int)$tokenRow['revoked'] === 1) {
        hanix_send_json(false, 'Invalid or revoked session token', null, 401);
    }

    if (strtotime((string)$tokenRow['access_expires_at']) <= time()) {
        hanix_send_json(false, 'Access token expired', null, 401);
    }

    $email = (string)$tokenRow['user_email'];
    $userStmt = $pdo->prepare('SELECT * FROM users WHERE email = :email LIMIT 1');
    $userStmt->execute([':email' => $email]);
    $userRow = $userStmt->fetch();

    if (!$userRow) {
        hanix_send_json(false, 'User account not found', null, 401);
    }

    $membership = hanix_resolve_membership($pdo, $email);
    $userId = (string)($userRow['id'] ?? $email);
    $displayName = (string)($userRow['name'] ?? explode('@', $email)[0]);

    return [
        'id'                    => $userId,
        'name'                  => $displayName,
        'email'                 => $email,
        'profileImage'          => $userRow['profile_image'] ?? null,
        'membership'            => $membership['membership'], // Strictly 'FREE' or 'VIP'
        'vipStatus'             => $membership['vipStatus'],
        'vipExpiry'             => $membership['vipExpiry'],
        'vipExpiryEpochSeconds' => $membership['vipExpiryEpochSeconds'],
    ];
}

/**
 * Verifies user password against existing `users.password` column.
 * Supports PHP `password_hash()` (`password_verify()`) and legacy website plaintext/MD5 if present.
 */
function hanix_verify_existing_password(string $inputPassword, string $storedHash): bool
{
    if ($storedHash === '') {
        return false;
    }
    $info = password_get_info($storedHash);
    if (!empty($info['algo'])) {
        return password_verify($inputPassword, $storedHash);
    }
    // Compatibility fallback if legacy website stored MD5 or direct string in older records
    if (strlen($storedHash) === 32 && ctype_xdigit($storedHash)) {
        return hash_equals(strtolower($storedHash), md5($inputPassword));
    }
    return hash_equals($storedHash, $inputPassword);
}
