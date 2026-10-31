package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BackendEndpointCatalog
import com.example.data.model.Episode
import com.example.data.model.MembershipTier
import com.example.data.model.MindGameQuestionBank
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackServerDto
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.User
import com.example.data.model.VipStatusPayload
import com.example.data.remote.FlexibleBooleanAdapter
import com.example.data.security.KeystoreCryptoManager
import com.example.data.security.SecureSessionManager
import com.example.player.BunnyPlaybackState
import com.example.player.BunnyStreamPlayerManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("hanix_urdu_secure_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        moshi = Moshi.Builder()
            .add(FlexibleBooleanAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun membershipTier_containsStrictlyFreeAndVipOnly() {
        val tiers = MembershipTier.entries.map { it.name }
        assertEquals(listOf("FREE", "VIP"), tiers)
        assertEquals(2, tiers.size)
    }

    @Test
    fun newUserSession_defaultsToFreeAndNeverAutoGrantsVip() {
        val sessionManager = SecureSessionManager(context, KeystoreCryptoManager())
        assertFalse(sessionManager.isAuthenticated.value)
        assertNull(sessionManager.currentUser.value)

        // Newly registered user with FREE membership
        val newUser = User(
            id = "101",
            name = "Amina Khan",
            email = "amina@hanixurdu.com",
            membership = "FREE",
            vipStatus = false,
            vipExpiry = null
        )
        sessionManager.saveAuthenticatedSession(
            accessToken = "access_token_free_101",
            refreshToken = "refresh_token_free_101",
            user = newUser
        )

        assertTrue(sessionManager.isAuthenticated.value)
        val savedUser = sessionManager.currentUser.value
        assertNotNull(savedUser)
        assertFalse(savedUser!!.isActiveVip)
        assertEquals(MembershipTier.FREE, savedUser.effectiveMembershipTier())
    }

    @Test
    fun activeVipUser_resolvesToVip_andExpiredVipImmediatelyRevertsToFree() {
        val nowSec = 1_750_000_000L
        val futureMysqlDate = "2035-12-31 23:59:59"
        val pastMysqlDate = "2020-01-01 00:00:00"

        // Active VIP with future expiry_date from premium_users
        val activeTier = MembershipTier.resolve(
            rawMembership = "VIP",
            vipStatusFlag = true,
            vipExpiryEpochSeconds = null,
            vipExpiryDateStr = futureMysqlDate,
            nowEpochSeconds = nowSec
        )
        assertEquals(MembershipTier.VIP, activeTier)

        // Expired VIP with past expiry_date must immediately revert to FREE
        val expiredTier = MembershipTier.resolve(
            rawMembership = "VIP",
            vipStatusFlag = true,
            vipExpiryEpochSeconds = null,
            vipExpiryDateStr = pastMysqlDate,
            nowEpochSeconds = nowSec
        )
        assertEquals(MembershipTier.FREE, expiredTier)

        // VipStatusPayload model also enforces expiry check and provides JazzCash paymentInfo
        val expiredStatus = VipStatusPayload(
            membership = "VIP",
            isVip = true,
            legacyExpiryDate = pastMysqlDate
        )
        assertEquals(MembershipTier.FREE, expiredStatus.effectiveTier(nowSec))
    }

    @Test
    fun secureSessionManager_blocksExpiredVipAndRequiresValidAccessToken() {
        val sessionManager = SecureSessionManager(context, KeystoreCryptoManager())
        val expiredVipUser = User(
            id = "202",
            name = "Bilal Ahmed",
            email = "bilal@hanixurdu.com",
            membership = "VIP",
            vipStatus = true,
            vipExpiry = "2021-06-15 12:00:00" // Past date
        )
        sessionManager.saveAuthenticatedSession(
            accessToken = "token_202",
            refreshToken = "refresh_202",
            user = expiredVipUser
        )

        // Even though user object had vipStatus = true, past expiry date downgrades session to FREE
        val currentUser = sessionManager.currentUser.value
        assertNotNull(currentUser)
        assertFalse(currentUser!!.isActiveVip)
        assertEquals(MembershipTier.FREE, currentUser.effectiveMembershipTier())
        assertEquals("FREE", currentUser.membership)

        // Clear session and attempt to tamper with preferences without a valid encrypted token
        sessionManager.clearSession()
        context.getSharedPreferences("hanix_urdu_secure_session", Context.MODE_PRIVATE)
            .edit()
            .putString("user_id", "202")
            .putString("user_membership", "VIP")
            .putBoolean("user_vip_status", true)
            .commit()

        val reloadedManager = SecureSessionManager(context, KeystoreCryptoManager())
        assertFalse(reloadedManager.isAuthenticated.value)
        assertNull(reloadedManager.currentUser.value)
    }

    @Test
    fun mysqlCompatibility_parsesSnakeCaseColumnsAndNumericBooleans() {
        val movieJson = """
            {
              "id": "42",
              "title": "Parwaaz Hai Junoon",
              "description": "Pakistani air force tribute drama film.",
              "poster_path": "https://hanixurdu.com/posters/parwaaz.jpg",
              "category": "trending,urdu_subtitles,premium",
              "is_premium": 1
            }
        """.trimIndent()

        val movieAdapter = moshi.adapter(Movie::class.java)
        val movie = movieAdapter.fromJson(movieJson)
        assertNotNull(movie)
        assertEquals("42", movie!!.resolvedId)
        assertEquals("Parwaaz Hai Junoon", movie.title)
        assertEquals("https://hanixurdu.com/posters/parwaaz.jpg", movie.resolvedPoster)
        assertTrue(movie.resolvedIsVip)
        assertEquals("VIP", movie.membershipBadge)

        val seriesJson = """
            {
              "id": "15",
              "series_name": "Dastaan-e-Ishq",
              "index_poster": "https://hanixurdu.com/posters/dastaan.jpg"
            }
        """.trimIndent()
        val series = moshi.adapter(Series::class.java).fromJson(seriesJson)
        assertNotNull(series)
        assertEquals("Dastaan-e-Ishq", series!!.resolvedTitle)
        assertEquals("https://hanixurdu.com/posters/dastaan.jpg", series.resolvedPoster)

        val seasonJson = """
            {
              "id": "7",
              "series_name": "Dastaan-e-Ishq",
              "season_name": "Season 1 — Origins"
            }
        """.trimIndent()
        val season = moshi.adapter(Season::class.java).fromJson(seasonJson)
        assertNotNull(season)
        assertEquals("Dastaan-e-Ishq", season!!.resolvedSeriesKey)
        assertEquals("Season 1 — Origins", season.resolvedSeasonName)

        val episodeJson = """
            {
              "id": "99",
              "seasonId": "7",
              "title": "Episode 3",
              "poster_path": "https://hanixurdu.com/posters/ep3.jpg",
              "is_premium": 1
            }
        """.trimIndent()
        val episode = moshi.adapter(Episode::class.java).fromJson(episodeJson)
        assertNotNull(episode)
        assertTrue(episode!!.resolvedIsVip)
        assertEquals("VIP", episode.membershipBadge)
        assertEquals("https://hanixurdu.com/posters/ep3.jpg", episode.resolvedPoster)
    }

    @Test
    fun bunnyPlaybackValidator_supportsHlsDirectAndMultiServerEmbeds() {
        val playerManager = BunnyStreamPlayerManager(context)
        val nowSec = 1_750_000_000L

        // 1. Authorized valid HTTPS HLS stream
        val validPayload = PlaybackAuthorizationPayload(
            playbackSessionId = "sess_123",
            contentId = "42",
            contentType = "movie",
            membershipRequired = "VIP",
            isAuthorized = true,
            requiresVip = true,
            processingStatus = "READY",
            authorizedHlsUrl = "https://vz-hanix.b-cdn.net/8f4d2c1a/playlist.m3u8?token=abc&expires=1750000900",
            tokenExpiresAtEpochSeconds = nowSec + 900L
        )
        val validResult = playerManager.validateAuthorizationPayload(validPayload, nowSec)
        assertTrue(validResult is BunnyPlaybackState.ReadyToPlay)
        assertEquals("hls", (validResult as BunnyPlaybackState.ReadyToPlay).playbackType)

        // 2. Multi-server switch to Server 2 embed (Google Drive /preview or YouTube from run_video.php)
        val server2 = PlaybackServerDto(
            id = "server2",
            label = "Server 2",
            type = "embed",
            url = "https://drive.google.com/file/d/1A2B3C4D5E/preview"
        )
        playerManager.switchServer(server2, validPayload, attachExoPlayer = false, nowEpochSeconds = nowSec)
        val switchedState = playerManager.playbackState.value
        assertTrue(switchedState is BunnyPlaybackState.ReadyToPlay)
        assertEquals("embed", (switchedState as BunnyPlaybackState.ReadyToPlay).playbackType)
        assertEquals("https://drive.google.com/file/d/1A2B3C4D5E/preview", switchedState.authorizedHlsUrl)

        // 3. Unauthorized FREE user attempting VIP movie
        val deniedVipPayload = PlaybackAuthorizationPayload(
            playbackSessionId = "",
            contentId = "42",
            contentType = "movie",
            membershipRequired = "VIP",
            isAuthorized = false,
            requiresVip = true,
            processingStatus = "READY",
            authorizedHlsUrl = null
        )
        val deniedResult = playerManager.validateAuthorizationPayload(deniedVipPayload, nowSec)
        assertTrue(deniedResult is BunnyPlaybackState.DeniedVipRequired)

        // 4. Expired signed Bunny Stream token
        val expiredTokenPayload = validPayload.copy(tokenExpiresAtEpochSeconds = nowSec - 10L)
        val expiredResult = playerManager.validateAuthorizationPayload(expiredTokenPayload, nowSec)
        assertTrue(expiredResult is BunnyPlaybackState.ExpiredPlaybackToken)
    }

    @Test
    fun mindGameQuestionBank_drawsTenRandomQuestionsFromMindGamePhp() {
        val drawn = MindGameQuestionBank.drawTenRandomQuestions()
        assertEquals(10, drawn.size)
        drawn.forEach { q ->
            assertTrue(q.question.isNotBlank())
            assertEquals(4, q.options.size)
            assertTrue(q.answerIndex in 0..3)
        }
    }

    @Test
    fun backendEndpointCatalog_containsAll38SpecificationRoutes() {
        assertEquals(38, BackendEndpointCatalog.endpoints.size)
        val paths = BackendEndpointCatalog.endpoints.map { "${it.method} ${it.path}" }
        assertTrue(paths.contains("POST /auth/login"))
        assertTrue(paths.contains("POST /auth/register"))
        assertTrue(paths.contains("POST /auth/refresh"))
        assertTrue(paths.contains("GET /vip/status"))
        assertTrue(paths.contains("GET /vip/plans"))
        assertTrue(paths.contains("POST /playback/authorize"))
        assertTrue(paths.contains("POST /downloads/authorize"))
        assertTrue(paths.contains("GET /mind-game/questions"))
        assertTrue(paths.contains("POST /studio/audition"))
        assertTrue(paths.contains("GET /updates"))
        assertTrue(paths.contains("GET /app/settings"))
        assertTrue(paths.contains("GET /app/version"))
    }

    @Test
    fun mysqlExpiryParser_parsesBothMysqlDatetimeAndDateOnly() {
        val mysqlTs = MembershipTier.parseMysqlExpiryToEpochSeconds("2030-05-10 14:30:00")
        assertNotNull(mysqlTs)
        assertTrue(mysqlTs!! > 1_800_000_000L)

        val isoTs = MembershipTier.parseMysqlExpiryToEpochSeconds("2030-05-10T14:30:00")
        assertNotNull(isoTs)
        assertEquals(mysqlTs, isoTs)
    }

    @Test
    fun realPhpApiContracts_parseSnakeCaseAuthHomeAndDirectPlaybackPayloads() {
        val loginDataJson = """
            {
              "user": {
                "id": "123",
                "email": "user@example.com",
                "membership": "FREE"
              },
              "access_token": "SERVER_GENERATED_TOKEN",
              "refresh_token": "SERVER_GENERATED_REFRESH_TOKEN"
            }
        """.trimIndent()
        val authPayload = moshi.adapter(com.example.data.model.AuthSessionPayload::class.java).fromJson(loginDataJson)
        assertNotNull(authPayload)
        assertEquals("SERVER_GENERATED_TOKEN", authPayload!!.resolvedAccessToken)
        assertEquals("SERVER_GENERATED_REFRESH_TOKEN", authPayload.resolvedRefreshToken)
        assertEquals(MembershipTier.FREE, authPayload.user.effectiveMembershipTier())

        val directPlaybackJson = """
            {
              "content_id": "123",
              "playback_type": "direct",
              "playback_url": "https://hanixurdu.com/videos/movie123.mp4",
              "expires_at": "2035-12-31T23:59:59Z"
            }
        """.trimIndent()
        val playbackPayload = moshi.adapter(PlaybackAuthorizationPayload::class.java).fromJson(directPlaybackJson)
        assertNotNull(playbackPayload)
        assertTrue(playbackPayload!!.resolvedIsAuthorized)
        assertEquals("https://hanixurdu.com/videos/movie123.mp4", playbackPayload.resolvedPlaybackUrl)

        val playerManager = BunnyStreamPlayerManager(context)
        val validationState = playerManager.validateAuthorizationPayload(playbackPayload, 1_750_000_000L)
        assertTrue(validationState is BunnyPlaybackState.ReadyToPlay)
        assertEquals("direct", (validationState as BunnyPlaybackState.ReadyToPlay).playbackType)
    }

    @Test
    fun hanixRoutes_encodesAndDecodesSeriesAndSeasonNamesWithSpacesAndColons() {
        val rawSeries = "Mehmed: Fatih Al Sultani"
        val encodedRoute = com.example.navigation.HaniXRoutes.seriesDetails(rawSeries)
        assertFalse(encodedRoute.contains(" "))
        val encodedArg = encodedRoute.removePrefix("series_details/")
        assertEquals(rawSeries, com.example.navigation.HaniXRoutes.decodeSegment(encodedArg))

        val rawSeason = "Young Ibn Sina||Season 1"
        val seasonRoute = com.example.navigation.HaniXRoutes.episodeList(rawSeason)
        val seasonArg = seasonRoute.removePrefix("episode_list/")
        assertEquals(rawSeason, com.example.navigation.HaniXRoutes.decodeSegment(seasonArg))
    }

    @Test
    fun liveWebsiteBridge_providesSiteRootAndVipPlans() {
        val sessionManager = SecureSessionManager(context, KeystoreCryptoManager())
        val bridge = com.example.data.remote.HaniXLiveWebsiteBridge(sessionManager)
        assertEquals("https://hanixurdu.com", bridge.resolveSiteRootUrl())
        val plansState = bridge.getLiveVipPlans()
        assertTrue(plansState is com.example.data.model.RemoteDataState.Success)
        val plans = (plansState as com.example.data.model.RemoteDataState.Success).data
        assertEquals(4, plans.size)
        assertTrue(plans.any { it.priceFormatted.contains("250") })
    }

    @Test
    fun securityShield_createsPrivateOfflineVaultWithNoMedia_andBlocksAudioCapture() {
        val vaultDir = com.example.data.security.HaniXSecurityShield.getOrCreatePrivateOfflineVaultDir(context)
        assertTrue(vaultDir.exists())
        assertTrue(vaultDir.isDirectory)
        val noMediaFile = java.io.File(vaultDir, ".nomedia")
        assertTrue("Private vault must contain .nomedia to hide files from Gallery", noMediaFile.exists())

        val audioAttrs = com.example.data.security.HaniXSecurityShield.buildProtectedMedia3AudioAttributes()
        assertEquals(
            androidx.media3.common.C.ALLOW_CAPTURE_BY_NONE,
            audioAttrs.allowedCapturePolicy
        )
    }

    @Test
    fun securityShield_sanitizesUiLabelsAndLocksSslPinningWithNoProxy() {
        val rawLeak = "run_video.php?id=42 • GET /movies/42 • https://hanixurdu.com/api/v1"
        val sanitized = com.example.data.security.HaniXSecurityShield.sanitizeUiLabel(rawLeak).orEmpty()
        assertFalse("Sanitized label must not leak .php filenames", sanitized.contains(".php"))
        assertFalse("Sanitized label must not leak raw https:// URLs", sanitized.contains("https://"))
        assertFalse("Sanitized label must not leak GET / routes", sanitized.contains("GET /"))

        val client = com.example.data.security.HaniXSecurityShield.configureSecureOkHttpClient(
            okhttp3.OkHttpClient.Builder()
        ).build()
        assertEquals(java.net.Proxy.NO_PROXY, client.proxy)
        assertTrue(client.certificatePinner.pins.isNotEmpty())
    }

    @Test
    fun movieBoxPlayer_supportsSpeedControlAndIframeSrcExtraction() {
        val playerManager = BunnyStreamPlayerManager(context)
        assertEquals(1.0f, playerManager.playbackSpeed.value, 0.01f)
        playerManager.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, playerManager.playbackSpeed.value, 0.01f)

        val sessionManager = SecureSessionManager(context, KeystoreCryptoManager())
        val bridge = com.example.data.remote.HaniXLiveWebsiteBridge(sessionManager)
        val (type, normUrl) = bridge.normalizeEmbedOrStreamUrl(
            """<iframe src="https://drive.google.com/file/d/ABC123XYZ/view" allowfullscreen></iframe>""",
            "iframe"
        )
        assertEquals("embed", type)
        assertEquals("https://drive.google.com/file/d/ABC123XYZ/preview", normUrl)
    }
}
