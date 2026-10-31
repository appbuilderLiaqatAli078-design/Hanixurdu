package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.local.HaniXDao
import com.example.data.local.NotificationEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.toEntity
import com.example.data.model.ApiResponse
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuthSessionPayload
import com.example.data.model.CategoryItem
import com.example.data.model.DownloadAuthorizationPayload
import com.example.data.model.DownloadAuthorizeRequest
import com.example.data.model.Episode
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.HomePayload
import com.example.data.model.LoginRequest
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
import com.example.data.model.VipVerifyRequest
import com.example.data.remote.HaniXApiService
import com.example.data.security.KeystoreCryptoManager
import com.example.data.security.SecureSessionManager
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * Single source of truth coordinating the Hostinger PHP + MySQL REST API (`HaniXApiService`),
 * Android Keystore session/download encryption (`SecureSessionManager`, `KeystoreCryptoManager`),
 * and local Room persistence (`HaniXDao`).
 *
 * Strictly adheres to Section 12:
 * - Never invents fake production movies/series.
 * - Never pretends authentication, VIP verification, or Bunny playback succeeded unless the
 *   configured PHP REST API returns a valid `{"success": true, "data": ...}` payload.
 * - Clearly surfaces Pending Backend Endpoint states with the corresponding legacy PHP file name.
 */
class HaniXRepository(
    private val appContext: Context,
    private val apiService: HaniXApiService,
    private val dao: HaniXDao,
    val sessionManager: SecureSessionManager,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
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
                    title = "PHP REST API v1 Integration Ready",
                    message = "Android client configured for https://hanixurdu.com/api/v1/. Deploy the PHP v1 router on Hostinger to link existing MySQL tables.",
                    category = "BACKEND_STATUS"
                )
            )
            dao.insertNotification(
                NotificationEntity(
                    title = "Bunny Stream HLS Security Enforced",
                    message = "Playback requires short-lived signed tokens from POST /playback/authorize. Zero Bunny API keys are stored in this APK.",
                    category = "SYSTEM"
                )
            )
            dao.insertNotification(
                NotificationEntity(
                    title = "Android Keystore AES-256-GCM Active",
                    message = "Session tokens and app-private offline downloads are hardware-encrypted via AndroidKeyStore.",
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

    /**
     * Executes a Retrofit API call and validates the standard HaniX JSON response envelope.
     * Maps HTTP 404 / 501 / HTML responses from the existing website to `RemoteDataState.PendingBackend`
     * so developers and testers immediately see which PHP endpoint needs to be deployed.
     */
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
                } else {
                    val serverMsg = body.message?.takeIf { it.isNotBlank() }
                        ?: "Server returned success=false on $endpointLabel."
                    return@withContext RemoteDataState.Error(
                        endpoint = endpointLabel,
                        httpCode = code,
                        message = serverMsg
                    )
                }
            }

            if (code == 404 || code == 405 || code == 501) {
                return@withContext RemoteDataState.PendingBackend(
                    endpoint = endpointLabel,
                    httpCode = code,
                    legacyPhpFile = legacyPhpFile,
                    details = "HTTP $code from ${sessionManager.getConfiguredBaseUrl()}. Proposed REST endpoint `$endpointLabel` (bridging `$legacyPhpFile`) is pending implementation on the Hostinger PHP backend."
                )
            }

            if (code == 401 || code == 403) {
                return@withContext RemoteDataState.Error(
                    endpoint = endpointLabel,
                    httpCode = code,
                    message = "Authentication / VIP authorization required (HTTP $code) for $endpointLabel."
                )
            }

            return@withContext RemoteDataState.Error(
                endpoint = endpointLabel,
                httpCode = code,
                message = "HTTP $code received from $endpointLabel."
            )
        } catch (e: JsonEncodingException) {
            // Occurs when Hostinger serves an HTML page (e.g. index.php or 404 page) instead of JSON
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "Hostinger returned non-JSON content for `$endpointLabel`. Deploy the JSON REST controller bridging `$legacyPhpFile`."
            )
        } catch (e: JsonDataException) {
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "JSON schema mismatch or HTML response on `$endpointLabel` (maps to `$legacyPhpFile`): ${e.localizedMessage ?: "Malformed JSON"}"
            )
        } catch (e: IOException) {
            val cached = cachedFallback?.invoke()
            if (cached != null) {
                return@withContext RemoteDataState.Offline(
                    endpoint = endpointLabel,
                    cachedData = cached,
                    message = "Could not reach ${sessionManager.getConfiguredBaseUrl()} (${e.localizedMessage ?: "Network error"}). Showing locally cached data."
                )
            }
            return@withContext RemoteDataState.PendingBackend(
                endpoint = endpointLabel,
                httpCode = null,
                legacyPhpFile = legacyPhpFile,
                details = "Connection to ${sessionManager.getConfiguredBaseUrl()} could not be established (${e.localizedMessage ?: "Host unreachable"}). Verify Hostinger HTTPS API deployment for `$endpointLabel` (`$legacyPhpFile`)."
            )
        } catch (e: Exception) {
            return@withContext RemoteDataState.Error(
                endpoint = endpointLabel,
                httpCode = null,
                message = e.localizedMessage ?: "Unexpected error communicating with $endpointLabel."
            )
        }
    }

    // -------------------------------------------------------------------------
    // Authentication Operations
    // -------------------------------------------------------------------------
    suspend fun login(email: String, password: String): RemoteDataState<AuthSessionPayload> {
        val state = executeContractCall(
            endpointLabel = "POST /auth/login",
            legacyPhpFile = "login_process.php"
        ) {
            apiService.login(LoginRequest(email = email.trim(), password = password))
        }
        if (state is RemoteDataState.Success) {
            sessionManager.saveAuthenticatedSession(
                accessToken = state.data.accessToken,
                refreshToken = state.data.refreshToken,
                user = state.data.user
            )
        }
        return state
    }

    suspend fun register(name: String, email: String, password: String): RemoteDataState<AuthSessionPayload> {
        val state = executeContractCall(
            endpointLabel = "POST /auth/register",
            legacyPhpFile = "signup.php"
        ) {
            apiService.register(
                RegisterRequest(name = name.trim(), email = email.trim(), password = password)
            )
        }
        if (state is RemoteDataState.Success) {
            sessionManager.saveAuthenticatedSession(
                accessToken = state.data.accessToken,
                refreshToken = state.data.refreshToken,
                user = state.data.user
            )
        }
        return state
    }

    suspend fun forgotPassword(email: String): RemoteDataState<Unit> {
        return executeContractCall(
            endpointLabel = "POST /auth/forgot-password",
            legacyPhpFile = "forgot_password.php"
        ) {
            apiService.forgotPassword(ForgotPasswordRequest(email = email.trim()))
        }
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
            legacyPhpFile = "config.php + users session"
        ) {
            apiService.getMe()
        }
        if (state is RemoteDataState.Success) {
            sessionManager.updateCurrentUser(state.data)
        }
        return state
    }

    // -------------------------------------------------------------------------
    // Home, Movies, Series, Seasons, Episodes & Search
    // -------------------------------------------------------------------------
    suspend fun fetchHomeDashboard(): RemoteDataState<HomePayload> {
        val state = executeContractCall(
            endpointLabel = "GET /home",
            legacyPhpFile = "index.php"
        ) {
            apiService.getHome()
        }
        if (state is RemoteDataState.Success) {
            state.data.featuredMovies?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.latestMovies?.let { dao.insertMovies(it.map { m -> m.toEntity() }) }
            state.data.featuredSeries?.let { dao.insertSeries(it.map { s -> s.toEntity() }) }
            state.data.latestSeries?.let { dao.insertSeries(it.map { s -> s.toEntity() }) }
        }
        return state
    }

    suspend fun fetchMovies(category: String? = null): RemoteDataState<List<Movie>> {
        val state = executeContractCall(
            endpointLabel = if (category == null) "GET /movies" else "GET /movies?category=$category",
            legacyPhpFile = "index.php / edit_movie.php",
            cachedFallback = {
                val cached = dao.observeCachedMovies().firstOrNull()?.map { it.toDomain() }
                cached?.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getMovies(category)
        }
        if (state is RemoteDataState.Success) {
            dao.insertMovies(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /movies", "No movies returned by the backend.")
            }
        }
        return state
    }

    suspend fun fetchMovieDetails(movieId: String): RemoteDataState<Movie> {
        val state = executeContractCall(
            endpointLabel = "GET /movies/$movieId",
            legacyPhpFile = "run_video.php / edit_movie.php",
            cachedFallback = { dao.getCachedMovieById(movieId)?.toDomain() }
        ) {
            apiService.getMovieById(movieId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertMovies(listOf(state.data.toEntity()))
        }
        return state
    }

    suspend fun fetchCategories(): RemoteDataState<List<CategoryItem>> {
        return executeContractCall(
            endpointLabel = "GET /home/categories",
            legacyPhpFile = "header.php / index.php"
        ) {
            apiService.getHomeCategories()
        }
    }

    suspend fun fetchSeries(category: String? = null): RemoteDataState<List<Series>> {
        val state = executeContractCall(
            endpointLabel = if (category == null) "GET /series" else "GET /series?category=$category",
            legacyPhpFile = "series.php",
            cachedFallback = {
                val cached = dao.observeCachedSeries().firstOrNull()?.map { it.toDomain() }
                cached?.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeries(category)
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeries(state.data.map { it.toEntity() })
            if (state.data.isEmpty()) {
                return RemoteDataState.Empty("GET /series", "No series returned by the backend.")
            }
        }
        return state
    }

    suspend fun fetchSeriesDetails(seriesId: String): RemoteDataState<Series> {
        val state = executeContractCall(
            endpointLabel = "GET /series/$seriesId",
            legacyPhpFile = "view_series.php / edit_series.php",
            cachedFallback = { dao.getCachedSeriesById(seriesId)?.toDomain() }
        ) {
            apiService.getSeriesById(seriesId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeries(listOf(state.data.toEntity()))
        }
        return state
    }

    suspend fun fetchSeriesSeasons(seriesId: String): RemoteDataState<List<Season>> {
        val state = executeContractCall(
            endpointLabel = "GET /series/$seriesId/seasons",
            legacyPhpFile = "view_series.php",
            cachedFallback = {
                dao.getCachedSeasonsForSeries(seriesId).map { it.toDomain() }.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeriesSeasons(seriesId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertSeasons(state.data.map { it.toEntity() })
        }
        return state
    }

    suspend fun fetchSeasonEpisodes(seasonId: String): RemoteDataState<List<Episode>> {
        val state = executeContractCall(
            endpointLabel = "GET /seasons/$seasonId/episodes",
            legacyPhpFile = "view_series.php",
            cachedFallback = {
                dao.getCachedEpisodesForSeason(seasonId).map { it.toDomain() }.takeIf { it.isNotEmpty() }
            }
        ) {
            apiService.getSeasonEpisodes(seasonId)
        }
        if (state is RemoteDataState.Success) {
            dao.insertEpisodes(state.data.map { it.toEntity() })
        }
        return state
    }

    suspend fun fetchEpisodeDetails(episodeId: String): RemoteDataState<Episode> {
        return executeContractCall(
            endpointLabel = "GET /episodes/$episodeId",
            legacyPhpFile = "run_video.php"
        ) {
            apiService.getEpisodeById(episodeId)
        }
    }

    suspend fun searchContent(query: String): RemoteDataState<SearchPayload> {
        return executeContractCall(
            endpointLabel = "GET /search?q=$query",
            legacyPhpFile = "search.php"
        ) {
            apiService.search(query)
        }
    }

    // -------------------------------------------------------------------------
    // VIP Membership (Backend-Enforced Only)
    // -------------------------------------------------------------------------
    suspend fun fetchVipStatus(): RemoteDataState<VipStatusPayload> {
        return executeContractCall(
            endpointLabel = "GET /vip/status",
            legacyPhpFile = "check_premium.php"
        ) {
            apiService.getVipStatus()
        }
    }

    suspend fun fetchVipPlans(): RemoteDataState<List<VipPlan>> {
        return executeContractCall(
            endpointLabel = "GET /vip/plans",
            legacyPhpFile = "add_premium.php"
        ) {
            apiService.getVipPlans()
        }
    }

    suspend fun verifyVipEntitlement(planId: String, referenceCode: String): RemoteDataState<VipStatusPayload> {
        return executeContractCall(
            endpointLabel = "POST /vip/verify",
            legacyPhpFile = "unlock_vip.php / add_premium.php"
        ) {
            apiService.verifyVip(
                VipVerifyRequest(
                    planId = planId,
                    voucherOrTransactionReference = referenceCode.trim()
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // Bunny Stream Playback Authorization & Progress Sync
    // -------------------------------------------------------------------------
    suspend fun requestPlaybackAuthorization(
        contentType: String,
        contentId: String
    ): RemoteDataState<PlaybackAuthorizationPayload> {
        return executeContractCall(
            endpointLabel = "POST /playback/authorize",
            legacyPhpFile = "run_video.php + check_premium.php"
        ) {
            apiService.authorizePlayback(
                PlaybackAuthorizeRequest(contentType = contentType, contentId = contentId)
            )
        }
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
    // Encrypted App-Private Downloads (Section 5 & Section 10)
    // -------------------------------------------------------------------------
    suspend fun requestAndEncryptDownload(
        contentType: String,
        contentId: String,
        qualityTier: String = "720p"
    ): RemoteDataState<EncryptedDownloadEntity> = withContext(Dispatchers.IO) {
        val authState = executeContractCall(
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

        when (authState) {
            is RemoteDataState.Success -> {
                val payload: DownloadAuthorizationPayload = authState.data
                val signedUrl = payload.signedDownloadStreamUrl
                if (!signedUrl.startsWith("https://", ignoreCase = true)) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "POST /downloads/authorize",
                        httpCode = null,
                        message = "Security Violation: Backend download stream URL must use HTTPS."
                    )
                }
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(signedUrl).build()
                    val streamBytes = client.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            return@withContext RemoteDataState.Error(
                                endpoint = "POST /downloads/authorize",
                                httpCode = resp.code,
                                message = "Failed to fetch authorized media stream (HTTP ${resp.code})."
                            )
                        }
                        resp.body?.bytes() ?: ByteArray(0)
                    }
                    val targetDir = File(appContext.filesDir, "encrypted_downloads")
                    val targetFile = File(targetDir, "${payload.downloadId}.hanixenc")
                    val ivBase64 = cryptoManager.writeEncryptedDownloadFile(targetFile, streamBytes)

                    val entity = EncryptedDownloadEntity(
                        downloadId = payload.downloadId,
                        contentId = payload.contentId,
                        contentType = payload.contentType,
                        title = payload.title,
                        poster = payload.poster,
                        privateEncryptedFilePath = targetFile.absolutePath,
                        encryptionIvBase64 = ivBase64,
                        fileSizeBytes = targetFile.length(),
                        licenseExpiresAtEpochSeconds = payload.licenseExpiresAtEpochSeconds,
                        requiresVipEntitlement = payload.requiresVipEntitlement
                    )
                    dao.upsertEncryptedDownload(entity)
                    RemoteDataState.Success(entity, "POST /downloads/authorize")
                } catch (e: Exception) {
                    RemoteDataState.Error(
                        endpoint = "POST /downloads/authorize",
                        httpCode = null,
                        message = "Failed to encrypt offline media: ${e.localizedMessage}"
                    )
                }
            }
            is RemoteDataState.PendingBackend -> authState
            is RemoteDataState.Error -> authState
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
        }
        return state
    }

    suspend fun syncServerWatchHistory(): RemoteDataState<List< com.example.data.model.WatchHistoryItemDto>> {
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
        return executeContractCall(
            endpointLabel = "GET /app/settings",
            legacyPhpFile = "config.php / shahkot.php"
        ) {
            apiService.getAppSettings()
        }
    }

    suspend fun fetchAppVersion(): RemoteDataState<AppVersionPayload> {
        return executeContractCall(
            endpointLabel = "GET /app/version",
            legacyPhpFile = "config.php"
        ) {
            apiService.getAppVersion()
        }
    }
}
