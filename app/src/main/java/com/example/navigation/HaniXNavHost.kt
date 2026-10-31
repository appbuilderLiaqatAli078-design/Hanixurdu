package com.example.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.HaniXBottomNavBar
import com.example.ui.screens.AboutHaniXScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ContinueWatchingScreen
import com.example.ui.screens.EpisodeListScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MovieDetailsScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.MyDownloadsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeasonSelectionScreen
import com.example.ui.screens.SeriesDetailsScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VipMembershipScreen
import com.example.ui.screens.WatchHistoryScreen
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextPrimaryIvory
import com.example.ui.viewmodel.HaniXViewModel

@Composable
fun HaniXUrduApp(viewModel: HaniXViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val sessionExpiredMessage by viewModel.sessionExpiredMessage.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()

    val authState by viewModel.authState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val moviesState by viewModel.moviesState.collectAsState()
    val selectedMovieState by viewModel.selectedMovieState.collectAsState()
    val seriesState by viewModel.seriesState.collectAsState()
    val selectedSeriesState by viewModel.selectedSeriesState.collectAsState()
    val seasonsState by viewModel.seasonsState.collectAsState()
    val episodesState by viewModel.episodesState.collectAsState()
    val categoriesState by viewModel.categoriesState.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val vipStatusState by viewModel.vipStatusState.collectAsState()
    val vipPlansState by viewModel.vipPlansState.collectAsState()
    val playbackAuthState by viewModel.playbackAuthState.collectAsState()
    val downloadActionState by viewModel.downloadActionState.collectAsState()
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()
    val appSettingsState by viewModel.appSettingsState.collectAsState()
    val appVersionState by viewModel.appVersionState.collectAsState()
    val settingsFeedback by viewModel.settingsFeedbackMessage.collectAsState()

    val watchHistory by viewModel.watchHistory.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val encryptedDownloads by viewModel.encryptedDownloads.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val bottomBarRoutes = setOf(
        HaniXRoutes.HOME,
        HaniXRoutes.MOVIES,
        HaniXRoutes.SERIES,
        HaniXRoutes.MY_DOWNLOADS,
        HaniXRoutes.USER_PROFILE
    )

    Scaffold(
        containerColor = OledBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                HaniXBottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!sessionExpiredMessage.isNullOrBlank()) {
                Surface(
                    color = CinemaCrimson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sessionExpiredMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimaryIvory,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.dismissSessionExpiredBanner()
                            navController.navigate(HaniXRoutes.LOGIN)
                        }) {
                            Text("Sign In", color = TextPrimaryIvory)
                        }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = HaniXRoutes.SPLASH,
                modifier = Modifier.weight(1f)
            ) {
                // 1. Splash
                composable(HaniXRoutes.SPLASH) {
                    SplashScreen(
                        onboardingCompleted = onboardingCompleted,
                        isAuthenticated = isAuthenticated,
                        onNavigateNext = { target ->
                            navController.navigate(target) {
                                popUpTo(HaniXRoutes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                // 2. Onboarding
                composable(HaniXRoutes.ONBOARDING) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            viewModel.completeOnboarding()
                            navController.navigate(HaniXRoutes.HOME) {
                                popUpTo(HaniXRoutes.ONBOARDING) { inclusive = true }
                            }
                        },
                        onSignInClick = {
                            viewModel.completeOnboarding()
                            navController.navigate(HaniXRoutes.LOGIN) {
                                popUpTo(HaniXRoutes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                // 3. Login
                composable(HaniXRoutes.LOGIN) {
                    LoginScreen(
                        authState = authState,
                        apiBaseUrl = apiBaseUrl,
                        onLoginSubmit = { email, password -> viewModel.login(email, password) },
                        onNavigateToRegister = {
                            viewModel.resetAuthFeedback()
                            navController.navigate(HaniXRoutes.REGISTER)
                        },
                        onNavigateToForgotPassword = {
                            viewModel.resetAuthFeedback()
                            navController.navigate(HaniXRoutes.FORGOT_PASSWORD)
                        },
                        onBackOrContinue = {
                            if (!navController.popBackStack()) {
                                navController.navigate(HaniXRoutes.HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 4. Registration
                composable(HaniXRoutes.REGISTER) {
                    RegisterScreen(
                        authState = authState,
                        onRegisterSubmit = { name, email, pass ->
                            viewModel.register(name, email, pass)
                        },
                        onNavigateToLogin = {
                            viewModel.resetAuthFeedback()
                            navController.popBackStack()
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 5. Forgot Password
                composable(HaniXRoutes.FORGOT_PASSWORD) {
                    ForgotPasswordScreen(
                        forgotPasswordState = forgotPasswordState,
                        onSubmitEmail = { email -> viewModel.requestPasswordReset(email) },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 6. Home
                composable(HaniXRoutes.HOME) {
                    HomeScreen(
                        homeState = homeState,
                        continueWatchingList = continueWatching,
                        currentUser = currentUser,
                        unreadNotifications = unreadNotifications,
                        apiBaseUrl = apiBaseUrl,
                        onRefreshHome = { viewModel.loadHomeDashboard() },
                        onNavigateToMovies = { navController.navigate(HaniXRoutes.MOVIES) },
                        onNavigateToSeries = { navController.navigate(HaniXRoutes.SERIES) },
                        onNavigateToCategories = { navController.navigate(HaniXRoutes.CATEGORIES) },
                        onNavigateToSearch = { navController.navigate(HaniXRoutes.SEARCH) },
                        onNavigateToVip = { navController.navigate(HaniXRoutes.VIP_MEMBERSHIP) },
                        onNavigateToNotifications = { navController.navigate(HaniXRoutes.NOTIFICATIONS) },
                        onNavigateToContinueWatching = { navController.navigate(HaniXRoutes.CONTINUE_WATCHING) },
                        onNavigateToWatchHistory = { navController.navigate(HaniXRoutes.WATCH_HISTORY) },
                        onNavigateToSettings = { navController.navigate(HaniXRoutes.APP_SETTINGS) },
                        onNavigateToAbout = { navController.navigate(HaniXRoutes.ABOUT_HANIX) },
                        onNavigateToLogin = { navController.navigate(HaniXRoutes.LOGIN) },
                        onMovieSelected = { id -> navController.navigate(HaniXRoutes.movieDetails(id)) },
                        onSeriesSelected = { id -> navController.navigate(HaniXRoutes.seriesDetails(id)) },
                        onResumePlayback = { type, id ->
                            navController.navigate(HaniXRoutes.videoPlayer(type, id))
                        }
                    )
                }

                // 7. Movies
                composable(HaniXRoutes.MOVIES) {
                    MoviesScreen(
                        moviesState = moviesState,
                        selectedCategory = selectedCategory,
                        unreadNotifications = unreadNotifications,
                        onLoadMovies = { cat -> viewModel.loadMovies(cat) },
                        onMovieClick = { id -> navController.navigate(HaniXRoutes.movieDetails(id)) },
                        onSearchClick = { navController.navigate(HaniXRoutes.SEARCH) },
                        onCategoriesClick = { navController.navigate(HaniXRoutes.CATEGORIES) },
                        onNotificationsClick = { navController.navigate(HaniXRoutes.NOTIFICATIONS) },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 8. Series
                composable(HaniXRoutes.SERIES) {
                    SeriesScreen(
                        seriesState = seriesState,
                        selectedCategory = selectedCategory,
                        unreadNotifications = unreadNotifications,
                        onLoadSeries = { cat -> viewModel.loadSeries(cat) },
                        onSeriesClick = { id -> navController.navigate(HaniXRoutes.seriesDetails(id)) },
                        onSearchClick = { navController.navigate(HaniXRoutes.SEARCH) },
                        onCategoriesClick = { navController.navigate(HaniXRoutes.CATEGORIES) },
                        onNotificationsClick = { navController.navigate(HaniXRoutes.NOTIFICATIONS) },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 9. Categories
                composable(HaniXRoutes.CATEGORIES) {
                    CategoriesScreen(
                        categoriesState = categoriesState,
                        onLoadCategories = { viewModel.loadCategories() },
                        onSelectCategoryForMovies = { cat ->
                            viewModel.loadMovies(cat)
                            navController.navigate(HaniXRoutes.MOVIES)
                        },
                        onSelectCategoryForSeries = { cat ->
                            viewModel.loadSeries(cat)
                            navController.navigate(HaniXRoutes.SERIES)
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 10. Search
                composable(HaniXRoutes.SEARCH) {
                    SearchScreen(
                        searchState = searchState,
                        onExecuteSearch = { q -> viewModel.performSearch(q) },
                        onMovieClick = { id -> navController.navigate(HaniXRoutes.movieDetails(id)) },
                        onSeriesClick = { id -> navController.navigate(HaniXRoutes.seriesDetails(id)) },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 11. Movie Details
                composable(
                    route = HaniXRoutes.MOVIE_DETAILS,
                    arguments = listOf(navArgument("movieId") { type = NavType.StringType })
                ) { entry ->
                    val movieId = entry.arguments?.getString("movieId") ?: "1"
                    MovieDetailsScreen(
                        movieId = movieId,
                        movieState = selectedMovieState,
                        downloadActionState = downloadActionState,
                        onLoadMovie = { id -> viewModel.loadMovieDetails(id) },
                        onPlayMovie = { id ->
                            navController.navigate(HaniXRoutes.videoPlayer("movie", id))
                        },
                        onDownloadMovie = { id ->
                            viewModel.requestEncryptedOfflineDownload("movie", id)
                        },
                        onNavigateToVip = { navController.navigate(HaniXRoutes.VIP_MEMBERSHIP) },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 12. Series Details
                composable(
                    route = HaniXRoutes.SERIES_DETAILS,
                    arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                ) { entry ->
                    val seriesId = entry.arguments?.getString("seriesId") ?: "1"
                    SeriesDetailsScreen(
                        seriesId = seriesId,
                        seriesState = selectedSeriesState,
                        seasonsState = seasonsState,
                        onLoadSeriesAndSeasons = { id -> viewModel.loadSeriesDetailsAndSeasons(id) },
                        onOpenSeasonSelection = { id ->
                            navController.navigate(HaniXRoutes.seasonSelection(id))
                        },
                        onSelectSeason = { seasonId ->
                            navController.navigate(HaniXRoutes.episodeList(seasonId))
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 13. Season Selection
                composable(
                    route = HaniXRoutes.SEASON_SELECTION,
                    arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                ) { entry ->
                    val seriesId = entry.arguments?.getString("seriesId") ?: "1"
                    SeasonSelectionScreen(
                        seriesId = seriesId,
                        seasonsState = seasonsState,
                        onLoadSeasons = { id -> viewModel.loadSeriesDetailsAndSeasons(id) },
                        onSeasonSelected = { seasonId ->
                            navController.navigate(HaniXRoutes.episodeList(seasonId))
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 14. Episode List
                composable(
                    route = HaniXRoutes.EPISODE_LIST,
                    arguments = listOf(navArgument("seasonId") { type = NavType.StringType })
                ) { entry ->
                    val seasonId = entry.arguments?.getString("seasonId") ?: "1"
                    EpisodeListScreen(
                        seasonId = seasonId,
                        episodesState = episodesState,
                        onLoadEpisodes = { id -> viewModel.loadSeasonEpisodes(id) },
                        onPlayEpisode = { episodeId ->
                            navController.navigate(HaniXRoutes.videoPlayer("episode", episodeId))
                        },
                        onDownloadEpisode = { episodeId ->
                            viewModel.requestEncryptedOfflineDownload("episode", episodeId)
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 15. Video Player (Bunny Stream HLS)
                composable(
                    route = HaniXRoutes.VIDEO_PLAYER,
                    arguments = listOf(
                        navArgument("contentType") { type = NavType.StringType },
                        navArgument("contentId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val contentType = entry.arguments?.getString("contentType") ?: "movie"
                    val contentId = entry.arguments?.getString("contentId") ?: "1"
                    VideoPlayerScreen(
                        contentType = contentType,
                        contentId = contentId,
                        playerManager = viewModel.bunnyPlayerManager,
                        playbackAuthState = playbackAuthState,
                        onAuthorizePlayback = { type, id ->
                            viewModel.authorizeAndStartBunnyPlayback(type, id)
                        },
                        onSaveProgress = {
                            viewModel.saveCurrentPlaybackProgress(
                                contentType = contentType,
                                contentId = contentId,
                                title = "HaniX ${contentType.replaceFirstChar { it.uppercase() }} #$contentId",
                                subtitle = "Bunny Stream HLS",
                                poster = null
                            )
                        },
                        onNavigateToVip = { navController.navigate(HaniXRoutes.VIP_MEMBERSHIP) },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 16. VIP Membership
                composable(HaniXRoutes.VIP_MEMBERSHIP) {
                    VipMembershipScreen(
                        vipStatusState = vipStatusState,
                        vipPlansState = vipPlansState,
                        currentUser = currentUser,
                        onLoadVipData = { viewModel.loadVipOverview() },
                        onVerifyVip = { planId, ref ->
                            viewModel.verifyVipSubscription(planId, ref)
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 17. User Profile
                composable(HaniXRoutes.USER_PROFILE) {
                    UserProfileScreen(
                        currentUser = currentUser,
                        isAuthenticated = isAuthenticated,
                        profileUpdateState = profileUpdateState,
                        unreadNotifications = unreadNotifications,
                        onUpdateProfile = { name, avatar ->
                            viewModel.updateUserProfile(name, avatar)
                        },
                        onNavigateToLogin = { navController.navigate(HaniXRoutes.LOGIN) },
                        onNavigateToVip = { navController.navigate(HaniXRoutes.VIP_MEMBERSHIP) },
                        onNavigateToWatchHistory = { navController.navigate(HaniXRoutes.WATCH_HISTORY) },
                        onNavigateToContinueWatching = { navController.navigate(HaniXRoutes.CONTINUE_WATCHING) },
                        onNavigateToDownloads = { navController.navigate(HaniXRoutes.MY_DOWNLOADS) },
                        onNavigateToNotifications = { navController.navigate(HaniXRoutes.NOTIFICATIONS) },
                        onNavigateToSettings = { navController.navigate(HaniXRoutes.APP_SETTINGS) },
                        onNavigateToAbout = { navController.navigate(HaniXRoutes.ABOUT_HANIX) },
                        onLogout = { viewModel.logout() },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 18. Watch History
                composable(HaniXRoutes.WATCH_HISTORY) {
                    WatchHistoryScreen(
                        historyList = watchHistory,
                        onSyncHistoryFromServer = { viewModel.syncServerWatchHistory() },
                        onClearAllHistory = { viewModel.clearWatchHistory() },
                        onDeleteEntry = { key -> viewModel.removeHistoryItem(key) },
                        onResumeItem = { type, id ->
                            navController.navigate(HaniXRoutes.videoPlayer(type, id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 19. Continue Watching
                composable(HaniXRoutes.CONTINUE_WATCHING) {
                    ContinueWatchingScreen(
                        continueWatchingList = continueWatching,
                        onResumeItem = { type, id ->
                            navController.navigate(HaniXRoutes.videoPlayer(type, id))
                        },
                        onRemoveItem = { key -> viewModel.removeHistoryItem(key) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 20. My Downloads
                composable(HaniXRoutes.MY_DOWNLOADS) {
                    MyDownloadsScreen(
                        encryptedDownloads = encryptedDownloads,
                        downloadActionState = downloadActionState,
                        unreadNotifications = unreadNotifications,
                        onRequestTestDownloadAuth = {
                            viewModel.requestEncryptedOfflineDownload("movie", "1")
                        },
                        onDeleteDownload = { id -> viewModel.deleteEncryptedDownload(id) },
                        onNotificationsClick = { navController.navigate(HaniXRoutes.NOTIFICATIONS) },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 21. Notifications
                composable(HaniXRoutes.NOTIFICATIONS) {
                    NotificationsScreen(
                        notifications = notifications,
                        onMarkRead = { id -> viewModel.markNotificationRead(id) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 22. App Settings
                composable(HaniXRoutes.APP_SETTINGS) {
                    AppSettingsScreen(
                        apiBaseUrl = apiBaseUrl,
                        appSettingsState = appSettingsState,
                        appVersionState = appVersionState,
                        feedbackMessage = settingsFeedback,
                        onLoadRemoteSettings = { viewModel.loadAppSettingsAndVersion() },
                        onUpdateApiBaseUrl = { url -> viewModel.updateApiBaseUrl(url) },
                        onBackClick = {
                            viewModel.clearSettingsFeedback()
                            navController.popBackStack()
                        },
                        onOpenChecklist = { navController.navigate(HaniXRoutes.ABOUT_HANIX) }
                    )
                }

                // 23. About HaniX Urdu & Integration Checklist
                composable(HaniXRoutes.ABOUT_HANIX) {
                    AboutHaniXScreen(
                        apiBaseUrl = apiBaseUrl,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
