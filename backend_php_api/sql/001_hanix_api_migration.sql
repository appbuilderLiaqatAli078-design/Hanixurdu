-- =============================================================================
-- HaniX Urdu — Non-Destructive MySQL Migration for Real Hostinger Database
-- Database: u742032481_hanix
-- Compatible with: config.php, signup.php, login_process.php, forgot_password.php,
--                  check_premium.php, add_premium.php, unlock_vip.php,
--                  shahkot.php, index.php, series.php, run_video.php, upload.php
-- =============================================================================

-- 1. Ensure existing `users` columns from signup.php exist without altering existing rows
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    secret_pin VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS secret_pin VARCHAR(255) NULL;

-- 2. Exact `premium_users` table from check_premium.php & unlock_vip.php
CREATE TABLE IF NOT EXISTS premium_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Exact `series` and `series_seasons` tables from shahkot.php
CREATE TABLE IF NOT EXISTS series (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    trailer_link VARCHAR(255) DEFAULT NULL,
    poster_path VARCHAR(255) DEFAULT NULL,
    index_title VARCHAR(255) DEFAULT NULL,
    index_poster VARCHAR(255) DEFAULT NULL,
    custom_url VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS series_seasons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    series_name VARCHAR(255) NOT NULL,
    season_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Exact `auditions` table from shahkot.php & upload.php
CREATE TABLE IF NOT EXISTS auditions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Mobile API Bearer & Refresh Token Table
CREATE TABLE IF NOT EXISTS api_auth_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    access_token_hash CHAR(64) NOT NULL UNIQUE,
    refresh_token_hash CHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(120) DEFAULT 'HaniX-Urdu-Android',
    expires_at DATETIME NOT NULL,
    refresh_expires_at DATETIME NOT NULL,
    revoked_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_tokens_user (user_id),
    INDEX idx_api_tokens_access (access_token_hash),
    INDEX idx_api_tokens_refresh (refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Cross-Device Watch History Table
CREATE TABLE IF NOT EXISTS watch_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    content_id INT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'MOVIE',
    episode_id INT DEFAULT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255) DEFAULT NULL,
    poster_url VARCHAR(500) DEFAULT NULL,
    position_seconds INT UNSIGNED NOT NULL DEFAULT 0,
    duration_seconds INT UNSIGNED NOT NULL DEFAULT 1,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    is_vip TINYINT(1) NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_content (user_id, content_id, content_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Authorized VIP Downloads Table
CREATE TABLE IF NOT EXISTS user_downloads (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    download_id VARCHAR(80) NOT NULL UNIQUE,
    content_id INT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'MOVIE',
    title VARCHAR(255) NOT NULL,
    poster_url VARCHAR(500) DEFAULT NULL,
    quality VARCHAR(20) NOT NULL DEFAULT '720p',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_downloads_uid (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Optional Bunny Stream Video Mapping Table
CREATE TABLE IF NOT EXISTS bunny_video_mappings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    content_type ENUM('MOVIE', 'EPISODE') NOT NULL DEFAULT 'MOVIE',
    content_id INT NOT NULL,
    bunny_library_id VARCHAR(64) DEFAULT NULL,
    bunny_video_id VARCHAR(128) NOT NULL,
    hls_playlist_url VARCHAR(500) DEFAULT NULL,
    is_vip_stream TINYINT(1) NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bunny_content (content_type, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
