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

    private fun encodeSegment(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun decodeSegment(value: String?): String =
        if (value.isNullOrEmpty()) "" else runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    fun movieDetails(movieId: String) = "movie_details/${encodeSegment(movieId)}"
    fun seriesDetails(seriesId: String) = "series_details/${encodeSegment(seriesId)}"
    fun seasonSelection(seriesId: String) = "season_selection/${encodeSegment(seriesId)}"
    fun episodeList(seasonId: String) = "episode_list/${encodeSegment(seasonId)}"
    fun videoPlayer(contentType: String, contentId: String) =
        "video_player/${encodeSegment(contentType)}/${encodeSegment(contentId)}"
}

enum class BottomNavTab(
    val route: String,
    val label: String,
    val testTag: String
) {
    HOME(HaniXRoutes.HOME, "Home", "bottom_nav_home"),
    DRAMAS(HaniXRoutes.MOVIES, "Dramas", "bottom_nav_dramas"),
    SERIES(HaniXRoutes.SERIES, "Series", "bottom_nav_series"),
    VIP(HaniXRoutes.VIP_MEMBERSHIP, "VIP", "bottom_nav_vip"),
    PROFILE(HaniXRoutes.USER_PROFILE, "Profile", "bottom_nav_profile")
}
