package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Consistent JSON response envelope required by Section 6 of the HaniX Urdu specification:
 * {
 *   "success": true,
 *   "message": "Content loaded successfully",
 *   "data": {}
 * }
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null
)

// =========================================================================
// 7. CORE CONTENT DATA MODELS (App-Level Models with Nullable Backend Fields)
// =========================================================================

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profileImage") val profileImage: String? = null,
    @Json(name = "vipStatus") val vipStatus: Boolean? = false,
    @Json(name = "vipExpiry") val vipExpiry: String? = null
)

@JsonClass(generateAdapter = true)
data class Movie(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = false,
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class Series(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "category") val category: String? = null
)

@JsonClass(generateAdapter = true)
data class Season(
    @Json(name = "id") val id: String,
    @Json(name = "seriesId") val seriesId: String,
    @Json(name = "seasonName") val seasonName: String? = null
)

@JsonClass(generateAdapter = true)
data class Episode(
    @Json(name = "id") val id: String,
    @Json(name = "seasonId") val seasonId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = false,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "playbackAvailability") val playbackAvailability: String? = null
)

// =========================================================================
// AUTHENTICATION DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "deviceName") val deviceName: String = "Android Native Client"
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthSessionPayload(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "expiresIn") val expiresIn: Long? = 3600L,
    @Json(name = "user") val user: User
)

// =========================================================================
// HOME, SEARCH & CATEGORIES DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class BannerItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "backdropUrl") val backdropUrl: String? = null,
    @Json(name = "contentType") val contentType: String = "movie", // "movie" or "series"
    @Json(name = "contentId") val contentId: String,
    @Json(name = "isPremium") val isPremium: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class CategoryItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "itemCount") val itemCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class HomePayload(
    @Json(name = "banners") val banners: List<BannerItem>? = emptyList(),
    @Json(name = "featuredMovies") val featuredMovies: List<Movie>? = emptyList(),
    @Json(name = "featuredSeries") val featuredSeries: List<Series>? = emptyList(),
    @Json(name = "latestMovies") val latestMovies: List<Movie>? = emptyList(),
    @Json(name = "latestSeries") val latestSeries: List<Series>? = emptyList(),
    @Json(name = "categories") val categories: List<CategoryItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class SearchPayload(
    @Json(name = "query") val query: String? = null,
    @Json(name = "movies") val movies: List<Movie>? = emptyList(),
    @Json(name = "series") val series: List<Series>? = emptyList()
)

// =========================================================================
// VIP MEMBERSHIP DTOs (Always Verified by PHP Backend)
// =========================================================================

@JsonClass(generateAdapter = true)
data class VipStatusPayload(
    @Json(name = "isVip") val isVip: Boolean = false,
    @Json(name = "planName") val planName: String? = null,
    @Json(name = "vipExpiry") val vipExpiry: String? = null,
    @Json(name = "entitlementSignature") val entitlementSignature: String? = null
)

@JsonClass(generateAdapter = true)
data class VipPlan(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "durationDays") val durationDays: Int,
    @Json(name = "priceFormatted") val priceFormatted: String,
    @Json(name = "features") val features: List<String>? = emptyList(),
    @Json(name = "badge") val badge: String? = null
)

@JsonClass(generateAdapter = true)
data class VipVerifyRequest(
    @Json(name = "planId") val planId: String,
    @Json(name = "voucherOrTransactionReference") val voucherOrTransactionReference: String
)

// =========================================================================
// BUNNY STREAM PLAYBACK & DOWNLOAD AUTHORIZATION DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class PlaybackAuthorizeRequest(
    @Json(name = "contentType") val contentType: String, // "movie" or "episode"
    @Json(name = "contentId") val contentId: String
)

/**
 * Returned exclusively by POST /playback/authorize after the PHP backend:
 * 1. Authenticates the user
 * 2. Verifies content availability
 * 3. Checks VIP requirements
 * 4. Checks access permissions
 * 5. Generates a short-lived Bunny Stream playback token
 * 6. Returns only authorized playback information (never Bunny API keys)
 */
@JsonClass(generateAdapter = true)
data class PlaybackAuthorizationPayload(
    @Json(name = "playbackSessionId") val playbackSessionId: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "isAuthorized") val isAuthorized: Boolean = false,
    @Json(name = "requiresVip") val requiresVip: Boolean = false,
    @Json(name = "processingStatus") val processingStatus: String = "READY", // READY, PROCESSING, UNAVAILABLE
    @Json(name = "authorizedHlsUrl") val authorizedHlsUrl: String? = null,
    @Json(name = "tokenExpiresAtEpochSeconds") val tokenExpiresAtEpochSeconds: Long? = null,
    @Json(name = "resumePositionMs") val resumePositionMs: Long? = 0L,
    @Json(name = "customHeaders") val customHeaders: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class PlaybackProgressRequest(
    @Json(name = "playbackSessionId") val playbackSessionId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "positionMs") val positionMs: Long,
    @Json(name = "durationMs") val durationMs: Long
)

@JsonClass(generateAdapter = true)
data class PlaybackCompleteRequest(
    @Json(name = "playbackSessionId") val playbackSessionId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String
)

@JsonClass(generateAdapter = true)
data class DownloadAuthorizeRequest(
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "qualityTier") val qualityTier: String = "720p"
)

@JsonClass(generateAdapter = true)
data class DownloadAuthorizationPayload(
    @Json(name = "downloadId") val downloadId: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "title") val title: String,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "signedDownloadStreamUrl") val signedDownloadStreamUrl: String,
    @Json(name = "licenseExpiresAtEpochSeconds") val licenseExpiresAtEpochSeconds: Long,
    @Json(name = "requiresVipEntitlement") val requiresVipEntitlement: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DownloadRecordDto(
    @Json(name = "id") val id: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "title") val title: String,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "licenseExpiresAtEpochSeconds") val licenseExpiresAtEpochSeconds: Long,
    @Json(name = "createdAt") val createdAt: String? = null
)

// =========================================================================
// USER PROFILE, WATCH HISTORY, APP SETTINGS & VERSION DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "name") val name: String,
    @Json(name = "profileImage") val profileImage: String? = null
)

@JsonClass(generateAdapter = true)
data class WatchHistoryItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "contentType") val contentType: String, // "movie" or "episode"
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "positionMs") val positionMs: Long = 0L,
    @Json(name = "durationMs") val durationMs: Long = 1L,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "isCompleted") val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AppSettingsPayload(
    @Json(name = "maintenanceMode") val maintenanceMode: Boolean = false,
    @Json(name = "supportEmail") val supportEmail: String? = null,
    @Json(name = "privacyPolicyUrl") val privacyPolicyUrl: String? = null,
    @Json(name = "termsUrl") val termsUrl: String? = null,
    @Json(name = "bunnyStreamEnabled") val bunnyStreamEnabled: Boolean = false,
    @Json(name = "vipSubscriptionsEnabled") val vipSubscriptionsEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AppVersionPayload(
    @Json(name = "latestVersionCode") val latestVersionCode: Int = 1,
    @Json(name = "latestVersionName") val latestVersionName: String = "1.0.0",
    @Json(name = "forceUpdate") val forceUpdate: Boolean = false,
    @Json(name = "changelog") val changelog: String? = null
)

// =========================================================================
// HONEST REMOTE STATE SEALED HIERARCHY (Section 12 Compliance)
// =========================================================================

sealed interface RemoteDataState<out T> {
    data object Idle : RemoteDataState<Nothing>
    data class Loading(val endpoint: String) : RemoteDataState<Nothing>
    data class Success<T>(val data: T, val endpoint: String) : RemoteDataState<T>
    data class Empty(val endpoint: String, val message: String) : RemoteDataState<Nothing>
    data class PendingBackend(
        val endpoint: String,
        val httpCode: Int?,
        val legacyPhpFile: String,
        val details: String
    ) : RemoteDataState<Nothing>
    data class Offline<T>(
        val endpoint: String,
        val cachedData: T? = null,
        val message: String
    ) : RemoteDataState<T>
    data class Error(
        val endpoint: String,
        val httpCode: Int? = null,
        val message: String
    ) : RemoteDataState<Nothing>
}

// =========================================================================
// PROPOSED PHP REST API v1 ENDPOINT CATALOG & HOSTINGER MAPPING
// =========================================================================

data class ProposedEndpointSpec(
    val category: String,
    val method: String,
    val path: String,
    val legacyPhpReference: String,
    val securityRequirement: String
)

object BackendEndpointCatalog {
    val endpoints: List<ProposedEndpointSpec> = listOf(
        // Authentication
        ProposedEndpointSpec("Authentication", "POST", "/auth/login", "login_process.php", "password_verify() + issue short-lived Bearer JWT & refresh token"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/register", "signup.php", "password_hash(PASSWORD_ARGON2ID/BCRYPT) + prepared MySQL INSERT"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/logout", "header.php / session destroy", "Revoke active refresh token in MySQL user_tokens table"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/refresh", "New token rotation handler", "Validate refresh token & rotate Bearer token pair"),
        ProposedEndpointSpec("Authentication", "GET", "/auth/me", "config.php + session user query", "Extract user ID from Bearer token; never trust client user_id"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/forgot-password", "forgot_password.php", "Generate time-limited reset token via Hostinger SMTP"),
        // Home
        ProposedEndpointSpec("Home", "GET", "/home", "index.php", "Aggregate banners, featured, latest & categories in single JSON response"),
        ProposedEndpointSpec("Home", "GET", "/home/banners", "index.php", "Query hero spotlight movies/series with poster/backdrop URLs"),
        ProposedEndpointSpec("Home", "GET", "/home/featured", "index.php", "Query curated featured movies & series"),
        ProposedEndpointSpec("Home", "GET", "/home/latest", "index.php", "Query newest uploads ordered by created/id DESC"),
        ProposedEndpointSpec("Home", "GET", "/home/categories", "header.php / index.php", "Distinct genres/categories with item counts"),
        // Movies
        ProposedEndpointSpec("Movies", "GET", "/movies", "index.php / edit_movie.php", "Paginated/filtered movie list mapped to Movie model"),
        ProposedEndpointSpec("Movies", "GET", "/movies/{id}", "run_video.php / edit_movie.php", "Movie metadata without exposing raw video file URL"),
        ProposedEndpointSpec("Movies", "GET", "/movies/categories", "index.php", "Movie category taxonomy"),
        // Series
        ProposedEndpointSpec("Series", "GET", "/series", "series.php", "All Urdu & dubbed series listings"),
        ProposedEndpointSpec("Series", "GET", "/series/{id}", "view_series.php / edit_series.php", "Series metadata & synopsis"),
        ProposedEndpointSpec("Series", "GET", "/series/{id}/seasons", "view_series.php", "Seasons belonging to seriesId"),
        ProposedEndpointSpec("Series", "GET", "/seasons/{id}/episodes", "view_series.php", "Episodes ordered by episode number for seasonId"),
        ProposedEndpointSpec("Series", "GET", "/episodes/{id}", "run_video.php", "Episode details & playbackAvailability status"),
        // Search
        ProposedEndpointSpec("Search", "GET", "/search?q={query}", "search.php", "Parameterized LIKE/FULLTEXT search across movies & series"),
        // VIP
        ProposedEndpointSpec("VIP", "GET", "/vip/status", "check_premium.php", "Server-side verification of vipStatus & vipExpiry"),
        ProposedEndpointSpec("VIP", "GET", "/vip/plans", "add_premium.php", "Available VIP membership tiers & pricing"),
        ProposedEndpointSpec("VIP", "POST", "/vip/verify", "unlock_vip.php / add_premium.php", "Verify voucher/payment server-side & update user VIP expiry"),
        // Playback
        ProposedEndpointSpec("Playback", "POST", "/playback/authorize", "run_video.php + check_premium.php", "Authenticate user, check VIP, sign short-lived Bunny HLS URL"),
        ProposedEndpointSpec("Playback", "POST", "/playback/progress", "New watch_progress handler", "Upsert playback positionMs for authenticated user"),
        ProposedEndpointSpec("Playback", "POST", "/playback/complete", "New watch_progress handler", "Mark movie/episode completed for authenticated user"),
        // Downloads
        ProposedEndpointSpec("Downloads", "POST", "/downloads/authorize", "run_video.php + check_premium.php", "Authorize offline encrypted download & issue expiring license"),
        ProposedEndpointSpec("Downloads", "GET", "/downloads", "New user_downloads handler", "List active authorized downloads for authenticated user"),
        ProposedEndpointSpec("Downloads", "DELETE", "/downloads/{id}", "New user_downloads handler", "Revoke offline download entitlement record"),
        // User
        ProposedEndpointSpec("User", "GET", "/user/profile", "config.php + users table", "Return authenticated user's profile (no password/PIN fields)"),
        ProposedEndpointSpec("User", "PUT", "/user/profile", "users table update", "Update authenticated user's display name / avatar"),
        ProposedEndpointSpec("User", "GET", "/user/history", "watch_history table", "Return server-synced watch history & continue watching list"),
        ProposedEndpointSpec("User", "DELETE", "/user/history", "watch_history table", "Clear authenticated user's watch history"),
        // App Settings
        ProposedEndpointSpec("App Settings", "GET", "/app/settings", "config.php / shahkot.php", "Public app flags, maintenance mode, Bunny Stream readiness"),
        ProposedEndpointSpec("App Settings", "GET", "/app/version", "config.php", "Minimum/latest Android APK version check")
    )
}
