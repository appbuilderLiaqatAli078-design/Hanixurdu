package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.RemoteDataState
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.player.BunnyPlaybackState
import com.example.player.BunnyStreamPlayerManager
import com.example.ui.components.ContractStateBanner
import com.example.ui.components.HaniXTopBar
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
import com.example.ui.theme.VipGoldDark
import com.example.ui.theme.VipGoldLight

// =========================================================================
// 11. MOVIE DETAILS SCREEN (GET /movies/{id})
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
            subtitle = "GET /movies/$movieId • edit_movie.php",
            onBackClick = onBackClick,
            onVipClick = onNavigateToVip
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ContractStateBanner(
                    state = movieState,
                    onRetry = { onLoadMovie(movieId) },
                    onOpenChecklist = onOpenChecklist
                )
            }

            if (movieState is RemoteDataState.Success) {
                val movie = movieState.data
                item {
                    VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (movie.isPremium == true) VipChampagneGold else CharcoalSurface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (movie.isPremium == true) "VVIP EXCLUSIVE" else "STANDARD ACCESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (movie.isPremium == true) OledBlack else TextPrimaryIvory,
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
                                color = TextPrimaryIvory
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = movie.description ?: "No synopsis provided by the PHP backend.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondarySilver
                            )
                        }
                    }
                }
            }

            // Playback & Encrypted Offline Download Actions (Always accessible to test POST /playback/authorize & POST /downloads/authorize)
            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Bunny Stream & Offline Entitlement Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Playback requests a short-lived signed Bunny HLS token from POST /playback/authorize. Downloads use POST /downloads/authorize and AES-256-GCM Android Keystore storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onPlayMovie(movieId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("movie_authorize_playback_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authorize & Stream via Bunny HLS", fontWeight = FontWeight.Bold)
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
                            Text("Authorize Encrypted Offline Download", color = VipGoldLight)
                        }
                    }
                }
            }

            if (downloadActionState !is RemoteDataState.Idle) {
                item {
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

// =========================================================================
// 12. SERIES DETAILS SCREEN (GET /series/{id})
// =========================================================================

@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    seriesState: RemoteDataState<Series>,
    seasonsState: RemoteDataState<List<Season>>,
    onLoadSeriesAndSeasons: (String) -> Unit,
    onOpenSeasonSelection: (String) -> Unit,
    onSelectSeason: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    LaunchedEffect(seriesId) {
        onLoadSeriesAndSeasons(seriesId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("series_details_screen")
    ) {
        HaniXTopBar(
            title = "Series Details",
            subtitle = "GET /series/$seriesId • view_series.php",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ContractStateBanner(
                    state = seriesState,
                    onRetry = { onLoadSeriesAndSeasons(seriesId) },
                    onOpenChecklist = onOpenChecklist
                )
            }

            if (seriesState is RemoteDataState.Success) {
                val series = seriesState.data
                item {
                    VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = series.category ?: "Urdu Series",
                                style = MaterialTheme.typography.labelMedium,
                                color = VipChampagneGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = series.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimaryIvory
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = series.description ?: "No series description provided by backend.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondarySilver
                            )
                        }
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Seasons & Episodes Navigation",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fetches seasons from GET /series/$seriesId/seasons and episodes from GET /seasons/{id}/episodes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onOpenSeasonSelection(seriesId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
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
                items(seasonsState.data, key = { it.id }) { season ->
                    VvipGlassCard(
                        onClick = { onSelectSeason(season.id) },
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
                                text = season.seasonName ?: "Season ${season.id}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                            Text(
                                text = "View Episodes →",
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

// =========================================================================
// 13. SEASON SELECTION SCREEN (GET /series/{id}/seasons)
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
            subtitle = "GET /series/$seriesId/seasons • view_series.php",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ContractStateBanner(
                    state = seasonsState,
                    onRetry = { onLoadSeasons(seriesId) },
                    onOpenChecklist = onOpenChecklist
                )
            }

            if (seasonsState is RemoteDataState.Success) {
                items(seasonsState.data, key = { it.id }) { season ->
                    VvipGlassCard(
                        onClick = { onSeasonSelected(season.id) },
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
                                    text = season.seasonName ?: "Season ${season.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "Series ID: ${season.seriesId} • Season ID: ${season.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                            Button(
                                onClick = { onSeasonSelected(season.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VipChampagneGold,
                                    contentColor = OledBlack
                                )
                            ) {
                                Text("Episodes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Contract explorer button to inspect Episode List screen even while backend is pending
            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Episode List Contract Inspector",
                            style = MaterialTheme.typography.titleSmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Inspect the Episode List screen (`GET /seasons/1/episodes`) and Episode Playback contract (`GET /episodes/{id}`):",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSeasonSelected("1") },
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
// 14. EPISODE LIST SCREEN (GET /seasons/{id}/episodes & GET /episodes/{id})
// =========================================================================

@Composable
fun EpisodeListScreen(
    seasonId: String,
    episodesState: RemoteDataState<List<Episode>>,
    onLoadEpisodes: (String) -> Unit,
    onPlayEpisode: (String) -> Unit,
    onDownloadEpisode: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
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
            title = "Season Episodes",
            subtitle = "GET /seasons/$seasonId/episodes • run_video.php",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ContractStateBanner(
                    state = episodesState,
                    onRetry = { onLoadEpisodes(seasonId) },
                    onOpenChecklist = onOpenChecklist
                )
            }

            if (episodesState is RemoteDataState.Success) {
                items(episodesState.data, key = { it.id }) { episode ->
                    VvipGlassCard(
                        borderColor = if (episode.isPremium == true) VipChampagneGold.copy(alpha = 0.6f) else GlassBorderSubtle
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = episode.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory,
                                    modifier = Modifier.weight(1f)
                                )
                                if (episode.isPremium == true) {
                                    Surface(
                                        color = VipChampagneGold,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "VVIP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OledBlack,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            if (!episode.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = episode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySilver
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Duration: ${episode.duration ?: "N/A"} • Availability: ${episode.playbackAvailability ?: "Backend Checked"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VipGoldLight
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onPlayEpisode(episode.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VipChampagneGold,
                                        contentColor = OledBlack
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play HLS", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onDownloadEpisode(episode.id) },
                                    border = BorderStroke(1.dp, VipChampagneGold)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Bunny Stream Episode Playback Verification",
                            style = MaterialTheme.typography.titleSmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Test `POST /playback/authorize` for Episode #1 in the Bunny Stream HLS Video Player:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onPlayEpisode("1") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier.testTag("episode_authorize_playback_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Bunny Stream Video Player", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 15. VIDEO PLAYER SCREEN (Bunny Stream HLS + POST /playback/authorize)
// =========================================================================

@Composable
fun VideoPlayerScreen(
    contentType: String,
    contentId: String,
    playerManager: BunnyStreamPlayerManager,
    playbackAuthState: RemoteDataState<PlaybackAuthorizationPayload>,
    onAuthorizePlayback: (String, String) -> Unit,
    onSaveProgress: () -> Unit,
    onNavigateToVip: () -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val bunnyState by playerManager.playbackState.collectAsState()

    LaunchedEffect(contentType, contentId) {
        onAuthorizePlayback(contentType, contentId)
    }

    DisposableEffect(contentType, contentId) {
        onDispose {
            onSaveProgress()
            playerManager.releasePlayer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("video_player_screen")
    ) {
        HaniXTopBar(
            title = "Bunny Stream HLS Player",
            subtitle = "POST /playback/authorize • $contentType #$contentId",
            onBackClick = onBackClick,
            onVipClick = onNavigateToVip
        )

        // 16:9 Cinema Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(CharcoalSurface),
            contentAlignment = Alignment.Center
        ) {
            when (val current = bunnyState) {
                is BunnyPlaybackState.ReadyToPlay -> {
                    val exoPlayer = playerManager.getPlayer()
                    if (exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("media3_exoplayer_view")
                        )
                    }
                }

                is BunnyPlaybackState.AuthorizingWithBackend -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VipChampagneGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Requesting Signed Bunny Stream Token...",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryIvory
                        )
                        Text(
                            text = "POST /playback/authorize ($contentType/$contentId)",
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
                            text = "VVIP Membership Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipGoldLight
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
                            )
                        ) {
                            Text("Unlock VVIP Access", fontWeight = FontWeight.Bold)
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
                            contentDescription = "Backend Authorization Pending",
                            tint = AmberPending,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting Backend Signed Bunny HLS Token",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberPending
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No fake Bunny Stream URL is used. Deploy POST /playback/authorize on hanixurdu.com to return an authorized .m3u8 URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }

                is BunnyPlaybackState.VideoProcessing -> {
                    Text(
                        text = "Bunny Stream Transcoding Status: ${current.status}",
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
                            Text("Re-Authorize Stream", color = VipChampagneGold)
                        }
                    }
                }

                BunnyPlaybackState.Idle -> Unit
            }
        }

        // 6-Step Playback Security Verification Checklist (Section 5 & Section 8)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ContractStateBanner(
                    state = playbackAuthState,
                    onRetry = { onAuthorizePlayback(contentType, contentId) },
                    onOpenChecklist = onOpenChecklist
                )
            }

            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.45f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldVerified
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "6-Step Bunny Stream Server Authorization Pipeline",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val steps = listOf(
                            "1. Authenticate user Bearer token on Hostinger PHP backend",
                            "2. Verify movie/episode availability in MySQL catalog",
                            "3. Check VIP requirements (check_premium.php logic)",
                            "4. Verify account access permissions & active status",
                            "5. Generate short-lived Bunny Stream SHA-256 playback token server-side",
                            "6. Return only signed HLS playlist URL — zero Bunny API keys in APK"
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySilver,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
