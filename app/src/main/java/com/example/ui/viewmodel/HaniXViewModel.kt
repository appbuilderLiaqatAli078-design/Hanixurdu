package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.local.HaniXDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuthSessionPayload
import com.example.data.model.CategoryItem
import com.example.data.model.Episode
import com.example.data.model.HomePayload
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.RemoteDataState
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.User
import com.example.data.model.VipPlan
import com.example.data.model.VipStatusPayload
import com.example.data.remote.NetworkModule
import com.example.data.repository.HaniXRepository
import com.example.data.security.SecureSessionManager
import com.example.player.BunnyStreamPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HaniXViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SecureSessionManager(application)
    private val database = HaniXDatabase.getInstance(application)
    private val apiService = NetworkModule.createApiService(sessionManager)
    val repository = HaniXRepository(
        appContext = application,
        apiService = apiService,
        dao = database.hanixDao(),
        sessionManager = sessionManager
    )
    val bunnyPlayerManager = BunnyStreamPlayerManager(application)

    // Session & Config Flows
    val currentUser: StateFlow<User?> = sessionManager.currentUser
    val isAuthenticated: StateFlow<Boolean> = sessionManager.isAuthenticated
    val apiBaseUrl: StateFlow<String> = sessionManager.apiBaseUrl
    val onboardingCompleted: StateFlow<Boolean> = sessionManager.onboardingCompleted
    val sessionExpiredMessage: StateFlow<String?> = sessionManager.sessionExpiredMessage

    // Local Room Flows
    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<WatchHistoryEntity>> = repository.continueWatchingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val encryptedDownloads: StateFlow<List<EncryptedDownloadEntity>> = repository.encryptedDownloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Remote Contract States
    private val _authState = MutableStateFlow<RemoteDataState<AuthSessionPayload>>(RemoteDataState.Idle)
    val authState: StateFlow<RemoteDataState<AuthSessionPayload>> = _authState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<RemoteDataState<Unit>>(RemoteDataState.Idle)
    val forgotPasswordState: StateFlow<RemoteDataState<Unit>> = _forgotPasswordState.asStateFlow()

    private val _homeState = MutableStateFlow<RemoteDataState<HomePayload>>(RemoteDataState.Idle)
    val homeState: StateFlow<RemoteDataState<HomePayload>> = _homeState.asStateFlow()

    private val _moviesState = MutableStateFlow<RemoteDataState<List<Movie>>>(RemoteDataState.Idle)
    val moviesState: StateFlow<RemoteDataState<List<Movie>>> = _moviesState.asStateFlow()

    private val _selectedMovieState = MutableStateFlow<RemoteDataState<Movie>>(RemoteDataState.Idle)
    val selectedMovieState: StateFlow<RemoteDataState<Movie>> = _selectedMovieState.asStateFlow()

    private val _seriesState = MutableStateFlow<RemoteDataState<List<Series>>>(RemoteDataState.Idle)
    val seriesState: StateFlow<RemoteDataState<List<Series>>> = _seriesState.asStateFlow()

    private val _selectedSeriesState = MutableStateFlow<RemoteDataState<Series>>(RemoteDataState.Idle)
    val selectedSeriesState: StateFlow<RemoteDataState<Series>> = _selectedSeriesState.asStateFlow()

    private val _seasonsState = MutableStateFlow<RemoteDataState<List<Season>>>(RemoteDataState.Idle)
    val seasonsState: StateFlow<RemoteDataState<List<Season>>> = _seasonsState.asStateFlow()

    private val _episodesState = MutableStateFlow<RemoteDataState<List<Episode>>>(RemoteDataState.Idle)
    val episodesState: StateFlow<RemoteDataState<List<Episode>>> = _episodesState.asStateFlow()

    private val _categoriesState = MutableStateFlow<RemoteDataState<List<CategoryItem>>>(RemoteDataState.Idle)
    val categoriesState: StateFlow<RemoteDataState<List<CategoryItem>>> = _categoriesState.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchState = MutableStateFlow<RemoteDataState<SearchPayload>>(RemoteDataState.Idle)
    val searchState: StateFlow<RemoteDataState<SearchPayload>> = _searchState.asStateFlow()

    private val _vipStatusState = MutableStateFlow<RemoteDataState<VipStatusPayload>>(RemoteDataState.Idle)
    val vipStatusState: StateFlow<RemoteDataState<VipStatusPayload>> = _vipStatusState.asStateFlow()

    private val _vipPlansState = MutableStateFlow<RemoteDataState<List<VipPlan>>>(RemoteDataState.Idle)
    val vipPlansState: StateFlow<RemoteDataState<List<VipPlan>>> = _vipPlansState.asStateFlow()

    private val _playbackAuthState = MutableStateFlow<RemoteDataState<PlaybackAuthorizationPayload>>(RemoteDataState.Idle)
    val playbackAuthState: StateFlow<RemoteDataState<PlaybackAuthorizationPayload>> = _playbackAuthState.asStateFlow()

    private val _downloadActionState = MutableStateFlow<RemoteDataState<EncryptedDownloadEntity>>(RemoteDataState.Idle)
    val downloadActionState: StateFlow<RemoteDataState<EncryptedDownloadEntity>> = _downloadActionState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<RemoteDataState<User>>(RemoteDataState.Idle)
    val profileUpdateState: StateFlow<RemoteDataState<User>> = _profileUpdateState.asStateFlow()

    private val _appSettingsState = MutableStateFlow<RemoteDataState<AppSettingsPayload>>(RemoteDataState.Idle)
    val appSettingsState: StateFlow<RemoteDataState<AppSettingsPayload>> = _appSettingsState.asStateFlow()

    private val _appVersionState = MutableStateFlow<RemoteDataState<AppVersionPayload>>(RemoteDataState.Idle)
    val appVersionState: StateFlow<RemoteDataState<AppVersionPayload>> = _appVersionState.asStateFlow()

    private val _settingsFeedbackMessage = MutableStateFlow<String?>(null)
    val settingsFeedbackMessage: StateFlow<String?> = _settingsFeedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSystemNotificationsIfNeeded()
        }
    }

    fun completeOnboarding() {
        sessionManager.completeOnboarding()
    }

    fun resetAuthFeedback() {
        _authState.value = RemoteDataState.Idle
        _forgotPasswordState.value = RemoteDataState.Idle
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = RemoteDataState.Loading("POST /auth/login")
            _authState.value = repository.login(email, password)
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = RemoteDataState.Loading("POST /auth/register")
            _authState.value = repository.register(name, email, password)
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = RemoteDataState.Loading("POST /auth/forgot-password")
            _forgotPasswordState.value = repository.forgotPassword(email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = RemoteDataState.Idle
        }
    }

    fun loadHomeDashboard() {
        viewModelScope.launch {
            _homeState.value = RemoteDataState.Loading("GET /home")
            _homeState.value = repository.fetchHomeDashboard()
        }
    }

    fun loadMovies(category: String? = _selectedCategoryFilter.value) {
        _selectedCategoryFilter.value = category
        viewModelScope.launch {
            val label = if (category == null) "GET /movies" else "GET /movies?category=$category"
            _moviesState.value = RemoteDataState.Loading(label)
            _moviesState.value = repository.fetchMovies(category)
        }
    }

    fun loadMovieDetails(movieId: String) {
        viewModelScope.launch {
            _selectedMovieState.value = RemoteDataState.Loading("GET /movies/$movieId")
            _selectedMovieState.value = repository.fetchMovieDetails(movieId)
        }
    }

    fun loadSeries(category: String? = _selectedCategoryFilter.value) {
        _selectedCategoryFilter.value = category
        viewModelScope.launch {
            val label = if (category == null) "GET /series" else "GET /series?category=$category"
            _seriesState.value = RemoteDataState.Loading(label)
            _seriesState.value = repository.fetchSeries(category)
        }
    }

    fun loadSeriesDetailsAndSeasons(seriesId: String) {
        viewModelScope.launch {
            _selectedSeriesState.value = RemoteDataState.Loading("GET /series/$seriesId")
            _seasonsState.value = RemoteDataState.Loading("GET /series/$seriesId/seasons")
            _selectedSeriesState.value = repository.fetchSeriesDetails(seriesId)
            _seasonsState.value = repository.fetchSeriesSeasons(seriesId)
        }
    }

    fun loadSeasonEpisodes(seasonId: String) {
        viewModelScope.launch {
            _episodesState.value = RemoteDataState.Loading("GET /seasons/$seasonId/episodes")
            _episodesState.value = repository.fetchSeasonEpisodes(seasonId)
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = RemoteDataState.Loading("GET /home/categories")
            _categoriesState.value = repository.fetchCategories()
        }
    }

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchState.value = RemoteDataState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = RemoteDataState.Loading("GET /search?q=$trimmed")
            _searchState.value = repository.searchContent(trimmed)
        }
    }

    fun loadVipOverview() {
        viewModelScope.launch {
            _vipStatusState.value = RemoteDataState.Loading("GET /vip/status")
            _vipPlansState.value = RemoteDataState.Loading("GET /vip/plans")
            _vipStatusState.value = repository.fetchVipStatus()
            _vipPlansState.value = repository.fetchVipPlans()
        }
    }

    fun verifyVipSubscription(planId: String, voucherOrRef: String) {
        viewModelScope.launch {
            _vipStatusState.value = RemoteDataState.Loading("POST /vip/verify")
            _vipStatusState.value = repository.verifyVipEntitlement(planId, voucherOrRef)
        }
    }

    fun authorizeAndStartBunnyPlayback(contentType: String, contentId: String) {
        viewModelScope.launch {
            bunnyPlayerManager.setAuthorizing()
            _playbackAuthState.value = RemoteDataState.Loading("POST /playback/authorize")
            val result = repository.requestPlaybackAuthorization(contentType, contentId)
            _playbackAuthState.value = result
            when (result) {
                is RemoteDataState.Success -> {
                    bunnyPlayerManager.initializeFromBackendAuthorization(result.data)
                }
                is RemoteDataState.PendingBackend -> {
                    bunnyPlayerManager.setBackendPending(result.endpoint, result.details)
                }
                is RemoteDataState.Error -> {
                    bunnyPlayerManager.setError(result.message)
                }
                is RemoteDataState.Offline -> {
                    bunnyPlayerManager.setError(result.message)
                }
                else -> Unit
            }
        }
    }

    fun saveCurrentPlaybackProgress(
        contentType: String,
        contentId: String,
        title: String,
        subtitle: String?,
        poster: String?
    ) {
        viewModelScope.launch {
            val (pos, dur) = bunnyPlayerManager.syncPlayerPosition()
            if (pos > 0L && dur > 0L) {
                val sessionId = (_playbackAuthState.value as? RemoteDataState.Success)?.data?.playbackSessionId
                repository.recordPlaybackProgress(
                    playbackSessionId = sessionId,
                    contentType = contentType,
                    contentId = contentId,
                    title = title,
                    subtitle = subtitle,
                    poster = poster,
                    positionMs = pos,
                    durationMs = dur
                )
            }
        }
    }

    fun requestEncryptedOfflineDownload(contentType: String, contentId: String, qualityTier: String = "720p") {
        viewModelScope.launch {
            _downloadActionState.value = RemoteDataState.Loading("POST /downloads/authorize")
            _downloadActionState.value = repository.requestAndEncryptDownload(contentType, contentId, qualityTier)
        }
    }

    fun deleteEncryptedDownload(downloadId: String) {
        viewModelScope.launch {
            repository.deleteEncryptedDownload(downloadId)
        }
    }

    fun updateUserProfile(name: String, avatarUrl: String?) {
        viewModelScope.launch {
            _profileUpdateState.value = RemoteDataState.Loading("PUT /user/profile")
            _profileUpdateState.value = repository.updateProfile(name, avatarUrl)
        }
    }

    fun syncServerWatchHistory() {
        viewModelScope.launch {
            repository.syncServerWatchHistory()
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearAllWatchHistory()
        }
    }

    fun removeHistoryItem(historyKey: String) {
        viewModelScope.launch {
            repository.removeSingleHistoryItem(historyKey)
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun loadAppSettingsAndVersion() {
        viewModelScope.launch {
            _appSettingsState.value = RemoteDataState.Loading("GET /app/settings")
            _appVersionState.value = RemoteDataState.Loading("GET /app/version")
            _appSettingsState.value = repository.fetchAppSettings()
            _appVersionState.value = repository.fetchAppVersion()
        }
    }

    fun updateApiBaseUrl(newUrl: String) {
        val result = sessionManager.updateApiBaseUrl(newUrl)
        _settingsFeedbackMessage.value = result.fold(
            onSuccess = { "HTTPS Base URL updated to: $it" },
            onFailure = { it.localizedMessage ?: "Invalid URL" }
        )
    }

    fun clearSettingsFeedback() {
        _settingsFeedbackMessage.value = null
    }

    fun dismissSessionExpiredBanner() {
        sessionManager.dismissSessionExpiredNotice()
    }

    override fun onCleared() {
        super.onCleared()
        bunnyPlayerManager.releasePlayer()
    }
}
