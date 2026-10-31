# HaniX Urdu — Backend REST API v1 & Shahkot Admin Integration Guide

**Base URL:** `https://hanixurdu.com/api/v1/`  
**Content-Type:** `application/json; charset=utf-8`  
**Authentication:** `Authorization: Bearer <accessToken>`  
**Membership Plans:** Strictly `FREE` and `VIP`

---

## 1. Standardized JSON Response Format

### Success Response
```json
{
  "success": true,
  "message": "Optional status message",
  "data": {}
}
```

### Error Response
```json
{
  "success": false,
  "message": "Human-readable error explanation",
  "error_code": "MACHINE_READABLE_CODE"
}
```

---

## 2. Existing MySQL Schema Compatibility

The PHP REST API (`/backend_php_api/public_html/api/v1/index.php`) and Android Moshi models (`ApiModels.kt` + `FlexibleBooleanAdapter`) directly support the existing HaniX Urdu MySQL tables without destructive changes:

- `users`: `id`, `username`, `email`, `password` (supports bcrypt `$2y$` and automatically upgrades legacy hashes on login)
- `premium_users`: `id`, `user_id`, `expiry_date` (`DATETIME` — single source of truth for `VIP` status)
- `movies`: `id`, `title`, `description`, `poster_path`, `category`, `is_premium` (`0` = `FREE`, `1` = `VIP`), `release_year`
- `series`: `id`, `series_name` / `index_title`, `description`, `poster_path` / `index_poster`, `is_premium`
- `series_seasons`: `id`, `series_id`, `season_name`, `custom_url`

Additional tables created by `/backend_php_api/sql/001_hanix_api_migration.sql`:
- `api_auth_tokens` — SHA-256 hashed access & refresh tokens with device tracking and revocation
- `watch_history` — Cross-device playback resume positions (`position_seconds`, `duration_seconds`, `completed`)
- `user_downloads` — Authorized VIP offline download metadata
- `bunny_video_mappings` — Maps `MOVIE` or `EPISODE` IDs to `bunny_library_id` and `bunny_video_id`
- `hanix_app_settings` — Global flags (`maintenance_mode`, `downloads_enabled`, `support_email`, `whatsapp_support`)

---

## 3. Complete 33 REST API Endpoints

### Authentication (6)
- `POST /auth/login`
- `POST /auth/register` *(Every newly registered user receives `membership: "FREE"`, `isVip: false`)*
- `POST /auth/logout`
- `POST /auth/refresh` *(Rotates access & refresh tokens)*
- `GET /auth/me`
- `POST /auth/forgot-password`

### Home (5)
- `GET /home`
- `GET /home/banners`
- `GET /home/featured`
- `GET /home/latest`
- `GET /home/categories`

### Movies (3)
- `GET /movies`
- `GET /movies/{id}`
- `GET /movies/categories`

### Series, Seasons & Episodes (5)
- `GET /series`
- `GET /series/{id}`
- `GET /series/{id}/seasons`
- `GET /seasons/{id}/episodes`
- `GET /episodes/{id}`

### Search (1)
- `GET /search?q={query}`

### Membership — FREE & VIP Only (2)
- `GET /vip/status`
- `GET /vip/plans`

### Bunny Stream Playback (3)
- `POST /playback/authorize` *(10-step verification: Bearer token -> User lookup -> Movie/Episode lookup -> FREE/VIP check -> `premium_users.expiry_date` check -> SHA-256 signed Bunny `.m3u8` URL)*
- `POST /playback/progress`
- `POST /playback/complete`

### VIP Downloads (3)
- `POST /downloads/authorize`
- `GET /downloads`
- `DELETE /downloads/{id}`

### User Profile & Watch History (4)
- `GET /user/profile`
- `PUT /user/profile`
- `GET /user/history`
- `DELETE /user/history`

### Application (2)
- `GET /app/settings`
- `GET /app/version`
