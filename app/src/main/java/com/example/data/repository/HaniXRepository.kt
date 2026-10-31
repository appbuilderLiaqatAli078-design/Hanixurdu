package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.local.HaniXDao
import com.example.data.local.NotificationEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.toEntity
import com.example.data.model.ApiResponse
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuditionUploadPayload
import com.example.data.model.AuthSessionPayload
import com.example.data.model.CategoryItem
import com.example.data.model.CommunityUpdatesPayload
import com.example.data.model.DownloadAuthorizationPayload
import com.example.data.model.DownloadAuthorizeRequest
import com.example.data.model.Episode
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.ForgotPasswordResetRequest
import com.example.data.model.ForgotPasswordVerifyRequest
import com.example.data.model.ForgotPasswordVerifyResponse
import com.example.data.model.HomePayload
import com.example.data.model.LoginRequest
import com.example.data.model.MembershipTier
import com.example.data.model.MindGamePayload
import com.example.data.model.MindGameQuestionBank
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackAuthorizeRequest
import com.example.data.model.PlaybackCompleteRequest
import com.example.data.model.PlaybackProgressRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.RemoteDataState
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.UpdateProfileRequest
import com.example.data.model.User
import com.example.data.model.VipPlan
import com.example.data.model.VipStatusPayload
import com.example.data.remote.HaniXApiService
import com.example.data.remote.HaniXLiveWebsiteBridge
import com.example.data.security.HaniXSecurityShield
import com.example.data.security.KeystoreCryptoManager
import com.example.data.security.SecureSessionManager
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * Single source of truth coordinating the Hostinger PHP + MySQL REST API (`HaniXApiService`),
 * Direct Live Website Bridge (`HaniXLiveWebsiteBridge` -> `index.php`, `search.php`, `series.php`,
 * `run_video.php`, `login_process.php`, `signup.php`, `forgot_password.php`, `check_premium.php`, `upload.php`),
 * Android Keystore session/download encryption (`SecureSessionManager`, `KeystoreCryptoManager`),
 * and local Room cache (`HaniXDao`).
 *
 * Strictly enforces:
 * - Only two membership plans: FREE and VIP.
 * - Every newly registered account starts as FREE.
 * - Expired VIP accounts immediately lose VIP privileges on the next authorization check.
 * - Zero fake movies, fake accounts, or fake subscriptions.
 */
class HaniXRepository(
    private val appContext: Context,
    private val apiService: HaniXApiService,
    private val dao: HaniXDao,
    val sessionManager: SecureSessionManager,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(),
    private val liveWebsiteBridge: HaniXLiveWebsiteBridge = HaniXLiveWebsiteBridge(sessionManager)
) {

    val cachedMoviesFlow: Flow<List<com.example.data.local.MovieEntity>> = dao.observeCachedMovies()
    val cachedSeriesFlow: Flow<List<com.example.data.local.SeriesEntity>> = dao.observeCachedSeries()
    val watchHistoryFlow: Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory()
    val continueWatchingFlow: Flow<List<WatchHistoryEntity>> = dao.observeContinueWatching()
    val encryptedDownloadsFlow: Flow<List<EncryptedDownloadEntity>> = dao.observeEncryptedDownloads()
    val notificationsFlow: Flow<List<NotificationEntity>> = dao.observeNotifications()

    suspend fun initializeSystemNotificationsIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.getNotificationCount() == 0) {
            dao.insertNotification(
                NotificationEntity(
                    title = "Connected to Live hanixurdu.com",
                    message = "Streaming real movies, series, episodes, and VIP status directly from hanixurdu.com.",
                    category = "BACKEND_STATUS"
                )
            )
            dao.insertNotification(
                NotificationEntity(
                    title = "FREE & VIP Membership Policy Active",
                    message = "Only FREE and VIP membership plans exist. VIP access is verified strictly via check_premium.php & premium_users on Hostinger.",
                    category = "VIP"
                )
            )
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun <T> executeContractCall(
        endpointLabel: String,
        legacyPhpFile: String,
        cachedFallback: (suspend () -> T?)? = null,
        call: suspend () -> Response<ApiResponse<T>>
    ): RemoteDataState<T> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            val cached = cachedFallback?.invoke()
            return@withContext RemoteDataState.Offline(
                endpoint = endpointLabel,
                cachedData = cached,
                message = "Device is offline. Connect to the internet to reach ${sessionManager.getConfiguredBaseUrl()}."
            )
        }

        try {
            val response = call()
            val code = response.code()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                if (body.success && body.data != null) {
                    return@withContext RemoteDataState.Success(
                        data = body.data,
                        endpoint = endpointLabel
                    )
                } else if (body.success && body.data == null) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext RemoteDataState.Success(
                        data = Unit as T,
                        endpoint = endpointLabel
                    )
                } else {
                    val serverMsg = body.message?.takeIf { it.isNotBlank() }
                        ?: "Request rejected by backend on $endpointLabel."
                    if (code == 401) {
                        return@withContext RemoteDataState.AuthRequired(
                            endpoint = endpointLabel,
                            message = serverMsg
                        )
                    }
                    return@withContext RemoteDataState.Error(
                        endpoint = endpointLabel,
                        httpCode = code,
                        message = serverMsg
                    )
                }
            }

            if (code == 401) {
                return@withContext RemoteDataState.AuthRequired(
                    endpoint = endpointLabel,
                    message = "Authentication required for $endpointLabel. Please sign in with your HaniX Urdu account."
                )
            }

            if (code == 403) {
                return@withContext RemoteDataState.Error(
                    endpoint = endpointLabel,
                    httpCode = 403,
                    message = "VIP Membership required. Send JazzCash payment to Muhammad Hasnain (03294806457) and share screenshot on WhatsApp (+92 316 0450074) for instant VIP activation."
                )
            }

            if (code == 404 || code == 405 || code == 501) {
                return@withContext RemoteDataState.PendingBackend(
                    endpoint = endpointLabel,
                    httpCode = code,
                    legacyPhpFile = legacyPhpFile,
                    details = "HTTP $code from ${sessionManager.getConfiguredBaseUrl()}. Bridging directly to live `$legacyPhpFile` on hanixurdu.com."
                )
            }

            return@withContext RemoteDataState.Error(
                endpoint = endpointLabel,
                httpCode = code,
                message = "Server returned HTTP $code on $endpointLabel."
            )
        } catch (e: JsonEncodingException) {
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "Hostinger returned HTML instead of JSON for `$endpointLabel`. Bridging to `$legacyPhpFile`."
            )
        } catch (e: JsonDataException) {
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "Unexpected JSON schema on `$endpointLabel` (`$legacyPhpFile`): ${e.localizedMessage ?: "Invalid JSON"}"
            )
        } catch (e: IOException) {
            val cached = cachedFallback?.invoke()
            if (cached != null) {
                return@withContext RemoteDataState.Offline(
                    endpoint = endpointLabel,
                    cachedData = cached,
                    message = "Network unreachable (${e.localizedMessage ?: "IO error"}). Displaying cached catalog."
                )
            }
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "Could not connect to ${sessionManager.getConfiguredBaseUrl()} (${e.localizedMessage ?: "Connection failed"})."
            )
        } catch (e: Exception) {
            return@withContext RemoteDataState.Error(
                endpoint = endpointLabel,
                httpCode = null,
                message = e.localizedMessage ?: "Unexpected error on $endpointLabel."
            )
        }
    }

    // -------------------------------------------------------------------------
    // Authentication (`login_process.php`, `signup.php`, `forgot_password.php`)
    // -------------------------------------------------------------------------
    suspend fun login(email: String, password: String): RemoteDataState<AuthSessionPayload> {
        var state = executeContractCall(
            endpointLabel = "POST /auth/login",
            legacyPhpFile = "login_process.php"
        ) {
            apiService.login(LoginRequest(email = email.trim(), password = password))
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.loginLive(email, password)
        }
        if (state is RemoteDataState.Success) {
            sessionManager.saveAuthenticatedSession(
                accessToken = state.data.resolvedAccessToken,
                refreshToken = state.data.resolvedRefreshToken,
                user = state.data.user
            )
        }
        return state
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        secretPin: String = ""
    ): RemoteDataState<AuthSessionPayload> {
        var state = executeContractCall(
            endpointLabel = "POST /auth/register",
            legacyPhpFile = "signup.php"
        ) {
            apiService.register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    secretPin = secretPin.trim().takeIf { it.isNotEmpty() }
                )
            )
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.registerLive(name, email, password, secretPin)
        }
        if (state is RemoteDataState.Success) {
            // Every newly registered account automatically receives FREE membership
            val newFreeUser = state.data.user.copy(
                membership = MembershipTier.FREE.name,
                vipStatus = false,
                vipExpiry = null,
                vipExpiryEpochSeconds = null
            )
            sessionManager.saveAuthenticatedSession(
                accessToken = state.data.resolvedAccessToken,
                refreshToken = state.data.resolvedRefreshToken,
                user = newFreeUser
            )
        }
        return state
    }

    suspend fun forgotPassword(email: String, secretPin: String? = null): RemoteDataState<Unit> {
        val state = executeContractCall(
            endpointLabel = "POST /auth/forgot-password",
            legacyPhpFile = "forgot_password.php"
        ) {
            apiService.forgotPassword(
                ForgotPasswordRequest(
                    email = email.trim(),
                    secretPin = secretPin?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
        if (state is RemoteDataState.PendingBackend && !secretPin.isNullOrBlank()) {
            val verifyState = liveWebsiteBridge.verifyForgotPinLive(email, secretPin)
            if (verifyState is RemoteDataState.Success) {
                return RemoteDataState.Success(Unit, verifyState.endpoint)
            } else if (verifyState is RemoteDataState.Error) {
                return verifyState
            }
        }
        return state
    }

    /**
     * Step 1 of `forgot_password.php`: Verify `email` + `secret_pin` in `users` table.
     */
    suspend fun verifyForgotPasswordPin(
        email: String,
        secretPin: String
    ): RemoteDataState<ForgotPasswordVerifyResponse> {
        val state = executeContractCall(
            endpointLabel = "POST /auth/forgot-password/verify",
            legacyPhpFile = "forgot_password.php"
        ) {
            apiService.verifyForgotPassword(
                ForgotPasswordVerifyRequest(
                    email = email.trim(),
                    secretPin = secretPin.trim()
                )
            )
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.verifyForgotPinLive(email, secretPin)
        }
        return state
    }

    /**
     * Step 2 of `forgot_password.php`: Update `users.password` (min 8 chars) using `password_hash(..., PASSWORD_DEFAULT)`.
     */
    suspend fun resetForgotPasswordWithPin(
        email: String,
        secretPin: String,
        newPassword: String,
        resetToken: String? = null
    ): RemoteDataState<Unit> {
        val state = executeContractCall(
            endpointLabel = "POST /auth/forgot-password/reset",
            legacyPhpFile = "forgot_password.php"
        ) {
            apiService.resetForgotPassword(
                ForgotPasswordResetRequest(
                    email = email.trim(),
                    newPassword = newPassword,
                    resetToken = resetToken,
                    secretPin = secretPin.trim()
                )
            )
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.resetPasswordWithPinLive(email, secretPin, newPassword)
        }
        return state
    }

    suspend fun logout() {
        try {
            if (isNetworkAvailable() && sessionManager.getAccessToken() != null) {
                apiService.logout()
            }
        } catch (_: Exception) {
        } finally {
            sessionManager.clearSession()
        }
    }

    suspend fun fetchCurrentUser(): RemoteDataState<User> {
        val state = executeContractCall(
            endpointLabel = "GET /auth/me",
            legacyPhpFile = "config.php + users + premium_users"
        ) {
            apiService.getMe()
        }
        if (state is RemoteDataState.Success) {
            sessionManager.updateCurrentUser(state.data)
            return state
        }
        val existingUser = sessionManager.currentUser.value
        if (state is RemoteDataState.PendingBackend && existingUser != null) {
            val vipState = liveWebsiteBridge.fetchLiveVipStatus()
            if (vipState is RemoteDataState.Success) {
                val updatedUser = existingUser.copy(
                    membership = vipState.data.membership,
                    vipStatus = vipState.data.isVip,
                    legacyIsVip = vipState.data.isVip
                )
                sessionManager.updateCurrentUser(updatedUser)
                return RemoteDataState.Success(updatedUser, "hanixurdu.com/check_premium.php")
            }
        }
        return state
    }

    // -------------------------------------------------------------------------
    // Content Loading (`index.php`, `series.php`, `run_video.php`, `search.php`)
    // -------------------------------------------------------------------------
    suspend fun fetchHomeDashboard(): RemoteDataState<HomePayload> {
        var state = executeContractCall(
            endpointLabel = "GET /home",
            legacyPhpFile = "index.php"
        ) {
            apiService.getHome()
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveHomePayload()
        }
        if (state is RemoteDataState.Success) {
            state.data.resolvedFeaturedMovies.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.latestMovies?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.urduSubtitles?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.allContent?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.comingSoon?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.hanixOriginals?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.premiumContent?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.resolvedFeaturedSeries.let { dao.insertSeries(it.map { s -> s.toEntity() }) }
            state.data.latestSeries?.let { dao.insertSeries(it.map { s -> s.toEntity() }) }
        }
        return state
    }

    suspend fun fetchMovies(category: String? = null): RemoteDataState<List<Movie>> {
        var state = executeContractCall(
            endpointLabel = if (category == null) "GET /movies" else "GET /movies?category=$category",
            legacyPhpFile = "index.php / edit_movie.php",
            cachedFallback = {
                val cached = dao.observeCachedMovies().firstOrNull()?.map { it.toDomain() }
                cached?.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getMovies(category)
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveMovies(category)
        }
        if (state is RemoteDataState.Success) {
            dao.insertMovies(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /movies", "No movies found in the database.")
            }
        }
        return state
    }

    suspend fun fetchMovieDetails(movieId: String): RemoteDataState<Movie> {
        var state = executeContractCall(
            endpointLabel = "GET /movies/$movieId",
            legacyPhpFile = "run_video.php / edit_movie.php",
            cachedFallback = { dao.getCachedMovieById(movieId)?.toDomain() }
        ) {
            apiService.getMovieById(movieId)
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveMovieDetails(movieId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertMovies(listOf(state.data.toEntity()))
        }
        return state
    }

    suspend fun fetchCategories(): RemoteDataState<List<CategoryItem>> {
        val state = executeContractCall(
            endpointLabel = "GET /home/categories",
            legacyPhpFile = "header.php / index.php"
        ) {
            apiService.getHomeCategories()
        }
        if (state is RemoteDataState.PendingBackend) {
            val home = liveWebsiteBridge.fetchLiveHomePayload()
            if (home is RemoteDataState.Success) {
                return RemoteDataState.Success(
                    data = home.data.categories.orEmpty(),
                    endpoint = "hanixurdu.com/index.php"
                )
            }
        }
        return state
    }

    suspend fun fetchSeries(category: String? = null): RemoteDataState<List<Series>> {
        var state = executeContractCall(
            endpointLabel = if (category == null) "GET /series" else "GET /series?category=$category",
            legacyPhpFile = "series.php",
            cachedFallback = {
                val cached = dao.observeCachedSeries().firstOrNull()?.map { it.toDomain() }
                cached?.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeries(category)
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveSeriesList(category)
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeries(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /series", "No series found in the database.")
            }
        }
        return state
    }

    suspend fun fetchSeriesDetails(seriesId: String): RemoteDataState<Series> {
        var state = executeContractCall(
            endpointLabel = "GET /series/$seriesId",
            legacyPhpFile = "view_series.php / edit_series.php",
            cachedFallback = { dao.getCachedSeriesById(seriesId)?.toDomain() }
        ) {
            apiService.getSeriesById(seriesId)
        }
        if (state is RemoteDataState.PendingBackend) {
            val (liveSeries, liveSeasons) = liveWebsiteBridge.fetchLiveSeriesDetailsAndSeasons(seriesId)
            state = liveSeries
            if (liveSeasons is RemoteDataState.Success) {
                dao.insertSeasons(liveSeasons.data.map { it.toEntity() })
            }
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeries(listOf(state.data.toEntity()))
        }
        return state
    }

    suspend fun fetchSeriesSeasons(seriesId: String): RemoteDataState<List<Season>> {
        var state = executeContractCall(
            endpointLabel = "GET /series/$seriesId/seasons",
            legacyPhpFile = "series.php (series_seasons + movies)",
            cachedFallback = {
                dao.getCachedSeasonsForSeries(seriesId).map { it.toDomain() }.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeriesSeasons(seriesId)
        }
        if (state is RemoteDataState.PendingBackend) {
            val (_, liveSeasons) = liveWebsiteBridge.fetchLiveSeriesDetailsAndSeasons(seriesId)
            state = liveSeasons
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeasons(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /series/$seriesId/seasons", "No seasons found for this series.")
            }
        }
        return state
    }

    suspend fun fetchSeasonEpisodes(seasonId: String): RemoteDataState<List<Episode>> {
        var state = executeContractCall(
            endpointLabel = "GET /seasons/$seasonId/episodes",
            legacyPhpFile = "series.php?ajax=1",
            cachedFallback = {
                dao.getCachedEpisodesForSeason(seasonId).map { it.toDomain() }.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeasonEpisodes(seasonId)
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveSeasonEpisodes(seasonId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertEpisodes(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /seasons/$seasonId/episodes", "No episodes found for this season.")
            }
        }
        return state
    }

    suspend fun fetchEpisodeDetails(episodeId: String): RemoteDataState<Episode> {
        val state = executeContractCall(
            endpointLabel = "GET /episodes/$episodeId",
            legacyPhpFile = "run_video.php"
        ) {
            apiService.getEpisodeById(episodeId)
        }
        if (state is RemoteDataState.PendingBackend) {
            val movieDetail = liveWebsiteBridge.fetchLiveMovieDetails(episodeId)
            if (movieDetail is RemoteDataState.Success) {
                val m = movieDetail.data
                return RemoteDataState.Success(
                    data = Episode(
                        id = m.resolvedId,
                        seasonId = m.seasonName ?: "Season 1",
                        seriesName = m.seriesName,
                        seasonName = m.seasonName,
                        title = m.title,
                        description = m.description,
                        poster = m.resolvedPoster,
                        posterPath = m.resolvedPoster,
                        isPremium = m.resolvedIsVip,
                        isPremiumSnake = m.resolvedIsVip,
                        hasVideo = m.resolvedHasVideo
                    ),
                    endpoint = movieDetail.endpoint
                )
            }
        }
        return state
    }

    suspend fun searchContent(query: String): RemoteDataState<SearchPayload> {
        val state = executeContractCall(
            endpointLabel = "GET /search?q=$query",
            legacyPhpFile = "search.php"
        ) {
            apiService.search(query)
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.searchLive(query)
        }
        return state
    }

    // -------------------------------------------------------------------------
    // Membership — FREE and VIP Only (`check_premium.php`, `add_premium.php`, `unlock_vip.php`)
    // -------------------------------------------------------------------------
    suspend fun fetchVipStatus(): RemoteDataState<VipStatusPayload> {
        var state = executeContractCall(
            endpointLabel = "GET /vip/status",
            legacyPhpFile = "check_premium.php"
        ) {
            apiService.getVipStatus()
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.fetchLiveVipStatus()
        }
        if (state is RemoteDataState.Success) {
            val tier = state.data.effectiveTier()
            sessionManager.applyServerVipStatus(
                tier = tier,
                expiryDate = state.data.vipExpiry ?: state.data.legacyExpiryDate,
                expiryEpochSeconds = state.data.vipExpiryEpochSeconds
            )
        }
        return state
    }

    suspend fun fetchVipPlans(): RemoteDataState<List<VipPlan>> {
        var state = executeContractCall(
            endpointLabel = "GET /vip/plans",
            legacyPhpFile = "index.php / series.php / add_premium.php"
        ) {
            apiService.getVipPlans()
        }
        if (state is RemoteDataState.PendingBackend) {
            state = liveWebsiteBridge.getLiveVipPlans()
        }
        if (state is RemoteDataState.Success) {
            val filtered = state.data.filter {
                it.planType.equals("FREE", ignoreCase = true) ||
                    it.planType.equals("VIP", ignoreCase = true)
            }
            return RemoteDataState.Success(filtered, state.endpoint)
        }
        return state
    }

    // -------------------------------------------------------------------------
    // Playback Authorization & Multi-Server Resolution (`run_video.php`)
    // -------------------------------------------------------------------------
    suspend fun requestPlaybackAuthorization(
        contentType: String,
        contentId: String,
        preferredServer: String? = null
    ): RemoteDataState<PlaybackAuthorizationPayload> {
        val state = executeContractCall(
            endpointLabel = "POST /playback/authorize",
            legacyPhpFile = "run_video.php + check_premium.php"
        ) {
            apiService.authorizePlayback(
                PlaybackAuthorizeRequest(
                    contentType = contentType,
                    contentId = contentId,
                    server = preferredServer
                )
            )
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.authorizeLivePlayback(contentType, contentId, preferredServer)
        }
        return state
    }

    suspend fun recordPlaybackProgress(
        playbackSessionId: String?,
        contentType: String,
        contentId: String,
        title: String,
        subtitle: String?,
        poster: String?,
        positionMs: Long,
        durationMs: Long
    ) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val completed = positionMs >= (safeDuration * 92L / 100L)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                historyKey = "${contentType}_$contentId",
                contentId = contentId,
                contentType = contentType,
                title = title,
                subtitle = subtitle,
                poster = poster,
                positionMs = positionMs,
                durationMs = safeDuration,
                isCompleted = completed
            )
        )
        if (!playbackSessionId.isNullOrBlank() && isNetworkAvailable()) {
            try {
                if (completed) {
                    apiService.reportPlaybackComplete(
                        PlaybackCompleteRequest(
                            playbackSessionId = playbackSessionId,
                            contentType = contentType,
                            contentId = contentId
                        )
                    )
                } else {
                    apiService.reportPlaybackProgress(
                        PlaybackProgressRequest(
                            playbackSessionId = playbackSessionId,
                            contentType = contentType,
                            contentId = contentId,
                            positionMs = positionMs,
                            durationMs = safeDuration
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Community Features: Mind Games Quiz (`mind_game.php`), Dubbing Studio (`studio.php` & `upload.php`), Updates (`sufyan.php`)
    // -------------------------------------------------------------------------
    suspend fun fetchMindGameQuestions(): RemoteDataState<MindGamePayload> = withContext(Dispatchers.IO) {
        val state = executeContractCall(
            endpointLabel = "GET /mind-game/questions",
            legacyPhpFile = "mind_game.php",
            cachedFallback = {
                val ten = MindGameQuestionBank.drawTenRandomQuestions()
                MindGamePayload(totalQuestions = ten.size, questions = ten)
            }
        ) {
            apiService.getMindGameQuestions()
        }
        if (state is RemoteDataState.PendingBackend || state is RemoteDataState.Offline) {
            val ten = MindGameQuestionBank.drawTenRandomQuestions()
            return@withContext RemoteDataState.Success(
                data = MindGamePayload(totalQuestions = ten.size, questions = ten),
                endpoint = "mind_game.php (10 Randomized Questions)"
            )
        }
        state
    }

    suspend fun submitStudioAudition(
        userName: String,
        whatsapp: String,
        role: String,
        fileUri: Uri
    ): RemoteDataState<AuditionUploadPayload> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = appContext.contentResolver
            var displayName = "audition_sample.mp3"
            var fileSize = 0L

            contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    if (sizeIdx >= 0) {
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (fileSize > 10L * 1024L * 1024L) {
                return@withContext RemoteDataState.Error(
                    endpoint = "POST /studio/audition",
                    httpCode = 422,
                    message = "File is too large! Maximum allowed size in upload.php is 10 MB."
                )
            }

            val bytes = contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return@withContext RemoteDataState.Error(
                    endpoint = "POST /studio/audition",
                    httpCode = null,
                    message = "Could not read selected audio/video file."
                )

            if (bytes.size > 10 * 1024 * 1024) {
                return@withContext RemoteDataState.Error(
                    endpoint = "POST /studio/audition",
                    httpCode = 422,
                    message = "File exceeds the 10 MB limit enforced by upload.php."
                )
            }

            val mimeType = contentResolver.getType(fileUri) ?: "audio/mpeg"
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val fileRequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("voice_file", displayName, fileRequestBody)

            val state = executeContractCall(
                endpointLabel = "POST /studio/audition",
                legacyPhpFile = "studio.php + upload.php"
            ) {
                apiService.submitStudioAudition(
                    userName = userName.trim().toRequestBody(textMediaType),
                    whatsapp = whatsapp.trim().toRequestBody(textMediaType),
                    role = role.trim().toRequestBody(textMediaType),
                    voiceFile = filePart
                )
            }
            if (state is RemoteDataState.PendingBackend) {
                return@withContext liveWebsiteBridge.submitStudioAuditionLive(
                    userName = userName,
                    whatsapp = whatsapp,
                    role = role,
                    fileName = displayName,
                    mimeType = mimeType,
                    fileBytes = bytes
                )
            }
            state
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "POST /studio/audition",
                httpCode = null,
                message = e.localizedMessage ?: "Failed to prepare audition upload."
            )
        }
    }

    suspend fun fetchCommunityUpdates(): RemoteDataState<CommunityUpdatesPayload> {
        val state = executeContractCall(
            endpointLabel = "GET /updates",
            legacyPhpFile = "sufyan.php + help.php"
        ) {
            apiService.getCommunityUpdates()
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.getLiveCommunityUpdates()
        }
        return state
    }

    // -------------------------------------------------------------------------
    // Encrypted App-Private Downloads (`filesDir/hanix_offline_vault` + `.nomedia`)
    // Strictly hidden from device Gallery / MediaStore.
    // Free content: all logged-in users. VIP content: active VIP members.
    // -------------------------------------------------------------------------
    suspend fun requestAndEncryptDownload(
        contentType: String,
        contentId: String,
        qualityTier: String = "720p"
    ): RemoteDataState<EncryptedDownloadEntity> = withContext(Dispatchers.IO) {
        var authState = executeContractCall(
            endpointLabel = "POST /downloads/authorize",
            legacyPhpFile = "run_video.php + check_premium.php"
        ) {
            apiService.authorizeDownload(
                DownloadAuthorizeRequest(
                    contentType = contentType,
                    contentId = contentId,
                    qualityTier = qualityTier
                )
            )
        }

        if (authState is RemoteDataState.PendingBackend) {
            authState = liveWebsiteBridge.authorizeLiveDownload(
                contentType = contentType,
                contentId = contentId,
                qualityTier = qualityTier
            )
        }

        when (authState) {
            is RemoteDataState.Success -> {
                val payload: DownloadAuthorizationPayload = authState.data
                val signedUrl = payload.signedDownloadStreamUrl
                if (!signedUrl.startsWith("https://", ignoreCase = true)) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "Offline Vault",
                        httpCode = null,
                        message = "Security Violation: Download stream URL must use HTTPS."
                    )
                }
                val nowSeconds = System.currentTimeMillis() / 1000L
                if (payload.licenseExpiresAtEpochSeconds <= nowSeconds) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "Offline Vault",
                        httpCode = 403,
                        message = "Download license returned by server is already expired."
                    )
                }
                try {
                    val client = HaniXSecurityShield.configureSecureOkHttpClient(
                        OkHttpClient.Builder()
                    ).build()

                    // Fetch stream bytes or encrypted offline stream descriptor (never saved to Gallery)
                    val isDirectBinaryCandidate = signedUrl.endsWith(".mp4", ignoreCase = true) ||
                        signedUrl.contains(".mp4?", ignoreCase = true)
                    val rawBytesToEncrypt: ByteArray = if (isDirectBinaryCandidate) {
                        val request = Request.Builder().url(signedUrl).build()
                        try {
                            client.newCall(request).execute().use { resp ->
                                val contentLen = resp.body?.contentLength() ?: -1L
                                if (resp.isSuccessful && contentLen in 1L..(35L * 1024L * 1024L)) {
                                    resp.body?.bytes() ?: signedUrl.encodeToByteArray()
                                } else {
                                    "HANIX_VAULT_STREAM|$signedUrl|${payload.title}|$qualityTier".encodeToByteArray()
                                }
                            }
                        } catch (_: Exception) {
                            "HANIX_VAULT_STREAM|$signedUrl|${payload.title}|$qualityTier".encodeToByteArray()
                        }
                    } else {
                        "HANIX_VAULT_STREAM|$signedUrl|${payload.title}|$qualityTier".encodeToByteArray()
                    }

                    // Store strictly inside app-private vault with .nomedia protection (invisible to Gallery/MediaStore)
                    val targetDir = HaniXSecurityShield.getOrCreatePrivateOfflineVaultDir(appContext)
                    val safeFileName = payload.downloadId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    val targetFile = File(targetDir, "$safeFileName.hxvault")
                    val ivBase64 = cryptoManager.writeEncryptedDownloadFile(targetFile, rawBytesToEncrypt)

                    val entity = EncryptedDownloadEntity(
                        downloadId = payload.downloadId,
                        contentId = payload.contentId,
                        contentType = payload.contentType,
                        title = payload.title,
                        poster = payload.poster,
                        privateEncryptedFilePath = targetFile.absolutePath,
                        encryptionIvBase64 = ivBase64,
                        fileSizeBytes = targetFile.length().coerceAtLeast(rawBytesToEncrypt.size.toLong()),
                        licenseExpiresAtEpochSeconds = payload.licenseExpiresAtEpochSeconds,
                        requiresVipEntitlement = payload.requiresVipEntitlement
                    )
                    dao.upsertEncryptedDownload(entity)
                    RemoteDataState.Success(entity, "App-Private Vault")
                } catch (e: Exception) {
                    RemoteDataState.Error(
                        endpoint = "Offline Vault",
                        httpCode = null,
                        message = "Failed to encrypt offline media: ${e.localizedMessage}"
                    )
                }
            }
            is RemoteDataState.PendingBackend -> authState
            is RemoteDataState.Error -> authState
            is RemoteDataState.AuthRequired -> authState
            is RemoteDataState.Offline -> RemoteDataState.Offline(
                endpoint = authState.endpoint,
                cachedData = null,
                message = authState.message
            )
            is RemoteDataState.Empty -> authState
            RemoteDataState.Idle -> RemoteDataState.Idle
            is RemoteDataState.Loading -> authState
        }
    }

    suspend fun deleteEncryptedDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getEncryptedDownloadById(downloadId)
        if (existing != null) {
            val file = File(existing.privateEncryptedFilePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteEncryptedDownloadById(downloadId)
        }
        if (isNetworkAvailable()) {
            try {
                apiService.deleteAuthorizedDownload(downloadId)
            } catch (_: Exception) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // User Profile, History, App Settings & Version
    // -------------------------------------------------------------------------
    suspend fun updateProfile(name: String, avatarUrl: String?): RemoteDataState<User> {
        val state = executeContractCall(
            endpointLabel = "PUT /user/profile",
            legacyPhpFile = "config.php + users table"
        ) {
            apiService.updateUserProfile(UpdateProfileRequest(name = name.trim(), profileImage = avatarUrl))
        }
        if (state is RemoteDataState.Success) {
            sessionManager.updateCurrentUser(state.data)
            return state
        }
        val current = sessionManager.currentUser.value
        if (state is RemoteDataState.PendingBackend && current != null) {
            val updated = current.copy(name = name.trim(), profileImage = avatarUrl ?: current.profileImage)
            sessionManager.updateCurrentUser(updated)
            return RemoteDataState.Success(updated, "hanixurdu.com/config.php")
        }
        return state
    }

    suspend fun syncServerWatchHistory(): RemoteDataState<List<com.example.data.model.WatchHistoryItemDto>> {
        val state = executeContractCall(
            endpointLabel = "GET /user/history",
            legacyPhpFile = "watch_history table"
        ) {
            apiService.getUserWatchHistory()
        }
        if (state is RemoteDataState.Success) {
            state.data.forEach { dto ->
                dao.upsertWatchHistory(
                    WatchHistoryEntity(
                        historyKey = "${dto.contentType}_${dto.contentId}",
                        contentId = dto.contentId,
                        contentType = dto.contentType,
                        title = dto.title,
                        subtitle = dto.subtitle,
                        poster = dto.poster,
                        positionMs = dto.positionMs,
                        durationMs = dto.durationMs.coerceAtLeast(1L),
                        isCompleted = dto.isCompleted
                    )
                )
            }
        }
        return state
    }

    suspend fun clearAllWatchHistory() = withContext(Dispatchers.IO) {
        dao.clearAllWatchHistory()
        if (isNetworkAvailable()) {
            try {
                apiService.clearUserWatchHistory()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun removeSingleHistoryItem(historyKey: String) = withContext(Dispatchers.IO) {
        dao.deleteWatchHistoryEntry(historyKey)
    }

    suspend fun markNotificationRead(id: Long) = withContext(Dispatchers.IO) {
        dao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead()
    }

    suspend fun fetchAppSettings(): RemoteDataState<AppSettingsPayload> {
        val state = executeContractCall(
            endpointLabel = "GET /app/settings",
            legacyPhpFile = "config.php / shahkot.php"
        ) {
            apiService.getAppSettings()
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.getLiveAppSettings()
        }
        return state
    }

    suspend fun fetchAppVersion(): RemoteDataState<AppVersionPayload> {
        val state = executeContractCall(
            endpointLabel = "GET /app/version",
            legacyPhpFile = "config.php"
        ) {
            apiService.getAppVersion()
        }
        if (state is RemoteDataState.PendingBackend) {
            return liveWebsiteBridge.getLiveAppVersion()
        }
        return state
    }
}
