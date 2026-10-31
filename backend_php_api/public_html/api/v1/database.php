<?php
/**
 * HaniX Urdu — Database Connection Adapter (`public_html/api/v1/database.php`)
 * Reuses existing Hostinger database credentials from `../../config.php` or environment variables
 * via PDO with prepared statements and strict error suppression.
 */

declare(strict_types=1);

require_once __DIR__ . '/response.php';

function hanix_get_pdo(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }

    // Attempt to load existing website config.php without outputting HTML
    $legacyConfigPath = dirname(__DIR__, 2) . '/config.php';
    if (is_file($legacyConfigPath)) {
        ob_start();
        require_once $legacyConfigPath;
        ob_end_clean();
    }

    // Reuse variables if defined in existing config.php ($pdo, $conn, $host, $dbname, $username, $password)
    if (isset($GLOBALS['pdo']) && $GLOBALS['pdo'] instanceof PDO) {
        $pdo = $GLOBALS['pdo'];
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        return $pdo;
    }

    $dbHost = $GLOBALS['servername'] ?? $GLOBALS['host'] ?? $GLOBALS['db_host'] ?? getenv('HANIX_DB_HOST') ?: 'localhost';
    $dbName = $GLOBALS['dbname'] ?? $GLOBALS['db_name'] ?? $GLOBALS['database'] ?? getenv('HANIX_DB_NAME') ?: '';
    $dbUser = $GLOBALS['username'] ?? $GLOBALS['db_user'] ?? $GLOBALS['user'] ?? getenv('HANIX_DB_USER') ?: '';
    $dbPass = $GLOBALS['password'] ?? $GLOBALS['db_pass'] ?? $GLOBALS['pass'] ?? getenv('HANIX_DB_PASS') ?: '';

    if ($dbName === '' || $dbUser === '') {
        hanix_send_json(false, 'Database configuration unavailable on server.', null, 500);
    }

    try {
        $dsn = "mysql:host={$dbHost};dbname={$dbName};charset=utf8mb4";
        $pdo = new PDO($dsn, (string)$dbUser, (string)$dbPass, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ]);
        return $pdo;
    } catch (Throwable $e) {
        error_log('HaniX API PDO Connection Error: ' . $e->getMessage());
        hanix_send_json(false, 'Service temporarily unavailable.', null, 500);
    }
}
