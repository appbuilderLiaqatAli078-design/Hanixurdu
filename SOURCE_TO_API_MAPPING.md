# HaniX Urdu — Real PHP Website to REST API v1 Source Mapping (`SOURCE_TO_API_MAPPING.md`)

This document maps every uploaded HaniX Urdu PHP website file in `hanixurdu.com` to the modular `/api/v1/` PHP REST API and native Android client (`com.hanixurdu.app`).

---

## 1. Verified MySQL Database Schema (`config.php`, `shahkot.php`, `upload.php`)

- **Database Name**: `u471204769_hanix_db` (parsed automatically from `public_html/config.php` or `.hanix_secrets.php` without modifying `config.php`)
- **Tables & Columns**:
  1. **`users`** (`signup.php`, `login_process.php`, `forgot_password.php`):
     - `id INT AUTO_INCREMENT PRIMARY KEY`
     - `email VARCHAR(255) NOT NULL UNIQUE`
     - `password VARCHAR(255) NOT NULL` (`password_hash(..., PASSWORD_DEFAULT)`)
     - `secret_pin VARCHAR(64) NOT NULL` (used by `forgot_password.php` for 2-step recovery)
  2. **`premium_users`** (`check_premium.php`, `add_premium.php`, `unlock_vip.php`):
     - `id INT AUTO_INCREMENT PRIMARY KEY`
     - `email VARCHAR(255) NOT NULL UNIQUE`
     - `expiry_date DATETIME NOT NULL`
     - Active VIP check: `SELECT expiry_date FROM premium_users WHERE LOWER(email) = LOWER(:email) AND expiry_date > NOW() ORDER BY expiry_date DESC LIMIT 1`
  3. **`movies`** (`index.php`, `search.php`, `run_video.php`, `shahkot.php`, `edit_movie.php`):
     - `id`, `title`, `description`, `poster_path`, `video_path`, `video_link`, `server1_link`, `server2_link`, `category` (comma-separated tags queried with `FIND_IN_SET`), `custom_url`, `is_premium` (`0` = FREE, `1` = VIP), `bunny_video_id`
  4. **`series`** (`index.php`, `series.php`, `view_series.php`, `shahkot.php`, `edit_series.php`):
     - `id`, `series_name`, `description`, `poster_path`, `index_poster`, `category`, `custom_url`, `is_premium`
  5. **`series_seasons`** (`series.php`, `view_series.php`, `run_video.php`, `shahkot.php`, `edit_series.php`):
     - `id`, `series_name` (joins `series.series_name`), `season_name`, `title`, `description`, `poster_path`, `video_path`, `video_link`, `server1_link`, `server2_link`, `is_premium`, `bunny_video_id`
  6. **`auditions`** (`studio.php`, `upload.php`):
     - `id`, `file_name`, `uploaded_at` (enforces 20 uploads/day limit and 10 MB max file size in `uploads/auditions/`)

---

## 2. File-by-File PHP to `/api/v1/` Endpoint Mapping

| Existing Website File | Existing PHP Logic & SQL Queries | REST API v1 Route(s) | Backend Controller & Model | Android Screen & Repository Method |
| :--- | :--- | :--- | :--- | :--- |
| `config.php` | `new mysqli($host, $user, $pass, $db)` + `session_start()` | All `/api/v1/*` routes | `config/Config.php` & `config/Database.php` | `HaniXApiService` (`https://hanixurdu.com/api/v1/`) |
| `signup.php` | `INSERT INTO users (email, password, secret_pin)` with `password_hash` | `POST /api/v1/auth/register` | `AuthController::register` (`UserModel::createUser`) | `RegisterScreen` (`register_secret_pin_input`) |
| `login_process.php` | `SELECT * FROM users WHERE email = ?` + `password_verify` + `premium_users` check | `POST /api/v1/auth/login` | `AuthController::login` + `VipMiddleware::resolveMembership` | `LoginScreen` -> `HaniXRepository.login` |
| `forgot_password.php` | Step 1: Verify `email + secret_pin`; Step 2: `UPDATE users SET password = ?` (min 8 chars) | `POST /api/v1/auth/forgot-password` & `POST /api/v1/auth/reset-password` | `AuthController::verifyForgotPassword` & `resetForgotPassword` | `ForgotPasswordScreen` (`forgot_password_pin_input`) |
| `logout.php` | Destroys session | `POST /api/v1/auth/logout` | `AuthController::logout` | `UserProfileScreen` -> `HaniXRepository.logout` |
| `index.php` | Coverflow (`FIND_IN_SET('trending', category)`) + 5 rails (`urdu_subtitles`, `all`, `coming_soon`, `fan_dubbing`, `premium`) | `GET /api/v1/home`, `/home/banners`, `/home/featured`, `/home/latest`, `/home/categories` | `HomeController` (`MovieModel`, `SeriesModel`) | `HomeScreen` (Trending, Urdu Subtitles 16:9, All, Coming Soon, Hanix Originals, Premium) |
| `search.php` | `SELECT * FROM movies WHERE title LIKE ?` + `SELECT * FROM series WHERE series_name LIKE ?` | `GET /api/v1/search?q={query}` | `SearchController::search` | `SearchScreen` |
| `series.php` & `view_series.php` | Lists `series` and groups `series_seasons` by `series_name` and `season_name` | `GET /api/v1/series`, `/series/{id}`, `/series/{id}/seasons`, `/seasons/{id}/episodes`, `/episodes/{id}` | `SeriesController` (`SeriesModel`) | `SeriesScreen`, `SeriesDetailsScreen`, `SeasonSelectionScreen`, `EpisodeListScreen` |
| `run_video.php` | Checks `is_premium` against `premium_users`; resolves `video_path` (MP4), `video_link` (YouTube/Rumble/Dailymotion/Google Drive `/preview`), `server1_link` (Server 2), and Recommended movies | `POST /api/v1/playback/authorize` | `PlaybackController::authorize` (`PlaybackService`) | `VideoPlayerScreen` (Native Media3 ExoPlayer + Embedded WebView + Server 1/2 Switcher) |
| `check_premium.php`, `add_premium.php`, `unlock_vip.php` | Queries/upserts `premium_users (email, expiry_date)` + JazzCash (`03294806457` — Muhammad Hasnain) & WhatsApp (`923160450074`) | `GET /api/v1/vip/status`, `GET /api/v1/vip/plans` | `MembershipController` (`VipMiddleware`) | `VipMembershipScreen` (`jazzcash_payment_card`) |
| `mind_game.php` | 20 Urdu/General Knowledge trivia questions, picks 10 at random | `GET /api/v1/mind-game/questions` | `CommunityController::mindGameQuestions` | `UserProfileScreen` (`mind_game_card`) |
| `studio.php` & `upload.php` | Multipart audio/video upload to `uploads/auditions/` + `INSERT INTO auditions (file_name)` (max 10 MB, 20/day) | `POST /api/v1/studio/audition` | `CommunityController::uploadAudition` | `UserProfileScreen` (`dubbing_studio_card`) |
| `help.php`, `footer.php`, `sufyan.php` | Official WhatsApp Support, WhatsApp Channel, YouTube, Instagram, TikTok, and Platform Updates | `GET /api/v1/updates` & `GET /api/v1/app/settings` | `CommunityController::updates` & `AppController::settings` | `UserProfileScreen` (`help_social_links_card`) |
| `shahkot.php`, `edit_movie.php`, `edit_series.php` | Admin panel for Movies, Series, Seasons, Episodes, and VIP activation | Preserved intact + optional `shahkot_vip_bunny_module.php` | `shahkot_extensions/shahkot_vip_bunny_module.php` | Managed on web admin panel |
