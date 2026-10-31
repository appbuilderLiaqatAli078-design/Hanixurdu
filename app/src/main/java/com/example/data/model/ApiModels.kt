package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Strict Two-Tier Membership Model required by Section 4 of the HaniX Urdu PRD.
 * Only FREE and VIP exist. VVIP, PLUS, MAX, and all other tiers are strictly prohibited.
 */
enum class MembershipTier {
    FREE,
    VIP;

    companion object {
        fun resolve(
            rawMembership: String?,
            vipStatusFlag: Boolean?,
            vipExpiryEpochSeconds: Long?,
            vipExpiryDateStr: String?,
            nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
        ): MembershipTier {
            val isMarkedVip = rawMembership.equals("VIP", ignoreCase = true) || vipStatusFlag == true
            if (!isMarkedVip) return FREE

            // Check epoch expiry if provided
            if (vipExpiryEpochSeconds != null) {
                return if (vipExpiryEpochSeconds > nowEpochSeconds) VIP else FREE
            }

            // Check MySQL / ISO date string (e.g. "2026-12-31 23:59:59" or "2026-12-31") from premium_users.expiry_date
            if (!vipExpiryDateStr.isNullOrBlank()) {
                val parsedEpoch = parseMysqlExpiryToEpochSeconds(vipExpiryDateStr)
                if (parsedEpoch != null && parsedEpoch <= nowEpochSeconds) {
                    return FREE
                }
            }

            return VIP
        }

        fun parseMysqlExpiryToEpochSeconds(dateStr: String): Long? {
            val trimmed = dateStr.trim()
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsed = sdf.parse(trimmed)
                    if (parsed != null) {
                        // If date-only (yyyy-MM-dd), treat end of that day as expiry
                        val extraSeconds = if (pattern == "yyyy-MM-dd") 86399L else 0L
                        return (parsed.time / 1000L) + extraSeconds
                    }
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}

/**
 * Common JSON response envelope required by Section 6:
 * {
 *   "success": true,
 *   "message": "Success",
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
// CORE CONTENT & USER MODELS (Compatible with Existing Hostinger MySQL Schema)
// =========================================================================

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profileImage") val profileImage: String? = null,
    @Json(name = "profile_image") val legacyProfileImage: String? = null,
    @Json(name = "membership") val membership: String? = "FREE", // "FREE" or "VIP" only
    @Json(name = "vipStatus") val vipStatus: Boolean? = false,
    @Json(name = "is_vip") val legacyIsVip: Boolean? = null,
    @Json(name = "vipExpiry") val vipExpiry: String? = null,
    @Json(name = "expiry_date") val legacyExpiryDate: String? = null,
    @Json(name = "vipExpiryEpochSeconds") val vipExpiryEpochSeconds: Long? = null
) {
    val resolvedProfileImage: String?
        get() = profileImage?.takeIf { it.isNotBlank() } ?: legacyProfileImage?.takeIf { it.isNotBlank() }

    val resolvedExpiryDate: String?
        get() = vipExpiry?.takeIf { it.isNotBlank() } ?: legacyExpiryDate?.takeIf { it.isNotBlank() }

    fun effectiveMembershipTier(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): MembershipTier {
        return MembershipTier.resolve(
            rawMembership = membership,
            vipStatusFlag = vipStatus == true || legacyIsVip == true,
            vipExpiryEpochSeconds = vipExpiryEpochSeconds,
            vipExpiryDateStr = resolvedExpiryDate,
            nowEpochSeconds = nowEpochSeconds
        )
    }

    val isActiveVip: Boolean
        get() = effectiveMembershipTier() == MembershipTier.VIP
}

/**
 * Movie model compatible with both REST DTOs and existing `movies` MySQL columns:
 * (title, description, poster_path, video_path, video_link, category, series_name,
 *  season_name, server1_link, server2_link, is_premium, custom_url)
 */
@JsonClass(generateAdapter = true)
data class Movie(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "series_name") val seriesName: String? = null,
    @Json(name = "season_name") val seasonName: String? = null,
    @Json(name = "contentType") val contentType: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = null,
    @Json(name = "is_premium") val isPremiumSnake: Boolean? = null,
    @Json(name = "has_video") val hasVideo: Boolean? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "custom_url") val customUrl: String? = null,
    @Json(name = "bunny_video_id") val bunnyVideoId: String? = null
) {
    val resolvedId: String
        get() = id.ifBlank { customUrl ?: title }

    val resolvedPoster: String?
        get() = poster?.takeIf { it.isNotBlank() } ?: posterPath?.takeIf { it.isNotBlank() }

    val resolvedIsVip: Boolean
        get() = (isPremium == true) || (isPremiumSnake == true) ||
            (category?.contains("premium", ignoreCase = true) == true) ||
            (category?.contains("vip", ignoreCase = true) == true)

    val resolvedIsSeriesItem: Boolean
        get() = contentType.equals("series", ignoreCase = true)

    val resolvedHasVideo: Boolean
        get() = hasVideo ?: true

    val membershipBadge: String
        get() = if (resolvedIsVip) "VIP" else "FREE"
}

/**
 * Series model compatible with existing `series` MySQL columns:
 * (category, series_name, description, trailer_link, poster_path, index_title, index_poster, custom_url)
 */
@JsonClass(generateAdapter = true)
data class Series(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String? = null,
    @Json(name = "series_name") val seriesName: String? = null,
    @Json(name = "index_title") val indexTitle: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "index_poster") val indexPoster: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "trailer_link") val trailerLink: String? = null,
    @Json(name = "custom_url") val customUrl: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = null
) {
    val resolvedId: String
        get() = id.ifBlank { customUrl ?: seriesName ?: title ?: "" }

    val resolvedTitle: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: seriesName?.takeIf { it.isNotBlank() }
            ?: indexTitle?.takeIf { it.isNotBlank() }
            ?: "Untitled Series"

    /**
     * Preserves existing PHP `index.php` poster fallback logic: `index_poster` -> `poster_path` -> `poster`.
     */
    val resolvedPoster: String?
        get() = indexPoster?.takeIf { it.isNotBlank() }
            ?: posterPath?.takeIf { it.isNotBlank() }
            ?: poster?.takeIf { it.isNotBlank() }

    val resolvedIsVip: Boolean
        get() = isPremium == true ||
            category?.contains("premium", ignoreCase = true) == true ||
            category?.contains("vip", ignoreCase = true) == true
}

/**
 * Season model compatible with existing `series_seasons` and `movies` distinct season queries:
 * (series_name, season_name)
 */
@JsonClass(generateAdapter = true)
data class Season(
    @Json(name = "id") val id: String = "",
    @Json(name = "seriesId") val seriesId: String? = null,
    @Json(name = "series_name") val seriesName: String? = null,
    @Json(name = "seasonName") val seasonName: String? = null,
    @Json(name = "season_name") val seasonNameSnake: String? = null
) {
    val resolvedId: String
        get() = id.ifBlank { seasonName ?: seasonNameSnake ?: "Season 1" }

    val resolvedSeriesKey: String
        get() = seriesId?.takeIf { it.isNotBlank() } ?: seriesName ?: ""

    val resolvedSeasonName: String
        get() = seasonName?.takeIf { it.isNotBlank() }
            ?: seasonNameSnake?.takeIf { it.isNotBlank() }
            ?: "Season $resolvedId"
}

/**
 * Episode model compatible with `movies` rows where `series_name` & `season_name` are populated (`series.php?ajax=1`).
 */
@JsonClass(generateAdapter = true)
data class Episode(
    @Json(name = "id") val id: String = "",
    @Json(name = "seasonId") val seasonId: String = "",
    @Json(name = "series_name") val seriesName: String? = null,
    @Json(name = "season_name") val seasonName: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = null,
    @Json(name = "is_premium") val isPremiumSnake: Boolean? = null,
    @Json(name = "has_video") val hasVideo: Boolean? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "custom_url") val customUrl: String? = null,
    @Json(name = "playbackAvailability") val playbackAvailability: String? = null
) {
    val resolvedPoster: String?
        get() = poster?.takeIf { it.isNotBlank() } ?: posterPath?.takeIf { it.isNotBlank() }

    val resolvedIsVip: Boolean
        get() = (isPremium == true) || (isPremiumSnake == true)

    val resolvedHasVideo: Boolean
        get() = hasVideo ?: (playbackAvailability != "UNAVAILABLE")

    val membershipBadge: String
        get() = if (resolvedIsVip) "VIP" else "FREE"
}

// =========================================================================
// AUTHENTICATION DTOs (`login_process.php`, `signup.php`, `forgot_password.php`)
// =========================================================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "deviceName") val deviceName: String = "HaniX-Urdu-Android"
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "secret_pin") val secretPin: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String,
    @Json(name = "secret_pin") val secretPin: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordVerifyRequest(
    @Json(name = "email") val email: String,
    @Json(name = "secret_pin") val secretPin: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordVerifyResponse(
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "reset_token") val resetTokenSnake: String? = null,
    @Json(name = "resetToken") val resetToken: String? = null
) {
    val resolvedResetToken: String?
        get() = resetToken?.takeIf { it.isNotBlank() } ?: resetTokenSnake?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class ForgotPasswordResetRequest(
    @Json(name = "email") val email: String,
    @Json(name = "new_password") val newPassword: String,
    @Json(name = "reset_token") val resetToken: String? = null,
    @Json(name = "secret_pin") val secretPin: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "refresh_token") val refreshTokenSnake: String = refreshToken
)

@JsonClass(generateAdapter = true)
data class AuthSessionPayload(
    @Json(name = "accessToken") val accessToken: String = "",
    @Json(name = "access_token") val accessTokenSnake: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "refresh_token") val refreshTokenSnake: String? = null,
    @Json(name = "expiresIn") val expiresIn: Long? = 3600L,
    @Json(name = "user") val user: User
) {
    val resolvedAccessToken: String
        get() = accessToken.ifBlank { accessTokenSnake.orEmpty() }

    val resolvedRefreshToken: String?
        get() = refreshToken?.takeIf { it.isNotBlank() } ?: refreshTokenSnake?.takeIf { it.isNotBlank() }
}

// =========================================================================
// HOME, SEARCH & CATEGORIES DTOs (`index.php` 5 Category Rails + Coverflow)
// =========================================================================

@JsonClass(generateAdapter = true)
data class BannerItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "backdropUrl") val backdropUrl: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "contentType") val contentType: String = "movie", // "movie" or "series"
    @Json(name = "contentId") val contentId: String,
    @Json(name = "seriesName") val seriesName: String? = null,
    @Json(name = "customUrl") val customUrl: String? = null,
    @Json(name = "isPremium") val isPremium: Boolean? = false,
    @Json(name = "is_premium") val isPremiumSnake: Boolean? = null
) {
    val resolvedIsVip: Boolean
        get() = (isPremium == true) || (isPremiumSnake == true)
}

@JsonClass(generateAdapter = true)
data class CategoryItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "itemCount") val itemCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class HomePayload(
    @Json(name = "banners") val banners: List<BannerItem>? = emptyList(),
    @Json(name = "trending_movies") val trendingMovies: List<Movie>? = emptyList(),
    @Json(name = "trending_series") val trendingSeries: List<Series>? = emptyList(),
    @Json(name = "featuredMovies") val featuredMovies: List<Movie>? = emptyList(),
    @Json(name = "featuredSeries") val featuredSeries: List<Series>? = emptyList(),
    @Json(name = "latestMovies") val latestMovies: List<Movie>? = emptyList(),
    @Json(name = "latestSeries") val latestSeries: List<Series>? = emptyList(),
    @Json(name = "urduSubtitles") val urduSubtitles: List<Movie>? = emptyList(),
    @Json(name = "allContent") val allContent: List<Movie>? = emptyList(),
    @Json(name = "comingSoon") val comingSoon: List<Movie>? = emptyList(),
    @Json(name = "hanixOriginals") val hanixOriginals: List<Movie>? = emptyList(),
    @Json(name = "premiumContent") val premiumContent: List<Movie>? = emptyList(),
    @Json(name = "categories") val categories: List<CategoryItem>? = emptyList()
) {
    val resolvedFeaturedMovies: List<Movie>
        get() = featuredMovies?.takeIf { it.isNotEmpty() } ?: trendingMovies.orEmpty()

    val resolvedFeaturedSeries: List<Series>
        get() = featuredSeries?.takeIf { it.isNotEmpty() } ?: trendingSeries.orEmpty()
}

@JsonClass(generateAdapter = true)
data class SearchPayload(
    @Json(name = "query") val query: String? = null,
    @Json(name = "page") val page: Int? = 1,
    @Json(name = "limit") val limit: Int? = 20,
    @Json(name = "movies") val movies: List<Movie>? = emptyList(),
    @Json(name = "series") val series: List<Series>? = emptyList()
)

// =========================================================================
// MEMBERSHIP & JAZZCASH / WHATSAPP DTOs (`check_premium.php`, `index.php`, `series.php`)
// =========================================================================

@JsonClass(generateAdapter = true)
data class PaymentInfoDto(
    @Json(name = "method") val method: String = "JazzCash",
    @Json(name = "accountName") val accountName: String = "Muhammad Hasnain",
    @Json(name = "accountNumber") val accountNumber: String = "03294806457",
    @Json(name = "whatsappNumber") val whatsappNumber: String = "+923160450074",
    @Json(name = "whatsappSupportUrl") val whatsappSupportUrl: String = "https://wa.me/923160450074",
    @Json(name = "whatsappChannelUrl") val whatsappChannelUrl: String = "https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q",
    @Json(name = "instructions") val instructions: String = "Send payment via JazzCash to Muhammad Hasnain (03294806457) and share the screenshot with your registered email on WhatsApp (+92 316 0450074) for instant VIP activation."
)

@JsonClass(generateAdapter = true)
data class VipStatusPayload(
    @Json(name = "membership") val membership: String = "FREE", // "FREE" or "VIP" only
    @Json(name = "isVip") val isVip: Boolean = false,
    @Json(name = "is_vip") val isVipSnake: Boolean? = null,
    @Json(name = "vipExpiry") val vipExpiry: String? = null,
    @Json(name = "expiry_date") val legacyExpiryDate: String? = null,
    @Json(name = "vipExpiryEpochSeconds") val vipExpiryEpochSeconds: Long? = null,
    @Json(name = "paymentInfo") val paymentInfo: PaymentInfoDto? = PaymentInfoDto()
) {
    fun effectiveTier(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): MembershipTier {
        return MembershipTier.resolve(
            rawMembership = membership,
            vipStatusFlag = isVip || (isVipSnake == true),
            vipExpiryEpochSeconds = vipExpiryEpochSeconds,
            vipExpiryDateStr = vipExpiry ?: legacyExpiryDate,
            nowEpochSeconds = nowEpochSeconds
        )
    }
}

@JsonClass(generateAdapter = true)
data class VipPlan(
    @Json(name = "id") val id: String,
    @Json(name = "planType") val planType: String = "VIP", // Only "FREE" or "VIP"
    @Json(name = "name") val name: String,
    @Json(name = "durationDays") val durationDays: Int,
    @Json(name = "priceFormatted") val priceFormatted: String,
    @Json(name = "features") val features: List<String>? = emptyList(),
    @Json(name = "paymentInfo") val paymentInfo: PaymentInfoDto? = null
)

// =========================================================================
// PLAYBACK & MULTI-SERVER AUTHORIZATION DTOs (`run_video.php` + Bunny Stream)
// =========================================================================

@JsonClass(generateAdapter = true)
data class PlaybackServerDto(
    @Json(name = "id") val id: String = "main",
    @Json(name = "label") val label: String = "Main Server",
    @Json(name = "type") val type: String = "direct", // "direct", "hls", or "embed"
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class PlaybackAuthorizeRequest(
    @Json(name = "contentType") val contentType: String = "movie", // "movie" or "episode"
    @Json(name = "contentId") val contentId: String,
    @Json(name = "content_id") val contentIdSnake: String = contentId,
    @Json(name = "server") val server: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaybackAuthorizationPayload(
    @Json(name = "playbackSessionId") val playbackSessionId: String = "",
    @Json(name = "contentId") val contentId: String = "",
    @Json(name = "content_id") val contentIdSnake: String? = null,
    @Json(name = "contentType") val contentType: String = "movie",
    @Json(name = "playback_type") val playbackType: String = "hls", // "hls", "direct", or "embed"
    @Json(name = "playback_url") val playbackUrl: String? = null,
    @Json(name = "selected_server") val selectedServer: String? = "main",
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "membershipRequired") val membershipRequired: String = "FREE", // "FREE" or "VIP"
    @Json(name = "isAuthorized") val isAuthorized: Boolean = false,
    @Json(name = "requiresVip") val requiresVip: Boolean = false,
    @Json(name = "processingStatus") val processingStatus: String = "READY", // READY, PROCESSING, UNAVAILABLE
    @Json(name = "authorizedHlsUrl") val authorizedHlsUrl: String? = null,
    @Json(name = "tokenExpiresAtEpochSeconds") val tokenExpiresAtEpochSeconds: Long? = null,
    @Json(name = "resumePositionMs") val resumePositionMs: Long? = 0L,
    @Json(name = "custom_url") val customUrl: String? = null,
    @Json(name = "available_servers") val availableServers: List<PlaybackServerDto>? = emptyList(),
    @Json(name = "recommended") val recommended: List<Movie>? = emptyList(),
    @Json(name = "customHeaders") val customHeaders: Map<String, String>? = null
) {
    val resolvedPlaybackUrl: String?
        get() = authorizedHlsUrl?.takeIf { it.isNotBlank() } ?: playbackUrl?.takeIf { it.isNotBlank() }

    val resolvedIsAuthorized: Boolean
        get() = isAuthorized || !resolvedPlaybackUrl.isNullOrBlank()

    val resolvedExpiryEpochSeconds: Long?
        get() = tokenExpiresAtEpochSeconds ?: expiresAt?.let { MembershipTier.parseMysqlExpiryToEpochSeconds(it.replace("Z", "")) }
}

@JsonClass(generateAdapter = true)
data class PlaybackProgressRequest(
    @Json(name = "playbackSessionId") val playbackSessionId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "content_id") val contentIdSnake: String = contentId,
    @Json(name = "positionMs") val positionMs: Long,
    @Json(name = "durationMs") val durationMs: Long
)

@JsonClass(generateAdapter = true)
data class PlaybackCompleteRequest(
    @Json(name = "playbackSessionId") val playbackSessionId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "content_id") val contentIdSnake: String = contentId
)

@JsonClass(generateAdapter = true)
data class DownloadAuthorizeRequest(
    @Json(name = "contentType") val contentType: String,
    @Json(name = "contentId") val contentId: String,
    @Json(name = "content_id") val contentIdSnake: String = contentId,
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
    @Json(name = "requiresVipEntitlement") val requiresVipEntitlement: Boolean = true
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
// MIND GAMES QUIZ (`mind_game.php`), DUBBING STUDIO (`studio.php` / `upload.php`) & UPDATES (`sufyan.php` / `help.php`)
// =========================================================================

@JsonClass(generateAdapter = true)
data class QuizQuestionDto(
    @Json(name = "q") val question: String,
    @Json(name = "options") val options: List<String>,
    @Json(name = "answer") val answerIndex: Int
)

@JsonClass(generateAdapter = true)
data class MindGamePayload(
    @Json(name = "totalQuestions") val totalQuestions: Int = 10,
    @Json(name = "questions") val questions: List<QuizQuestionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AuditionUploadPayload(
    @Json(name = "auditionId") val auditionId: Int = 0,
    @Json(name = "userName") val userName: String = "",
    @Json(name = "whatsapp") val whatsapp: String = "",
    @Json(name = "role") val role: String = "",
    @Json(name = "filePath") val filePath: String = "",
    @Json(name = "remainingToday") val remainingToday: Int = 20
)

@JsonClass(generateAdapter = true)
data class PlatformUpdateDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "badge") val badge: String = "Update",
    @Json(name = "title") val title: String = "",
    @Json(name = "date") val date: String = "",
    @Json(name = "description") val description: String = ""
)

@JsonClass(generateAdapter = true)
data class SupportLinksDto(
    @Json(name = "whatsappSupportUrl") val whatsappSupportUrl: String = "https://wa.me/923160450074",
    @Json(name = "whatsappChannelUrl") val whatsappChannelUrl: String = "https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q",
    @Json(name = "youtubeUrl") val youtubeUrl: String = "https://youtube.com/@Hanixurdu?si=c9-0aD5e5f_P2AC-",
    @Json(name = "instagramUrl") val instagramUrl: String = "https://www.instagram.com/hanixurdu?igsh=MW54Z3k2OTR6b3B4cA==",
    @Json(name = "facebookUrl") val facebookUrl: String = "https://www.facebook.com/share/1779797B6C/",
    @Json(name = "tiktokUrl") val tiktokUrl: String = "https://www.tiktok.com/@hanixurdu",
    @Json(name = "supportEmail") val supportEmail: String = "hannnix88@gmail.com",
    @Json(name = "jazzCashName") val jazzCashName: String = "Muhammad Hasnain",
    @Json(name = "jazzCashNumber") val jazzCashNumber: String = "03294806457"
)

@JsonClass(generateAdapter = true)
data class CommunityUpdatesPayload(
    @Json(name = "updates") val updates: List<PlatformUpdateDto> = emptyList(),
    @Json(name = "support") val support: SupportLinksDto = SupportLinksDto()
)

/**
 * Exact 20-question bank from the uploaded `mind_game.php` source file (`array_rand($all_questions, 10)`).
 */
object MindGameQuestionBank {
    private val allQuestions = listOf(
        QuizQuestionDto("What is the capital of Pakistan?", listOf("Lahore", "Karachi", "Islamabad", "Peshawar"), 2),
        QuizQuestionDto("Which planet is known as the Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1),
        QuizQuestionDto("How many continents are there in the world?", listOf("5", "6", "7", "8"), 2),
        QuizQuestionDto("What is the largest mammal in the world?", listOf("Elephant", "Blue Whale", "Giraffe", "Shark"), 1),
        QuizQuestionDto("Who wrote the national anthem of Pakistan?", listOf("Allama Iqbal", "Hafeez Jalandhari", "Faiz Ahmed Faiz", "Mirza Ghalib"), 1),
        QuizQuestionDto("Which gas do plants absorb from the atmosphere?", listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"), 2),
        QuizQuestionDto("What is the boiling point of water?", listOf("50°C", "90°C", "100°C", "120°C"), 2),
        QuizQuestionDto("Which is the longest river in the world?", listOf("Amazon", "Nile", "Indus", "Yangtze"), 1),
        QuizQuestionDto("How many players are there in a cricket team?", listOf("9", "10", "11", "12"), 2),
        QuizQuestionDto("Which element has the chemical symbol O?", listOf("Gold", "Silver", "Oxygen", "Iron"), 2),
        QuizQuestionDto("What is the national flower of Pakistan?", listOf("Rose", "Jasmine", "Sunflower", "Tulip"), 1),
        QuizQuestionDto("Which ocean is the largest in the world?", listOf("Atlantic", "Indian", "Arctic", "Pacific"), 3),
        QuizQuestionDto("Who invented the light bulb?", listOf("Nikola Tesla", "Thomas Edison", "Alexander Bell", "Isaac Newton"), 1),
        QuizQuestionDto("Which country is home to the Kangaroo?", listOf("Canada", "Brazil", "Australia", "South Africa"), 2),
        QuizQuestionDto("How many bones are in the adult human body?", listOf("196", "206", "216", "226"), 1),
        QuizQuestionDto("Which is the highest mountain in the world?", listOf("K2", "Mount Everest", "Nanga Parbat", "Kilimanjaro"), 1),
        QuizQuestionDto("What is the currency of Japan?", listOf("Yuan", "Won", "Yen", "Ringgit"), 2),
        QuizQuestionDto("Which organ pumps blood in the human body?", listOf("Brain", "Lungs", "Heart", "Liver"), 2),
        QuizQuestionDto("What is the speed of light approximately?", listOf("300,000 km/s", "150,000 km/s", "1,000 km/s", "30,000 km/s"), 0),
        QuizQuestionDto("Which language has the most native speakers?", listOf("English", "Spanish", "Mandarin Chinese", "Hindi"), 2)
    )

    fun drawTenRandomQuestions(): List<QuizQuestionDto> = allQuestions.shuffled().take(10)
}

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
    @Json(name = "supportEmail") val supportEmail: String? = "hannnix88@gmail.com",
    @Json(name = "whatsappSupportUrl") val whatsappSupportUrl: String? = "https://wa.me/923160450074",
    @Json(name = "whatsappChannelUrl") val whatsappChannelUrl: String? = "https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q",
    @Json(name = "jazzCashAccountName") val jazzCashAccountName: String? = "Muhammad Hasnain",
    @Json(name = "jazzCashAccountNumber") val jazzCashAccountNumber: String? = "03294806457",
    @Json(name = "privacyPolicyUrl") val privacyPolicyUrl: String? = null,
    @Json(name = "termsUrl") val termsUrl: String? = null,
    @Json(name = "bunnyStreamEnabled") val bunnyStreamEnabled: Boolean = false,
    @Json(name = "downloadsEnabled") val downloadsEnabled: Boolean = false,
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
// REMOTE STATE HIERARCHY (Section 8 & Section 14)
// =========================================================================

sealed interface RemoteDataState<out T> {
    data object Idle : RemoteDataState<Nothing>
    data class Loading(val endpoint: String) : RemoteDataState<Nothing>
    data class Success<T>(val data: T, val endpoint: String) : RemoteDataState<T>
    data class Empty(val endpoint: String, val message: String) : RemoteDataState<Nothing>
    data class AuthRequired(val endpoint: String, val message: String) : RemoteDataState<Nothing>
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
// REQUIRED PHP REST API v1 ENDPOINT CATALOG
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
        // Authentication (7)
        ProposedEndpointSpec("Authentication", "POST", "/auth/login", "login_process.php", "Verify existing password hash via password_verify(); issue Bearer token & refresh token"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/register", "signup.php", "Hash password via password_hash(); store secret_pin; create account with FREE membership by default"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/logout", "logout.php", "Revoke active refresh token in MySQL api_auth_tokens table"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/refresh", "login_process.php", "Validate refresh token & rotate Bearer token pair"),
        ProposedEndpointSpec("Authentication", "GET", "/auth/me", "config.php + users + premium_users", "Resolve authenticated user from Bearer token; compute FREE or VIP from premium_users.expiry_date by email"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/forgot-password/verify", "forgot_password.php (Step 1)", "Verify registered email + secret_pin in users table and issue short-lived reset_token"),
        ProposedEndpointSpec("Authentication", "POST", "/auth/forgot-password/reset", "forgot_password.php (Step 2)", "Update users.password with password_hash(new_password, PASSWORD_DEFAULT) and revoke tokens"),
        // Home (5)
        ProposedEndpointSpec("Home", "GET", "/home", "index.php", "Load coverflow banners, urduSubtitles (16:9), allContent, comingSoon, hanixOriginals (fan_dubbing), premiumContent"),
        ProposedEndpointSpec("Home", "GET", "/home/banners", "index.php", "Query FIND_IN_SET('trending', category) series and standalone movies"),
        ProposedEndpointSpec("Home", "GET", "/home/featured", "index.php", "Query featured movies & series from MySQL"),
        ProposedEndpointSpec("Home", "GET", "/home/latest", "index.php", "Query latest standalone movies ordered by ID DESC"),
        ProposedEndpointSpec("Home", "GET", "/home/categories", "index.php / header.php", "Return official HaniX Urdu category rails (trending, urdu_subtitles, all, coming_soon, fan_dubbing, premium)"),
        // Movies (3)
        ProposedEndpointSpec("Movies", "GET", "/movies", "index.php / edit_movie.php", "Query real movies table (title, description, poster_path, category, is_premium, custom_url)"),
        ProposedEndpointSpec("Movies", "GET", "/movies/{id}", "run_video.php / edit_movie.php", "Return movie details by id/custom_url without raw video links"),
        ProposedEndpointSpec("Movies", "GET", "/movies/categories", "index.php", "Return distinct movie categories"),
        // Series (5)
        ProposedEndpointSpec("Series", "GET", "/series", "series.php / view_series.php", "Query real series table (series_name, index_title, index_poster, poster_path, category, custom_url)"),
        ProposedEndpointSpec("Series", "GET", "/series/{id}", "series.php / edit_series.php", "Return series details by id, series_name, or custom_url"),
        ProposedEndpointSpec("Series", "GET", "/series/{id}/seasons", "series.php (series_seasons + movies)", "Query distinct season_name from movies and series_seasons"),
        ProposedEndpointSpec("Series", "GET", "/series/{id}/episodes?season={season}", "series.php?ajax=1", "Query episodes matching series_name and season_name ordered by id ASC"),
        ProposedEndpointSpec("Series", "GET", "/episodes/{id}", "run_video.php", "Return single episode metadata and FREE/VIP status"),
        // Search (1)
        ProposedEndpointSpec("Search", "GET", "/search?q={query}", "search.php", "Prepared PDO search across series and standalone movies"),
        // Membership (2)
        ProposedEndpointSpec("Membership", "GET", "/vip/status", "check_premium.php (premium_users)", "Verify user's FREE or VIP status via SELECT expiry_date FROM premium_users WHERE email = ? AND expiry_date > NOW()"),
        ProposedEndpointSpec("Membership", "GET", "/vip/plans", "index.php / series.php / add_premium.php", "Return FREE & VIP plans (1 Month Rs. 149/250, 6 Months Rs. 599, 1 Year Rs. 999) + JazzCash & WhatsApp activation info"),
        // Playback (3)
        ProposedEndpointSpec("Playback", "POST", "/playback/authorize", "run_video.php + check_premium.php", "Validate Bearer token, check FREE/VIP + expiry_date, return Main Server & Server 2 (direct MP4, HLS, or YouTube/Drive/Rumble embed)"),
        ProposedEndpointSpec("Playback", "POST", "/playback/progress", "watch_history", "Record playback positionMs for authenticated user"),
        ProposedEndpointSpec("Playback", "POST", "/playback/complete", "watch_history", "Mark movie or episode completed for authenticated user"),
        // Downloads (3)
        ProposedEndpointSpec("Downloads", "POST", "/downloads/authorize", "run_video.php + check_premium.php", "Verify VIP membership + issue expiring download stream URL"),
        ProposedEndpointSpec("Downloads", "GET", "/downloads", "user_downloads", "List active VIP download entitlements for authenticated user"),
        ProposedEndpointSpec("Downloads", "DELETE", "/downloads/{id}", "user_downloads", "Revoke download entitlement record"),
        // Community & Studio (3)
        ProposedEndpointSpec("Community", "GET", "/mind-game/questions", "mind_game.php", "Return 10 randomized quiz questions from the 20-question bank"),
        ProposedEndpointSpec("Community", "POST", "/studio/audition", "studio.php + upload.php", "Authenticated multipart upload to auditions table (max 20/day, max 10 MB audio/video)"),
        ProposedEndpointSpec("Community", "GET", "/updates", "sufyan.php + help.php + footer.php", "Return platform updates, WhatsApp support (+923160450074), WhatsApp Channel, and social links"),
        // User (4)
        ProposedEndpointSpec("User", "GET", "/user/profile", "users + premium_users", "Return authenticated user's profile with FREE or VIP status (no password/secret_pin)"),
        ProposedEndpointSpec("User", "PUT", "/user/profile", "users table update", "Update authenticated user's display name"),
        ProposedEndpointSpec("User", "GET", "/user/history", "watch_history table", "Return authenticated user's watch history"),
        ProposedEndpointSpec("User", "DELETE", "/user/history", "watch_history table", "Clear authenticated user's watch history"),
        // Application (2)
        ProposedEndpointSpec("Application", "GET", "/app/settings", "config.php / shahkot.php / help.php", "Return maintenanceMode, JazzCash details, WhatsApp support links, and feature flags"),
        ProposedEndpointSpec("Application", "GET", "/app/version", "config.php", "Return latestVersionCode, latestVersionName, forceUpdate")
    )
}
