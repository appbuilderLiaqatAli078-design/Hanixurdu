<?php
declare(strict_types=1);

namespace HaniX\Api\Services;

use HaniX\Api\Config\Config;
use HaniX\Api\Models\MovieModel;
use PDO;
use Throwable;

/**
 * Playback Resolution Service (Preserves `run_video.php` video source resolution + server-side Bunny Stream signing)
 *
 * Inspects actual `movies` columns in priority order matching `run_video.php`:
 * 1. Server-side Bunny Stream mapping (`bunny_video_id` or `bunny_video_mappings`) if configured -> `hls`
 * 2. `video_path` (uploaded MP4/MKV file on server) -> `direct`
 * 3. `video_link` (YouTube, Rumble, Dailymotion, Google Drive `/preview`, or direct/HLS stream) -> `embed` | `hls` | `direct`
 * 4. `server1_link` (Displayed as "Server 2" button in `run_video.php`) -> `embed` | `hls` | `direct`
 * 5. `server2_link` (Additional fallback server) -> `embed` | `hls` | `direct`
 * Also increments `views` counter on `movies` and returns `recommended` items just like `run_video.php`.
 */
final class PlaybackService
{
    public static function resolveAndAuthorize(PDO $pdo, array $movieRow, string $preferredServer = ''): ?array
    {
        $contentId = (int) ($movieRow['id'] ?? 0);
        $expiresUnix = time() + 900; // 15 minutes
        $expiresIso = gmdate('Y-m-d\TH:i:s\Z', $expiresUnix);
        $sessionId = bin2hex(random_bytes(16));

        $servers = self::collectServers($pdo, $movieRow, $expiresUnix);
        if (empty($servers)) {
            return null;
        }

        $selectedServer = $servers[0];
        if ($preferredServer !== '') {
            foreach ($servers as $srv) {
                if (strcasecmp((string) $srv['id'], $preferredServer) === 0 || strcasecmp((string) $srv['label'], $preferredServer) === 0) {
                    $selectedServer = $srv;
                    break;
                }
            }
        }

        self::incrementViewCount($pdo, $contentId);
        $recommended = MovieModel::getRecommendedMovies($pdo, $contentId, 20);

        return [
            'content_id'                 => $contentId,
            'contentId'                  => (string) $contentId,
            'playback_type'              => $selectedServer['type'],
            'playback_url'               => $selectedServer['url'],
            'authorizedHlsUrl'           => $selectedServer['url'],
            'selected_server'            => $selectedServer['id'],
            'expires_at'                 => $expiresIso,
            'tokenExpiresAtEpochSeconds' => $expiresUnix,
            'playbackSessionId'          => $sessionId,
            'isAuthorized'               => true,
            'processingStatus'           => 'READY',
            'custom_url'                 => trim((string) ($movieRow['custom_url'] ?? '')),
            'available_servers'          => $servers,
            'recommended'                => $recommended,
        ];
    }

    /**
     * Collects all playable servers for the movie/episode in `run_video.php` order:
     * - Bunny Stream HLS (if configured)
     * - Main Server (`video_link` embed or `video_path` MP4)
     * - Server 2 (`server1_link` embed)
     * - Server 3 (`server2_link` embed)
     */
    public static function collectServers(PDO $pdo, array $movieRow, int $expiresUnix = 0): array
    {
        if ($expiresUnix <= 0) {
            $expiresUnix = time() + 900;
        }
        $contentId = (int) ($movieRow['id'] ?? 0);
        $servers = [];

        // 1. Bunny Stream HLS if mapped
        $bunnyVideoId = trim((string) ($movieRow['bunny_video_id'] ?? ''));
        if ($bunnyVideoId === '' && $contentId > 0) {
            try {
                $stmt = $pdo->prepare('SELECT bunny_video_id FROM bunny_video_mappings WHERE content_id = :cid LIMIT 1');
                $stmt->execute([':cid' => $contentId]);
                $bunnyVideoId = trim((string) ($stmt->fetchColumn() ?: ''));
            } catch (Throwable $ignored) {
            }
        }
        if ($bunnyVideoId !== '' && Config::bunnyCdnHostname() !== '') {
            $signedHls = self::signBunnyStreamUrl($bunnyVideoId, $expiresUnix);
            $servers[] = [
                'id'    => 'bunny_hls',
                'label' => 'Main Server (HD)',
                'type'  => 'hls',
                'url'   => $signedHls,
            ];
        }

        // 2. Main Server from `video_path` (direct MP4) and/or `video_link` (embed/stream) matching `run_video.php`
        $videoPath = trim((string) ($movieRow['video_path'] ?? ''));
        $videoLink = trim((string) ($movieRow['video_link'] ?? ''));

        if ($videoPath !== '') {
            $directUrl = Config::assetUrl($videoPath);
            $servers[] = [
                'id'    => 'main',
                'label' => 'Main Server',
                'type'  => self::classifyPlaybackType($directUrl),
                'url'   => $directUrl,
            ];
        }

        if ($videoLink !== '') {
            $converted = self::convertVideoLink($videoLink);
            $servers[] = [
                'id'    => $videoPath !== '' ? 'main_embed' : 'main',
                'label' => $videoPath !== '' ? 'Main Server (Stream)' : 'Main Server',
                'type'  => $converted['type'],
                'url'   => $converted['url'],
            ];
        }

        // 3. Server 2 (`server1_link` in `run_video.php`: `<button onclick="changeServer('server1')">Server 2</button>`)
        $server1Link = trim((string) ($movieRow['server1_link'] ?? ''));
        if ($server1Link !== '') {
            $convertedS1 = self::convertVideoLink($server1Link);
            $servers[] = [
                'id'    => 'server1',
                'label' => 'Server 2',
                'type'  => $convertedS1['type'],
                'url'   => $convertedS1['url'],
            ];
        }

        // 4. Server 3 (`server2_link` if populated)
        $server2Link = trim((string) ($movieRow['server2_link'] ?? ''));
        if ($server2Link !== '') {
            $convertedS2 = self::convertVideoLink($server2Link);
            $servers[] = [
                'id'    => 'server2',
                'label' => 'Server 3',
                'type'  => $convertedS2['type'],
                'url'   => $convertedS2['url'],
            ];
        }

        return $servers;
    }

    /**
     * Exact port of `run_video.php` link conversion for YouTube, Rumble, Dailymotion, Google Drive, and direct/HLS streams.
     */
    public static function convertVideoLink(string $rawLink): array
    {
        $link = trim($rawLink);

        // 1. YouTube (`youtube.com/watch?v=` or `youtu.be/`)
        if (str_contains($link, 'youtube.com/watch?v=')) {
            $parts = explode('v=', $link);
            $videoId = explode('&', $parts[1] ?? '')[0];
            return [
                'type' => 'embed',
                'url'  => 'https://www.youtube.com/embed/' . $videoId . '?rel=0&modestbranding=1',
            ];
        }
        if (str_contains($link, 'youtu.be/')) {
            $parts = explode('youtu.be/', $link);
            $videoId = explode('?', $parts[1] ?? '')[0];
            return [
                'type' => 'embed',
                'url'  => 'https://www.youtube.com/embed/' . $videoId . '?rel=0&modestbranding=1',
            ];
        }

        // 2. Rumble (`rumble.com`)
        if (str_contains($link, 'rumble.com')) {
            if (str_contains($link, '/embed/')) {
                return ['type' => 'embed', 'url' => $link];
            }
            $path = (string) parse_url($link, PHP_URL_PATH);
            $parts = explode('-', trim($path, '/'));
            $videoId = $parts[0] ?? '';
            return [
                'type' => 'embed',
                'url'  => 'https://rumble.com/embed/' . $videoId,
            ];
        }

        // 3. Dailymotion (`dai.ly/` or `dailymotion.com`)
        if (str_contains($link, 'dai.ly/')) {
            $parts = explode('dai.ly/', $link);
            $videoId = explode('?', $parts[1] ?? '')[0];
            return [
                'type' => 'embed',
                'url'  => 'https://www.dailymotion.com/embed/video/' . $videoId,
            ];
        }
        if (str_contains($link, 'dailymotion.com/embed')) {
            return ['type' => 'embed', 'url' => $link];
        }

        // 4. Google Drive (`drive.google.com` `/view` -> `/preview`)
        if (str_contains($link, 'drive.google.com')) {
            $previewUrl = str_replace(['/view?usp=sharing', '/view?usp=drive_link', '/view'], '/preview', $link);
            return [
                'type' => 'embed',
                'url'  => $previewUrl,
            ];
        }

        // 5. Direct HLS or MP4 stream vs generic iframe embed
        $normalized = self::normalizeStreamUrl($link);
        $classified = self::classifyPlaybackType($normalized);
        return [
            'type' => $classified,
            'url'  => $normalized,
        ];
    }

    private static function normalizeStreamUrl(string $raw): string
    {
        $trimmed = trim($raw);
        if (str_starts_with($trimmed, 'https://')) {
            return $trimmed;
        }
        if (str_starts_with($trimmed, 'http://')) {
            return 'https://' . substr($trimmed, strlen('http://'));
        }
        return Config::assetUrl($trimmed);
    }

    private static function classifyPlaybackType(string $url): string
    {
        $lower = strtolower(parse_url($url, PHP_URL_PATH) ?: $url);
        if (str_ends_with($lower, '.m3u8') || str_contains($lower, 'playlist.m3u8')) {
            return 'hls';
        }
        if (str_ends_with($lower, '.mp4') || str_ends_with($lower, '.mkv') || str_ends_with($lower, '.webm')) {
            return 'direct';
        }
        // Any non-.mp4/.m3u8 external link in `server1_link` or `video_link` (such as vidsrc, streamtape, ok.ru, etc.) renders in an iframe in `run_video.php`
        return 'embed';
    }

    private static function signBunnyStreamUrl(string $bunnyVideoId, int $expiresUnix): string
    {
        $cdnHost = Config::bunnyCdnHostname();
        $tokenKey = Config::bunnyTokenKey();
        $path = '/' . trim($bunnyVideoId, '/') . '/playlist.m3u8';

        if ($tokenKey !== '') {
            $rawHash = hash('sha256', $tokenKey . $path . $expiresUnix, true);
            $token = rtrim(strtr(base64_encode($rawHash), '+/', '-_'), '=');
            return sprintf('https://%s%s?token=%s&expires=%d', $cdnHost, $path, $token, $expiresUnix);
        }
        return sprintf('https://%s%s', $cdnHost, $path);
    }

    private static function incrementViewCount(PDO $pdo, int $contentId): void
    {
        if ($contentId <= 0) {
            return;
        }
        try {
            $stmt = $pdo->prepare('UPDATE movies SET views = COALESCE(views, 0) + 1 WHERE id = :id');
            $stmt->execute([':id' => $contentId]);
        } catch (Throwable $ignored) {
        }
    }
}
