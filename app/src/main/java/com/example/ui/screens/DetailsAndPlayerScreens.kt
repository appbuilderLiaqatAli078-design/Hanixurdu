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
import androidx.compose.foundation.clickable
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
                        Button(
                            onClick = { onPlayMovie(movieId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaCrimson,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("movie_authorize_playback_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("▶ Watch Now (MovieBox Player)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { onDownloadMovie(movieId) },
                            border = BorderStroke(1.dp, VipChampagneGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("movie_authorize_download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = VipChampagneGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Private App Vault (No Gallery)", color = VipGoldLight)
                        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("series_details_screen")
    ) {
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
                item {
                    VvipGlassCard(borderColor = CinemaCrimson.copy(alpha = 0.6f)) {
                        Column {
                            if (!series.resolvedPoster.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
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
                                                        OledBlack.copy(alpha = 0.94f)
                                                    )
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = series.category ?: "Urdu Historical & Drama Series",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VipChampagneGold
                                        )
                                        Text(
                                            text = series.resolvedTitle,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                if (series.resolvedPoster.isNullOrBlank()) {
                                    Text(
                                        text = series.resolvedTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = series.description ?: "Watch all seasons and episodes of ${series.resolvedTitle} on HaniX Urdu.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondarySilver
                                )
                            }
                        }
                    }
                }
            }

            // Season Selector Pills
            if (seasonsState is RemoteDataState.Success && seasonsState.data.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Select Season (${seasonsState.data.size} Available)",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(seasonsState.data, key = { it.resolvedId }) { season ->
                                val isSelected = activeSeasonId == season.resolvedId
                                Button(
                                    onClick = {
                                        activeSeasonId = season.resolvedId
                                        onLoadSeasonEpisodes(season.resolvedId)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) CinemaCrimson else CharcoalSurface,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = season.resolvedSeasonName,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inline Episodes List for the selected Season
            if (episodesState is RemoteDataState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = CinemaCrimson, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Loading Season Episodes...", color = TextSecondarySilver)
                    }
                }
            } else if (episodesState is RemoteDataState.Success && episodesState.data.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes (${episodesState.data.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentUser != null) {
                            Surface(
                                color = if (isUserVip) VipChampagneGold.copy(alpha = 0.2f) else CharcoalSurface,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUserVip) VipChampagneGold else EmeraldVerified
                                )
                            ) {
                                Text(
                                    text = if (isUserVip) {
                                        "${currentUser.name} • 👑 VIP Unlocked"
                                    } else {
                                        "${currentUser.name} • FREE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUserVip) VipGoldLight else EmeraldVerified,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
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
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Dedicated Season & Episode Screens",
                            style = MaterialTheme.typography.titleSmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onOpenSeasonSelection(seriesId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_season_selection_button")
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Season Selection Screen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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
    VvipGlassCard(
        onClick = {
            if (isLocked) onUnlockVipClick() else onPlayClick()
        },
        borderColor = if (isVip) VipChampagneGold.copy(alpha = 0.65f) else GlassBorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CharcoalSurface),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.resolvedPoster.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.resolvedPoster,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isVip) VipChampagneGold.copy(alpha = 0.92f) else CinemaCrimson.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isVip) OledBlack else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryIvory,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (isVip) VipChampagneGold else CharcoalSurface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when {
                                isVip && isUserVip -> "👑 VIP UNLOCKED"
                                isVip -> "👑 VIP"
                                else -> "FREE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVip) OledBlack else EmeraldVerified,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!episode.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = episode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isLocked) onUnlockVipClick() else onPlayClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocked) VipChampagneGold else CinemaCrimson,
                            contentColor = if (isLocked) OledBlack else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isLocked) "🔒 Unlock VIP" else "▶ Watch Episode",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (onDownloadClick != null && !isLocked) {
                        OutlinedButton(
                            onClick = onDownloadClick,
                            border = BorderStroke(1.dp, VipChampagneGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Save to App Vault",
                                tint = VipChampagneGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vault",
                                style = MaterialTheme.typography.labelSmall,
                                color = VipGoldLight
                            )
                        }
                    }
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
                            Button(
                                onClick = { onSeasonSelected(season.resolvedId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CinemaCrimson,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Episodes", fontWeight = FontWeight.Bold)
                            }
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
                        OutlinedButton(
                            onClick = {
                                val firstId = (seasonsState as? RemoteDataState.Success)?.data?.firstOrNull()?.resolvedId
                                    ?: "$seriesId||Season 1"
                                onSeasonSelected(firstId)
                            },
                            border = BorderStroke(1.dp, VipChampagneGold),
                            modifier = Modifier.testTag("inspect_episode_list_button")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Episode List (Season #1)", color = VipGoldLight)
                        }
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
                            text = "Play immediately in the MovieBox Player with Main Server, Server 2, and Direct Web Player:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val firstEpId = (episodesState as? RemoteDataState.Success)
                                    ?.data
                                    ?.firstOrNull { isUserVip || !it.id.startsWith("vip_locked_") }
                                    ?.id
                                    ?.removePrefix("vip_locked_")
                                    ?: "1"
                                onPlayEpisode(firstEpId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier.testTag("episode_authorize_playback_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open MovieBox Video Player", fontWeight = FontWeight.Bold)
                        }
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
    onSwitchServer: (PlaybackServerDto) -> Unit = {},
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
    val availableServers = payload?.availableServers.orEmpty()
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
            activeWebViewRef?.evaluateJavascript(
                "(function(){ var v = document.querySelector('video'); if(v){ if(v.paused) v.play(); else v.pause(); } })();",
                null
            )
        } else {
            playerManager.togglePlayPause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("video_player_screen")
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
                                            view?.evaluateJavascript(isolatePlayerJs, null)
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
                                color = OledBlack.copy(alpha = 0.75f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("moviebox_rewind_10s_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = TextPrimaryIvory,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { togglePlayPauseAction() },
                                color = CinemaCrimson,
                                shape = CircleShape,
                                border = BorderStroke(2.dp, VipChampagneGold),
                                modifier = Modifier
                                    .size(62.dp)
                                    .testTag("moviebox_play_pause_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = { triggerSeekDelta(10_000L) },
                                color = OledBlack.copy(alpha = 0.75f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("moviebox_forward_10s_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 10 seconds",
                                        tint = TextPrimaryIvory,
                                        modifier = Modifier.size(24.dp)
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
                        Button(
                            onClick = onNavigateToVip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier.testTag("player_vip_required_button")
                        ) {
                            Text("Upgrade via JazzCash & WhatsApp", fontWeight = FontWeight.Bold)
                        }
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
                        Button(
                            onClick = { onAuthorizePlayback(contentType, contentId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Renew Stream Session", fontWeight = FontWeight.Bold)
                        }
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
                        OutlinedButton(onClick = { onAuthorizePlayback(contentType, contentId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = VipChampagneGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Stream", color = VipChampagneGold)
                        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Drama Header & Compact Action Bar
                item {
                    val dramaTitle = episodesList.find { it.id == contentId }?.title
                        ?: if (contentType.equals("series", ignoreCase = true)) "Drama Series" else "Drama #$contentId"

                    VvipGlassCard(
                        borderColor = VipChampagneGold.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_info_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dramaTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Urdu Dubbed • 1080p Ultra HD • HaniX Official",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedSlate
                                    )
                                }

                                Surface(
                                    color = if (payload?.requiresVip == true) VipChampagneGold.copy(alpha = 0.18f) else EmeraldVerified.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, if (payload?.requiresVip == true) VipChampagneGold else EmeraldVerified)
                                ) {
                                    Text(
                                        text = if (payload?.requiresVip == true) "VIP ONLY" else "FREE HD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (payload?.requiresVip == true) VipGoldLight else EmeraldVerified,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sleek Action Buttons Row: [Download HD], [Episodes], [WhatsApp Support], [VIP Plans]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showDownloadDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CinemaCrimson,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("player_private_vault_download_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }

                                if (episodesList.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showEpisodeDrawer = true },
                                        border = BorderStroke(1.dp, GlassBorderSubtle),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = TextPrimaryIvory, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Episodes", color = TextPrimaryIvory, style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://wa.me/923160450074?text=Assalam-o-Alaikum%20HaniX%20Urdu%2C%20I%20need%20help%20with%20$contentType%20%23$contentId")
                                            )
                                            context.startActivity(intent)
                                        }
                                    },
                                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("player_whatsapp_download_button")
                                ) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = "WhatsApp", tint = EmeraldVerified, modifier = Modifier.size(16.dp))
                                }

                                OutlinedButton(
                                    onClick = onNavigateToVip,
                                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP", tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (downloadActionState is RemoteDataState.Success) {
                                Spacer(modifier = Modifier.height(10.dp))
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
                            } else if (downloadActionState is RemoteDataState.Error) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = downloadActionState.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberPending
                                )
                            }
                        }
                    }
                }

                // 2. Multi-Server Switching Row (Main Server / Server 2 / Direct Web Player)
                if (availableServers.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = VipChampagneGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Select Server",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VipChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Auto-connected to HD Web Stream",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableServers, key = { it.id }) { server ->
                                    val isSelected = selectedServerKey == server.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedServerKey = server.id
                                            onSwitchServer(server)
                                        },
                                        label = {
                                            Text(
                                                text = server.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CinemaCrimson,
                                            selectedLabelColor = Color.White,
                                            containerColor = CharcoalSurface,
                                            labelColor = TextPrimaryIvory
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) VipChampagneGold else GlassBorderSubtle
                                        ),
                                        modifier = Modifier.testTag("server_switch_${server.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Episodes Rail (if series/episodes are available)
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

                // 5. Recommended Dramas Rail
                if (recommendedMovies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recommended Dramas",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                                Button(
                                    onClick = { onAuthorizePlayback(contentType, contentId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Playback", fontWeight = FontWeight.Bold)
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

                        Button(
                            onClick = {
                                showDownloadDialog = false
                                onDownloadCurrentMedia(contentType, contentId, selectedQualityTier)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download ($selectedQualityTier)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            <video src="$safeUrl" controls autoplay playsinline preload="auto" controlsList="nodownload"></video>
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
