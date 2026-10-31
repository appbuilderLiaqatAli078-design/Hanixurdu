<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use HaniX\Api\Middleware\VipMiddleware;
use HaniX\Api\Models\MovieModel;
use HaniX\Api\Services\PlaybackService;
use PDO;
use Throwable;

/**
 * Playback Controller (`run_video.php` + `check_premium.php`)
 *
 * Implements `POST /playback/authorize`:
 * 1. Find the actual `movies` record by `content_id` / `contentId`.
 * 2. Identify its content type and premium restrictions (`is_premium == 1` or `category` contains `'premium'`/`'vip'`).
 * 3. Authenticate the user via Bearer token (`AuthMiddleware`).
 * 4. Check FREE or VIP permissions (`VipMiddleware`).
 * 5. Check VIP expiry in `premium_users.expiry_date`.
 * 6. Resolve the valid playback source (`video_path`, `video_link`, `server1_link`, `server2_link`, or Bunny Stream HLS).
 * 7. Return structured JSON with `content_id`, `playback_type`, `playback_url`, `expires_at` (plus camelCase DTO fields).
 */
final class PlaybackController
{
    public static function authorize(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $body = ResponseHelper::jsonBody();

        $rawContentId = (string) ($body['content_id'] ?? $body['contentId'] ?? '');
        $preferredServer = trim((string) ($body['server'] ?? ''));

        if ($rawContentId === '') {
            ResponseHelper::error('content_id is required.', 422, 'VALIDATION_ERROR');
        }

        // Step 1: Find the actual movie/episode record in `movies`
        $movieRow = MovieModel::findByIdOrSlug($pdo, $rawContentId);
        if (!$movieRow) {
            ResponseHelper::error('Requested content was not found.', 404, 'CONTENT_NOT_FOUND');
        }

        // Step 2: Identify premium restriction
        $requiresVip = MovieModel::isPremiumRow($movieRow);

        // Step 3-5: Verify FREE or VIP permission & VIP expiry
        if ($requiresVip) {
            VipMiddleware::requireActiveVip($auth);
        }

        // Step 6: Resolve valid playback source
        $resolved = PlaybackService::resolveAndAuthorize($pdo, $movieRow, $preferredServer);
        if ($resolved === null) {
            ResponseHelper::error(
                'No valid playback source (video_path, video_link, server1_link, server2_link, or Bunny Stream) is configured for this title.',
                404,
                'VIDEO_SOURCE_MISSING'
            );
        }

        // Lookup resume position from watch_history if available
        $resumePosMs = 0;
        try {
            $rStmt = $pdo->prepare('
                SELECT position_seconds
                FROM watch_history
                WHERE user_id = :uid AND content_id = :cid
                LIMIT 1
            ');
            $rStmt->execute([':uid' => $auth['userId'], ':cid' => (int) $movieRow['id']]);
            $resumePosMs = ((int) ($rStmt->fetchColumn() ?: 0)) * 1000;
        } catch (Throwable $ignored) {
        }

        $resolved['membershipRequired'] = $requiresVip ? 'VIP' : 'FREE';
        $resolved['requiresVip'] = $requiresVip;
        $resolved['resumePositionMs'] = $resumePosMs;
        $resolved['contentType'] = !empty($movieRow['series_name']) && strtolower((string) $movieRow['series_name']) !== 'general'
            ? 'episode'
            : 'movie';

        ResponseHelper::success($resolved, 'Playback authorized');
    }

    public static function progress(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $body = ResponseHelper::jsonBody();

        $contentId = (int) ($body['content_id'] ?? $body['contentId'] ?? 0);
        $contentType = strtoupper(trim((string) ($body['contentType'] ?? $body['content_type'] ?? 'MOVIE')));
        $positionMs = max(0, (int) ($body['positionMs'] ?? ((int) ($body['positionSeconds'] ?? 0) * 1000)));
        $durationMs = max(1000, (int) ($body['durationMs'] ?? ((int) ($body['durationSeconds'] ?? 1) * 1000)));
        $positionSec = (int) ($positionMs / 1000);
        $durationSec = max(1, (int) ($durationMs / 1000));

        $movieRow = MovieModel::findByIdOrSlug($pdo, (string) $contentId);
        $title = $movieRow ? (string) ($movieRow['title'] ?? ('Title #' . $contentId)) : ('Title #' . $contentId);
        $poster = $movieRow ? (string) ($movieRow['poster_path'] ?? '') : '';

        $stmt = $pdo->prepare('
            INSERT INTO watch_history
                (user_id, content_id, content_type, title, poster_url, position_seconds, duration_seconds, completed, updated_at)
            VALUES
                (:uid, :cid, :ctype, :title, :poster, :pos, :dur, :comp, NOW())
            ON DUPLICATE KEY UPDATE
                position_seconds = VALUES(position_seconds),
                duration_seconds = VALUES(duration_seconds),
                completed = VALUES(completed),
                updated_at = NOW()
        ');
        $stmt->execute([
            ':uid'    => $auth['userId'],
            ':cid'    => $contentId,
            ':ctype'  => $contentType,
            ':title'  => $title,
            ':poster' => $poster,
            ':pos'    => $positionSec,
            ':dur'    => $durationSec,
            ':comp'   => ($positionSec >= (int) ($durationSec * 0.92)) ? 1 : 0,
        ]);

        ResponseHelper::success(null, 'Playback progress saved');
    }

    public static function complete(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $body = ResponseHelper::jsonBody();
        $contentId = (int) ($body['content_id'] ?? $body['contentId'] ?? 0);
        $contentType = strtoupper(trim((string) ($body['contentType'] ?? 'MOVIE')));

        $stmt = $pdo->prepare('
            UPDATE watch_history
            SET completed = 1, updated_at = NOW()
            WHERE user_id = :uid AND content_id = :cid AND content_type = :ctype
        ');
        $stmt->execute([
            ':uid'   => $auth['userId'],
            ':cid'   => $contentId,
            ':ctype' => $contentType,
        ]);

        ResponseHelper::success(null, 'Playback marked complete');
    }
}
