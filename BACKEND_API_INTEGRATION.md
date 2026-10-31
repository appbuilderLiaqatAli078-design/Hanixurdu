# HaniX Urdu — Hostinger PHP + MySQL REST API v1 & Bunny Stream Integration Guide

This document provides the complete backend integration blueprint to connect the **HaniX Urdu** Android app to your existing **Hostinger PHP + MySQL** website (`https://hanixurdu.com`) and **Bunny Stream** without breaking existing web pages or `shahkot.php`.

---

## 1. JSON Envelope Contract

Every endpoint under `https://hanixurdu.com/api/v1/` must return `Content-Type: application/json; charset=utf-8` with the following structure:

### Success Response
```json
{
  "success": true,
  "message": "Content loaded successfully",
  "data": {}
}
```

### Error Response
```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

---

## 2. Mapping Existing PHP Files to `/api/v1/` Endpoints

Create a `/public_html/api/v1/` directory on Hostinger that includes your existing database connection (`require_once '../../config.php';`) using PDO prepared statements:

| Category | HTTP Method & Route | Existing Website PHP Reference | Backend Implementation Notes |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST /auth/login` | `login_process.php` | Verify password with `password_verify()`, issue short-lived Bearer token & refresh token, return `AuthSessionPayload`. |
| **Auth** | `POST /auth/register` | `signup.php` | Hash password with `password_hash()`, insert into existing users table, return `AuthSessionPayload`. |
| **Auth** | `POST /auth/logout` | Session logout | Revoke active token in database. |
| **Auth** | `POST /auth/refresh` | Token rotation | Validate refresh token and issue a fresh Bearer token pair. |
| **Auth** | `GET /auth/me` | `config.php` | Resolve user strictly from `Authorization: Bearer <token>` header. Never trust client-supplied `user_id`. |
| **Auth** | `POST /auth/forgot-password` | `forgot_password.php` | Dispatch password reset link via Hostinger SMTP. |
| **Home** | `GET /home` | `index.php` | Return `banners`, `featuredMovies`, `featuredSeries`, `latestMovies`, `latestSeries`, and `categories`. |
| **Home** | `GET /home/banners` | `index.php` | Spotlight hero items. |
| **Home** | `GET /home/featured` | `index.php` | Featured movies list. |
| **Home** | `GET /home/latest` | `index.php` | Latest uploaded movies list. |
| **Home** | `GET /home/categories` | `header.php` / `index.php` | Distinct categories with counts. |
| **Movies** | `GET /movies` | `index.php` / `edit_movie.php` | List movies (optional `?category=` filter). Map columns to `Movie(id, title, description, poster, category, isPremium, duration)`. |
| **Movies** | `GET /movies/{id}` | `run_video.php` / `edit_movie.php` | Return single `Movie` metadata (do NOT expose raw video file URL here). |
| **Movies** | `GET /movies/categories` | `index.php` | Movie categories list. |
| **Series** | `GET /series` | `series.php` | List series mapped to `Series(id, title, description, poster, category)`. |
| **Series** | `GET /series/{id}` | `view_series.php` / `edit_series.php` | Single `Series` details. |
| **Series** | `GET /series/{id}/seasons` | `view_series.php` | Seasons for series mapped to `Season(id, seriesId, seasonName)`. |
| **Series** | `GET /seasons/{id}/episodes` | `view_series.php` | Episodes for season mapped to `Episode(id, seasonId, title, description, poster, isPremium, duration, playbackAvailability)`. |
| **Series** | `GET /episodes/{id}` | `run_video.php` | Single `Episode` metadata. |
| **Search** | `GET /search?q={query}` | `search.php` | Parameterized search across movies and series tables. |
| **VIP** | `GET /vip/status` | `check_premium.php` | Verify `vipStatus` and `vipExpiry` for the authenticated user on the server. |
| **VIP** | `GET /vip/plans` | `add_premium.php` | Return available VIP membership tiers. |
| **VIP** | `POST /vip/verify` | `unlock_vip.php` / `add_premium.php` | Validate voucher/payment reference server-side and update user's VIP expiry in MySQL. |
| **Playback** | `POST /playback/authorize` | `run_video.php` + `check_premium.php` | 6-step verification + Bunny Stream signed HLS URL generation (see Section 3 below). |
| **Playback** | `POST /playback/progress` | Watch progress handler | Save `positionMs` and `durationMs` for authenticated user. |
| **Playback** | `POST /playback/complete` | Watch progress handler | Mark movie or episode completed. |
| **Downloads** | `POST /downloads/authorize` | `run_video.php` + `check_premium.php` | Verify VIP/entitlement and return short-lived `signedDownloadStreamUrl` + `licenseExpiresAtEpochSeconds`. |
| **Downloads** | `GET /downloads` | Downloads handler | List user's authorized offline download entitlements. |
| **Downloads** | `DELETE /downloads/{id}` | Downloads handler | Revoke download entitlement. |
| **User** | `GET /user/profile` | Users table | Authenticated user's profile (strip password hashes & secret PINs). |
| **User** | `PUT /user/profile` | Users table | Update user's `name` / `profileImage`. |
| **User** | `GET /user/history` | Watch history table | Return user's watch history items. |
| **User** | `DELETE /user/history` | Watch history table | Clear user's watch history. |
| **Settings** | `GET /app/settings` | `config.php` / `shahkot.php` | Maintenance mode, Bunny Stream enabled flag, support email. |
| **Settings** | `GET /app/version` | `config.php` | Latest Android version code & changelog. |

---

## 3. Bunny Stream Server-Side Token Signing (`POST /playback/authorize`)

Never put Bunny Stream API keys or library tokens inside the Android app. Instead, store `BUNNY_STREAM_SECURITY_KEY` and `BUNNY_CDN_HOSTNAME` in a non-public config on Hostinger and sign the HLS URL in PHP:

```php
// Inside /api/v1/playback/authorize.php
// 1. Authenticate user via Bearer token
// 2. Load movie/episode record and verify availability
// 3. If isPremium == 1, verify user's VIP status & expiry (check_premium.php logic)
// 4. Generate short-lived Bunny token (e.g. 900 seconds = 15 minutes):

$securityKey = getenv('BUNNY_STREAM_SECURITY_KEY'); // Kept strictly on server
$cdnHostname = getenv('BUNNY_CDN_HOSTNAME');        // e.g. vz-xxxxxxx.b-cdn.net
$bunnyVideoId = $contentRow['bunny_video_id'];
$expires = time() + 900;
$tokenPath = "/{$bunnyVideoId}/";
$hashableBase = $securityKey . $tokenPath . $expires;
$token = rtrim(strtr(base64_encode(hash('sha256', $hashableBase, true)), '+/', '-_'), '=');

$signedHlsUrl = "https://{$cdnHostname}/{$bunnyVideoId}/playlist.m3u8?token={$token}&expires={$expires}";

echo json_encode([
    "success" => true,
    "message" => "Playback authorized",
    "data" => [
        "playbackSessionId" => bin2hex(random_bytes(16)),
        "contentId" => (string)$contentId,
        "contentType" => $contentType,
        "isAuthorized" => true,
        "requiresVip" => (bool)$contentRow['is_premium'],
        "processingStatus" => "READY",
        "authorizedHlsUrl" => $signedHlsUrl,
        "tokenExpiresAtEpochSeconds" => $expires,
        "resumePositionMs" => (int)$savedResumeMs
    ]
]);
```
