<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\RateLimiter;
use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use HaniX\Api\Middleware\VipMiddleware;
use HaniX\Api\Models\UserModel;
use HaniX\Api\Services\TokenService;
use PDO;
use Throwable;

/**
 * Authentication Controller (`login_process.php`, `signup.php`, `logout.php`, `forgot_password.php`)
 */
final class AuthController
{
    public static function login(PDO $pdo): never
    {
        RateLimiter::check($pdo, 'auth_login', 10, 300);
        $body = ResponseHelper::jsonBody();
        $email = trim((string) ($body['email'] ?? ''));
        $password = trim((string) ($body['password'] ?? ''));
        $deviceName = trim((string) ($body['deviceName'] ?? $body['device_name'] ?? 'HaniX-Urdu-Android'));

        if ($email === '' || $password === '') {
            ResponseHelper::error('Please fill in all fields', 422, 'VALIDATION_ERROR');
        }

        $userRow = UserModel::findByEmail($pdo, $email);
        if (!$userRow) {
            ResponseHelper::error('Account not found. Please Sign Up first.', 401, 'ACCOUNT_NOT_FOUND');
        }

        if (!UserModel::verifyPassword($pdo, $userRow, $password)) {
            ResponseHelper::error('Incorrect password', 401, 'INVALID_CREDENTIALS');
        }

        $userId = (int) $userRow['id'];
        $tokens = TokenService::issueTokens($pdo, $userId, $deviceName);
        $membership = VipMiddleware::resolveMembership($pdo, $userId, (string) ($userRow['email'] ?? $email), $userRow);
        $sanitizedUser = UserModel::sanitize($userRow, $membership);

        ResponseHelper::success([
            'user'          => $sanitizedUser,
            'access_token'  => $tokens['access_token'],
            'refresh_token' => $tokens['refresh_token'],
            'accessToken'   => $tokens['accessToken'],
            'refreshToken'  => $tokens['refreshToken'],
            'expiresIn'     => $tokens['expiresIn'],
        ], 'Login successful');
    }

    /**
     * Matches `signup.php`:
     *   INSERT INTO users (email, password, secret_pin) VALUES (?, ?, ?)
     */
    public static function register(PDO $pdo): never
    {
        RateLimiter::check($pdo, 'auth_register', 6, 300);
        $body = ResponseHelper::jsonBody();
        $name = trim((string) ($body['name'] ?? $body['username'] ?? ''));
        $email = trim((string) ($body['email'] ?? ''));
        $password = trim((string) ($body['password'] ?? ''));
        $secretPin = trim((string) ($body['secret_pin'] ?? $body['secretPin'] ?? '1234'));
        $deviceName = trim((string) ($body['deviceName'] ?? 'HaniX-Urdu-Android'));

        if ($email === '' || $password === '' || $secretPin === '') {
            ResponseHelper::error(
                'Tamam fields (Email, Password, Secret PIN) pur karna lazmi hain.',
                422,
                'VALIDATION_ERROR'
            );
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            ResponseHelper::error('Please enter a valid email address.', 422, 'INVALID_EMAIL');
        }

        if (UserModel::findByEmail($pdo, $email) !== null) {
            ResponseHelper::error(
                'Yeh email pehle se mojood hai. Barah-e-karam koi aur email chunein.',
                409,
                'EMAIL_EXISTS'
            );
        }

        $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

        // Exact query from signup.php: INSERT INTO users (email, password, secret_pin) VALUES (?, ?, ?)
        $stmt = $pdo->prepare('INSERT INTO users (email, password, secret_pin) VALUES (:email, :password, :pin)');
        $stmt->execute([
            ':email'    => $email,
            ':password' => $hashedPassword,
            ':pin'      => $secretPin,
        ]);

        $userId = (int) $pdo->lastInsertId();
        $tokens = TokenService::issueTokens($pdo, $userId, $deviceName);

        // Check if this email was already pre-activated in `premium_users`, otherwise defaults to FREE
        $membership = VipMiddleware::resolveMembership($pdo, $userId, $email);

        $userRow = [
            'id'       => $userId,
            'username' => $name !== '' ? $name : explode('@', $email)[0],
            'email'    => $email,
        ];

        ResponseHelper::success([
            'user'          => UserModel::sanitize($userRow, $membership),
            'access_token'  => $tokens['access_token'],
            'refresh_token' => $tokens['refresh_token'],
            'accessToken'   => $tokens['accessToken'],
            'refreshToken'  => $tokens['refreshToken'],
            'expiresIn'     => $tokens['expiresIn'],
        ], 'Aapka account kamyabi se ban gaya hai! Aap successfully sign up ho chuke hain.', 201);
    }

    public static function logout(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $stmt = $pdo->prepare('UPDATE api_auth_tokens SET revoked_at = NOW() WHERE id = :id');
        $stmt->execute([':id' => $auth['tokenId']]);
        ResponseHelper::success(null, 'Logged out successfully!');
    }

    public static function refresh(PDO $pdo): never
    {
        $body = ResponseHelper::jsonBody();
        $refreshToken = trim((string) ($body['refreshToken'] ?? $body['refresh_token'] ?? ''));
        if ($refreshToken === '') {
            ResponseHelper::error('Refresh token is required.', 400, 'MISSING_REFRESH_TOKEN');
        }

        $refreshHash = hash('sha256', $refreshToken);
        $stmt = $pdo->prepare('
            SELECT id, user_id, device_name, refresh_expires_at, revoked_at
            FROM api_auth_tokens
            WHERE refresh_token_hash = :hash
            LIMIT 1
        ');
        $stmt->execute([':hash' => $refreshHash]);
        $tokenRow = $stmt->fetch();

        if (!$tokenRow || !empty($tokenRow['revoked_at']) || strtotime((string) $tokenRow['refresh_expires_at']) <= time()) {
            ResponseHelper::error('Refresh token is invalid or expired.', 401, 'INVALID_REFRESH_TOKEN');
        }

        $revokeStmt = $pdo->prepare('UPDATE api_auth_tokens SET revoked_at = NOW() WHERE id = :id');
        $revokeStmt->execute([':id' => (int) $tokenRow['id']]);

        $userId = (int) $tokenRow['user_id'];
        $userRow = UserModel::findById($pdo, $userId);
        if (!$userRow) {
            ResponseHelper::error('User account not found.', 401, 'USER_NOT_FOUND');
        }

        $tokens = TokenService::issueTokens($pdo, $userId, (string) ($tokenRow['device_name'] ?? 'HaniX-Urdu-Android'));
        $membership = VipMiddleware::resolveMembership($pdo, $userId, (string) ($userRow['email'] ?? ''), $userRow);

        ResponseHelper::success([
            'user'          => UserModel::sanitize($userRow, $membership),
            'access_token'  => $tokens['access_token'],
            'refresh_token' => $tokens['refresh_token'],
            'accessToken'   => $tokens['accessToken'],
            'refreshToken'  => $tokens['refreshToken'],
            'expiresIn'     => $tokens['expiresIn'],
        ], 'Token refreshed');
    }

    /**
     * Step 1 of `forgot_password.php`:
     * Verifies `email` and `secret_pin` (`SELECT id, secret_pin FROM users WHERE email = ?`).
     */
    public static function verifyForgotPassword(PDO $pdo): never
    {
        RateLimiter::check($pdo, 'forgot_verify', 6, 300);
        $body = ResponseHelper::jsonBody();
        $email = trim((string) ($body['email'] ?? ''));
        $secretPin = trim((string) ($body['secret_pin'] ?? $body['secretPin'] ?? ''));

        if ($email === '' || $secretPin === '') {
            ResponseHelper::error('Tamam fields pur karna lazmi hain.', 422, 'VALIDATION_ERROR');
        }

        $userRow = UserModel::findByEmail($pdo, $email);
        if (!$userRow) {
            ResponseHelper::error('Yeh email system mein mojood nahi hai.', 404, 'EMAIL_NOT_FOUND');
        }

        if (!UserModel::verifySecretPin($userRow, $secretPin)) {
            ResponseHelper::error('Ghalat Secret PIN darj kiya gaya hai.', 401, 'INVALID_RECOVERY_PIN');
        }

        $userId = (int) $userRow['id'];
        $currentPasswordHash = (string) ($userRow['password'] ?? '');
        $resetToken = TokenService::createPasswordResetToken($userId, $email, $currentPasswordHash);

        ResponseHelper::success([
            'verified'    => true,
            'reset_token' => $resetToken,
            'resetToken'  => $resetToken,
        ], 'Secret PIN verified! Ab naya password banayein.');
    }

    /**
     * Step 2 of `forgot_password.php`:
     * Verifies `strlen($new_password) >= 8` and updates `UPDATE users SET password = ? WHERE email = ?`.
     */
    public static function resetForgotPassword(PDO $pdo): never
    {
        RateLimiter::check($pdo, 'forgot_reset', 6, 300);
        $body = ResponseHelper::jsonBody();
        $email = trim((string) ($body['email'] ?? ''));
        $newPassword = (string) ($body['new_password'] ?? $body['newPassword'] ?? '');
        $confirmPassword = (string) ($body['confirm_password'] ?? $body['confirmPassword'] ?? $newPassword);
        $resetToken = trim((string) ($body['reset_token'] ?? $body['resetToken'] ?? ''));
        $secretPin = trim((string) ($body['secret_pin'] ?? $body['secretPin'] ?? ''));

        if (strlen($newPassword) < 8) {
            ResponseHelper::error('Password kam az kam 8 characters ka hona chahiye.', 422, 'PASSWORD_TOO_SHORT');
        }

        if ($newPassword !== $confirmPassword) {
            ResponseHelper::error('New password aur confirm password match nahi ho rahe.', 422, 'PASSWORD_MISMATCH');
        }

        $userRow = UserModel::findByEmail($pdo, $email);
        if (!$userRow) {
            ResponseHelper::error('Yeh email system mein mojood nahi hai.', 404, 'EMAIL_NOT_FOUND');
        }

        $userId = (int) $userRow['id'];
        $currentPasswordHash = (string) ($userRow['password'] ?? '');
        $authorized = false;

        if ($resetToken !== '') {
            $authorized = TokenService::verifyPasswordResetToken($resetToken, $userId, $email, $currentPasswordHash);
        } elseif ($secretPin !== '') {
            $authorized = UserModel::verifySecretPin($userRow, $secretPin);
        }

        if (!$authorized) {
            ResponseHelper::error('Ghalat Secret PIN ya expired reset session.', 401, 'RESET_UNAUTHORIZED');
        }

        $hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare('UPDATE users SET password = :hash WHERE email = :email');
        $stmt->execute([':hash' => $hashedPassword, ':email' => $userRow['email']]);

        try {
            $rStmt = $pdo->prepare('UPDATE api_auth_tokens SET revoked_at = NOW() WHERE user_id = :uid AND revoked_at IS NULL');
            $rStmt->execute([':uid' => $userId]);
        } catch (Throwable $ignored) {
        }

        ResponseHelper::success(null, 'Password successfully reset ho gaya hai!');
    }
}
