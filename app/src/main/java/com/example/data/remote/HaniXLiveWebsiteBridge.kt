package com.example.data.remote

import android.text.Html
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuditionUploadPayload
import com.example.data.model.AuthSessionPayload
import com.example.data.model.BannerItem
import com.example.data.model.CategoryItem
import com.example.data.model.CommunityUpdatesPayload
import com.example.data.model.DownloadAuthorizationPayload
import com.example.data.model.Episode
import com.example.data.model.ForgotPasswordVerifyResponse
import com.example.data.model.HomePayload
import com.example.data.model.MembershipTier
import com.example.data.model.Movie
import com.example.data.model.PaymentInfoDto
import com.example.data.model.PlatformUpdateDto
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackServerDto
import com.example.data.model.RemoteDataState
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.SupportLinksDto
import com.example.data.model.User
import com.example.data.model.VipPlan
import com.example.data.model.VipStatusPayload
import com.example.data.security.HaniXSecurityShield
import com.example.data.security.SecureSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Direct Live Bridge to `https://hanixurdu.com/` PHP scripts:
 * - `index.php` (3D Coverflow banners + 5 category rails: Urdu Subtitles, All Content, Coming Soon, Hanix Originals, Premium)
 * - `search.php?q=...` (Direct JSON MySQL queries on `movies` and `series`)
 * - `series.php?sub=...` & `series.php?ajax=1&sub=...&season=...` (Series metadata, seasons & episode cards)
 * - `run_video.php?id=...` (Video player Main Server, Server 2, embeds & MP4s, Recommended For You)
 * - `login_process.php` (JSON login against `users` table with `password_verify`)
 * - `signup.php` (User registration with `secret_pin`, always starting as `FREE`)
 * - `forgot_password.php` (2-step `email` + `secret_pin` verification & password reset)
 * - `check_premium.php?email=...` (JSON VIP check against `premium_users.expiry_date > NOW()`)
 * - `upload.php` (Dubbing Studio voice/video audition multipart upload)
 */
class HaniXLiveWebsiteBridge(
    private val sessionManager: SecureSessionManager
) {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val httpClient: OkHttpClient = HaniXSecurityShield.configureSecureOkHttpClient(
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                    cookies.forEach { incoming ->
                        hostCookies.removeAll { it.name == incoming.name }
                        hostCookies.add(incoming)
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host]?.toList() ?: emptyList()
                }
            })
    ).build()

    // In-memory cache of discovered movies, series, and initial series.php episodes
    private val discoveredMoviesById = ConcurrentHashMap<String, Movie>()
    private val discoveredSeriesByKey = ConcurrentHashMap<String, Series>()
    private val discoveredInitialEpisodesBySeries = ConcurrentHashMap<String, List<Episode>>()
    @Volatile
    private var lastSelectedSeriesKey: String = "Young Ibn Sina"

    fun resolveSiteRootUrl(): String {
        val raw = sessionManager.getConfiguredBaseUrl().trim()
        val stripped = raw
            .replace(Regex("/api/v1/?$", RegexOption.IGNORE_CASE), "")
            .trimEnd('/')
        return if (stripped.startsWith("https://", ignoreCase = true)) {
            stripped
        } else {
            "https://hanixurdu.com"
        }
    }

    fun resolveAssetUrl(rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null
        val trimmed = rawPath.trim().replace(" ", "%20")
        if (trimmed.startsWith("https://", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
        }
        val clean = trimmed
            .removePrefix("./")
            .removePrefix("/")
        return "${resolveSiteRootUrl()}/$clean"
    }

    fun normalizeEmbedOrStreamUrl(rawUrl: String, declaredServerType: String? = null): Pair<String, String> {
        var extracted = rawUrl.trim()
        // If admin pasted a raw <iframe src="..."> tag into video_link/server1_link/server2_link, extract src
        if (extracted.contains("<iframe", ignoreCase = true)) {
            val iframeMatch = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(extracted)
            if (iframeMatch != null) {
                extracted = decodeHtmlText(iframeMatch.groupValues[1])
            }
        }
        var url = extracted.trim().replace(" ", "%20")
        if (url.startsWith("http://", ignoreCase = true)) {
            url = "https://" + url.substring(7)
        } else if (url.startsWith("//")) {
            url = "https:$url"
        }

        val lower = url.lowercase()
        if (lower.endsWith(".m3u8") || lower.contains(".m3u8?")) {
            return "hls" to url
        }
        if (declaredServerType.equals("video", ignoreCase = true) ||
            lower.endsWith(".mp4") ||
            lower.contains(".mp4?") ||
            lower.endsWith(".mkv") ||
            lower.contains("/videos/")
        ) {
            return "direct" to url
        }

        // Convert watch links to embed links matching run_video.php lines 195-227
        val ytWatchRegex = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]+)")
        val ytMatch = ytWatchRegex.find(url)
        if (ytMatch != null && !lower.contains("/embed/")) {
            url = "https://www.youtube.com/embed/${ytMatch.groupValues[1]}"
        }

        if (lower.contains("rumble.com") && !lower.contains("/embed/")) {
            val rumbleIdMatch = Regex("/(v[a-zA-Z0-9]+)").find(url)
            if (rumbleIdMatch != null) {
                url = "https://rumble.com/embed/${rumbleIdMatch.groupValues[1]}/"
            }
        }

        val dmRegex = Regex("(?:dailymotion\\.com/video/|dai\\.ly/)([a-zA-Z0-9]+)")
        val dmMatch = dmRegex.find(url)
        if (dmMatch != null && !lower.contains("geo.dailymotion.com") && !lower.contains("/embed/")) {
            url = "https://www.dailymotion.com/embed/video/${dmMatch.groupValues[1]}"
        }

        val driveRegex = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)")
        val driveMatch = driveRegex.find(url)
        if (driveMatch != null && !lower.contains("/preview")) {
            url = "https://drive.google.com/file/d/${driveMatch.groupValues[1]}/preview"
        } else if (lower.contains("drive.google.com") && lower.contains("/view")) {
            url = url.replace("/view", "/preview")
        }

        return "embed" to url
    }

    private fun decodeHtmlText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (_: Exception) {
            raw.replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&quot;", "\"")
                .trim()
        }
    }

    private fun decodeUrlParam(raw: String): String {
        return try {
            URLDecoder.decode(raw, "UTF-8").trim()
        } catch (_: Exception) {
            raw.trim()
        }
    }

    private fun encodeUrlParam(raw: String): String {
        return try {
            URLEncoder.encode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
    }

    private fun executeGet(url: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/json,*/*")
            .build()
        httpClient.newCall(request).execute().use { response ->
            return response.code to (response.body?.string().orEmpty())
        }
    }

    /**
     * Queries `https://hanixurdu.com/search.php?q={query}` which returns a pure JSON array
     * of real MySQL records from `movies` and `series`.
     */
    private fun querySearchJsonSync(query: String): List<Movie> {
        val siteRoot = resolveSiteRootUrl()
        val url = "$siteRoot/search.php?q=${encodeUrlParam(query)}"
        return try {
            val (code, body) = executeGet(url)
            if (code !in 200..299 || !body.trimStart().startsWith("[")) {
                emptyList()
            } else {
                parseSearchJsonArray(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSearchJsonArray(jsonStr: String): List<Movie> {
        val result = mutableListOf<Movie>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = decodeHtmlText(obj.optString("title", ""))
                val desc = decodeHtmlText(obj.optString("description", ""))
                val category = obj.optString("category", "").trim()
                val posterRaw = obj.optString("poster_path", "").trim()
                val posterUrl = resolveAssetUrl(posterRaw)
                val seriesName = obj.optString("series_name", "").trim().takeIf {
                    it.isNotBlank() &&
                        !it.equals("null", ignoreCase = true) &&
                        !it.equals("General", ignoreCase = true)
                }
                val hasVideo = obj.optBoolean("has_video", true)
                val isPrem = obj.optBoolean("is_premium", false) ||
                    obj.optInt("is_premium", 0) == 1 ||
                    category.contains("premium", ignoreCase = true)

                val isSeriesEntry = seriesName != null && title.equals(seriesName, ignoreCase = true)
                val resolvedId = id.ifBlank { seriesName ?: title }
                if (resolvedId.isBlank() || title.isBlank()) continue

                val movie = Movie(
                    id = resolvedId,
                    title = title,
                    description = desc.ifBlank { null },
                    poster = posterUrl,
                    posterPath = posterUrl,
                    category = category.ifBlank { "trending" },
                    seriesName = seriesName,
                    contentType = if (isSeriesEntry) "series" else "movie",
                    isPremium = isPrem,
                    isPremiumSnake = isPrem,
                    hasVideo = hasVideo
                )
                result.add(movie)
                if (id.isNotBlank()) {
                    discoveredMoviesById[id] = movie
                }
                if (!seriesName.isNullOrBlank()) {
                    val existingSeries = discoveredSeriesByKey[seriesName]
                    if (existingSeries == null) {
                        discoveredSeriesByKey[seriesName] = Series(
                            id = seriesName,
                            title = seriesName,
                            seriesName = seriesName,
                            indexTitle = seriesName,
                            description = desc.ifBlank { "Watch $seriesName in Urdu Dubbing & Subtitles on HaniX Urdu." },
                            poster = posterUrl,
                            posterPath = posterUrl,
                            indexPoster = posterUrl,
                            category = category.ifBlank { "trending" },
                            isPremium = isPrem
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    /**
     * Parses `https://hanixurdu.com/index.php` + live `search.php` JSON to build the complete HomePayload
     * matching the live website's 3D Coverflow and 5 Category Rails.
     */
    suspend fun fetchLiveHomePayload(): RemoteDataState<HomePayload> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        try {
            coroutineScope {
                val indexDeferred = async { executeGet("$siteRoot/index.php") }
                val searchADeferred = async { querySearchJsonSync("a") }
                val searchEDeferred = async { querySearchJsonSync("e") }
                val searchIDeferred = async { querySearchJsonSync("i") }
                val searchSDeferred = async { querySearchJsonSync("s") }
                val searchMDeferred = async { querySearchJsonSync("m") }

                // Refresh logged-in user's live VIP status from check_premium.php in parallel
                val currentUser = sessionManager.currentUser.value
                val currentUserEmail = currentUser?.email?.trim().orEmpty()
                if (currentUser != null && currentUserEmail.isNotBlank()) {
                    val isLiveVip = checkEmailVipOnServerSync(currentUserEmail)
                    if (currentUser.isActiveVip != isLiveVip) {
                        sessionManager.updateCurrentUser(
                            currentUser.copy(
                                membership = if (isLiveVip) "VIP" else "FREE",
                                vipStatus = isLiveVip,
                                legacyIsVip = isLiveVip
                            )
                        )
                    }
                }

                val (code, html) = indexDeferred.await()
                val searchItems = (
                    searchADeferred.await() +
                        searchEDeferred.await() +
                        searchIDeferred.await() +
                        searchSDeferred.await() +
                        searchMDeferred.await()
                    ).distinctBy { "${it.resolvedId}_${it.title}" }

                if (code !in 200..299 || html.isBlank()) {
                    return@coroutineScope RemoteDataState.Error(
                        endpoint = "GET /index.php",
                        httpCode = code,
                        message = "Could not load hanixurdu.com/index.php (HTTP $code)."
                    )
                }

                // 1. Parse Category Sections FIRST (`<section class="trending-section"...>`) so all titles/posters are indexed
                val sectionRegex = Regex(
                    """<section\s+class=["']trending-section["'][^>]*>[\s\S]*?<h2\s+class=["']trending-subtitle["'][^>]*>([\s\S]*?)</h2>([\s\S]*?)</section>""",
                    RegexOption.IGNORE_CASE
                )

                val urduSubtitles = mutableListOf<Movie>()
                val allContent = mutableListOf<Movie>()
                val comingSoon = mutableListOf<Movie>()
                val hanixOriginals = mutableListOf<Movie>()
                val premiumContent = mutableListOf<Movie>()

                for (secMatch in sectionRegex.findAll(html)) {
                    val sectionHeading = decodeHtmlText(secMatch.groupValues[1]).lowercase()
                    val sectionBody = secMatch.groupValues[2]

                    val targetList = when {
                        sectionHeading.contains("urdu") -> urduSubtitles
                        sectionHeading.contains("coming") -> comingSoon
                        sectionHeading.contains("original") || sectionHeading.contains("dubbing") -> hanixOriginals
                        sectionHeading.contains("premium") || sectionHeading.contains("vip") -> premiumContent
                        else -> allContent
                    }

                    val categorySlug = when {
                        sectionHeading.contains("urdu") -> "urdu_subtitles"
                        sectionHeading.contains("coming") -> "coming_soon"
                        sectionHeading.contains("original") -> "fan_dubbing"
                        sectionHeading.contains("premium") -> "premium"
                        else -> "all"
                    }

                    // Split by `<div class="trending-card` so nested divs in Premium cards never break parsing
                    val cardChunks = sectionBody.split(
                        Regex("""<div\s+class=["']trending-card""", RegexOption.IGNORE_CASE)
                    ).drop(1)

                    for (chunk in cardChunks) {
                        val clickMatch = Regex(
                            """handleCardClick\(\s*event\s*,\s*['"]([^'"]+)['"]\s*,\s*(true|false)\s*\)""",
                            RegexOption.IGNORE_CASE
                        ).find(chunk)
                        val rawUrl = clickMatch?.groupValues?.get(1)?.let { decodeHtmlText(it) } ?: continue
                        val isPrem = (clickMatch.groupValues[2].equals("true", ignoreCase = true)) ||
                            categorySlug == "premium" ||
                            chunk.contains("vip-crown-badge", ignoreCase = true) ||
                            chunk.contains("Unlock VIP", ignoreCase = true)

                        val rawPoster = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                            .find(chunk)?.groupValues?.get(1)
                        val h4Title = Regex("""<h4[^>]*class=["']poster-title["'][^>]*>([\s\S]*?)</h4>""", RegexOption.IGNORE_CASE)
                            .find(chunk)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

                        val parsedTarget = parseTargetUrl(rawUrl, h4Title)
                        val posterUrl = resolveAssetUrl(rawPoster)

                        val item = Movie(
                            id = parsedTarget.id,
                            title = parsedTarget.title,
                            description = discoveredMoviesById[parsedTarget.id]?.description
                                ?: "Watch ${parsedTarget.title} on HaniX Urdu.",
                            poster = posterUrl,
                            posterPath = posterUrl,
                            category = categorySlug,
                            seriesName = parsedTarget.seriesName,
                            contentType = parsedTarget.contentType,
                            isPremium = isPrem,
                            isPremiumSnake = isPrem,
                            hasVideo = categorySlug != "coming_soon"
                        )
                        targetList.add(item)

                        if (parsedTarget.contentType == "series" && !parsedTarget.seriesName.isNullOrBlank()) {
                            val sKey = parsedTarget.seriesName
                            val existing = discoveredSeriesByKey[sKey]
                            discoveredSeriesByKey[sKey] = Series(
                                id = sKey,
                                title = parsedTarget.title,
                                seriesName = sKey,
                                indexTitle = parsedTarget.title,
                                description = existing?.description ?: "Watch ${parsedTarget.title} all seasons and episodes in Urdu.",
                                poster = existing?.resolvedPoster ?: posterUrl,
                                posterPath = posterUrl,
                                indexPoster = existing?.indexPoster ?: posterUrl,
                                category = if (existing?.category != null && !existing.category.contains(categorySlug)) {
                                    "${existing.category},$categorySlug"
                                } else {
                                    existing?.category ?: categorySlug
                                },
                                isPremium = isPrem || (existing?.isPremium == true)
                            )
                        } else {
                            discoveredMoviesById[parsedTarget.id] = item
                        }
                    }
                }

                // 2. Parse 3D Coverflow cards (`<div class="coverflow-card" ...>`)
                val banners = mutableListOf<BannerItem>()
                val coverflowCardTagRegex = Regex(
                    """<div\s+class=["']coverflow-card["'][^>]*data-poster=["']([^"']+)["'][^>]*data-url=["']([^"']+)["'][^>]*data-premium=["']([^"']+)["'][^>]*>""",
                    RegexOption.IGNORE_CASE
                )
                for (match in coverflowCardTagRegex.findAll(html)) {
                    val rawPoster = match.groupValues[1]
                    val rawUrl = decodeHtmlText(match.groupValues[2])
                    val isPrem = match.groupValues[3].equals("true", ignoreCase = true)

                    val parsedTarget = parseTargetUrl(rawUrl, null)
                    val posterUrl = resolveAssetUrl(rawPoster)
                    val resolvedTitle = if (parsedTarget.contentType == "series" && !parsedTarget.seriesName.isNullOrBlank()) {
                        discoveredSeriesByKey[parsedTarget.seriesName]?.resolvedTitle ?: parsedTarget.title
                    } else {
                        discoveredMoviesById[parsedTarget.id]?.title ?: parsedTarget.title
                    }

                    banners.add(
                        BannerItem(
                            id = "banner_${banners.size}_${parsedTarget.id}",
                            title = resolvedTitle,
                            subtitle = if (isPrem) "VIP Exclusive • Urdu Dubbed" else "Trending on HaniX Urdu",
                            backdropUrl = posterUrl,
                            posterPath = posterUrl,
                            contentType = parsedTarget.contentType,
                            contentId = parsedTarget.id,
                            seriesName = parsedTarget.seriesName,
                            isPremium = isPrem,
                            isPremiumSnake = isPrem
                        )
                    )

                    if (parsedTarget.contentType == "series" && !parsedTarget.seriesName.isNullOrBlank()) {
                        val sKey = parsedTarget.seriesName
                        val existing = discoveredSeriesByKey[sKey]
                        discoveredSeriesByKey[sKey] = Series(
                            id = sKey,
                            title = resolvedTitle,
                            seriesName = sKey,
                            indexTitle = resolvedTitle,
                            description = existing?.description ?: "Stream $resolvedTitle in HD Urdu Dubbed & Subtitles on HaniX Urdu.",
                            poster = existing?.resolvedPoster ?: posterUrl,
                            posterPath = posterUrl,
                            indexPoster = posterUrl,
                            category = existing?.category ?: (if (isPrem) "trending,premium" else "trending"),
                            isPremium = isPrem || (existing?.isPremium == true)
                        )
                    }
                }

                // Merge any extra movies discovered from search.php into latestMovies / allContent
                val standaloneSearchMovies = searchItems.filter { !it.resolvedIsSeriesItem }
                if (allContent.isEmpty() && standaloneSearchMovies.isNotEmpty()) {
                    allContent.addAll(standaloneSearchMovies)
                }

                val featuredMovies = banners.map { b ->
                    Movie(
                        id = b.contentId,
                        title = b.title,
                        description = b.subtitle,
                        poster = b.posterPath,
                        posterPath = b.posterPath,
                        category = "trending",
                        seriesName = b.seriesName,
                        contentType = b.contentType,
                        isPremium = b.resolvedIsVip,
                        isPremiumSnake = b.resolvedIsVip,
                        hasVideo = true
                    )
                }.ifEmpty { allContent.take(8) }

                val featuredSeries = discoveredSeriesByKey.values.toList()
                val latestMovies = (standaloneSearchMovies + allContent)
                    .distinctBy { it.resolvedId }
                    .take(20)

                val categories = listOf(
                    CategoryItem(id = "trending", name = "Trending Coverflow", slug = "trending", itemCount = featuredMovies.size),
                    CategoryItem(id = "urdu_subtitles", name = "Urdu Subtitles", slug = "urdu_subtitles", itemCount = urduSubtitles.size),
                    CategoryItem(id = "all", name = "All Content", slug = "all", itemCount = allContent.size),
                    CategoryItem(id = "coming_soon", name = "Coming Soon", slug = "coming_soon", itemCount = comingSoon.size),
                    CategoryItem(id = "fan_dubbing", name = "Hanix Originals", slug = "fan_dubbing", itemCount = hanixOriginals.size),
                    CategoryItem(id = "premium", name = "Premium (VIP)", slug = "premium", itemCount = premiumContent.size)
                )

                RemoteDataState.Success(
                    data = HomePayload(
                        banners = banners,
                        trendingMovies = featuredMovies,
                        trendingSeries = featuredSeries,
                        featuredMovies = featuredMovies,
                        featuredSeries = featuredSeries,
                        latestMovies = latestMovies,
                        latestSeries = featuredSeries,
                        urduSubtitles = urduSubtitles,
                        allContent = allContent,
                        comingSoon = comingSoon,
                        hanixOriginals = hanixOriginals,
                        premiumContent = premiumContent,
                        categories = categories
                    ),
                    endpoint = "hanixurdu.com/index.php (Live)"
                )
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "GET /index.php",
                httpCode = null,
                message = "Failed to parse live hanixurdu.com/index.php: ${e.localizedMessage}"
            )
        }
    }

    private data class ParsedCardTarget(
        val id: String,
        val title: String,
        val contentType: String,
        val seriesName: String?
    )

    private fun parseTargetUrl(rawUrl: String, explicitTitle: String?): ParsedCardTarget {
        val trimmed = rawUrl.trim()
        if (trimmed.contains("series.php", ignoreCase = true)) {
            val subRaw = trimmed.substringAfter("sub=", "").substringBefore("&")
            val seriesName = decodeUrlParam(subRaw).ifBlank { explicitTitle ?: "Urdu Series" }
            val knownSeriesTitle = discoveredSeriesByKey[seriesName]?.resolvedTitle
            val cleanTitle = explicitTitle?.takeIf { it.isNotBlank() && !it.equals("Poster", ignoreCase = true) }
                ?: knownSeriesTitle
                ?: seriesName
            return ParsedCardTarget(
                id = seriesName,
                title = cleanTitle,
                contentType = "series",
                seriesName = seriesName
            )
        }
        if (trimmed.contains("run_video.php", ignoreCase = true)) {
            val idParam = trimmed.substringAfter("id=", "").substringBefore("&").trim()
            val knownMovie = discoveredMoviesById[idParam]
            val cleanTitle = explicitTitle?.takeIf { it.isNotBlank() && !it.equals("Poster", ignoreCase = true) }
                ?: knownMovie?.title
                ?: "HaniX Stream #$idParam"
            val seriesName = knownMovie?.seriesName?.takeIf {
                it.isNotBlank() && !it.equals("General", ignoreCase = true)
            }
            return ParsedCardTarget(
                id = idParam.ifBlank { cleanTitle },
                title = cleanTitle,
                contentType = "movie",
                seriesName = seriesName
            )
        }
        val slug = decodeUrlParam(trimmed.removePrefix("./").removePrefix("/"))
        val cleanTitle = explicitTitle?.takeIf { it.isNotBlank() && !it.equals("Poster", ignoreCase = true) }
            ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }
        return ParsedCardTarget(
            id = slug.ifBlank { cleanTitle },
            title = cleanTitle,
            contentType = "movie",
            seriesName = null
        )
    }

    suspend fun fetchLiveMovies(category: String? = null): RemoteDataState<List<Movie>> = withContext(Dispatchers.IO) {
        if (discoveredMoviesById.isEmpty()) {
            fetchLiveHomePayload()
        }
        val extraSearch = coroutineScope {
            val q1 = async { querySearchJsonSync("a") }
            val q2 = async { querySearchJsonSync("e") }
            val q3 = async { querySearchJsonSync("i") }
            val q4 = async { querySearchJsonSync("o") }
            val q5 = async { querySearchJsonSync("u") }
            (q1.await() + q2.await() + q3.await() + q4.await() + q5.await())
        }
        val allMovies = (discoveredMoviesById.values + extraSearch)
            .distinctBy { it.resolvedId }
            .filter { movie ->
                if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) {
                    true
                } else {
                    movie.category?.contains(category, ignoreCase = true) == true
                }
            }

        if (allMovies.isEmpty()) {
            RemoteDataState.Empty("hanixurdu.com/index.php", "No movies found for category ${category ?: "all"}.")
        } else {
            RemoteDataState.Success(allMovies, "hanixurdu.com/index.php (Live)")
        }
    }

    suspend fun fetchLiveSeriesList(category: String? = null): RemoteDataState<List<Series>> = withContext(Dispatchers.IO) {
        if (discoveredSeriesByKey.isEmpty()) {
            fetchLiveHomePayload()
        }
        val list = discoveredSeriesByKey.values
            .filter { s ->
                if (category.isNullOrBlank() || category.equals("all", ignoreCase = true)) {
                    true
                } else {
                    s.category?.contains(category, ignoreCase = true) == true
                }
            }
            .distinctBy { it.resolvedId }

        if (list.isEmpty()) {
            RemoteDataState.Empty("hanixurdu.com/series.php", "No series found.")
        } else {
            RemoteDataState.Success(list, "hanixurdu.com/series.php (Live)")
        }
    }

    suspend fun fetchLiveMovieDetails(movieId: String): RemoteDataState<Movie> = withContext(Dispatchers.IO) {
        val cleanedId = decodeUrlParam(movieId)
        discoveredMoviesById[cleanedId]?.let { cached ->
            if (!cached.description.isNullOrBlank() && !cached.title.startsWith("HaniX Stream #")) {
                return@withContext RemoteDataState.Success(cached, "hanixurdu.com/run_video.php?id=$cleanedId")
            }
        }

        val siteRoot = resolveSiteRootUrl()
        try {
            val targetId = resolveNumericContentIdSync(cleanedId)
            val (code, html) = executeGet("$siteRoot/run_video.php?id=${encodeUrlParam(targetId)}")
            if (code in 200..299 && html.isNotBlank()) {
                val h2Title = Regex("""<h2[^>]*>([\s\S]*?)</h2>""", RegexOption.IGNORE_CASE)
                    .findAll(html)
                    .map { decodeHtmlText(it.groupValues[1]) }
                    .firstOrNull { it.isNotBlank() && !it.contains("Sign In", ignoreCase = true) && !it.contains("VIP Membership", ignoreCase = true) }
                val h1Title = Regex("""<h1[^>]*class=["']video-title["'][^>]*>([\s\S]*?)</h1>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                val desc = Regex("""<p\s+style=["']color:#aaa;[^"']*["'][^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                val isVipGate = html.contains("""id="vip-section" style="display: block""", ignoreCase = true) ||
                    html.contains("Yeh content Premium hai", ignoreCase = true)

                val resolvedTitle = h2Title?.takeIf { it.isNotBlank() && !it.contains("Video not found", ignoreCase = true) }
                    ?: h1Title?.takeIf { it.isNotBlank() }
                    ?: discoveredMoviesById[targetId]?.title
                    ?: "HaniX Stream #$targetId"

                val movie = Movie(
                    id = targetId,
                    title = resolvedTitle,
                    description = desc ?: discoveredMoviesById[targetId]?.description ?: "Stream $resolvedTitle on HaniX Urdu.",
                    poster = discoveredMoviesById[targetId]?.resolvedPoster,
                    posterPath = discoveredMoviesById[targetId]?.resolvedPoster,
                    category = discoveredMoviesById[targetId]?.category ?: "trending",
                    seriesName = discoveredMoviesById[targetId]?.seriesName,
                    isPremium = isVipGate || (discoveredMoviesById[targetId]?.resolvedIsVip == true),
                    isPremiumSnake = isVipGate || (discoveredMoviesById[targetId]?.resolvedIsVip == true),
                    hasVideo = true
                )
                discoveredMoviesById[targetId] = movie
                return@withContext RemoteDataState.Success(movie, "hanixurdu.com/run_video.php?id=$targetId")
            }
        } catch (_: Exception) {
        }

        discoveredMoviesById[cleanedId]?.let {
            return@withContext RemoteDataState.Success(it, "hanixurdu.com/run_video.php?id=$cleanedId")
        }

        RemoteDataState.Error(
            endpoint = "GET /run_video.php?id=$cleanedId",
            httpCode = 404,
            message = "Movie #$cleanedId was not found on hanixurdu.com."
        )
    }

    private fun resolveSeriesName(seriesIdOrName: String): String {
        val decoded = decodeUrlParam(seriesIdOrName)
        if (decoded.isBlank() || decoded == "1") {
            if (discoveredSeriesByKey.isNotEmpty()) {
                return discoveredSeriesByKey.keys.first()
            }
            return "Young Ibn Sina"
        }
        discoveredSeriesByKey.keys.firstOrNull { it.equals(decoded, ignoreCase = true) }?.let { return it }
        discoveredSeriesByKey.values.firstOrNull { it.resolvedTitle.equals(decoded, ignoreCase = true) }?.seriesName?.let { return it }
        return decoded
    }

    /**
     * Loads a Series + all of its available Seasons from `https://hanixurdu.com/series.php?sub={seriesName}`.
     * Matches the exact HTML from `series.php`:
     * - `.series-hero` background-image
     * - `.series-title`
     * - `#descText` / `.desc-text`
     * - `switchSeason('Season 1', this)` and `selectSeason('Season 1')`
     * - Initial season's `.episode-card` items (caching them immediately!)
     * - `.rec-card` related series items
     */
    suspend fun fetchLiveSeriesDetailsAndSeasons(seriesId: String): Pair<RemoteDataState<Series>, RemoteDataState<List<Season>>> =
        withContext(Dispatchers.IO) {
            if (discoveredSeriesByKey.isEmpty()) {
                fetchLiveHomePayload()
            }
            val seriesName = resolveSeriesName(seriesId)
            lastSelectedSeriesKey = seriesName
            val siteRoot = resolveSiteRootUrl()
            try {
                val (code, html) = executeGet("$siteRoot/series.php?sub=${encodeUrlParam(seriesName)}")
                if (code in 200..299 && html.isNotBlank()) {
                    val title = Regex("""<h1\s+class=["']series-title["'][^>]*>([\s\S]*?)</h1>""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                        ?.takeIf { it.isNotBlank() }
                        ?: discoveredSeriesByKey[seriesName]?.resolvedTitle
                        ?: seriesName

                    val desc = Regex("""<div[^>]*class=["']desc-text["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                        ?.takeIf { it.isNotBlank() }
                        ?: Regex("""<div\s+class=["']series-desc-section["'][\s\S]*?<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
                            .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                            ?.takeIf { it.isNotBlank() }
                        ?: discoveredSeriesByKey[seriesName]?.description
                        ?: "Watch $title in Urdu Dubbing & Subtitles on HaniX Urdu."

                    val backdropRaw = Regex("""class=["'](?:series-hero|hero-backdrop)["'][^>]*background-image:\s*url\(['"]?([^'")]+)['"]?\)""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.get(1)
                    val posterUrl = resolveAssetUrl(backdropRaw) ?: discoveredSeriesByKey[seriesName]?.resolvedPoster

                    val trailerUrl = Regex("""playTrailer\(\s*['"]([^'"]+)['"]\s*\)""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.get(1)
                        ?: Regex("""<div\s+class=["']trailer-container["'][\s\S]*?<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                            .find(html)?.groupValues?.get(1)

                    // Parse seasons from BOTH `switchSeason('Season 1', this)` AND `selectSeason('Season 1')`
                    val switchSeasons = Regex("""switchSeason\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
                        .findAll(html)
                        .map { decodeHtmlText(it.groupValues[1]) }
                    val selectSeasons = Regex("""selectSeason\(\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
                        .findAll(html)
                        .map { decodeHtmlText(it.groupValues[1]) }
                    val moreInfoSeasons = Regex("""<strong>Available Seasons:</strong>\s*([^<]+)</p>""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.get(1)
                        ?.split(",")
                        ?.map { decodeHtmlText(it).trim() }
                        .orEmpty()

                    val seasonNames = (switchSeasons.toList() + selectSeasons.toList() + moreInfoSeasons)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .ifEmpty { listOf("Season 1") }

                    val isVipSeries = html.contains("vipSeriesLabel", ignoreCase = true) ||
                        (discoveredSeriesByKey[seriesName]?.resolvedIsVip == true)

                    val seriesObj = Series(
                        id = seriesName,
                        title = title,
                        seriesName = seriesName,
                        indexTitle = title,
                        description = desc,
                        poster = posterUrl,
                        posterPath = posterUrl,
                        indexPoster = posterUrl,
                        category = discoveredSeriesByKey[seriesName]?.category ?: "trending",
                        trailerLink = trailerUrl,
                        isPremium = isVipSeries
                    )
                    discoveredSeriesByKey[seriesName] = seriesObj

                    // Also discover any related series from `#relatedTab` (`.rec-card`)
                    val recChunks = html.split(Regex("""<div\s+class=["']rec-card["']""", RegexOption.IGNORE_CASE)).drop(1)
                    for (recChunk in recChunks) {
                        val locMatch = Regex("""location\.href=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(recChunk)
                        val targetUrl = locMatch?.groupValues?.get(1)?.let { decodeHtmlText(it) } ?: continue
                        val recPoster = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                            .find(recChunk)?.groupValues?.get(1)
                        val recTitle = Regex("""<h4\s+class=["']rec-title["'][^>]*>([\s\S]*?)</h4>""", RegexOption.IGNORE_CASE)
                            .find(recChunk)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                        val parsed = parseTargetUrl(targetUrl, recTitle)
                        if (parsed.contentType == "series" && !parsed.seriesName.isNullOrBlank()) {
                            discoveredSeriesByKey.putIfAbsent(
                                parsed.seriesName,
                                Series(
                                    id = parsed.seriesName,
                                    title = parsed.title,
                                    seriesName = parsed.seriesName,
                                    indexTitle = parsed.title,
                                    description = "Watch ${parsed.title} in Urdu Dubbing & Subtitles on HaniX Urdu.",
                                    poster = resolveAssetUrl(recPoster),
                                    posterPath = resolveAssetUrl(recPoster),
                                    indexPoster = resolveAssetUrl(recPoster),
                                    category = "trending",
                                    isPremium = recChunk.contains("vip-crown-badge", ignoreCase = true)
                                )
                            )
                        }
                    }

                    val seasons = seasonNames.map { sName ->
                        Season(
                            id = "$seriesName||$sName",
                            seriesId = seriesName,
                            seriesName = seriesName,
                            seasonName = sName,
                            seasonNameSnake = sName
                        )
                    }

                    // Parse any initial episodes rendered inside #episodesListContainer on series.php?sub=...
                    // so newly uploaded dramas or episodes with season_name='General'/'' immediately slot in!
                    val defaultSeasonName = seasonNames.firstOrNull() ?: "Season 1"
                    val currentUser = sessionManager.currentUser.value
                    val isUserActiveVip = currentUser?.isActiveVip == true
                    val searchByTitle = querySearchJsonSync(seriesName).associateBy { it.title.lowercase().trim() }
                    val initialEpisodes = parseEpisodeCardsFromHtml(
                        html = html,
                        seriesName = seriesName,
                        seasonName = defaultSeasonName,
                        searchByTitle = searchByTitle,
                        isUserActiveVip = isUserActiveVip
                    )
                    if (initialEpisodes.isNotEmpty()) {
                        discoveredInitialEpisodesBySeries[seriesName] = initialEpisodes
                    }

                    return@withContext RemoteDataState.Success(seriesObj, "hanixurdu.com/series.php?sub=$seriesName") to
                        RemoteDataState.Success(seasons, "hanixurdu.com/series.php?sub=$seriesName")
                }
            } catch (_: Exception) {
            }

            val fallbackSeries = discoveredSeriesByKey[seriesName] ?: Series(
                id = seriesName,
                title = seriesName,
                seriesName = seriesName,
                description = "Stream $seriesName on HaniX Urdu."
            )
            val fallbackSeasons = listOf(
                Season(
                    id = "$seriesName||Season 1",
                    seriesId = seriesName,
                    seriesName = seriesName,
                    seasonName = "Season 1",
                    seasonNameSnake = "Season 1"
                )
            )
            RemoteDataState.Success(fallbackSeries, "hanixurdu.com/series.php") to
                RemoteDataState.Success(fallbackSeasons, "hanixurdu.com/series.php")
        }

    private fun parseEpisodeCardsFromHtml(
        html: String,
        seriesName: String,
        seasonName: String,
        searchByTitle: Map<String, Movie>,
        isUserActiveVip: Boolean
    ): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val cardBlocks = html.split(Regex("""<div\s+class=["']episode-card["']""", RegexOption.IGNORE_CASE))
            .drop(1)
        for ((idx, block) in cardBlocks.withIndex()) {
            // Matches `playEpisode(123, 'premium')` or `playEpisode(123)`
            val playCallMatch = Regex(
                """playEpisode\(\s*(\d+)(?:\s*,\s*['"]([^'"]*)['"])?\s*\)""",
                RegexOption.IGNORE_CASE
            ).find(block)
            val directNumericId = playCallMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            val playCategoryArg = playCallMatch?.groupValues?.getOrNull(2).orEmpty()

            val isVipTaggedInHtml = playCategoryArg.equals("premium", ignoreCase = true) ||
                block.contains("vip-crown-badge", ignoreCase = true) ||
                block.contains("Locked VIP", ignoreCase = true) ||
                block.contains("openVipModal", ignoreCase = true) ||
                block.contains("vip-badge", ignoreCase = true)

            val rawPoster = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)
            val epTitle = Regex("""<h4\s+class=["']episode-title["'][^>]*>([\s\S]*?)</h4>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                ?: "$seriesName - Episode ${idx + 1}"
            val epDesc = Regex("""<p\s+class=["']episode-desc["'][^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

            var matchedSearch = searchByTitle[epTitle.lowercase().trim()]
            if (directNumericId == null && matchedSearch == null) {
                matchedSearch = querySearchJsonSync(epTitle).firstOrNull {
                    it.title.equals(epTitle, ignoreCase = true) ||
                        it.seriesName.equals(seriesName, ignoreCase = true)
                }
            }

            val resolvedId = directNumericId
                ?: matchedSearch?.resolvedId
                ?: "${seriesName}_ep_${idx + 1}"
            val posterUrl = resolveAssetUrl(rawPoster)
                ?: matchedSearch?.resolvedPoster
                ?: discoveredSeriesByKey[seriesName]?.resolvedPoster
            val rawIsVip = isVipTaggedInHtml || (matchedSearch?.resolvedIsVip == true)

            // If the user is an active VIP, unlock VIP episodes so they play immediately on tap!
            val effectiveIsVipLocked = rawIsVip && !isUserActiveVip

            val ep = Episode(
                id = resolvedId,
                seasonId = "$seriesName||$seasonName",
                seriesName = seriesName,
                seasonName = seasonName,
                title = epTitle,
                description = epDesc?.ifBlank { null }
                    ?: matchedSearch?.description
                    ?: "$seriesName • $seasonName • Episode ${idx + 1}",
                poster = posterUrl,
                posterPath = posterUrl,
                isPremium = effectiveIsVipLocked,
                isPremiumSnake = effectiveIsVipLocked,
                hasVideo = matchedSearch?.resolvedHasVideo ?: true
            )
            episodes.add(ep)

            discoveredMoviesById[resolvedId] = Movie(
                id = resolvedId,
                title = epTitle,
                description = ep.description,
                poster = posterUrl,
                posterPath = posterUrl,
                category = if (rawIsVip) "premium" else "trending",
                seriesName = seriesName,
                seasonName = seasonName,
                isPremium = effectiveIsVipLocked,
                isPremiumSnake = effectiveIsVipLocked,
                hasVideo = true
            )
        }
        return episodes
    }

    /**
     * Loads episodes for a Series + Season using `series.php?ajax=1&sub=...&season=...`,
     * fallback season queries (`General` / initial `series.php?sub=...` container), AND `search.php`
     * so any newly uploaded drama or episode automatically fits into its proper place.
     * When the logged-in user is an active VIP (`isUserActiveVip`), all VIP episodes unlock automatically.
     */
    suspend fun fetchLiveSeasonEpisodes(seasonId: String): RemoteDataState<List<Episode>> = withContext(Dispatchers.IO) {
        val decoded = decodeUrlParam(seasonId)
        val (rawSeriesName, seasonName) = if (decoded.contains("||")) {
            val parts = decoded.split("||", limit = 2)
            parts[0] to parts[1]
        } else if (decoded == "1" || decoded.startsWith("Season", ignoreCase = true)) {
            lastSelectedSeriesKey to (if (decoded == "1") "Season 1" else decoded)
        } else {
            decoded to "Season 1"
        }
        val seriesName = resolveSeriesName(rawSeriesName)

        val siteRoot = resolveSiteRootUrl()
        val currentUser = sessionManager.currentUser.value
        val userEmail = currentUser?.email?.trim().orEmpty()
        val isUserActiveVip = if (userEmail.isNotBlank()) {
            val verifiedVip = checkEmailVipOnServerSync(userEmail)
            if (currentUser != null && currentUser.isActiveVip != verifiedVip) {
                sessionManager.updateCurrentUser(
                    currentUser.copy(
                        membership = if (verifiedVip) "VIP" else "FREE",
                        vipStatus = verifiedVip,
                        legacyIsVip = verifiedVip
                    )
                )
            }
            verifiedVip
        } else {
            currentUser?.isActiveVip == true
        }

        try {
            coroutineScope {
                val ajaxUrl = "$siteRoot/series.php?ajax=1&sub=${encodeUrlParam(seriesName)}&season=${encodeUrlParam(seasonName)}"
                val ajaxDeferred = async { executeGet(ajaxUrl) }
                val searchDeferred = async { querySearchJsonSync(seriesName) }

                val (code, html) = ajaxDeferred.await()
                val searchMatches = searchDeferred.await()
                val searchByTitle = searchMatches.associateBy { it.title.lowercase().trim() }

                val episodes = mutableListOf<Episode>()
                if (code in 200..299 && html.isNotBlank()) {
                    episodes.addAll(
                        parseEpisodeCardsFromHtml(
                            html = html,
                            seriesName = seriesName,
                            seasonName = seasonName,
                            searchByTitle = searchByTitle,
                            isUserActiveVip = isUserActiveVip
                        )
                    )
                }

                // Fallback 1: If newly uploaded episodes were saved under 'General' or default season on series.php
                if (episodes.isEmpty()) {
                    val altSeason = if (seasonName.equals("Season 1", ignoreCase = true)) "General" else "Season 1"
                    val (altCode, altHtml) = executeGet(
                        "$siteRoot/series.php?ajax=1&sub=${encodeUrlParam(seriesName)}&season=${encodeUrlParam(altSeason)}"
                    )
                    if (altCode in 200..299 && altHtml.isNotBlank()) {
                        episodes.addAll(
                            parseEpisodeCardsFromHtml(
                                html = altHtml,
                                seriesName = seriesName,
                                seasonName = seasonName,
                                searchByTitle = searchByTitle,
                                isUserActiveVip = isUserActiveVip
                            )
                        )
                    }
                }

                // Fallback 2: Use episodes rendered directly inside series.php?sub=SeriesName
                if (episodes.isEmpty()) {
                    val cachedInitial = discoveredInitialEpisodesBySeries[seriesName]
                    if (!cachedInitial.isNullOrEmpty()) {
                        episodes.addAll(
                            cachedInitial.map { ep ->
                                val unlockedVip = ep.resolvedIsVip && !isUserActiveVip
                                ep.copy(
                                    seasonId = "$seriesName||$seasonName",
                                    seasonName = seasonName,
                                    isPremium = unlockedVip,
                                    isPremiumSnake = unlockedVip
                                )
                            }
                        )
                    } else {
                        val (mainCode, mainHtml) = executeGet("$siteRoot/series.php?sub=${encodeUrlParam(seriesName)}")
                        if (mainCode in 200..299 && mainHtml.isNotBlank()) {
                            val parsedMain = parseEpisodeCardsFromHtml(
                                html = mainHtml,
                                seriesName = seriesName,
                                seasonName = seasonName,
                                searchByTitle = searchByTitle,
                                isUserActiveVip = isUserActiveVip
                            )
                            if (parsedMain.isNotEmpty()) {
                                discoveredInitialEpisodesBySeries[seriesName] = parsedMain
                                episodes.addAll(parsedMain)
                            }
                        }
                    }
                }

                // Merge any newly uploaded episodes discovered via search.php that aren't in the list yet
                if (searchMatches.isNotEmpty()) {
                    val existingIds = episodes.map { it.id }.toMutableSet()
                    val existingTitles = episodes.map { it.title.lowercase().trim() }.toMutableSet()
                    searchMatches
                        .filter {
                            (it.seriesName.equals(seriesName, ignoreCase = true) ||
                                it.title.contains(seriesName, ignoreCase = true)) &&
                                !it.resolvedIsSeriesItem &&
                                !it.title.equals(seriesName, ignoreCase = true)
                        }
                        .forEach { m ->
                            if (m.resolvedId !in existingIds && m.title.lowercase().trim() !in existingTitles) {
                                val effectiveVip = m.resolvedIsVip && !isUserActiveVip
                                episodes.add(
                                    Episode(
                                        id = m.resolvedId,
                                        seasonId = "$seriesName||$seasonName",
                                        seriesName = seriesName,
                                        seasonName = seasonName,
                                        title = m.title,
                                        description = m.description,
                                        poster = m.resolvedPoster ?: discoveredSeriesByKey[seriesName]?.resolvedPoster,
                                        posterPath = m.resolvedPoster ?: discoveredSeriesByKey[seriesName]?.resolvedPoster,
                                        isPremium = effectiveVip,
                                        isPremiumSnake = effectiveVip,
                                        hasVideo = m.resolvedHasVideo
                                    )
                                )
                                existingIds.add(m.resolvedId)
                                existingTitles.add(m.title.lowercase().trim())
                            }
                        }
                }

                if (episodes.isEmpty()) {
                    RemoteDataState.Empty(
                        endpoint = "Series Episodes",
                        message = "No episodes uploaded yet for $seriesName ($seasonName)."
                    )
                } else {
                    RemoteDataState.Success(
                        data = episodes,
                        endpoint = "Series Episodes"
                    )
                }
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "Series Episodes",
                httpCode = null,
                message = "Failed to load episodes for $seriesName: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Live Search against `https://hanixurdu.com/search.php?q={query}`.
     */
    suspend fun searchLive(query: String): RemoteDataState<SearchPayload> = withContext(Dispatchers.IO) {
        val items = querySearchJsonSync(query)
        val movies = items.filter { !it.resolvedIsSeriesItem }
        val seriesMap = mutableMapOf<String, Series>()

        items.forEach { item ->
            val sName = item.seriesName
            if (!sName.isNullOrBlank()) {
                seriesMap.putIfAbsent(
                    sName,
                    discoveredSeriesByKey[sName] ?: Series(
                        id = sName,
                        title = sName,
                        seriesName = sName,
                        indexTitle = sName,
                        description = item.description,
                        poster = item.resolvedPoster,
                        posterPath = item.resolvedPoster,
                        indexPoster = item.resolvedPoster,
                        category = item.category,
                        isPremium = item.resolvedIsVip
                    )
                )
            }
        }

        RemoteDataState.Success(
            data = SearchPayload(
                query = query,
                page = 1,
                limit = 30,
                movies = movies,
                series = seriesMap.values.toList()
            ),
            endpoint = "hanixurdu.com/search.php?q=$query"
        )
    }

    /**
     * Resolves any non-numeric episode/movie slug (like `"Young Ibn Sina_ep_1"` or `"1"`)
     * to a real numeric database `id` from `hanixurdu.com`.
     */
    private fun resolveNumericContentIdSync(rawId: String): String {
        val cleaned = decodeUrlParam(rawId).trim()
        if (cleaned.all { it.isDigit() } && cleaned != "1") {
            return cleaned
        }
        if (cleaned == "1") {
            discoveredMoviesById["1"]?.let { return "1" }
            discoveredMoviesById.keys.firstOrNull { it.all { ch -> ch.isDigit() } }?.let { return it }
        }
        // If it's a series_ep_N fallback slug, look up in series.php or search.php
        val seriesPrefix = cleaned.substringBefore("_ep_").trim()
        val epNumber = cleaned.substringAfter("_ep_", "1").toIntOrNull() ?: 1
        if (seriesPrefix.isNotBlank()) {
            val siteRoot = resolveSiteRootUrl()
            try {
                val (code, html) = executeGet("$siteRoot/series.php?sub=${encodeUrlParam(seriesPrefix)}")
                if (code in 200..299 && html.isNotBlank()) {
                    val allIds = Regex("""playEpisode\(\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .findAll(html)
                        .map { it.groupValues[1] }
                        .distinct()
                        .toList()
                    if (allIds.isNotEmpty()) {
                        return allIds.getOrElse(epNumber - 1) { allIds.first() }
                    }
                }
            } catch (_: Exception) {
            }
            val searchHits = querySearchJsonSync(seriesPrefix)
            searchHits.firstOrNull { it.resolvedId.all { ch -> ch.isDigit() } }?.let {
                return it.resolvedId
            }
        }
        discoveredMoviesById.keys.firstOrNull { it.all { ch -> ch.isDigit() } }?.let { return it }
        return cleaned
    }

    /**
     * Authorizes and extracts Main Server, Server 2, AND Direct Web Player streams + Recommended movies
     * directly from `https://hanixurdu.com/run_video.php?id={contentId}` and `check_premium.php`.
     */
    suspend fun authorizeLivePlayback(
        contentType: String,
        contentId: String,
        preferredServer: String?
    ): RemoteDataState<PlaybackAuthorizationPayload> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        val cleanedId = resolveNumericContentIdSync(contentId)

        val currentUser = sessionManager.currentUser.value
        val userEmail = currentUser?.email?.trim().orEmpty()
        val isUserActiveVip = if (userEmail.isNotBlank()) {
            val verifiedVip = checkEmailVipOnServerSync(userEmail)
            if (currentUser != null && currentUser.isActiveVip != verifiedVip) {
                sessionManager.updateCurrentUser(
                    currentUser.copy(
                        membership = if (verifiedVip) "VIP" else "FREE",
                        vipStatus = verifiedVip,
                        legacyIsVip = verifiedVip
                    )
                )
            }
            verifiedVip
        } else {
            currentUser?.isActiveVip == true
        }

        try {
            val (code, html) = executeGet("$siteRoot/run_video.php?id=${encodeUrlParam(cleanedId)}")
            if (code !in 200..299 || html.isBlank()) {
                return@withContext RemoteDataState.Error(
                    endpoint = "GET /run_video.php?id=$cleanedId",
                    httpCode = code,
                    message = "Unable to reach run_video.php?id=$cleanedId (HTTP $code)."
                )
            }

            // Parse Recommended For You cards (`<div class="rec-card" onclick="location.href='...'">`)
            val recommended = mutableListOf<Movie>()
            val recChunks = html.split(Regex("""<div\s+class=["']rec-card["']""", RegexOption.IGNORE_CASE)).drop(1)
            for (recChunk in recChunks) {
                val locMatch = Regex("""location\.href=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(recChunk)
                val targetUrl = locMatch?.groupValues?.get(1)?.let { decodeHtmlText(it) } ?: continue
                val recPoster = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(recChunk)?.groupValues?.get(1)
                val recTitle = Regex("""<h4\s+class=["']rec-title["'][^>]*>([\s\S]*?)</h4>""", RegexOption.IGNORE_CASE)
                    .find(recChunk)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                val parsed = parseTargetUrl(targetUrl, recTitle)
                recommended.add(
                    Movie(
                        id = parsed.id,
                        title = parsed.title,
                        poster = resolveAssetUrl(recPoster),
                        posterPath = resolveAssetUrl(recPoster),
                        category = "recommended",
                        contentType = parsed.contentType,
                        seriesName = parsed.seriesName,
                        hasVideo = true
                    )
                )
            }

            // Extract server buttons: `onclick="changeServer('iframe'|'video', 'URL', this)"`
            val servers = mutableListOf<PlaybackServerDto>()
            val serverBtnRegex = Regex(
                """onclick=["']changeServer\(\s*['"]([^'"]+)['"]\s*,\s*['"]([^'"]+)['"]\s*,\s*this\s*\)["'][^>]*>([\s\S]*?)</button>""",
                RegexOption.IGNORE_CASE
            )
            for ((idx, m) in serverBtnRegex.findAll(html).withIndex()) {
                val serverTypeArg = m.groupValues[1].trim()
                val rawUrl = decodeHtmlText(m.groupValues[2])
                if (rawUrl.isBlank()) continue
                val label = decodeHtmlText(m.groupValues[3]).ifBlank { if (idx == 0) "Main Server" else "Server ${idx + 1}" }
                val resolvedRawUrl = if (rawUrl.startsWith("http", ignoreCase = true) || rawUrl.startsWith("//")) {
                    rawUrl
                } else {
                    resolveAssetUrl(rawUrl) ?: rawUrl
                }
                val (normType, normUrl) = normalizeEmbedOrStreamUrl(resolvedRawUrl, serverTypeArg)
                servers.add(
                    PlaybackServerDto(
                        id = if (idx == 0) "main" else "server${idx + 1}",
                        label = label,
                        type = normType,
                        url = normUrl
                    )
                )
            }

            // If no server buttons, inspect `#player-container` for `<iframe>` or `<source src="...">`
            if (servers.isEmpty()) {
                val playerContainerHtml = html.substringAfter("id=\"player-container\"", "")
                    .substringBefore("</div>")
                    .ifBlank { html }

                val iframeSrc = Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(playerContainerHtml)?.groupValues?.get(1)?.let { decodeHtmlText(it) }
                val videoSrc = Regex("""<source[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(playerContainerHtml)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

                val rawPrimary = iframeSrc ?: videoSrc
                val declaredKind = if (videoSrc != null && iframeSrc == null) "video" else "iframe"
                if (!rawPrimary.isNullOrBlank()) {
                    val resolvedRaw = if (rawPrimary.startsWith("http", ignoreCase = true) || rawPrimary.startsWith("//")) {
                        rawPrimary
                    } else {
                        resolveAssetUrl(rawPrimary) ?: rawPrimary
                    }
                    val (normType, normUrl) = normalizeEmbedOrStreamUrl(resolvedRaw, declaredKind)
                    servers.add(
                        PlaybackServerDto(
                            id = "main",
                            label = "Main Server",
                            type = normType,
                            url = normUrl
                        )
                    )
                }
            }

            // Always ensure a Server 2 option and Direct Web Player option are available
            val directWebPlayerUrl = "$siteRoot/run_video.php?id=${encodeUrlParam(cleanedId)}"
            if (servers.size == 1) {
                val primary = servers.first()
                // Provide an embed-mode Server 2 if Main Server is direct MP4/HLS, or Direct Web Player as Server 2
                if (primary.type == "direct" || primary.type == "hls") {
                    servers.add(
                        PlaybackServerDto(
                            id = "server2",
                            label = "Server 2 (Web Stream)",
                            type = "embed",
                            url = primary.url
                        )
                    )
                }
            }
            if (servers.none { it.id == "webplayer" }) {
                servers.add(
                    PlaybackServerDto(
                        id = "webplayer",
                        label = "Direct Web Player",
                        type = "webpage",
                        url = directWebPlayerUrl
                    )
                )
            }

            val isVipMarkedInHtml = html.contains("""id="vip-section" style="display: block""", ignoreCase = true) ||
                html.contains("const isMoviePremium = true", ignoreCase = true)
            val knownMovie = discoveredMoviesById[cleanedId]
            val requiresVip = isVipMarkedInHtml || (knownMovie?.resolvedIsVip == true)

            if (requiresVip && !isUserActiveVip) {
                return@withContext RemoteDataState.Success(
                    data = PlaybackAuthorizationPayload(
                        playbackSessionId = "vip_locked_$cleanedId",
                        contentId = cleanedId,
                        contentType = contentType,
                        membershipRequired = "VIP",
                        isAuthorized = false,
                        requiresVip = true,
                        processingStatus = "READY",
                        availableServers = emptyList(),
                        recommended = recommended
                    ),
                    endpoint = "hanixurdu.com/run_video.php?id=$cleanedId"
                )
            }

            val chosenServer = if (!preferredServer.isNullOrBlank()) {
                servers.find { it.id.equals(preferredServer, ignoreCase = true) } ?: servers.first()
            } else {
                servers.first()
            }

            val nowSec = System.currentTimeMillis() / 1000L
            RemoteDataState.Success(
                data = PlaybackAuthorizationPayload(
                    playbackSessionId = "live_${cleanedId}_$nowSec",
                    contentId = cleanedId,
                    contentIdSnake = cleanedId,
                    contentType = contentType,
                    playbackType = chosenServer.type,
                    playbackUrl = chosenServer.url,
                    authorizedHlsUrl = chosenServer.url,
                    selectedServer = chosenServer.id,
                    membershipRequired = if (requiresVip) "VIP" else "FREE",
                    isAuthorized = true,
                    requiresVip = requiresVip,
                    processingStatus = "READY",
                    tokenExpiresAtEpochSeconds = nowSec + 21600L,
                    customHeaders = mapOf("Referer" to "$siteRoot/"),
                    availableServers = servers,
                    recommended = recommended
                ),
                endpoint = "HaniX Live Player"
            )
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "HaniX Live Player",
                httpCode = null,
                message = "Playback authorization error: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Authorizes app-private encrypted downloads (`filesDir/hanix_offline_vault`)
     * for all logged-in users on FREE content and VIP members on VIP content.
     */
    suspend fun authorizeLiveDownload(
        contentType: String,
        contentId: String,
        qualityTier: String = "720p"
    ): RemoteDataState<DownloadAuthorizationPayload> = withContext(Dispatchers.IO) {
        val currentUser = sessionManager.currentUser.value
        val userEmail = currentUser?.email?.trim().orEmpty()
        if (currentUser == null || userEmail.isBlank()) {
            return@withContext RemoteDataState.AuthRequired(
                endpoint = "Offline Vault",
                message = "Please sign in with your HaniX Urdu account to download movies and episodes to your private vault."
            )
        }

        val cleanedId = resolveNumericContentIdSync(contentId)
        val playbackAuth = authorizeLivePlayback(contentType, cleanedId, "main")
        if (playbackAuth is RemoteDataState.Success) {
            val data = playbackAuth.data
            if (data.requiresVip && !data.resolvedIsAuthorized) {
                return@withContext RemoteDataState.Error(
                    endpoint = "Offline Vault",
                    httpCode = 403,
                    message = "VIP Membership is required to download VIP episodes. Free episodes can be downloaded by all logged-in users."
                )
            }
            val movieMeta = discoveredMoviesById[cleanedId]
            val resolvedTitle = movieMeta?.title ?: "HaniX Urdu #$cleanedId ($qualityTier)"
            val resolvedPoster = movieMeta?.resolvedPoster
            val streamUrl = data.resolvedPlaybackUrl ?: "${resolveSiteRootUrl()}/run_video.php?id=$cleanedId"
            val nowSec = System.currentTimeMillis() / 1000L
            return@withContext RemoteDataState.Success(
                data = DownloadAuthorizationPayload(
                    downloadId = "vault_${contentType}_$cleanedId",
                    contentId = cleanedId,
                    contentType = contentType,
                    title = resolvedTitle,
                    poster = resolvedPoster,
                    signedDownloadStreamUrl = streamUrl,
                    licenseExpiresAtEpochSeconds = nowSec + (30L * 86400L),
                    requiresVipEntitlement = data.requiresVip
                ),
                endpoint = "Offline Vault"
            )
        }
        return@withContext RemoteDataState.Error(
            endpoint = "Offline Vault",
            httpCode = 404,
            message = "Could not authorize offline download for #$cleanedId."
        )
    }

    /**
     * Checks `https://hanixurdu.com/check_premium.php?email={email}` which returns `{"is_premium": true|false}`.
     */
    private fun checkEmailVipOnServerSync(email: String): Boolean {
        if (email.isBlank()) return false
        val siteRoot = resolveSiteRootUrl()
        val url = "$siteRoot/check_premium.php?email=${encodeUrlParam(email.trim())}"
        return try {
            val (code, body) = executeGet(url)
            if (code in 200..299 && body.trimStart().startsWith("{")) {
                val json = JSONObject(body)
                json.optBoolean("is_premium", false)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Formats a clean, genuine human-readable name from an email or explicit name,
     * preserving any custom name the user already set on their profile.
     */
    fun formatGenuineUserName(email: String, explicitName: String? = null): String {
        val existingUser = sessionManager.currentUser.value
        if (!explicitName.isNullOrBlank()) {
            return explicitName.trim()
        }
        val existingName = existingUser?.name?.trim().orEmpty()
        if (existingUser != null &&
            existingUser.email.equals(email.trim(), ignoreCase = true) &&
            existingName.isNotBlank() &&
            !existingName.contains("@")
        ) {
            return existingName
        }
        val prefix = email.trim().substringBefore("@")
        val cleanedWords = prefix
            .replace(Regex("[._\\-]+"), " ")
            .replace(Regex("\\d+$"), "")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { ch -> ch.uppercase() }
            }
        return cleanedWords.ifBlank { prefix.replaceFirstChar { it.uppercase() } }
    }

    suspend fun fetchLiveVipStatus(): RemoteDataState<VipStatusPayload> = withContext(Dispatchers.IO) {
        val currentUser = sessionManager.currentUser.value
        val email = currentUser?.email?.trim().orEmpty()
        val isVip = if (email.isNotBlank()) checkEmailVipOnServerSync(email) else false
        val tier = if (isVip) MembershipTier.VIP else MembershipTier.FREE

        if (currentUser != null && email.isNotBlank()) {
            val updatedUser = currentUser.copy(
                name = formatGenuineUserName(email, currentUser.name),
                membership = tier.name,
                vipStatus = isVip,
                legacyIsVip = isVip
            )
            sessionManager.updateCurrentUser(updatedUser)
        }

        RemoteDataState.Success(
            data = VipStatusPayload(
                membership = tier.name,
                isVip = isVip,
                isVipSnake = isVip,
                vipExpiry = if (isVip) currentUser?.resolvedExpiryDate else null,
                paymentInfo = PaymentInfoDto()
            ),
            endpoint = "hanixurdu.com/check_premium.php"
        )
    }

    fun getLiveVipPlans(): RemoteDataState<List<VipPlan>> {
        val paymentInfo = PaymentInfoDto()
        val plans = listOf(
            VipPlan(
                id = "free_tier",
                planType = "FREE",
                name = "FREE Standard Plan",
                durationDays = 3650,
                priceFormatted = "Rs. 0 (Free)",
                features = listOf(
                    "Stream all FREE Urdu dubbed series & movies",
                    "Access Mind Games Quiz & Dubbing Studio",
                    "Multi-Server streaming (Main Server & Server 2)"
                ),
                paymentInfo = paymentInfo
            ),
            VipPlan(
                id = "vip_monthly",
                planType = "VIP",
                name = "VIP 1 Month Pass",
                durationDays = 30,
                priceFormatted = "Rs. 250 / Month",
                features = listOf(
                    "Unlock all VIP Exclusive series & movies",
                    "Ad-free Full HD 1080p streaming",
                    "Instant activation via JazzCash (03294806457) & WhatsApp (+92 316 0450074)"
                ),
                paymentInfo = paymentInfo
            ),
            VipPlan(
                id = "vip_six_months",
                planType = "VIP",
                name = "VIP 6 Months Pass",
                durationDays = 180,
                priceFormatted = "Rs. 599 / 6 Months",
                features = listOf(
                    "Unlock all VIP Exclusive series & movies for 180 days",
                    "Priority WhatsApp episode requests & support",
                    "Verified via premium_users.expiry_date on hanixurdu.com"
                ),
                paymentInfo = paymentInfo
            ),
            VipPlan(
                id = "vip_yearly",
                planType = "VIP",
                name = "VIP 1 Year Pass",
                durationDays = 365,
                priceFormatted = "Rs. 999 / Year",
                features = listOf(
                    "Full 365 days of unlimited VIP access",
                    "All Premium Turkish & Historical Urdu series unlocked",
                    "Direct JazzCash activation with Muhammad Hasnain"
                ),
                paymentInfo = paymentInfo
            )
        )
        return RemoteDataState.Success(plans, "hanixurdu.com/check_premium.php")
    }

    /**
     * Authenticates directly against `https://hanixurdu.com/login_process.php`
     * which accepts JSON `{"email": "...", "password": "..."}` and returns
     * `{"status": "success", "message": "Login successful", "email": "..."}`.
     */
    suspend fun loginLive(email: String, password: String): RemoteDataState<AuthSessionPayload> =
        withContext(Dispatchers.IO) {
            val siteRoot = resolveSiteRootUrl()
            val trimmedEmail = email.trim()
            try {
                val payloadJson = JSONObject().apply {
                    put("email", trimmedEmail)
                    put("password", password)
                }.toString()

                val request = Request.Builder()
                    .url("$siteRoot/login_process.php")
                    .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty().trim()
                    if (bodyStr.startsWith("{")) {
                        val json = JSONObject(bodyStr)
                        val status = json.optString("status", "")
                        val message = json.optString("message", "Login failed")
                        if (status.equals("success", ignoreCase = true)) {
                            val resolvedEmail = json.optString("email", trimmedEmail).ifBlank { trimmedEmail }
                            val serverName = json.optString("name", "").ifBlank { json.optString("username", "") }
                            val isVip = checkEmailVipOnServerSync(resolvedEmail)
                            val displayName = formatGenuineUserName(resolvedEmail, serverName)
                            val user = User(
                                id = resolvedEmail,
                                name = displayName,
                                email = resolvedEmail,
                                membership = if (isVip) "VIP" else "FREE",
                                vipStatus = isVip,
                                legacyIsVip = isVip
                            )
                            val sessionToken = "live_php_session_${UUID.randomUUID()}"
                            return@withContext RemoteDataState.Success(
                                data = AuthSessionPayload(
                                    accessToken = sessionToken,
                                    refreshToken = "refresh_$sessionToken",
                                    expiresIn = 86400L,
                                    user = user
                                ),
                                endpoint = "hanixurdu.com/login_process.php"
                            )
                        } else {
                            return@withContext RemoteDataState.Error(
                                endpoint = "POST /login_process.php",
                                httpCode = 401,
                                message = message
                            )
                        }
                    }
                    RemoteDataState.Error(
                        endpoint = "POST /login_process.php",
                        httpCode = response.code,
                        message = "Unexpected response from login_process.php (HTTP ${response.code})."
                    )
                }
            } catch (e: Exception) {
                RemoteDataState.Error(
                    endpoint = "POST /login_process.php",
                    httpCode = null,
                    message = "Could not connect to hanixurdu.com/login_process.php: ${e.localizedMessage}"
                )
            }
        }

    /**
     * Registers a new user via `https://hanixurdu.com/signup.php` (`email`, `password`, `secret_pin`).
     * Every newly created user starts as `FREE`.
     */
    suspend fun registerLive(
        name: String,
        email: String,
        password: String,
        secretPin: String
    ): RemoteDataState<AuthSessionPayload> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        val trimmedEmail = email.trim()
        val pinToUse = secretPin.trim().ifBlank { "1234" }

        try {
            val formBody = FormBody.Builder()
                .add("email", trimmedEmail)
                .add("password", password)
                .add("secret_pin", pinToUse)
                .build()

            val request = Request.Builder()
                .url("$siteRoot/signup.php")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                val errorMatch = Regex("""<div\s+class=['"]alert-error['"][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

                if (!errorMatch.isNullOrBlank()) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "POST /signup.php",
                        httpCode = 400,
                        message = errorMatch
                    )
                }

                // Registration succeeded on signup.php; always assign FREE membership to new accounts
                val displayName = formatGenuineUserName(trimmedEmail, name)
                val newUser = User(
                    id = trimmedEmail,
                    name = displayName,
                    email = trimmedEmail,
                    membership = "FREE",
                    vipStatus = false,
                    legacyIsVip = false
                )
                val sessionToken = "live_php_session_${UUID.randomUUID()}"
                RemoteDataState.Success(
                    data = AuthSessionPayload(
                        accessToken = sessionToken,
                        refreshToken = "refresh_$sessionToken",
                        expiresIn = 86400L,
                        user = newUser
                    ),
                    endpoint = "hanixurdu.com/signup.php"
                )
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "POST /signup.php",
                httpCode = null,
                message = "Registration failed on hanixurdu.com/signup.php: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Step 1 of `https://hanixurdu.com/forgot_password.php`:
     * Sends `verify_pin=1&email=...&secret_pin=...`.
     */
    suspend fun verifyForgotPinLive(
        email: String,
        secretPin: String
    ): RemoteDataState<ForgotPasswordVerifyResponse> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        try {
            val formBody = FormBody.Builder()
                .add("verify_pin", "1")
                .add("email", email.trim())
                .add("secret_pin", secretPin.trim())
                .build()

            val request = Request.Builder()
                .url("$siteRoot/forgot_password.php")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                val errorMatch = Regex("""<div\s+class=['"]alert\s+alert-error['"][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

                if (!errorMatch.isNullOrBlank()) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "POST /forgot_password.php",
                        httpCode = 400,
                        message = errorMatch
                    )
                }

                if (html.contains("name=\"new_password\"", ignoreCase = true) ||
                    html.contains("PIN verified", ignoreCase = true)
                ) {
                    return@withContext RemoteDataState.Success(
                        data = ForgotPasswordVerifyResponse(
                            verified = true,
                            resetToken = "php_session_verified"
                        ),
                        endpoint = "hanixurdu.com/forgot_password.php"
                    )
                }

                RemoteDataState.Error(
                    endpoint = "POST /forgot_password.php",
                    httpCode = 400,
                    message = "Invalid Email or Secret Recovery PIN."
                )
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "POST /forgot_password.php",
                httpCode = null,
                message = "Could not verify PIN on forgot_password.php: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Step 2 of `https://hanixurdu.com/forgot_password.php`:
     * Sends `reset_pwd=1&new_password=...&confirm_password=...` using the verified session cookie.
     */
    suspend fun resetPasswordWithPinLive(
        email: String,
        secretPin: String,
        newPassword: String
    ): RemoteDataState<Unit> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        try {
            // Ensure session has `$_SESSION['reset_email']` set
            verifyForgotPinLive(email, secretPin)

            val formBody = FormBody.Builder()
                .add("reset_pwd", "1")
                .add("new_password", newPassword)
                .add("confirm_password", newPassword)
                .build()

            val request = Request.Builder()
                .url("$siteRoot/forgot_password.php")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                val errorMatch = Regex("""<div\s+class=['"]alert\s+alert-error['"][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.let { decodeHtmlText(it) }

                if (!errorMatch.isNullOrBlank()) {
                    return@withContext RemoteDataState.Error(
                        endpoint = "POST /forgot_password.php",
                        httpCode = 400,
                        message = errorMatch
                    )
                }

                if (html.contains("Password successfully updated", ignoreCase = true) ||
                    html.contains("alert-success", ignoreCase = true)
                ) {
                    return@withContext RemoteDataState.Success(Unit, "hanixurdu.com/forgot_password.php")
                }

                RemoteDataState.Error(
                    endpoint = "POST /forgot_password.php",
                    httpCode = 400,
                    message = "Could not update password. Please verify your Secret PIN and try again."
                )
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "POST /forgot_password.php",
                httpCode = null,
                message = "Password reset error: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Submits a Dubbing Studio audition directly to `https://hanixurdu.com/upload.php`
     * (`user_name`, `whatsapp`, `role`, `voice_file`).
     */
    suspend fun submitStudioAuditionLive(
        userName: String,
        whatsapp: String,
        role: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): RemoteDataState<AuditionUploadPayload> = withContext(Dispatchers.IO) {
        val siteRoot = resolveSiteRootUrl()
        try {
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_name", userName.trim())
                .addFormDataPart("whatsapp", whatsapp.trim())
                .addFormDataPart("role", role.trim())
                .addFormDataPart(
                    "voice_file",
                    fileName,
                    fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$siteRoot/upload.php")
                .post(multipartBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; HaniXUrduApp)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (response.isSuccessful && (html.contains("Success", ignoreCase = true) || html.contains("Mubarak", ignoreCase = true) || html.contains("submitted", ignoreCase = true))) {
                    return@withContext RemoteDataState.Success(
                        data = AuditionUploadPayload(
                            auditionId = (System.currentTimeMillis() % 100000).toInt(),
                            userName = userName.trim(),
                            whatsapp = whatsapp.trim(),
                            role = role.trim(),
                            filePath = "uploads/$fileName",
                            remainingToday = 19
                        ),
                        endpoint = "hanixurdu.com/upload.php"
                    )
                }
                val alertMsg = Regex("""alert\(['"]([^'"]+)['"]\)""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)
                    ?: "Audition received by HaniX Dubbing Studio (upload.php)."
                if (alertMsg.contains("Success", ignoreCase = true) || alertMsg.contains("Mubarak", ignoreCase = true)) {
                    return@withContext RemoteDataState.Success(
                        data = AuditionUploadPayload(
                            auditionId = (System.currentTimeMillis() % 100000).toInt(),
                            userName = userName.trim(),
                            whatsapp = whatsapp.trim(),
                            role = role.trim(),
                            filePath = "uploads/$fileName",
                            remainingToday = 19
                        ),
                        endpoint = "hanixurdu.com/upload.php"
                    )
                }
                RemoteDataState.Error(
                    endpoint = "POST /upload.php",
                    httpCode = response.code,
                    message = alertMsg
                )
            }
        } catch (e: Exception) {
            RemoteDataState.Error(
                endpoint = "POST /upload.php",
                httpCode = null,
                message = "Audition upload failed: ${e.localizedMessage}"
            )
        }
    }

    fun getLiveCommunityUpdates(): RemoteDataState<CommunityUpdatesPayload> {
        return RemoteDataState.Success(
            data = CommunityUpdatesPayload(
                updates = listOf(
                    PlatformUpdateDto(
                        id = "upd_1",
                        badge = "New Episodes",
                        title = "Mehmed: Fatih Al Sultani & Young Ibn Sina Urdu Dubbed",
                        date = "Live Now",
                        description = "Stream the latest episodes of Mehmed: Fatih Al Sultani, Young Ibn Sina, Maulana Jalaluddin Rumi, and The Great Seljuks on Main Server & Server 2."
                    ),
                    PlatformUpdateDto(
                        id = "upd_2",
                        badge = "Dubbing Studio",
                        title = "HaniX Urdu Voice & Video Editing Auditions Open",
                        date = "Studio Update",
                        description = "Submit your 10 MB audio/video sample via HaniX Studio to join the official Urdu dubbing team."
                    ),
                    PlatformUpdateDto(
                        id = "upd_3",
                        badge = "VIP Membership",
                        title = "Instant VIP Activation via JazzCash & WhatsApp",
                        date = "VIP Support",
                        description = "Send payment to JazzCash 03294806457 (Muhammad Hasnain) and share your registered email on WhatsApp (+92 316 0450074)."
                    )
                ),
                support = SupportLinksDto()
            ),
            endpoint = "hanixurdu.com/sufyan.php"
        )
    }

    fun getLiveAppSettings(): RemoteDataState<AppSettingsPayload> {
        return RemoteDataState.Success(
            data = AppSettingsPayload(
                maintenanceMode = false,
                supportEmail = "hannnix88@gmail.com",
                whatsappSupportUrl = "https://wa.me/923160450074",
                whatsappChannelUrl = "https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q",
                jazzCashAccountName = "Muhammad Hasnain",
                jazzCashAccountNumber = "03294806457",
                bunnyStreamEnabled = true,
                downloadsEnabled = true,
                vipSubscriptionsEnabled = true
            ),
            endpoint = "hanixurdu.com/config.php"
        )
    }

    fun getLiveAppVersion(): RemoteDataState<AppVersionPayload> {
        return RemoteDataState.Success(
            data = AppVersionPayload(
                latestVersionCode = 2,
                latestVersionName = "2.0.0-Live",
                forceUpdate = false,
                changelog = "Connected directly to live hanixurdu.com catalog, series episodes, multi-server video player, Mind Games Quiz, and Dubbing Studio."
            ),
            endpoint = "hanixurdu.com/config.php"
        )
    }
}
