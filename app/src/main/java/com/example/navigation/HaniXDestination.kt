package com.example.navigation

/**
 * Defines all 23 Android app screens specified in Section 4 of the HaniX Urdu specification.
 */
object HaniXRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val MOVIE_DETAILS = "movie_details/{movieId}"
    const val SERIES_DETAILS = "series_details/{seriesId}"
    const val SEASON_SELECTION = "season_selection/{seriesId}"
    const val EPISODE_LIST = "episode_list/{seasonId}"
    const val VIDEO_PLAYER = "video_player/{contentType}/{contentId}"
    const val VIP_MEMBERSHIP = "vip_membership"
    const val USER_PROFILE = "user_profile"
    const val WATCH_HISTORY = "watch_history"
    const val CONTINUE_WATCHING = "continue_watching"
    const val MY_DOWNLOADS = "my_downloads"
    const val NOTIFICATIONS = "notifications"
    const val APP_SETTINGS = "app_settings"
    const val ABOUT_HANIX = "about_hanix"

    fun movieDetails(movieId: String) = "movie_details/$movieId"
    fun seriesDetails(seriesId: String) = "series_details/$seriesId"
    fun seasonSelection(seriesId: String) = "season_selection/$seriesId"
    fun episodeList(seasonId: String) = "episode_list/$seasonId"
    fun videoPlayer(contentType: String, contentId: String) = "video_player/$contentType/$contentId"
}

enum class BottomNavTab(
    val route: String,
    val label: String,
    val testTag: String
) {
    HOME(HaniXRoutes.HOME, "Home", "bottom_nav_home"),
    MOVIES(HaniXRoutes.MOVIES, "Movies", "bottom_nav_movies"),
    SERIES(HaniXRoutes.SERIES, "Series", "bottom_nav_series"),
    DOWNLOADS(HaniXRoutes.MY_DOWNLOADS, "Downloads", "bottom_nav_downloads"),
    PROFILE(HaniXRoutes.USER_PROFILE, "Profile", "bottom_nav_profile")
}
