# HaniX Urdu — Native Android Application

A production-ready native Android entertainment & streaming client for **HaniX Urdu** built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM Clean Architecture**, **Retrofit + OkHttp (HTTPS TLS 1.3)**, **Room**, **Android Keystore (AES-256-GCM)**, and **AndroidX Media3 ExoPlayer (HLS Adaptive Streaming for Bunny Stream)**.

---

## 1. Architecture & Shared Hostinger Database Principle

- **Shared Backend**: Connects to your existing Hostinger PHP + MySQL website via the proposed REST API v1 contract (`https://hanixurdu.com/api/v1/`).
- **Non-Destructive Coexistence**: Does **not** replace or modify your existing PHP website (`index.php`, `series.php`, `view_series.php`, `run_video.php`, etc.) or your admin panel (`shahkot.php`).
- **No Separate Production Database**: Shares User accounts, Movies, Series, Seasons, Episodes, Posters, and VIP memberships with the website.
- **Honest Backend Integration State**: Until the `/api/v1/` PHP controller endpoints are deployed on Hostinger, the app transparently identifies pending backend endpoints (`RemoteDataState.PendingBackend`) along with their legacy PHP counterparts rather than displaying fake sample content or faking authentication/VIP/playback.

---

## 2. Implemented Screens (23 Total)

1. **Splash Screen** (`SplashScreen`)
2. **Onboarding** (`OnboardingScreen`)
3. **Login** (`LoginScreen` — `POST /auth/login`)
4. **Registration** (`RegisterScreen` — `POST /auth/register`)
5. **Forgot Password** (`ForgotPasswordScreen` — `POST /auth/forgot-password`)
6. **Home** (`HomeScreen` — `GET /home`, `/home/banners`, `/home/featured`, `/home/latest`, `/home/categories`)
7. **Movies** (`MoviesScreen` — `GET /movies`, `/movies/categories`)
8. **Series** (`SeriesScreen` — `GET /series`)
9. **Categories** (`CategoriesScreen` — `GET /home/categories`)
10. **Search** (`SearchScreen` — `GET /search?q={query}`)
11. **Movie Details** (`MovieDetailsScreen` — `GET /movies/{id}`)
12. **Series Details** (`SeriesDetailsScreen` — `GET /series/{id}`)
13. **Season Selection** (`SeasonSelectionScreen` — `GET /series/{id}/seasons`)
14. **Episode List** (`EpisodeListScreen` — `GET /seasons/{id}/episodes`, `GET /episodes/{id}`)
15. **Video Player** (`VideoPlayerScreen` — Bunny Stream HLS via `POST /playback/authorize`, `/playback/progress`, `/playback/complete`)
16. **VIP Membership** (`VipMembershipScreen` — `GET /vip/status`, `GET /vip/plans`, `POST /vip/verify`)
17. **User Profile** (`UserProfileScreen` — `GET /user/profile`, `PUT /user/profile`)
18. **Watch History** (`WatchHistoryScreen` — `GET /user/history`, `DELETE /user/history` + Room cache)
19. **Continue Watching** (`ContinueWatchingScreen` — Resume playback queue)
20. **My Downloads** (`MyDownloadsScreen` — `POST /downloads/authorize`, `GET /downloads`, `DELETE /downloads/{id}`)
21. **Notifications** (`NotificationsScreen`)
22. **App Settings** (`AppSettingsScreen` — `GET /app/settings`, `GET /app/version`, HTTPS API Base URL configuration)
23. **About HaniX Urdu & API Checklist** (`AboutHaniXScreen`)

---

## 3. Security Highlights

- **HTTPS Only**: `network_security_config.xml` and `AndroidManifest.xml` (`usesCleartextTraffic="false"`) enforce TLS 1.2/1.3 with system trust anchors.
- **Android Keystore AES-256-GCM**: Access tokens, refresh tokens, and offline downloaded media files in `filesDir/encrypted_downloads/` are encrypted with hardware-backed keys in `AndroidKeyStore`.
- **Zero Embedded Secrets**: No MySQL credentials, admin credentials, or Bunny Stream API keys are stored inside the APK.
- **R8 Obfuscation Rules**: `app/proguard-rules.pro` preserves Moshi/Retrofit/Room serialization contracts while obfuscating internal source filenames.

---

## 4. Build & Testing Instructions

### Building Debug & Release APKs
```bash
gradle :app:assembleDebug
```

### Running Local JVM & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Configuring the PHP REST API Base URL
- Default Base URL: `https://hanixurdu.com/api/v1/` (defined in `app/build.gradle.kts` via `BuildConfig.HANIX_API_BASE_URL`).
- You can also test a staging HTTPS URL at runtime from **Profile → App Settings & API Base URL**.

See [`BACKEND_API_INTEGRATION.md`](./BACKEND_API_INTEGRATION.md) for the complete Hostinger PHP + MySQL & Bunny Stream backend implementation guide.
