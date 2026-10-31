package com.example.data.remote

import com.example.data.model.ApiResponse
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuthSessionPayload
import com.example.data.model.BannerItem
import com.example.data.model.CategoryItem
import com.example.data.model.DownloadAuthorizationPayload
import com.example.data.model.DownloadAuthorizeRequest
import com.example.data.model.DownloadRecordDto
import com.example.data.model.Episode
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.HomePayload
import com.example.data.model.LoginRequest
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackAuthorizeRequest
import com.example.data.model.PlaybackCompleteRequest
import com.example.data.model.PlaybackProgressRequest
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.UpdateProfileRequest
import com.example.data.model.User
import com.example.data.model.VipPlan
import com.example.data.model.VipStatusPayload
import com.example.data.model.VipVerifyRequest
import com.example.data.model.WatchHistoryItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Proposed REST API v1 contract for HaniX Urdu (https://hanixurdu.com/api/v1/).
 * Designed to bridge the existing Hostinger PHP + MySQL website without altering
 * existing web pages or shahkot.php.
 */
interface HaniXApiService {

    // -------------------------------------------------------------------------
    // Authentication (Maps to login_process.php, signup.php, forgot_password.php)
    // -------------------------------------------------------------------------
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthSessionPayload>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthSessionPayload>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthSessionPayload>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<User>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Home (Maps to index.php, header.php)
    // -------------------------------------------------------------------------
    @GET("home")
    suspend fun getHome(): Response<ApiResponse<HomePayload>>

    @GET("home/banners")
    suspend fun getHomeBanners(): Response<ApiResponse<List<BannerItem>>>

    @GET("home/featured")
    suspend fun getHomeFeatured(): Response<ApiResponse<List<Movie>>>

    @GET("home/latest")
    suspend fun getHomeLatest(): Response<ApiResponse<List<Movie>>>

    @GET("home/categories")
    suspend fun getHomeCategories(): Response<ApiResponse<List<CategoryItem>>>

    // -------------------------------------------------------------------------
    // Movies (Maps to index.php, edit_movie.php, run_video.php)
    // -------------------------------------------------------------------------
    @GET("movies")
    suspend fun getMovies(
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<Movie>>>

    @GET("movies/{id}")
    suspend fun getMovieById(@Path("id") id: String): Response<ApiResponse<Movie>>

    @GET("movies/categories")
    suspend fun getMovieCategories(): Response<ApiResponse<List<CategoryItem>>>

    // -------------------------------------------------------------------------
    // Series, Seasons & Episodes (Maps to series.php, view_series.php, run_video.php)
    // -------------------------------------------------------------------------
    @GET("series")
    suspend fun getSeries(
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<Series>>>

    @GET("series/{id}")
    suspend fun getSeriesById(@Path("id") id: String): Response<ApiResponse<Series>>

    @GET("series/{id}/seasons")
    suspend fun getSeriesSeasons(@Path("id") seriesId: String): Response<ApiResponse<List<Season>>>

    @GET("seasons/{id}/episodes")
    suspend fun getSeasonEpisodes(@Path("id") seasonId: String): Response<ApiResponse<List<Episode>>>

    @GET("episodes/{id}")
    suspend fun getEpisodeById(@Path("id") episodeId: String): Response<ApiResponse<Episode>>

    // -------------------------------------------------------------------------
    // Search (Maps to search.php)
    // -------------------------------------------------------------------------
    @GET("search")
    suspend fun search(@Query("q") query: String): Response<ApiResponse<SearchPayload>>

    // -------------------------------------------------------------------------
    // VIP Membership (Maps to check_premium.php, add_premium.php, unlock_vip.php)
    // -------------------------------------------------------------------------
    @GET("vip/status")
    suspend fun getVipStatus(): Response<ApiResponse<VipStatusPayload>>

    @GET("vip/plans")
    suspend fun getVipPlans(): Response<ApiResponse<List<VipPlan>>>

    @POST("vip/verify")
    suspend fun verifyVip(@Body request: VipVerifyRequest): Response<ApiResponse<VipStatusPayload>>

    // -------------------------------------------------------------------------
    // Bunny Stream Playback Authorization & Telemetry (Maps to run_video.php)
    // -------------------------------------------------------------------------
    @POST("playback/authorize")
    suspend fun authorizePlayback(
        @Body request: PlaybackAuthorizeRequest
    ): Response<ApiResponse<PlaybackAuthorizationPayload>>

    @POST("playback/progress")
    suspend fun reportPlaybackProgress(
        @Body request: PlaybackProgressRequest
    ): Response<ApiResponse<Unit>>

    @POST("playback/complete")
    suspend fun reportPlaybackComplete(
        @Body request: PlaybackCompleteRequest
    ): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // Encrypted Offline Downloads
    // -------------------------------------------------------------------------
    @POST("downloads/authorize")
    suspend fun authorizeDownload(
        @Body request: DownloadAuthorizeRequest
    ): Response<ApiResponse<DownloadAuthorizationPayload>>

    @GET("downloads")
    suspend fun getAuthorizedDownloads(): Response<ApiResponse<List<DownloadRecordDto>>>

    @DELETE("downloads/{id}")
    suspend fun deleteAuthorizedDownload(@Path("id") downloadId: String): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // User Profile & Watch History
    // -------------------------------------------------------------------------
    @GET("user/profile")
    suspend fun getUserProfile(): Response<ApiResponse<User>>

    @PUT("user/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<User>>

    @GET("user/history")
    suspend fun getUserWatchHistory(): Response<ApiResponse<List<WatchHistoryItemDto>>>

    @DELETE("user/history")
    suspend fun clearUserWatchHistory(): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------------------
    // App Settings & Version
    // -------------------------------------------------------------------------
    @GET("app/settings")
    suspend fun getAppSettings(): Response<ApiResponse<AppSettingsPayload>>

    @GET("app/version")
    suspend fun getAppVersion(): Response<ApiResponse<AppVersionPayload>>
}
