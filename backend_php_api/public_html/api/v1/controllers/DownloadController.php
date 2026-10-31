<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Config\Config;
use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use HaniX\Api\Middleware\VipMiddleware;
use HaniX\Api\Models\MovieModel;
use PDO;

/**
 * Download Authorization Controller (`POST /downloads/authorize`, `GET /downloads`, `DELETE /downloads/{id}`)
 */
final class DownloadController
{
    public static function authorize(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        VipMiddleware::requireActiveVip($auth);

        $body = ResponseHelper::jsonBody();
        $rawContentId = (string) ($body['content_id'] ?? $body['contentId'] ?? '');
        $qualityTier = trim((string) ($body['qualityTier'] ?? $body['quality'] ?? '720p'));

        if ($rawContentId === '') {
            ResponseHelper::error('content_id is required.', 422, 'VALIDATION_ERROR');
        }

        $movieRow = MovieModel::findByIdOrSlug($pdo, $rawContentId);
        if (!$movieRow) {
            ResponseHelper::error('Requested content was not found.', 404, 'CONTENT_NOT_FOUND');
        }

        $videoPath = trim((string) ($movieRow['video_path'] ?? ''));
        $videoLink = trim((string) ($movieRow['video_link'] ?? ''));
        $downloadUrl = $videoPath !== '' ? Config::assetUrl($videoPath) : ($videoLink !== '' ? Config::assetUrl($videoLink) : '');

        if ($downloadUrl === '') {
            ResponseHelper::error('Direct offline download file is not available for this title.', 404, 'DOWNLOAD_UNAVAILABLE');
        }

        $downloadId = 'dl_' . (int) $movieRow['id'] . '_' . bin2hex(random_bytes(6));
        $expiresEpoch = $auth['membership']['vipExpiryEpochSeconds'] ?? (time() + 86400 * 7);

        $stmt = $pdo->prepare('
            INSERT INTO user_downloads
                (user_id, download_id, content_id, content_type, title, poster_url, quality, expires_at, created_at)
            VALUES
                (:uid, :did, :cid, :ctype, :title, :poster, :qual, :exp, NOW())
        ');
        $stmt->execute([
            ':uid'    => $auth['userId'],
            ':did'    => $downloadId,
            ':cid'    => (int) $movieRow['id'],
            ':ctype'  => 'MOVIE',
            ':title'  => (string) ($movieRow['title'] ?? ''),
            ':poster' => Config::assetUrl((string) ($movieRow['poster_path'] ?? '')),
            ':qual'   => $qualityTier,
            ':exp'    => gmdate('Y-m-d H:i:s', (int) $expiresEpoch),
        ]);

        ResponseHelper::success([
            'downloadId'            => $downloadId,
            'download_id'           => $downloadId,
            'contentId'             => (string) $movieRow['id'],
            'content_id'            => (int) $movieRow['id'],
            'isAuthorized'          => true,
            'authorizedDownloadUrl' => $downloadUrl,
            'download_url'          => $downloadUrl,
            'expiresAtEpochSeconds' => (int) $expiresEpoch,
            'expires_at'            => gmdate('Y-m-d\TH:i:s\Z', (int) $expiresEpoch),
        ], 'Download authorized');
    }

    public static function index(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $stmt = $pdo->prepare('SELECT * FROM user_downloads WHERE user_id = :uid ORDER BY created_at DESC');
        $stmt->execute([':uid' => $auth['userId']]);

        $items = [];
        foreach ($stmt->fetchAll() as $row) {
            $expTs = strtotime((string) ($row['expires_at'] ?? '')) ?: (time() + 86400);
            $items[] = [
                'id'                    => (string) ($row['download_id'] ?? $row['id']),
                'contentId'             => (string) ($row['content_id'] ?? ''),
                'contentType'           => strtolower((string) ($row['content_type'] ?? 'movie')),
                'title'                 => (string) ($row['title'] ?? ''),
                'poster'                => Config::assetUrl((string) ($row['poster_url'] ?? '')),
                'qualityTier'           => (string) ($row['quality'] ?? '720p'),
                'status'                => 'AUTHORIZED',
                'expiresAtEpochSeconds' => $expTs,
            ];
        }
        ResponseHelper::success($items);
    }

    public static function destroy(PDO $pdo, string $downloadId): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $stmt = $pdo->prepare('DELETE FROM user_downloads WHERE user_id = :uid AND (download_id = :did OR id = :id)');
        $stmt->execute([
            ':uid' => $auth['userId'],
            ':did' => $downloadId,
            ':id'  => ctype_digit($downloadId) ? (int) $downloadId : 0,
        ]);
        ResponseHelper::success(null, 'Download removed');
    }
}
