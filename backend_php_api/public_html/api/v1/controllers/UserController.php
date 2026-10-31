<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Config\Config;
use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use HaniX\Api\Models\UserModel;
use PDO;

/**
 * User Profile & Watch History Controller
 */
final class UserController
{
    public static function profile(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        ResponseHelper::success($auth['user'], 'User profile loaded');
    }

    public static function updateProfile(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $body = ResponseHelper::jsonBody();
        $name = trim((string) ($body['name'] ?? $body['username'] ?? ''));

        if ($name === '') {
            ResponseHelper::error('Name is required.', 422, 'VALIDATION_ERROR');
        }

        $stmt = $pdo->prepare('UPDATE users SET username = :name WHERE id = :id');
        $stmt->execute([':name' => $name, ':id' => $auth['userId']]);

        $updatedRow = UserModel::findById($pdo, $auth['userId']) ?: $auth['userRow'];
        $updatedRow['username'] = $name;

        ResponseHelper::success(UserModel::sanitize($updatedRow, $auth['membership']), 'Profile updated');
    }

    public static function history(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $stmt = $pdo->prepare('SELECT * FROM watch_history WHERE user_id = :uid ORDER BY updated_at DESC LIMIT 50');
        $stmt->execute([':uid' => $auth['userId']]);

        $items = [];
        foreach ($stmt->fetchAll() as $row) {
            $items[] = [
                'id'               => (string) $row['id'],
                'contentId'        => (string) $row['content_id'],
                'contentType'      => strtolower((string) ($row['content_type'] ?? 'movie')),
                'title'            => (string) ($row['title'] ?? ''),
                'poster'           => Config::assetUrl((string) ($row['poster_url'] ?? '')),
                'positionMs'       => ((int) ($row['position_seconds'] ?? 0)) * 1000,
                'durationMs'       => max(1000, ((int) ($row['duration_seconds'] ?? 1)) * 1000),
                'updatedAtIso'     => (string) ($row['updated_at'] ?? ''),
                'isCompleted'      => ((int) ($row['completed'] ?? 0)) === 1,
            ];
        }
        ResponseHelper::success($items, 'Watch history loaded');
    }

    public static function clearHistory(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $stmt = $pdo->prepare('DELETE FROM watch_history WHERE user_id = :uid');
        $stmt->execute([':uid' => $auth['userId']]);
        ResponseHelper::success(null, 'Watch history cleared');
    }
}
