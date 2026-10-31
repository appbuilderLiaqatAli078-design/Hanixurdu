<?php
declare(strict_types=1);

namespace HaniX\Api\Config;

use PDO;
use PDOException;
use RuntimeException;

/**
 * HaniX Urdu — Protected Database & Environment Configuration
 *
 * Automatically connects to the Hostinger MySQL database by:
 * 1. Checking protected environment variables or `.hanix_secrets.php` outside `public_html`
 * 2. Or safely extracting the `new mysqli(...)` credentials from the existing `public_html/config.php`
 *    so it works immediately when uploaded to `public_html/api/v1/` on Hostinger.
 */
final class Config
{
    private static ?PDO $pdo = null;

    public static function getPdo(): PDO
    {
        if (self::$pdo instanceof PDO) {
            return self::$pdo;
        }

        // 1. Check protected configuration outside public_html first
        $privateConfigPaths = [
            dirname(__DIR__, 4) . '/.hanix_secrets.php',
            dirname(__DIR__, 3) . '/.hanix_secrets.php',
        ];
        foreach ($privateConfigPaths as $privatePath) {
            if (is_file($privatePath) && is_readable($privatePath)) {
                require_once $privatePath;
            }
        }

        $host = getenv('HANIX_DB_HOST') ?: ($_ENV['HANIX_DB_HOST'] ?? '');
        $name = getenv('HANIX_DB_NAME') ?: ($_ENV['HANIX_DB_NAME'] ?? '');
        $user = getenv('HANIX_DB_USER') ?: ($_ENV['HANIX_DB_USER'] ?? '');
        $pass = getenv('HANIX_DB_PASS') ?: ($_ENV['HANIX_DB_PASS'] ?? '');

        // 2. Inspect existing website public_html/config.php if env vars are not set
        $legacyConfig = dirname(__DIR__, 2) . '/config.php';
        if (($name === '' || $user === '') && is_file($legacyConfig) && is_readable($legacyConfig)) {
            $rawConfig = (string) @file_get_contents($legacyConfig);
            // Match `$conn = new mysqli("localhost", "user", "pass", "db");` from uploaded config.php
            if (preg_match('/new\s+mysqli\s*\(\s*["\']([^"\']+)["\']\s*,\s*["\']([^"\']+)["\']\s*,\s*["\']([^"\']*)["\']\s*,\s*["\']([^"\']+)["\']\s*\)/i', $rawConfig, $m)) {
                $host = $host !== '' ? $host : $m[1];
                $user = $user !== '' ? $user : $m[2];
                $pass = $pass !== '' ? $pass : $m[3];
                $name = $name !== '' ? $name : $m[4];
            }
        }

        if ($host === '') {
            $host = 'localhost';
        }
        if ($user === '') {
            $user = 'u742032481_hanixurdu';
        }
        if ($pass === '') {
            $pass = 'Gct2024a27';
        }
        if ($name === '') {
            $name = 'u742032481_hanix';
        }

        $dsn = sprintf('mysql:host=%s;dbname=%s;charset=utf8mb4', $host, $name);
        try {
            self::$pdo = new PDO($dsn, $user, $pass, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);
            return self::$pdo;
        } catch (PDOException $e) {
            throw new RuntimeException('Database connection failed.');
        }
    }

    public static function siteBaseUrl(): string
    {
        $url = getenv('HANIX_SITE_URL') ?: 'https://hanixurdu.com';
        return rtrim($url, '/');
    }

    public static function assetUrl(?string $rawPath): string
    {
        if ($rawPath === null || trim($rawPath) === '') {
            return '';
        }
        $trimmed = trim($rawPath);
        if (str_starts_with($trimmed, 'https://')) {
            return $trimmed;
        }
        if (str_starts_with($trimmed, 'http://')) {
            return 'https://' . substr($trimmed, strlen('http://'));
        }
        $cleaned = preg_replace('#^\./+#', '', $trimmed) ?? $trimmed;
        return self::siteBaseUrl() . '/' . ltrim($cleaned, '/');
    }

    public static function bunnyLibraryId(): string
    {
        return trim((string) (getenv('HANIX_BUNNY_LIBRARY_ID') ?: ''));
    }

    public static function bunnyCdnHostname(): string
    {
        return trim((string) (getenv('HANIX_BUNNY_CDN_HOSTNAME') ?: ''));
    }

    public static function bunnyTokenKey(): string
    {
        return trim((string) (getenv('HANIX_BUNNY_STREAM_TOKEN_KEY') ?: ''));
    }
}
