# HaniX Urdu — Native Android Application & Hostinger PHP + MySQL REST API v1

**Application Name**: `HaniX Urdu`  
**Android Application ID**: `com.hanixurdu.app`  
**Backend Base URL**: `https://hanixurdu.com/api/v1/`  
**Existing Website**: `https://hanixurdu.com` (`u471204769_hanix_db`)  
**Existing Admin Panel**: `shahkot.php` (preserved intact)

---

## 1. Architecture & Real Source Code Integration

This repository contains two decoupled, production-ready deliverables built directly from your uploaded `hanixurdu.com` PHP website files:

1. **Native Android App (`/app/`)** — Built with Kotlin, Jetpack Compose, Material 3 (`OledBlack`, `VipChampagneGold`, `CinemaCrimson`), MVVM (`HaniXViewModel` + `HaniXRepository`), Retrofit + OkHttp + Moshi, Room (`HaniXDatabase`), Android Keystore (`AES-256-GCM` for session tokens & encrypted VIP offline downloads), Media3 ExoPlayer (for direct `video_path` MP4s & signed Bunny Stream HLS `.m3u8`), and an embedded provider WebView player with **Main Server / Server 2 switching** (for YouTube, Rumble, Dailymotion, and Google Drive `/preview` links).
2. **Hostinger PHP REST API v1 (`/backend_php_api/public_html/api/v1/`)** — A clean, modular PHP 8 + PDO REST API that connects to your existing Hostinger MySQL database (`u471204769_hanix_db`) by reading `public_html/config.php` or `.hanix_secrets.php` without modifying or breaking your existing website pages (`index.php`, `series.php`, `view_series.php`, `run_video.php`, `shahkot.php`, `mind_game.php`, `studio.php`, `upload.php`).

---

## 2. Verified MySQL Tables & Business Logic

- **`users` (`signup.php`, `login_process.php`, `forgot_password.php`)**:
  - Columns: `id`, `email`, `password` (`password_hash` / `password_verify`), `secret_pin` (used for 2-step password recovery in `forgot_password.php`).
  - Every new user starts as **`FREE`**.
- **`premium_users` (`check_premium.php`, `add_premium.php`, `unlock_vip.php`)**:
  - Columns: `id`, `email` (`UNIQUE`), `expiry_date` (`DATETIME`).
  - Active VIP check: `SELECT expiry_date FROM premium_users WHERE LOWER(email) = LOWER(:email) AND expiry_date > NOW() ORDER BY expiry_date DESC LIMIT 1`.
  - When `expiry_date <= NOW()`, membership immediately reverts to **`FREE`** on both the backend and Android client.
- **`movies` & `series` (`index.php`, `search.php`, `edit_movie.php`, `edit_series.php`)**:
  - Supports `FIND_IN_SET(:cat, category) > 0` for all 5 official `index.php` rails:
    1. **Trending Coverflow (`trending`)**
    2. **Urdu Subtitles (`urdu_subtitles` — 16:9 landscape cards)**
    3. **All Content (`all`)**
    4. **Coming Soon (`coming_soon`)**
    5. **Hanix Originals / Fan Dubbing (`fan_dubbing`)**
    6. **Premium (`premium` — VIP Exclusive)**
  - Normalizes `./posters/...` and `./videos/...` paths to `https://hanixurdu.com/posters/...` and `https://hanixurdu.com/videos/...`.
- **`series_seasons` (`series.php`, `view_series.php`, `run_video.php`)**:
  - Groups seasons and episodes by `series_name` and `season_name`.
- **Multi-Source & Multi-Server Video Playback (`run_video.php`)**:
  - Resolves **Main Server** (`video_path` MP4, `video_link` YouTube/Rumble/Dailymotion/Google Drive `/preview`, or signed Bunny Stream HLS) and **Server 2** (`server1_link`), plus **Recommended for You** movies.
- **JazzCash & WhatsApp VIP Upgrade Flow (`index.php`, `series.php`)**:
  - Displays official VIP plans (`1 Month — Rs. 149 / Rs. 250`, `6 Months — Rs. 599`, `1 Year — Rs. 999`), **JazzCash details (`Muhammad Hasnain — 03294806457`)**, **WhatsApp Activation (`https://wa.me/923160450074`)**, and **WhatsApp Channel (`https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q`)**.
- **Mind Games Quiz (`mind_game.php`) & Dubbing Studio (`studio.php` & `upload.php`)**:
  - Interactive 10-question randomized trivia quiz (`GET /api/v1/mind-game/questions`) and authenticated multipart audio/video audition upload (`POST /api/v1/studio/audition`, up to 10 MB, 20 uploads/day in `auditions` table).

---

## 3. Deploying the PHP REST API v1 on Hostinger

1. Run `/backend_php_api/sql/001_hanix_api_migration.sql` in Hostinger phpMyAdmin on database `u471204769_hanix_db`.
2. Upload `/backend_php_api/public_html/api/v1/` into `public_html/api/v1/` on Hostinger (`https://hanixurdu.com/api/v1/`).
3. See `/SOURCE_TO_API_MAPPING.md` and `/BACKEND_API_INTEGRATION.md` for complete endpoint documentation and `curl` verification commands.
