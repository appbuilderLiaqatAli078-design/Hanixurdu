package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackServerDto
import com.example.data.model.RemoteDataState
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.User
import com.example.data.security.HaniXSecurityShield
import com.example.player.BunnyPlaybackState
import com.example.player.BunnyStreamPlayerManager
import com.example.ui.components.ContractStateBanner
import com.example.ui.components.HaniXTopBar
import com.example.ui.components.MoviePosterCard
import com.example.ui.components.VvipGlassCard
import com.example.ui.components.Cinema3DButton
import com.example.ui.components.Cinema3DButtonVariant
import com.example.ui.components.FloatingChatButton
import com.example.ui.components.FloatingMindGamesPill
import com.example.ui.components.WhatsAppFollowPill
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextPrimaryIvory
import com.example.ui.theme.TextSecondarySilver
import com.example.ui.theme.VipChampagneGold
import com.example.ui.theme.VipGoldLight
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// =========================================================================
// 11. MOVIE DETAILS SCREEN
// =========================================================================

@Composable
fun MovieDetailsScreen(
    movieId: String,
    movieState: RemoteDataState<Movie>,
    downloadActionState: RemoteDataState<EncryptedDownloadEntity>,
    onLoadMovie: (String) -> Unit,
    onPlayMovie: (String) -> Unit,
    onDownloadMovie: (String) -> Unit,
    onNavigateToVip: () -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(movieId) {
        onLoadMovie(movieId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("movie_details_screen")
    ) {
        HaniXTopBar(
            title = "Movie Details",
            subtitle = "HD Urdu Dubbed & Subtitled",
            onBackClick = onBackClick,
            onVipClick = onNavigateToVip
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (movieState !is RemoteDataState.Success) {
                item {
                    ContractStateBanner(
                        state = movieState,
                        onRetry = { onLoadMovie(movieId) },
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            if (movieState is RemoteDataState.Success) {
                val movie = movieState.data
                val isVip = movie.resolvedIsVip
                item {
                    VvipGlassCard(borderColor = if (isVip) VipChampagneGold else CinemaCrimson.copy(alpha = 0.6f)) {
                        Column {
                            if (!movie.resolvedPoster.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                ) {
                                    AsyncImage(
                                        model = movie.resolvedPoster,
                                        contentDescription = movie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        OledBlack.copy(alpha = 0.92f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (isVip) VipChampagneGold else CharcoalSurface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isVip) "👑 VIP" else "FREE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isVip) OledBlack else EmeraldVerified,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (!movie.category.isNullOrBlank()) {
                                        Text(
                                            text = movie.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VipGoldLight
                                        )
                                    }
                                    if (!movie.duration.isNullOrBlank()) {
                                        Text(
                                            text = "• ${movie.duration}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondarySilver
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimaryIvory,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = movie.description ?: "Watch ${movie.title} in HD Urdu Dubbed & Subtitled on HaniX Urdu.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondarySilver
                                )
                            }
                        }
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MovieBox Player & App-Private Offline Vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Watch in the MovieBox Player (Main Server, Server 2, Direct Web Player) or save to your App-Private Vault (never shown in phone Gallery).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Cinema3DButton(
                            text = "▶ Watch Now (MovieBox Player)",
                            onClick = { onPlayMovie(movieId) },
                            variant = Cinema3DButtonVariant.Crimson,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "movie_authorize_playback_button"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Cinema3DButton(
                            text = "Save to Private App Vault (No Gallery)",
                            onClick = { onDownloadMovie(movieId) },
                            variant = Cinema3DButtonVariant.Gold,
                            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "movie_authorize_download_button"
                        )
                    }
                }
            }

            if (downloadActionState !is RemoteDataState.Idle) {
                item {
                    if (downloadActionState is RemoteDataState.Success) {
                        VvipGlassCard(borderColor = EmeraldVerified) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldVerified)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Saved to App-Private Offline Vault (Hidden from phone Gallery).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        ContractStateBanner(
                            state = downloadActionState,
                            onRetry = { onDownloadMovie(movieId) },
                            onOpenChecklist = onOpenChecklist
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        FloatingChatButton(
            onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/hanixurdu_support"))
                    context.startActivity(intent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingMindGamesPill(
            onClick = { /* Home screen handles Mind Games state, navigation usually required or local state */ }
        )
    }
}

// =========================================================================
// 12. SERIES DETAILS SCREEN
// =========================================================================

@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    seriesState: RemoteDataState<Series>,
    seasonsState: RemoteDataState<List<Season>>,
    episodesState: RemoteDataState<List<Episode>> = RemoteDataState.Idle,
    currentUser: User? = null,
    onLoadSeriesAndSeasons: (String) -> Unit,
    onLoadSeasonEpisodes: (String) -> Unit = {},
    onOpenSeasonSelection: (String) -> Unit,
    onSelectSeason: (String) -> Unit,
    onPlayEpisode: (String) -> Unit = {},
    onDownloadEpisode: (String) -> Unit = {},
    onNavigateToVip: () -> Unit = {},
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var activeSeasonId by rememberSaveable(seriesId) { mutableStateOf("") }
    val isUserVip = currentUser?.isActiveVip == true
    val resolvedSeriesTitle = (seriesState as? RemoteDataState.Success)?.data?.resolvedTitle ?: seriesId

    LaunchedEffect(seriesId) {
        onLoadSeriesAndSeasons(seriesId)
    }

    // Automatically load the first season's episodes inline
    LaunchedEffect(seasonsState) {
        if (seasonsState is RemoteDataState.Success && seasonsState.data.isNotEmpty()) {
            val firstSeasonId = seasonsState.data.first().resolvedId
            if (activeSeasonId.isBlank() || seasonsState.data.none { it.resolvedId == activeSeasonId }) {
                activeSeasonId = firstSeasonId
                onLoadSeasonEpisodes(firstSeasonId)
            }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf("Episodes") }
    var showWhatsAppFollow by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("series_details_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HaniXTopBar(
                title = resolvedSeriesTitle,
                subtitle = "Seasons & Episodes • Auto-Updated",
                onBackClick = onBackClick,
                onVipClick = onNavigateToVip
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (seriesState !is RemoteDataState.Success) {
                    item {
                        ContractStateBanner(
                            state = seriesState,
                            onRetry = { onLoadSeriesAndSeasons(seriesId) },
                            onOpenChecklist = onOpenChecklist
                        )
                    }
                }

                if (seriesState is RemoteDataState.Success) {
                    val series = seriesState.data
                    val firstEpId = (episodesState as? RemoteDataState.Success)
                        ?.data
                        ?.firstOrNull { isUserVip || !it.id.startsWith("vip_locked_") }
                        ?.id
                        ?.removePrefix("vip_locked_")
                        ?: "1"

                    item {
                        VvipGlassCard(borderColor = CinemaCrimson.copy(alpha = 0.5f)) {
                            Column {
                                if (!series.resolvedPoster.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                    ) {
                                        AsyncImage(
                                            model = series.resolvedPoster,
                                            contentDescription = series.resolvedTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color(0xFF0F1116).copy(alpha = 0.95f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = series.resolvedTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (seasonsState is RemoteDataState.Success) seasonsState.data.size else 1} Seasons • Trending • all • fan_dubbing • premium • 👑 VIP Series",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VipChampagneGold
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Action buttons: 3D White Watch Now + Share
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Cinema3DButton(
                                            text = "▶ Watch Now",
                                            onClick = { onPlayEpisode(firstEpId) },
                                            variant = Cinema3DButtonVariant.White,
                                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            testTag = "series_watch_now_button"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .shadow(4.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(Color(0xFF262934))
                                                .border(1.dp, Color(0xFF404656), CircleShape)
                                                .clickable {
                                                    runCatching {
                                                        val sendIntent = Intent().apply {
                                                            action = Intent.ACTION_SEND
                                                            putExtra(
                                                                Intent.EXTRA_TEXT,
                                                                "Watch ${series.resolvedTitle} on HaniX Urdu: https://hanixurdu.com/series.php?sub=${series.resolvedTitle}"
                                                            )
                                                            type = "text/plain"
                                                        }
                                                        context.startActivity(Intent.createChooser(sendIntent, "Share Series"))
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Historical   + More",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFAAAAAA)
                                    )
                                }
                            }
                        }
                    }

                    // Tabs: Episodes, Details, Related (matching website)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            listOf("Episodes", "Details", "Related").forEach { tab ->
                                val isSelected = selectedTab == tab
                                Column(
                                    modifier = Modifier
                                        .clickable { selectedTab = tab }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tab,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) Color.White else Color(0xFF7A7E8C),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 36.dp else 0.dp)
                                            .height(3.dp)
                                            .background(if (isSelected) CinemaCrimson else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }

            // Season Selector Pills & Episodes (when selectedTab == "Episodes")
            if (selectedTab == "Episodes") {
                if (seasonsState is RemoteDataState.Success && seasonsState.data.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(seasonsState.data, key = { it.resolvedId }) { season ->
                                val isSelected = activeSeasonId == season.resolvedId
                                Cinema3DButton(
                                    text = season.resolvedSeasonName,
                                    onClick = {
                                        activeSeasonId = season.resolvedId
                                        onLoadSeasonEpisodes(season.resolvedId)
                                    },
                                    variant = if (isSelected) Cinema3DButtonVariant.Crimson else Cinema3DButtonVariant.Dark,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    testTag = "season_button_${season.resolvedId}"
                                )
                            }
                        }
                    }
                }

                // Inline Episodes List
                if (episodesState is RemoteDataState.Loading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = CinemaCrimson, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Loading Episodes...", color = TextSecondarySilver)
                        }
                    }
                } else if (episodesState is RemoteDataState.Success && episodesState.data.isNotEmpty()) {
                    items(episodesState.data, key = { "series_ep_${it.id}" }) { episode ->
                        EpisodeCardItem(
                            episode = episode,
                            isUserVip = isUserVip,
                            onPlayClick = {
                                val cleanEpisodeId = episode.id.removePrefix("vip_locked_")
                                if (episode.resolvedIsVip && !isUserVip && episode.id.startsWith("vip_locked_")) {
                                    onNavigateToVip()
                                } else {
                                    onPlayEpisode(cleanEpisodeId)
                                }
                            },
                            onUnlockVipClick = onNavigateToVip,
                            onDownloadClick = {
                                val cleanEpisodeId = episode.id.removePrefix("vip_locked_")
                                onDownloadEpisode(cleanEpisodeId)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Cinema3DButton(
                        text = "← Back to Home",
                        onClick = onBackClick,
                        variant = Cinema3DButtonVariant.Crimson,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
                        modifier = Modifier.testTag("back_to_home_button")
                    )
                }

                // About HaniX Urdu & Auditions (matching screenshot)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 70.dp)
                    ) {
                        Text(
                            text = "About HaniX Urdu & Auditions",
                            style = MaterialTheme.typography.titleMedium,
                            color = CinemaCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HaniX Urdu is your ultimate Entertainment Hub, bringing you the best in trending web series, movies, and exclusive originals. Stream your favorite Turkish series on our YouTube channel with crisp Urdu voiceovers, or join our community of passionate creators and fans collaborate to bring top-tier Anime and Drama to life.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryIvory,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else if (selectedTab == "Details") {
                item {
                    if (seriesState is RemoteDataState.Success) {
                        val series = seriesState.data
                        VvipGlassCard(borderColor = GlassBorderSubtle) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = series.resolvedTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = series.category ?: "Premium Urdu dubbed high-definition drama stream.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondarySilver
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = CharcoalSurface, shape = RoundedCornerShape(4.dp)) {
                                        Text("Urdu Dubbed", color = EmeraldVerified, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(6.dp, 3.dp))
                                    }
                                    Surface(color = CharcoalSurface, shape = RoundedCornerShape(4.dp)) {
                                        Text("Full HD 1080p", color = VipChampagneGold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(6.dp, 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == "Related") {
                if (seasonsState is RemoteDataState.Success && seasonsState.data.isNotEmpty()) {
                    items(seasonsState.data, key = { "season_row_${it.resolvedId}" }) { season ->
                        VvipGlassCard(
                            onClick = { onSelectSeason(season.resolvedId) },
                            borderColor = VipChampagneGold.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = season.resolvedSeasonName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "Open Full Episode List →",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VipChampagneGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

        // Floating WhatsApp Follow Pill & Chat button over the screen
        if (showWhatsAppFollow) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                WhatsAppFollowPill(
                    onFollowClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Va9Wk684Spk9jV5JzQ0y"))
                            context.startActivity(intent)
                        }
                    },
                    onDismissClick = { showWhatsAppFollow = false }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            FloatingChatButton(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/hanixurdu_support"))
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}



@Composable
private fun EpisodeCardItem(
    episode: Episode,
    isUserVip: Boolean = false,
    onPlayClick: () -> Unit,
    onUnlockVipClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null
) {
    val isVip = episode.resolvedIsVip
    val isLocked = (isVip && !isUserVip) || (episode.id.startsWith("vip_locked_") && !isUserVip)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked) onUnlockVipClick() else onPlayClick()
            }
            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .testTag("episode_card_${episode.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181C26)),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0xFF3D445A), Color(0xFF131620))))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle Glossy Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.03f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Thumbnail with subtle rounded border
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1116))
                        .border(1.dp, Color(0xFF2C3140), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!episode.resolvedPoster.isNullOrBlank()) {
                        AsyncImage(
                            model = episode.resolvedPoster,
                            contentDescription = episode.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF555C70),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "VIP Locked",
                                tint = VipChampagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Episode title in center
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVip) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = if (isUserVip) EmeraldVerified.copy(alpha = 0.15f) else VipChampagneGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (isUserVip) EmeraldVerified else VipChampagneGold)
                        ) {
                            Text(
                                text = if (isUserVip) "👑 VIP UNLOCKED" else "🔒 VIP ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUserVip) EmeraldVerified else VipChampagneGold,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Circular Play Button on the right (3D Style)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape, spotColor = if (isLocked) VipChampagneGold else Color.Black)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isLocked) listOf(Color(0xFF443D2A), Color(0xFF1A160D))
                                else listOf(Color(0xFF3D445A), Color(0xFF131620))
                            )
                        )
                        .border(1.dp, if (isLocked) VipChampagneGold.copy(alpha = 0.6f) else Color(0xFF5A637C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isLocked) VipChampagneGold else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 13. SEASON SELECTION SCREEN
// =========================================================================

@Composable
fun SeasonSelectionScreen(
    seriesId: String,
    seasonsState: RemoteDataState<List<Season>>,
    onLoadSeasons: (String) -> Unit,
    onSeasonSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    LaunchedEffect(seriesId) {
        onLoadSeasons(seriesId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("season_selection_screen")
    ) {
        HaniXTopBar(
            title = "Select Season",
            subtitle = seriesId,
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (seasonsState !is RemoteDataState.Success) {
                item {
                    ContractStateBanner(
                        state = seasonsState,
                        onRetry = { onLoadSeasons(seriesId) },
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            if (seasonsState is RemoteDataState.Success) {
                items(seasonsState.data, key = { it.resolvedId }) { season ->
                    VvipGlassCard(
                        onClick = { onSeasonSelected(season.resolvedId) },
                        borderColor = VipChampagneGold.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = season.resolvedSeasonName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "Series: ${season.resolvedSeriesKey} • Season: ${season.resolvedSeasonName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                            Cinema3DButton(
                                text = "Episodes",
                                onClick = { onSeasonSelected(season.resolvedId) },
                                variant = Cinema3DButtonVariant.Crimson,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Quick Episode List Access",
                            style = MaterialTheme.typography.titleSmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open the full episode list and start MovieBox playback:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Cinema3DButton(
                            text = "Open Episode List (Season #1)",
                            onClick = {
                                val firstId = (seasonsState as? RemoteDataState.Success)?.data?.firstOrNull()?.resolvedId
                                    ?: "$seriesId||Season 1"
                                onSeasonSelected(firstId)
                            },
                            variant = Cinema3DButtonVariant.Gold,
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("inspect_episode_list_button")
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 14. EPISODE LIST SCREEN
// =========================================================================

@Composable
fun EpisodeListScreen(
    seasonId: String,
    episodesState: RemoteDataState<List<Episode>>,
    currentUser: User? = null,
    onLoadEpisodes: (String) -> Unit,
    onPlayEpisode: (String) -> Unit,
    onDownloadEpisode: (String) -> Unit,
    onNavigateToVip: () -> Unit = {},
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val isUserVip = currentUser?.isActiveVip == true

    LaunchedEffect(seasonId) {
        onLoadEpisodes(seasonId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("episode_list_screen")
    ) {
        HaniXTopBar(
            title = seasonId.replace("||", " • "),
            subtitle = "Urdu Dubbed & Subtitled Episodes",
            onBackClick = onBackClick,
            onVipClick = onNavigateToVip
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (episodesState !is RemoteDataState.Success) {
                item {
                    ContractStateBanner(
                        state = episodesState,
                        onRetry = { onLoadEpisodes(seasonId) },
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            if (episodesState is RemoteDataState.Success) {
                items(episodesState.data, key = { it.id }) { episode ->
                    EpisodeCardItem(
                        episode = episode,
                        isUserVip = isUserVip,
                        onPlayClick = {
                            onPlayEpisode(episode.id.removePrefix("vip_locked_"))
                        },
                        onUnlockVipClick = onNavigateToVip,
                        onDownloadClick = {
                            onDownloadEpisode(episode.id.removePrefix("vip_locked_"))
                        }
                    )
                }
            }

            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MovieBox Multi-Server Playback",
                            style = MaterialTheme.typography.titleSmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Play immediately in the high-performance MovieBox Player with automated server selection:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    Cinema3DButton(
                        text = "Open MovieBox Video Player",
                        onClick = {
                            val firstEpId = (episodesState as? RemoteDataState.Success)
                                ?.data
                                ?.firstOrNull { isUserVip || !it.id.startsWith("vip_locked_") }
                                ?.id
                                ?.removePrefix("vip_locked_")
                                ?: "1"
                            onPlayEpisode(firstEpId)
                        },
                        variant = Cinema3DButtonVariant.Crimson,
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                        modifier = Modifier.testTag("episode_authorize_playback_button")
                    )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 15. MOVIEBOX-STYLE INTERACTIVE VIDEO PLAYER SCREEN
//     Features:
//     - Fullscreen Landscape Toggle + Immersive System Bars
//     - Left Vertical Swipe for Brightness (0%-100%)
//     - Right Vertical Swipe for Volume (0%-100%)
//     - Double-Tap Left/Right for -10s / +10s Seek
//     - Playback Speed Selector (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
//     - Aspect Ratio Toggle (Fit / Fill / Zoom)
//     - Screen Lock Toggle (blocks accidental touches)
//     - Multi-Server Switcher (Main Server, Server 2, Direct Web Player)
//     - In-Player Episodes Drawer & App-Private Offline Vault Download
//     - Screen Recording Allowed + Internal Audio Capture Strictly Blocked
// =========================================================================

@OptIn(UnstableApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    contentType: String,
    contentId: String,
    playerManager: BunnyStreamPlayerManager,
    playbackAuthState: RemoteDataState<PlaybackAuthorizationPayload>,
    currentUser: User? = null,
    episodesList: List<Episode> = emptyList(),
    downloadActionState: RemoteDataState<EncryptedDownloadEntity> = RemoteDataState.Idle,
    onAuthorizePlayback: (String, String) -> Unit,
    onSelectRecommendedMovie: (String) -> Unit = {},
    onSelectRecommendedSeries: (String) -> Unit = {},
    onSelectEpisodeFromDrawer: (String) -> Unit = {},
    onDownloadCurrentMedia: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveProgress: () -> Unit,
    onNavigateToVip: () -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val bunnyState by playerManager.playbackState.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()

    var selectedServerKey by rememberSaveable(contentType, contentId) { mutableStateOf("main") }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isScreenLocked by rememberSaveable { mutableStateOf(false) }
    var showControlsOverlay by rememberSaveable { mutableStateOf(true) }
    var showSpeedPicker by rememberSaveable { mutableStateOf(false) }
    var showEpisodeDrawer by rememberSaveable { mutableStateOf(false) }
    var showDownloadDialog by rememberSaveable { mutableStateOf(false) }
    var isWebPlaying by rememberSaveable { mutableStateOf(true) }
    var selectedQualityTier by rememberSaveable { mutableStateOf("720p") }

    // Auto-hide player controls overlay after 3.5 seconds of playback
    LaunchedEffect(showControlsOverlay, isPlaying, isWebPlaying, isScreenLocked) {
        if (showControlsOverlay && !isScreenLocked) {
            val currentlyPlaying = if (bunnyState is BunnyPlaybackState.ReadyToPlay &&
                (bunnyState as BunnyPlaybackState.ReadyToPlay).playbackType in listOf("webpage", "embed")) {
                isWebPlaying
            } else {
                isPlaying
            }
            if (currentlyPlaying) {
                delay(3500L)
                showControlsOverlay = false
            }
        }
    }

    // 0 = Fit, 1 = Fill, 2 = Zoom
    var resizeModeIndex by rememberSaveable { mutableIntStateOf(0) }
    val resizeModeLabel = when (resizeModeIndex) {
        1 -> "Fill"
        2 -> "Zoom"
        else -> "Fit"
    }
    val exoResizeMode = when (resizeModeIndex) {
        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    // Gesture feedback states (Brightness, Volume, Double-Tap Seek)
    var brightnessLevel by rememberSaveable { mutableFloatStateOf(0.75f) }
    var volumeLevel by rememberSaveable {
        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2)
        mutableFloatStateOf((curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }
    var activeGestureBadge by remember { mutableStateOf<String?>(null) }
    var gestureTriggerTick by remember { mutableIntStateOf(0) }
    var activeWebViewRef by remember { mutableStateOf<WebView?>(null) }

    // Enforce Screen Record Allowed + Audio Capture Blocked on entry
    LaunchedEffect(activity) {
        if (activity != null) {
            HaniXSecurityShield.enforceScreenRecordAllowedButAudioBlocked(activity)
        } else {
            HaniXSecurityShield.enforceAudioCaptureBlock(context)
        }
    }

    // Fullscreen orientation & immersive system bars management
    LaunchedEffect(isFullscreen, activity) {
        if (activity != null) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Back press in fullscreen exits fullscreen first
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Auto-hide gesture badge after 1.1 seconds
    LaunchedEffect(gestureTriggerTick) {
        if (activeGestureBadge != null) {
            delay(1100L)
            activeGestureBadge = null
        }
    }

    // Periodically sync ExoPlayer position while playing
    LaunchedEffect(bunnyState, isPlaying) {
        while (bunnyState is BunnyPlaybackState.ReadyToPlay) {
            playerManager.syncPlayerPosition()
            delay(1000L)
        }
    }

    LaunchedEffect(contentType, contentId) {
        onAuthorizePlayback(contentType, contentId)
    }

    LaunchedEffect(bunnyState) {
        val ready = bunnyState as? BunnyPlaybackState.ReadyToPlay
        if (ready != null && ready.selectedServerId.isNotBlank()) {
            selectedServerKey = ready.selectedServerId
        }
    }

    DisposableEffect(contentType, contentId) {
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            onSaveProgress()
            playerManager.releasePlayer()
        }
    }

    val payload = (playbackAuthState as? RemoteDataState.Success)?.data
    val recommendedMovies = payload?.recommended.orEmpty()
    val userEmailForWeb = currentUser?.email?.trim().orEmpty()
    val isUserVip = currentUser?.isActiveVip == true

    fun triggerSeekDelta(deltaMs: Long) {
        val label = if (deltaMs > 0) "⏩ +10s" else "⏪ -10s"
        activeGestureBadge = label
        gestureTriggerTick++
        playerManager.seekByDeltaMs(deltaMs)
        val deltaSec = deltaMs / 1000
        activeWebViewRef?.evaluateJavascript(
            "(function(){ var v = document.querySelector('video'); if(v){ v.currentTime = Math.max(0, v.currentTime + ($deltaSec)); } })();",
            null
        )
    }

    fun applySpeedToAllPlayers(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        activeWebViewRef?.evaluateJavascript(
            "(function(){ var v = document.querySelector('video'); if(v){ v.playbackRate = $speed; } })();",
            null
        )
        activeGestureBadge = "⚡ Speed ${speed}x"
        gestureTriggerTick++
    }

    fun cycleAspectRatio() {
        resizeModeIndex = (resizeModeIndex + 1) % 3
        val cssFit = when (resizeModeIndex) {
            1 -> "fill"
            2 -> "cover"
            else -> "contain"
        }
        activeWebViewRef?.evaluateJavascript(
            "(function(){ var v = document.querySelector('video'); if(v){ v.style.objectFit = '$cssFit'; } })();",
            null
        )
        activeGestureBadge = "📐 Aspect: ${when (resizeModeIndex) { 1 -> "Fill"; 2 -> "Zoom"; else -> "Fit" }}"
        gestureTriggerTick++
    }

    val isCurrentlyPlaying = if (bunnyState is BunnyPlaybackState.ReadyToPlay &&
        (bunnyState as BunnyPlaybackState.ReadyToPlay).playbackType in listOf("webpage", "embed")) {
        isWebPlaying
    } else {
        isPlaying
    }

    fun togglePlayPauseAction() {
        if (bunnyState is BunnyPlaybackState.ReadyToPlay &&
            (bunnyState as BunnyPlaybackState.ReadyToPlay).playbackType in listOf("webpage", "embed")) {
            isWebPlaying = !isWebPlaying
            val jsCmd = if (isWebPlaying) {
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) {
                        v.play().catch(function() {
                            v.muted = true;
                            v.play().then(function() {
                                setTimeout(function() { v.muted = false; }, 300);
                            });
                        });
                    }
                    var b = document.querySelector('.vjs-big-play-button') || document.querySelector('#play-btn');
                    if (b) try { b.click(); } catch(e) {}
                })();
                """.trimIndent()
            } else {
                "(function(){ var v = document.querySelector('video'); if(v) v.pause(); })();"
            }
            activeWebViewRef?.evaluateJavascript(jsCmd, null)
        } else {
            playerManager.togglePlayPause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_player_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
        ) {
        if (!isFullscreen) {
            HaniXTopBar(
                title = "HaniX Cinema Player",
                subtitle = "Multi-Server HD • Live Stream",
                onBackClick = onBackClick,
                onVipClick = onNavigateToVip
            )
        }

        // Cinema Viewport (16:9 in Portrait, 100% Fullscreen in Landscape)
        Box(
            modifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(CharcoalSurface)
            },
            contentAlignment = Alignment.Center
        ) {
            when (val current = bunnyState) {
                is BunnyPlaybackState.ReadyToPlay -> {
                    val streamUrl = current.authorizedHlsUrl
                    val exoPlayer = playerManager.getPlayer()

                    if (current.playbackType == "webpage") {
                        // Direct Web Player with hardware acceleration and clean container
                        val webHeaders = mapOf("Referer" to "https://hanixurdu.com/")
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    activeWebViewRef = this
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                    webChromeClient = object : WebChromeClient() {
                                        override fun getDefaultVideoPoster(): Bitmap? {
                                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val safeEmail = userEmailForWeb.replace("'", "\\'")
                                            val isolatePlayerJs = """
                                                (function() {
                                                    try {
                                                        if ('$safeEmail' !== '') {
                                                            localStorage.setItem('userEmail', '$safeEmail');
                                                        }
                                                        var vipSec = document.getElementById('vip-section');
                                                        if (vipSec) vipSec.style.display = 'none';
                                                        var pCont = document.getElementById('player-container') || document.querySelector('.video-container');
                                                        if (pCont) {
                                                            pCont.style.width = '100vw';
                                                            pCont.style.height = '100vh';
                                                            pCont.style.position = 'fixed';
                                                            pCont.style.top = '0';
                                                            pCont.style.left = '0';
                                                            pCont.style.zIndex = '99999';
                                                        }
                                                        var toHide = ['header', '.navbar', 'footer', '#vipModal'];
                                                        toHide.forEach(function(sel) {
                                                            var els = document.querySelectorAll(sel);
                                                            for (var i = 0; i < els.length; i++) {
                                                                els[i].style.display = 'none';
                                                            }
                                                        });
                                                        document.body.style.margin = '0';
                                                        document.body.style.padding = '0';
                                                        document.body.style.background = '#000';
                                                    } catch(e) {}
                                                })();
                                            """.trimIndent()
                                            val autoPlayScript = """
                                                (function() {
                                                    var attempts = 0;
                                                    var maxAttempts = 150;
                                                    function triggerAutoPlayback() {
                                                        try {
                                                            var vids = document.querySelectorAll('video');
                                                            vids.forEach(function(v) {
                                                                v.autoplay = true;
                                                                v.muted = true;
                                                                v.setAttribute('playsinline', 'true');
                                                                v.setAttribute('muted', 'true');
                                                                v.setAttribute('autoplay', 'true');
                                                                if (v.paused) {
                                                                    v.play().then(function() {
                                                                        setTimeout(function() { 
                                                                            v.muted = false; 
                                                                            v.volume = 1.0;
                                                                        }, 1800);
                                                                    }).catch(function() {
                                                                        v.muted = true;
                                                                        v.play();
                                                                    });
                                                                }
                                                                // Aggressive interaction simulation
                                                                if (attempts < 20) {
                                                                    v.click();
                                                                    v.focus();
                                                                }
                                                            });
                                                            
                                                            // Hide all possible poster/loading elements that block interaction
                                                            var covers = document.querySelectorAll('.vjs-poster, #poster, .art-player-cover, .plyr__poster, .video-poster, .poster-img, .loading-screen, .big-play-button, .player-poster, .ytp-cued-thumbnail-overlay');
                                                            covers.forEach(function(c) { 
                                                                c.style.display = 'none'; 
                                                                c.style.opacity = '0'; 
                                                                c.style.pointerEvents = 'none';
                                                            });

                                                            var playSelectors = [
                                                                '#play-btn', '.play-btn', '.vjs-big-play-button', '.plyr__control--overlaid',
                                                                '.jw-display-icon-container', '[aria-label="Play"]', '.art-icon-play',
                                                                '.play-button', '.play-icon', '.vjs-play-control', '.plyr__play-large',
                                                                '.video-overlay-play', '.video-play-button', '.ytp-large-play-button',
                                                                '.player-play', '.bitmovinplayer-poster', '.vjs-play-button', '.ytp-play-button',
                                                                '.playbutton', '.play_button', '.play-btn-round', '.play-center', '.play-overlay'
                                                            ];
                                                            
                                                            playSelectors.forEach(function(sel) {
                                                                var btns = document.querySelectorAll(sel);
                                                                btns.forEach(function(b) {
                                                                    try {
                                                                        if (b.offsetWidth > 0 || b.offsetHeight > 0 || attempts < 10) {
                                                                            b.click();
                                                                            b.focus();
                                                                            ['mousedown', 'mouseup', 'click', 'touchstart', 'touchend'].forEach(function(ev) {
                                                                                b.dispatchEvent(new MouseEvent(ev, {bubbles:true, cancelable:true, view:window}));
                                                                                if (ev.startsWith('touch')) b.dispatchEvent(new TouchEvent(ev, {bubbles:true, cancelable:true}));
                                                                            });
                                                                        }
                                                                    } catch(e) {}
                                                                });
                                                            });

                                                            // Generic search for elements containing "play" in class or ID
                                                            var allTags = document.querySelectorAll('div, button, i, span, svg');
                                                            allTags.forEach(function(el) {
                                                                var name = (el.className || '').toString().toLowerCase() + (el.id || '').toString().toLowerCase();
                                                                if (name.indexOf('play') !== -1 && name.indexOf('player') === -1 && (el.offsetWidth > 0 || el.offsetHeight > 0)) {
                                                                    try {
                                                                        el.click();
                                                                        el.dispatchEvent(new MouseEvent('click', {bubbles:true}));
                                                                    } catch(e) {}
                                                                }
                                                            });
                                                            
                                                            if (attempts % 5 === 0) {
                                                                document.body.click();
                                                                window.dispatchEvent(new Event('click'));
                                                            }
                                                            
                                                            var iframes = document.querySelectorAll('iframe');
                                                            iframes.forEach(function(f) {
                                                                try { 
                                                                    f.click();
                                                                    f.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                                                                } catch(e) {}
                                                            });
                                                        } catch(e) {}
                                                    }
                                                    var observer = new MutationObserver(function() { triggerAutoPlayback(); });
                                                    observer.observe(document.body, { childList: true, subtree: true });
                                                    triggerAutoPlayback();
                                                    var itv = setInterval(function() {
                                                        triggerAutoPlayback();
                                                        if (attempts++ > maxAttempts) clearInterval(itv);
                                                    }, 300);
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(isolatePlayerJs, null)
                                            view?.evaluateJavascript(autoPlayScript, null)
                                            view?.evaluateJavascript(
                                                HaniXSecurityShield.buildWebViewAudioAndLinkProtectionJs(),
                                                null
                                            )
                                        }
                                    }
                                    tag = streamUrl
                                    loadUrl(streamUrl, webHeaders)
                                }
                            },
                            update = { webView ->
                                activeWebViewRef = webView
                                if (webView.tag != streamUrl) {
                                    webView.tag = streamUrl
                                    webView.loadUrl(streamUrl, webHeaders)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("embed_webview_player")
                        )
                    } else if (current.playbackType == "embed" || exoPlayer == null) {
                        val embedHeaders = mapOf("Referer" to "https://hanixurdu.com/")
                        val lowerStream = streamUrl.lowercase()
                        val isDirectVideoFile = lowerStream.endsWith(".mp4") ||
                            lowerStream.endsWith(".mkv") ||
                            lowerStream.endsWith(".webm") ||
                            lowerStream.contains(".mp4?")
                        val isGoogleDrivePreview = lowerStream.contains("drive.google.com/")

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    activeWebViewRef = this
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                    webChromeClient = object : WebChromeClient() {
                                        override fun getDefaultVideoPoster(): Bitmap? {
                                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            view?.evaluateJavascript(
                                                HaniXSecurityShield.buildWebViewAudioAndLinkProtectionJs(),
                                                null
                                            )
                                            val autoPlayJs = """
                                                (function() {
                                                    var attempts = 0;
                                                    var maxAttempts = 150;
                                                    function triggerPlayback() {
                                                        try {
                                                            var videos = document.getElementsByTagName('video');
                                                            for (var i = 0; i < videos.length; i++) {
                                                                var v = videos[i];
                                                                v.autoplay = true;
                                                                v.playsInline = true;
                                                                v.muted = true;
                                                                v.setAttribute('muted', 'true');
                                                                v.setAttribute('autoplay', 'true');
                                                                v.setAttribute('playsinline', 'true');
                                                                if (v.paused) {
                                                                    v.play().then(function(){
                                                                        setTimeout(function(){ 
                                                                            v.muted = false; 
                                                                            v.volume = 1.0;
                                                                        }, 1800);
                                                                    }).catch(function(e) {
                                                                        v.muted = true;
                                                                        v.play();
                                                                    });
                                                                }
                                                                if (attempts < 20) v.click();
                                                            }
                                                            var playSelectors = [
                                                                '.vjs-big-play-button', '#play-btn', '.play-button', '.jw-display-icon-container',
                                                                'button[aria-label="Play"]', '.plyr__control--overlaid', '.art-icon-play',
                                                                '.play-icon', '.vjs-play-control', '.video-overlay-play', '.player-play',
                                                                '.vjs-play-button', '.ytp-large-play-button', '.plyr__play-large',
                                                                '.playbutton', '.play_button', '.play-btn-round', '.play-center', '.play-overlay'
                                                            ];
                                                            playSelectors.forEach(function(sel) {
                                                                var btns = document.querySelectorAll(sel);
                                                                for (var j = 0; j < btns.length; j++) {
                                                                    try {
                                                                        var b = btns[j];
                                                                        if (b.offsetWidth > 0 || b.offsetHeight > 0) {
                                                                            b.click();
                                                                            b.dispatchEvent(new MouseEvent('mousedown', {bubbles:true}));
                                                                            b.dispatchEvent(new MouseEvent('mouseup', {bubbles:true}));
                                                                            b.dispatchEvent(new MouseEvent('click', {bubbles:true}));
                                                                            b.dispatchEvent(new TouchEvent('touchstart', {bubbles:true}));
                                                                            b.dispatchEvent(new TouchEvent('touchend', {bubbles:true}));
                                                                        }
                                                                    } catch(e){}
                                                                }
                                                            });

                                                            // Generic search
                                                            var allTags = document.querySelectorAll('div, button, i, span, svg');
                                                            allTags.forEach(function(el) {
                                                                var name = (el.className || '').toString().toLowerCase() + (el.id || '').toString().toLowerCase();
                                                                if (name.indexOf('play') !== -1 && name.indexOf('player') === -1 && (el.offsetWidth > 0 || el.offsetHeight > 0)) {
                                                                    try {
                                                                        el.click();
                                                                        el.dispatchEvent(new MouseEvent('click', {bubbles:true}));
                                                                    } catch(e) {}
                                                                }
                                                            });
                                                            if (attempts % 5 === 0) {
                                                                document.body.click();
                                                            }
                                                        } catch(e) {}
                                                    }
                                                    triggerPlayback();
                                                    var itv = setInterval(function(){
                                                        triggerPlayback();
                                                        if(attempts++ > maxAttempts) clearInterval(itv);
                                                    }, 300);
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(autoPlayJs, null)
                                        }
                                    }
                                    tag = streamUrl
                                    if (isGoogleDrivePreview) {
                                        loadUrl(streamUrl, embedHeaders)
                                    } else {
                                        val html = buildResponsivePlayerHtml(streamUrl, isDirectVideoFile)
                                        loadDataWithBaseURL(
                                            "https://hanixurdu.com/",
                                            html,
                                            "text/html",
                                            "utf-8",
                                            null
                                        )
                                    }
                                }
                            },
                            update = { webView ->
                                activeWebViewRef = webView
                                if (webView.tag != streamUrl) {
                                    webView.tag = streamUrl
                                    if (isGoogleDrivePreview) {
                                        webView.loadUrl(streamUrl, embedHeaders)
                                    } else {
                                        val html = buildResponsivePlayerHtml(streamUrl, isDirectVideoFile)
                                        webView.loadDataWithBaseURL(
                                            "https://hanixurdu.com/",
                                            html,
                                            "text/html",
                                            "utf-8",
                                            null
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("embed_webview_player")
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    resizeMode = exoResizeMode
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                                view.resizeMode = exoResizeMode
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("media3_exoplayer_view")
                        )
                    }

                    // Gesture & Control Layer (Universal: Works on Web Stream and Media3)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isScreenLocked) {
                                detectTapGestures(
                                    onTap = {
                                        showControlsOverlay = !showControlsOverlay
                                        activeWebViewRef?.evaluateJavascript(
                                            "(function(){ var v = document.querySelector('video'); if(v && v.paused){ v.play().catch(function(){ v.muted = true; v.play(); }); } var b = document.querySelector('.vjs-big-play-button') || document.querySelector('#play-btn'); if(b) b.click(); })();",
                                            null
                                        )
                                    },
                                    onDoubleTap = { offset ->
                                        if (!isScreenLocked) {
                                            if (offset.x < size.width / 2f) {
                                                triggerSeekDelta(-10_000L)
                                            } else {
                                                triggerSeekDelta(10_000L)
                                            }
                                        }
                                    }
                                )
                            }
                            .pointerInput(isScreenLocked) {
                                var dragStartX = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { startOffset ->
                                        dragStartX = startOffset.x
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        if (isScreenLocked) return@detectVerticalDragGestures
                                        change.consume()
                                        val deltaFraction = (-dragAmount / size.height.coerceAtLeast(1)).coerceIn(-0.15f, 0.15f)
                                        if (dragStartX < size.width / 2f) {
                                            // Left side: Brightness
                                            brightnessLevel = (brightnessLevel + deltaFraction).coerceIn(0.05f, 1.0f)
                                            activity?.window?.let { win ->
                                                val lp = win.attributes
                                                lp.screenBrightness = brightnessLevel
                                                win.attributes = lp
                                            }
                                            activeGestureBadge = "☀️ Brightness ${(brightnessLevel * 100).roundToInt()}%"
                                            gestureTriggerTick++
                                        } else {
                                            // Right side: Volume
                                            volumeLevel = (volumeLevel + deltaFraction).coerceIn(0.0f, 1.0f)
                                            audioManager?.let { am ->
                                                val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                val targetV = (volumeLevel * maxV).roundToInt().coerceIn(0, maxV)
                                                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetV, 0)
                                            }
                                            activeGestureBadge = "🔊 Volume ${(volumeLevel * 100).roundToInt()}%"
                                            gestureTriggerTick++
                                        }
                                    }
                                )
                            }
                    )

                    // Center Gesture Feedback Pill (Brightness / Volume / Seek / Speed / Aspect Ratio)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeGestureBadge != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            color = OledBlack.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VipChampagneGold)
                        ) {
                            Text(
                                text = activeGestureBadge.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Floating Screen Lock / Unlock Pill
                    if (isScreenLocked) {
                        Surface(
                            onClick = { isScreenLocked = false },
                            color = OledBlack.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VipChampagneGold),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .testTag("moviebox_unlock_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Unlock Screen",
                                    tint = VipChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tap to Unlock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VipGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (showControlsOverlay) {
                        // Semi-transparent cinema overlay scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.40f))
                        )

                        // Top bar controls (Title, Episode Drawer, Speed, Aspect, Lock, Fullscreen)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFullscreen) {
                                    IconButton(
                                        onClick = { isFullscreen = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Exit Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Surface(
                                    color = OledBlack.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "HaniX HD Player",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VipGoldLight,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (episodesList.isNotEmpty()) {
                                    Surface(
                                        onClick = { showEpisodeDrawer = !showEpisodeDrawer },
                                        color = CharcoalSurface,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, CinemaCrimson),
                                        modifier = Modifier.testTag("moviebox_drawer_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = CinemaCrimson, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "EP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Speed selector pill
                                Surface(
                                    onClick = { showSpeedPicker = !showSpeedPicker },
                                    color = CharcoalSurface,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("moviebox_speed_button")
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VipGoldLight,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Aspect Ratio pill
                                Surface(
                                    onClick = { cycleAspectRatio() },
                                    color = CharcoalSurface,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, GlassBorderSubtle),
                                    modifier = Modifier.testTag("moviebox_aspect_ratio_button")
                                ) {
                                    Text(
                                        text = resizeModeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Lock screen button
                                IconButton(
                                    onClick = { isScreenLocked = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("moviebox_lock_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Lock Player Controls",
                                        tint = VipChampagneGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Fullscreen toggle button
                                IconButton(
                                    onClick = { isFullscreen = !isFullscreen },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("moviebox_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Fullscreen",
                                        tint = VipChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Center Play/Pause & Seek Controls (Rewind -10s, Big Play/Pause, Forward +10s)
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("moviebox_center_controls"),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { triggerSeekDelta(-10_000L) },
                                color = Color.Transparent,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("moviebox_rewind_10s_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(8.dp, CircleShape, spotColor = Color.Black)
                                        .background(Brush.verticalGradient(listOf(Color(0xFF3D445A), Color(0xFF131620))))
                                        .border(1.5.dp, Color(0xFF5A637C).copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = TextPrimaryIvory,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { togglePlayPauseAction() },
                                color = Color.Transparent,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("moviebox_play_pause_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(10.dp, CircleShape, spotColor = CinemaCrimson)
                                        .background(Brush.verticalGradient(listOf(Color(0xFFFF3344), Color(0xFF8B0000))))
                                        .border(2.dp, VipChampagneGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Subtle Glow
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { triggerSeekDelta(10_000L) },
                                color = Color.Transparent,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("moviebox_forward_10s_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(8.dp, CircleShape, spotColor = Color.Black)
                                        .background(Brush.verticalGradient(listOf(Color(0xFF3D445A), Color(0xFF131620))))
                                        .border(1.5.dp, Color(0xFF5A637C).copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 10 seconds",
                                        tint = TextPrimaryIvory,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Bottom position row
                        if (durationMs > 0L) {
                            Surface(
                                color = OledBlack.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${formatDurationMs(currentPosMs)} / ${formatDurationMs(durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySilver,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Speed Picker Popup Overlay
                        if (showSpeedPicker) {
                            Surface(
                                color = OledBlack.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                        val selected = playbackSpeed == spd
                                        Surface(
                                            onClick = {
                                                applySpeedToAllPlayers(spd)
                                                showSpeedPicker = false
                                            },
                                            color = if (selected) CinemaCrimson else CharcoalSurface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${spd}x",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In-Player Episode Drawer Overlay
                    if (showEpisodeDrawer && episodesList.isNotEmpty()) {
                        Surface(
                            color = OledBlack.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, VipChampagneGold),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(270.dp)
                                .testTag("moviebox_episode_drawer")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Episodes (${episodesList.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VipChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { showEpisodeDrawer = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Drawer",
                                            tint = TextPrimaryIvory
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(episodesList, key = { "drawer_ep_${it.id}" }) { ep ->
                                        val cleanId = ep.id.removePrefix("vip_locked_")
                                        val isCurrentEp = cleanId == contentId
                                        val epLocked = ep.resolvedIsVip && !isUserVip
                                        Surface(
                                            onClick = {
                                                showEpisodeDrawer = false
                                                if (epLocked) {
                                                    onNavigateToVip()
                                                } else {
                                                    onSelectEpisodeFromDrawer(cleanId)
                                                }
                                            },
                                            color = if (isCurrentEp) CinemaCrimson else CharcoalSurface,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = ep.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (epLocked) "🔒 VIP" else if (isCurrentEp) "▶ Playing" else "Play",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (epLocked) VipChampagneGold else EmeraldVerified,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is BunnyPlaybackState.AuthorizingWithBackend -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CinemaCrimson)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Preparing MovieBox Stream...",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryIvory
                        )
                        Text(
                            text = "Verifying membership & server links",
                            style = MaterialTheme.typography.labelSmall,
                            color = VipChampagneGold
                        )
                    }
                }

                is BunnyPlaybackState.DeniedVipRequired -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Required",
                            tint = VipChampagneGold,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "👑 VIP Membership Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = current.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Cinema3DButton(
                            text = "Upgrade via JazzCash & WhatsApp",
                            onClick = onNavigateToVip,
                            variant = Cinema3DButtonVariant.Gold,
                            modifier = Modifier.testTag("player_vip_required_button")
                        )
                    }
                }

                is BunnyPlaybackState.ExpiredPlaybackToken -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = current.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberPending
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Cinema3DButton(
                            text = "Renew Stream Session",
                            onClick = { onAuthorizePlayback(contentType, contentId) },
                            variant = Cinema3DButtonVariant.Gold,
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black) }
                        )
                    }
                }

                is BunnyPlaybackState.BackendPendingIntegration -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Stream Pending",
                            tint = AmberPending,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting Stream Authorization",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberPending
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Retry below to reload the stream.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }

                is BunnyPlaybackState.VideoProcessing -> {
                    Text(
                        text = "Stream Status: ${current.status}",
                        style = MaterialTheme.typography.titleMedium,
                        color = VipChampagneGold
                    )
                }

                is BunnyPlaybackState.PlaybackFailure -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CinemaCrimson
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Cinema3DButton(
                            text = "Retry Stream",
                            onClick = { onAuthorizePlayback(contentType, contentId) },
                            variant = Cinema3DButtonVariant.Crimson,
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                        )
                    }
                }

                BunnyPlaybackState.Idle -> Unit
            }
        }

        // Below-Player Controls & Sections (Visible in Portrait Mode)
        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 2. 3D Download via WhatsApp Button & Caption
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Cinema3DButton(
                            text = "Download via WhatsApp",
                            onClick = {
                                runCatching {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://whatsapp.com/channel/0029Va9Wk684Spk9jV5JzQ0y")
                                    )
                                    context.startActivity(intent)
                                }
                                showDownloadDialog = true
                            },
                            variant = Cinema3DButtonVariant.Green,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            testTag = "player_whatsapp_download_button"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To download this video or episode, join our free WhatsApp Channel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                // 3. Drama Title & Category
                item {
                    val dramaTitle = episodesList.find { it.id == contentId }?.title
                        ?: if (contentType.equals("series", ignoreCase = true)) "Episode 1" else "Drama #$contentId"
                    Column {
                        Text(
                            text = dramaTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Historical",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888)
                        )
                    }
                }

                // 4. Download Status Feedback
                if (downloadActionState is RemoteDataState.Success) {
                    item {
                        Surface(
                            color = EmeraldVerified.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Saved in App-Private Offline Vault (${downloadActionState.data.title}) — Hidden from Gallery.",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else if (downloadActionState is RemoteDataState.Error) {
                    item {
                        Text(
                            text = downloadActionState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberPending
                        )
                    }
                }

                // 5. Section Header: | MORE VIDEOS & RECOMMENDED FOR YOU
                item {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .background(CinemaCrimson)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MORE VIDEOS & RECOMMENDED FOR YOU",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. Recommended Dramas Rail
                if (recommendedMovies.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recommendedMovies, key = { "rec_${it.resolvedId}" }) { recMovie ->
                                MoviePosterCard(
                                    movie = recMovie,
                                    onClick = {
                                        if (!recMovie.seriesName.isNullOrBlank()) {
                                            onSelectRecommendedSeries(recMovie.seriesName)
                                        } else {
                                            onSelectRecommendedMovie(recMovie.resolvedId)
                                        }
                                    },
                                    modifier = Modifier.width(148.dp)
                                )
                            }
                        }
                    }
                }

                // 7. Episodes Rail (if available)
                if (episodesList.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Episodes (${episodesList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(episodesList, key = { "ep_rail_${it.id}" }) { ep ->
                                    val cleanId = ep.id.removePrefix("vip_locked_")
                                    val isCurrent = cleanId == contentId
                                    val epLocked = ep.resolvedIsVip && !isUserVip
                                    Surface(
                                        onClick = {
                                            if (epLocked) {
                                                onNavigateToVip()
                                            } else if (!isCurrent) {
                                                onSelectEpisodeFromDrawer(cleanId)
                                            }
                                        },
                                        color = if (isCurrent) CinemaCrimson else CharcoalSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isCurrent) VipChampagneGold else GlassBorderSubtle),
                                        modifier = Modifier.widthIn(min = 68.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (ep.title.startsWith("Episode", ignoreCase = true)) ep.title else "EP ${ep.id}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (epLocked) {
                                                Text(
                                                    text = "VIP",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VipGoldLight,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (playbackAuthState is RemoteDataState.Error) {
                    item {
                        VvipGlassCard(
                            borderColor = CinemaCrimson.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = playbackAuthState.message.ifBlank { "Could not load video. Please switch servers or retry." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberPending,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Cinema3DButton(
                                    text = "Retry Playback",
                                    onClick = { onAuthorizePlayback(contentType, contentId) },
                                    variant = Cinema3DButtonVariant.Crimson,
                                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sleek Offline Download Quality Selection Dialog (1080p, 720p, 360p)
        if (showDownloadDialog) {
            Dialog(onDismissRequest = { showDownloadDialog = false }) {
                VvipGlassCard(
                    borderColor = VipChampagneGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("download_quality_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = VipChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download Drama",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VipChampagneGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showDownloadDialog = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondarySilver,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Saved in HaniX Private Vault (.nomedia + AES-256). Hidden from phone Gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        listOf(
                            Triple("1080p", "1080p Full HD", "Best crystal-clear quality"),
                            Triple("720p", "720p HD", "Standard high definition"),
                            Triple("360p", "360p Data Saver", "Fastest download & low storage")
                        ).forEach { (tier, title, desc) ->
                            val isSelected = selectedQualityTier == tier
                            Surface(
                                onClick = { selectedQualityTier = tier },
                                color = if (isSelected) CinemaCrimson.copy(alpha = 0.22f) else CharcoalSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) VipChampagneGold else GlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedQualityTier = tier },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = VipChampagneGold,
                                            unselectedColor = TextSecondarySilver
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimaryIvory,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedSlate
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Cinema3DButton(
                            text = "Download ($selectedQualityTier)",
                            onClick = {
                                showDownloadDialog = false
                                onDownloadCurrentMedia(contentType, contentId, selectedQualityTier)
                            },
                            variant = Cinema3DButtonVariant.Crimson,
                            icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_download_button"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        if (!isFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp)
            ) {
                FloatingChatButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/923160450074?text=Assalam-o-Alaikum%20HaniX%20Urdu%20Support")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
            ) {
                FloatingMindGamesPill(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hanixurdu.com/mind-games/"))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}



private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun buildResponsivePlayerHtml(streamUrl: String, isDirectVideoFile: Boolean): String {
    val safeUrl = streamUrl.replace("\"", "&quot;")
    return if (isDirectVideoFile) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #000;
                    overflow: hidden;
                    position: relative;
                }
                video {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    background: #000;
                }
            </style>
        </head>
        <body oncontextmenu="return false;">
            <video src="$safeUrl" controls autoplay muted playsinline preload="auto" controlsList="nodownload"></video>
        </body>
        </html>
        """.trimIndent()
    } else {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #000;
                    overflow: hidden;
                    position: relative;
                }
                iframe {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: none;
                    background: #000;
                }
            </style>
        </head>
        <body oncontextmenu="return false;">
            <iframe src="$safeUrl" allowfullscreen="true" webkitallowfullscreen="true" mozallowfullscreen="true" allow="autoplay; fullscreen; encrypted-media; picture-in-picture"></iframe>
        </body>
        </html>
        """.trimIndent()
    }
}
