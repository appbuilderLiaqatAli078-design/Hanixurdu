package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.BannerItem
import com.example.data.model.CategoryItem
import com.example.data.model.HomePayload
import com.example.data.model.MindGameQuestionBank
import com.example.data.model.Movie
import com.example.data.model.RemoteDataState
import com.example.data.model.SearchPayload
import com.example.data.model.Series
import com.example.data.model.User
import com.example.ui.components.ContractStateBanner
import com.example.ui.components.HaniXTopBar
import com.example.ui.components.MoviePosterCard
import com.example.ui.components.SecurityAssuranceStrip
import com.example.ui.components.SeriesPosterCard
import com.example.ui.components.VvipGlassCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextPrimaryIvory
import com.example.ui.theme.TextSecondarySilver
import com.example.ui.theme.VipChampagneGold
import com.example.ui.theme.VipGoldDark
import com.example.ui.theme.VipGoldLight

// =========================================================================
// 6. HOME SCREEN — Matches Live `hanixurdu.com/index.php` & `header.php`
// =========================================================================

@Composable
fun HomeScreen(
    homeState: RemoteDataState<HomePayload>,
    continueWatchingList: List<WatchHistoryEntity>,
    currentUser: User?,
    unreadNotifications: Int,
    apiBaseUrl: String,
    onRefreshHome: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToVip: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToContinueWatching: () -> Unit,
    onNavigateToWatchHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onMovieSelected: (String) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onResumePlayback: (String, String) -> Unit
) {
    LaunchedEffect(Unit) {
        if (homeState is RemoteDataState.Idle) {
            onRefreshHome()
        }
    }

    var showMindGameModal by rememberSaveable { mutableStateOf(false) }
    var vipModalPendingTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(id, isSeries)
    var showVipModal by rememberSaveable { mutableStateOf(false) }
    var addedToListFeedback by rememberSaveable { mutableStateOf(false) }

    val handleCardNavigation: (Movie) -> Unit = { movie ->
        val isSeries = movie.resolvedIsSeriesItem || !movie.seriesName.isNullOrBlank()
        val targetId = if (isSeries) (movie.seriesName ?: movie.resolvedId) else movie.resolvedId
        if (movie.resolvedIsVip && currentUser?.isActiveVip != true) {
            vipModalPendingTarget = targetId to isSeries
            showVipModal = true
        } else if (isSeries) {
            onSeriesSelected(targetId)
        } else {
            onMovieSelected(targetId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .testTag("home_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Exact `hanixurdu.com` Top Header Bar (`header.php`)
            HaniXWebsiteHeader(
                currentUser = currentUser,
                unreadNotifications = unreadNotifications,
                onHomeClick = onRefreshHome,
                onStudioClick = onNavigateToProfile,
                onHelpClick = onNavigateToProfile,
                onSearchClick = onNavigateToSearch,
                onLoginClick = onNavigateToLogin,
                onProfileClick = onNavigateToProfile,
                onVipClick = { showVipModal = true },
                onNotificationsClick = onNavigateToNotifications
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. 3D Coverflow Hero Carousel (matching `index.php` `.hero-coverflow-section`)
                if (homeState is RemoteDataState.Success) {
                    val payload = homeState.data
                    val banners = payload.banners.orEmpty().ifEmpty {
                        payload.resolvedFeaturedMovies.mapIndexed { idx, m ->
                            BannerItem(
                                id = "banner_${idx}_${m.resolvedId}",
                                title = m.title,
                                subtitle = m.description,
                                backdropUrl = m.resolvedPoster,
                                posterPath = m.resolvedPoster,
                                contentType = if (m.resolvedIsSeriesItem) "series" else "movie",
                                contentId = m.resolvedId,
                                seriesName = m.seriesName,
                                isPremium = m.resolvedIsVip
                            )
                        }
                    }

                    if (banners.isNotEmpty()) {
                        item {
                            HaniXHeroCoverflowSection(
                                banners = banners,
                                isActiveVip = currentUser?.isActiveVip == true,
                                addedToListFeedback = addedToListFeedback,
                                onWatchBanner = { banner ->
                                    val isSeries = banner.contentType.equals("series", ignoreCase = true) ||
                                        !banner.seriesName.isNullOrBlank()
                                    val targetId = if (isSeries) (banner.seriesName ?: banner.contentId) else banner.contentId
                                    if (banner.resolvedIsVip && currentUser?.isActiveVip != true) {
                                        vipModalPendingTarget = targetId to isSeries
                                        showVipModal = true
                                    } else if (isSeries) {
                                        onSeriesSelected(targetId)
                                    } else {
                                        onMovieSelected(targetId)
                                    }
                                },
                                onAddToMyList = { addedToListFeedback = !addedToListFeedback },
                                onOpenVipModal = {
                                    vipModalPendingTarget = null
                                    showVipModal = true
                                }
                            )
                        }
                    }
                } else if (homeState is RemoteDataState.Error) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CinemaCrimson.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = homeState.message.ifBlank { "Could not load content. Please check internet connection." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimaryIvory,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onRefreshHome,
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (homeState is RemoteDataState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VipChampagneGold, strokeWidth = 2.5.dp)
                        }
                    }
                }

                // Quick Navigation Bar (Categories, Studio, Mind Games, VIP, Continue Watching, History, Help)
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            QuickNavPill(
                                icon = Icons.Default.Category,
                                label = "Categories",
                                testTag = "home_quick_categories",
                                onClick = onNavigateToCategories
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.Mic,
                                label = "Dubbing Studio",
                                testTag = "home_quick_dubbing_studio",
                                onClick = onNavigateToProfile
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.Quiz,
                                label = "Mind Games Quiz",
                                testTag = "home_quick_mind_game",
                                onClick = { showMindGameModal = true }
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.WorkspacePremium,
                                label = "VIP Membership",
                                testTag = "home_quick_vip",
                                onClick = onNavigateToVip
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.PlayArrow,
                                label = "Continue Watching",
                                testTag = "home_quick_continue_watching",
                                onClick = onNavigateToContinueWatching
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.History,
                                label = "Watch History",
                                testTag = "home_quick_watch_history",
                                onClick = onNavigateToWatchHistory
                            )
                        }
                        item {
                            QuickNavPill(
                                icon = Icons.Default.SupportAgent,
                                label = "Help & WhatsApp",
                                testTag = "home_quick_help",
                                onClick = onNavigateToProfile
                            )
                        }
                    }
                }

                // Continue Watching Rail (if local Room has active progress items)
                if (continueWatchingList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeaderRow(
                                title = "Continue Watching",
                                actionLabel = "View All",
                                onActionClick = onNavigateToContinueWatching
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(continueWatchingList, key = { it.historyKey }) { item ->
                                    VvipGlassCard(
                                        modifier = Modifier.width(240.dp),
                                        onClick = { onResumePlayback(item.contentType, item.contentId) }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimaryIvory,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!item.subtitle.isNullOrBlank()) {
                                                Text(
                                                    text = item.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondarySilver,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(
                                                progress = { item.progressFraction },
                                                color = Color(0xFFFF0000),
                                                trackColor = GlassCardElevated,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. All 5 Live Category Rails with Drama Titles
                if (homeState is RemoteDataState.Success) {
                    val payload = homeState.data

                    // Section 1: Urdu Subtitles
                    if (!payload.urduSubtitles.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "Urdu Subtitles",
                                items = payload.urduSubtitles,
                                isLandscape = true,
                                keyPrefix = "urdu",
                                onActionClick = onNavigateToSeries,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }

                    // Section 2: All Dramas
                    if (!payload.allContent.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "All Dramas",
                                items = payload.allContent,
                                isLandscape = false,
                                keyPrefix = "all",
                                onActionClick = onNavigateToMovies,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }

                    // Section 3: Coming Soon
                    if (!payload.comingSoon.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "Coming Soon",
                                items = payload.comingSoon,
                                isLandscape = false,
                                keyPrefix = "soon",
                                onActionClick = onNavigateToMovies,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }

                    // Section 4: HaniX Originals
                    if (!payload.hanixOriginals.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "HaniX Originals",
                                items = payload.hanixOriginals,
                                isLandscape = false,
                                keyPrefix = "orig",
                                onActionClick = onNavigateToSeries,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }

                    // Section 5: VIP Exclusives
                    if (!payload.premiumContent.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "VIP Exclusives",
                                items = payload.premiumContent,
                                isLandscape = false,
                                keyPrefix = "prem",
                                onActionClick = onNavigateToVip,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }

                    // Trending Series rail (`series.php`)
                    if (!payload.featuredSeries.isNullOrEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                WebsiteRedBarHeading(
                                    title = "Urdu Dubbed Series",
                                    actionLabel = "All Series",
                                    onActionClick = onNavigateToSeries
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    itemsIndexed(
                                        payload.featuredSeries,
                                        key = { idx, s -> "sfeat_${idx}_${s.resolvedId}" }
                                    ) { index, series ->
                                        SeriesPosterCard(
                                            series = series,
                                            rankNumber = index + 1,
                                            onClick = {
                                                if (series.resolvedIsVip && currentUser?.isActiveVip != true) {
                                                    vipModalPendingTarget = series.resolvedId to true
                                                    showVipModal = true
                                                } else {
                                                    onSeriesSelected(series.resolvedId)
                                                }
                                            },
                                            modifier = Modifier.width(152.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Latest Standalone Movies / Episodes (`run_video.php`)
                    if (!payload.latestMovies.isNullOrEmpty()) {
                        item {
                            WebsiteCategoryRail(
                                title = "Latest Episodes & Movies",
                                items = payload.latestMovies,
                                isLandscape = false,
                                keyPrefix = "lat",
                                onActionClick = onNavigateToMovies,
                                onCardClick = handleCardNavigation
                            )
                        }
                    }
                }

                // 3. "🔥 Check Out Our Latest Updates — View Updates →" Card (matching `index.php` lines 163-167)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D))
                                )
                            )
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🔥 Check Out Our Latest Updates",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "See what's new, upcoming series, and platform improvements!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAAA),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToProfile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE50914),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ) {
                                Text(
                                    text = "View Updates →",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Footer & Contract Quick Links
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToAbout,
                                border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_hero_checklist_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("API Checklist", color = VipGoldLight, style = MaterialTheme.typography.labelSmall)
                            }
                            if (currentUser == null) {
                                OutlinedButton(
                                    onClick = onNavigateToLogin,
                                    border = BorderStroke(1.dp, Color(0xFFFF0000)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_hero_login_button")
                                ) {
                                    Text("Sign In / Register", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onNavigateToVip,
                                    border = BorderStroke(1.dp, VipChampagneGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_hero_vip_button")
                                ) {
                                    Text("Membership Status", color = VipChampagneGold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        SecurityAssuranceStrip()
                    }
                }
            }
        }

        // Floating `🧠 Mind Games` Gradient Pill Button (matching `.mind-game-btn` in `header.php`)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(50))
                .clickable { showMindGameModal = true }
                .testTag("floating_mind_games_button"),
            color = Color.Transparent,
            shape = RoundedCornerShape(50),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)),
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF0055), Color(0xFFFF9900))
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧠 Mind Games",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    // Interactive Mind Games Quiz Modal (`mind_game.php`)
    if (showMindGameModal) {
        MindGameQuizDialog(
            onDismiss = { showMindGameModal = false }
        )
    }

    // Interactive VIP Upgrade Modal (`#vipModal` from `index.php` & `series.php`)
    if (showVipModal) {
        VipUpgradeModalDialog(
            onDismiss = { showVipModal = false },
            onOpenFullVipScreen = {
                showVipModal = false
                onNavigateToVip()
            },
            onPreviewDetailsAnyway = vipModalPendingTarget?.let { (targetId, isSeries) ->
                {
                    showVipModal = false
                    if (isSeries) onSeriesSelected(targetId) else onMovieSelected(targetId)
                }
            }
        )
    }
}

/**
 * Top Website Navbar matching `hanixurdu.com` (`header.php`):
 * `HaniX` (Red X) + `URDU` | `HOME` | `STUDIO` | `HELP` | `🔍` | `LOGIN`
 */
@Composable
private fun HaniXWebsiteHeader(
    currentUser: User?,
    unreadNotifications: Int,
    onHomeClick: () -> Unit,
    onStudioClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onVipClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0A0A0A).copy(alpha = 0.96f),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: `HaniX` + `URDU` Logo
            Column(
                modifier = Modifier
                    .clickable(onClick = onHomeClick)
                    .testTag("website_header_logo")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hani",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "X",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFF0000),
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "U R D U",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    fontWeight = FontWeight.Bold
                )
            }

            // Center: HOME | STUDIO | HELP
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOME",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF0000),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onHomeClick)
                )
                Text(
                    text = "STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onStudioClick)
                )
                Text(
                    text = "HELP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onHelpClick)
                )
            }

            // Right: Search Icon + Notifications + LOGIN or Genuine User Name + FREE/VIP Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search movies and series",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(
                                    containerColor = Color(0xFFFF0000),
                                    contentColor = Color.White
                                ) {
                                    Text(unreadNotifications.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (currentUser == null) {
                    Surface(
                        color = Color(0xFFFF0000),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable(onClick = onLoginClick)
                            .testTag("top_bar_login_button")
                    ) {
                        Text(
                            text = "LOGIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    val genuineName = currentUser.name.orEmpty()
                        .ifBlank { currentUser.email.orEmpty().substringBefore("@") }
                        .ifBlank { "Member" }
                        .take(12)
                    val isVip = currentUser.isActiveVip

                    Surface(
                        color = if (isVip) Color(0xFF1F1905) else Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (isVip) Color(0xFFFFD700) else Color(0xFF2DD4BF)
                        ),
                        modifier = Modifier
                            .clickable(onClick = if (isVip) onProfileClick else onVipClick)
                            .testTag("top_bar_vip_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = genuineName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (isVip) Color(0xFFFFD700) else Color(0xFF0F2922),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (isVip) "👑 VIP" else "FREE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isVip) Color.Black else EmeraldVerified,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D Coverflow Hero Carousel matching `hanixurdu.com/index.php` (`.hero-coverflow-section`):
 * - Dynamic blurred background poster of the active centered item
 * - Left / Center / Right 3D cards with active red border glow (`#FF0000`) and gold VIP crown (`👑`)
 * - Centered action bar: `▶ Watch Now`, `+`, `👑 Join VIP`
 */
@Composable
private fun HaniXHeroCoverflowSection(
    banners: List<BannerItem>,
    isActiveVip: Boolean,
    addedToListFeedback: Boolean,
    onWatchBanner: (BannerItem) -> Unit,
    onAddToMyList: () -> Unit,
    onOpenVipModal: () -> Unit
) {
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = activeIndex.coerceIn(0, (banners.size - 1).coerceAtLeast(0))
    val activeBanner = banners.getOrNull(safeIndex) ?: return
    val prevIndex = if (safeIndex - 1 >= 0) safeIndex - 1 else banners.lastIndex
    val nextIndex = if (safeIndex + 1 < banners.size) safeIndex + 1 else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(410.dp)
    ) {
        // Blurred dynamic background image (`#hero-bg` in `index.php`)
        val bgPoster = activeBanner.posterPath ?: activeBanner.backdropUrl
        if (!bgPoster.isNullOrBlank()) {
            AsyncImage(
                model = bgPoster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .graphicsLayer { alpha = 0.55f }
            )
        }

        // Vignette gradient overlay (`linear-gradient(to bottom, rgba(0,0,0,0.4) 0%, #0a0a0a 100%)`)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color(0xFF0A0A0A).copy(alpha = 0.75f),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 3D Coverflow Stage (Left Card, Active Center Card, Right Card + Prev/Next Arrows)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left Background Card (`prev` in `index.php`)
                if (banners.size > 1) {
                    val leftBanner = banners[prevIndex]
                    CoverflowCardItem(
                        banner = leftBanner,
                        isActive = false,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp)
                            .width(128.dp)
                            .height(190.dp)
                            .graphicsLayer {
                                scaleX = 0.85f
                                scaleY = 0.85f
                                alpha = 0.6f
                                rotationY = 18f
                            },
                        onClick = { activeIndex = prevIndex }
                    )
                }

                // Right Background Card (`next` in `index.php`)
                if (banners.size > 1) {
                    val rightBanner = banners[nextIndex]
                    CoverflowCardItem(
                        banner = rightBanner,
                        isActive = false,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .width(128.dp)
                            .height(190.dp)
                            .graphicsLayer {
                                scaleX = 0.85f
                                scaleY = 0.85f
                                alpha = 0.6f
                                rotationY = -18f
                            },
                        onClick = { activeIndex = nextIndex }
                    )
                }

                // Center Active Card (`active` in `index.php` with `#FF0000` border & glow)
                val activeScale by animateFloatAsState(targetValue = 1f, label = "coverflow_active_scale")
                CoverflowCardItem(
                    banner = activeBanner,
                    isActive = true,
                    modifier = Modifier
                        .width(172.dp)
                        .height(246.dp)
                        .graphicsLayer {
                            scaleX = activeScale
                            scaleY = activeScale
                        },
                    onClick = { onWatchBanner(activeBanner) }
                )

                // Left Arrow (`❮`)
                if (banners.size > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clickable { activeIndex = prevIndex }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Banner",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right Arrow (`❯`)
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clickable { activeIndex = nextIndex }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Banner",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Active Coverflow Title + Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = activeBanner.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (activeBanner.resolvedIsVip) "👑 VIP Exclusive • Urdu Dubbed & Subtitles" else "Trending on HaniX Urdu",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeBanner.resolvedIsVip) Color(0xFFFFD700) else Color(0xFFCCCCCC)
                )
            }

            // 3 Centered Action Buttons (`▶ Watch Now`, `+`, `👑 Join VIP`) matching `.hero-actions-center` in `index.php`
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 1. `▶ Watch Now` (White pill button with black bold text)
                Button(
                    onClick = { onWatchBanner(activeBanner) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("coverflow_watch_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Now",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }

                // 2. `+` (Dark translucent square glass button)
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onAddToMyList)
                        .testTag("coverflow_add_list_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (addedToListFeedback) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Add to My List",
                            tint = if (addedToListFeedback) EmeraldVerified else Color.White
                        )
                    }
                }

                // 3. `👑 Join VIP` (Gold gradient button `#FFD700 -> #FFAA00` with black bold text)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenVipModal)
                        .testTag("coverflow_join_vip_button")
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFAA00))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isActiveVip) "👑 VIP Active" else "👑 Join VIP",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverflowCardItem(
    banner: BannerItem,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val posterUrl = banner.posterPath ?: banner.backdropUrl
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) Color(0xFFFF0000) else Color.White.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 16.dp else 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (banner.resolvedIsVip) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👑", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Horizontal category rail matching `hanixurdu.com/index.php` (`.trending-section`):
 * Red vertical bar heading (`border-left: 4px solid #ff0000`) + numbered cards (`1, 2, 3...`).
 */
@Composable
private fun WebsiteCategoryRail(
    title: String,
    items: List<Movie>,
    isLandscape: Boolean,
    keyPrefix: String,
    onActionClick: () -> Unit,
    onCardClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        WebsiteRedBarHeading(
            title = title,
            actionLabel = "See All",
            onActionClick = onActionClick
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(
                items,
                key = { idx, item -> "${keyPrefix}_${idx}_${item.resolvedId}" }
            ) { index, movie ->
                MoviePosterCard(
                    movie = movie,
                    rankNumber = index + 1,
                    isLandscape = isLandscape,
                    onClick = { onCardClick(movie) },
                    modifier = Modifier.width(if (isLandscape) 235.dp else 152.dp)
                )
            }
        }
    }
}

@Composable
private fun WebsiteRedBarHeading(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .background(Color(0xFFFF0000))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(onClick = onActionClick) {
            Text(text = actionLabel, color = Color(0xFFCCCCCC), style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = actionLabel,
                tint = Color(0xFFFF0000),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Interactive VIP Upgrade Modal matching `#vipModal` in `index.php` & `series.php`:
 * Shows JazzCash details (`03294806457` — `Muhammad Hasnain`), Rs. 250 / Month, and WhatsApp activation button.
 */
@Composable
fun VipUpgradeModalDialog(
    onDismiss: () -> Unit,
    onOpenFullVipScreen: () -> Unit,
    onPreviewDetailsAnyway: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            border = BorderStroke(2.dp, Color(0xFFFFD700)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFAAAAAA))
                    }
                }
                Text("👑", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "VIP Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unlock all VIP Exclusive series & movies on HaniX Urdu!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Payment Details Box (matching `.payment-details` in `index.php`)
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📅 1 Month VIP: Rs. 250",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🏦 Method: JazzCash",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDDDDD)
                        )
                        Text(
                            text = "📱 Account Number: 03294806457",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "👤 Account Name: Muhammad Hasnain",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDDDDD)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Send the payment and share the screenshot along with your registered Email on our WhatsApp to activate your VIP status immediately.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Green WhatsApp Button (`https://wa.me/923160450074`)
                Button(
                    onClick = {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/923160450074?text=Hello%20HaniX%20Urdu!%20I%20have%20paid%20Rs.%20250%20for%20VIP%20Membership.%20Here%20is%20my%20screenshot%20and%20email:")
                            )
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💬 Send Screenshot on WhatsApp", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFullVipScreen,
                        border = BorderStroke(1.dp, Color(0xFFFFD700)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("All VIP Plans", color = Color(0xFFFFD700), style = MaterialTheme.typography.labelSmall)
                    }
                    if (onPreviewDetailsAnyway != null) {
                        OutlinedButton(
                            onClick = onPreviewDetailsAnyway,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Episodes", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive 10-Question Mind Games Quiz Dialog matching `hanixurdu.com/mind_game.php`.
 */
@Composable
fun MindGameQuizDialog(
    onDismiss: () -> Unit
) {
    var questions by remember { mutableStateOf(MindGameQuestionBank.drawTenRandomQuestions()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isFinished by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
            border = BorderStroke(1.5.dp, Color(0xFFFF0055).copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 Mind Refreshing Game",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isFinished && currentIndex < questions.size) {
                    val q = questions[currentIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} / ${questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFAAAAAA)
                        )
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                        color = Color(0xFFFF0055),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    q.options.forEachIndexed { idx, optText ->
                        val isSelected = selectedOption == idx
                        val isCorrect = idx == q.answerIndex
                        val bgColor = when {
                            selectedOption == -1 -> Color.White.copy(alpha = 0.06f)
                            isCorrect -> Color(0xFF28A745).copy(alpha = 0.25f)
                            isSelected -> Color(0xFFDC3545).copy(alpha = 0.25f)
                            else -> Color.White.copy(alpha = 0.04f)
                        }
                        val borderColor = when {
                            selectedOption == -1 -> Color.White.copy(alpha = 0.15f)
                            isCorrect -> Color(0xFF28A745)
                            isSelected -> Color(0xFFDC3545)
                            else -> Color.White.copy(alpha = 0.08f)
                        }

                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = selectedOption == -1) {
                                    selectedOption = idx
                                    if (idx == q.answerIndex) {
                                        score++
                                    }
                                }
                        ) {
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    if (selectedOption != -1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (currentIndex + 1 < questions.size) {
                                    currentIndex++
                                    selectedOption = -1
                                } else {
                                    isFinished = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0055),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentIndex + 1 < questions.size) "Next Question →" else "See Final Score",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (score == 10) "🏆" else if (score >= 7) "🎉" else "👍",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$score / ${questions.size}",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            score == 10 -> "Unbelievable! 10/10! Genius Level!"
                            score >= 7 -> "Great Job! Your mind is super sharp and refreshed!"
                            score >= 5 -> "Good effort! Let's play again to get a higher score."
                            else -> "Nice try! Play again with 10 new random questions!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCCCCCC),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            questions = MindGameQuestionBank.drawTenRandomQuestions()
                            currentIndex = 0
                            score = 0
                            selectedOption = -1
                            isFinished = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0055),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔄 Play Again (New Questions)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNavPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VipChampagneGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimaryIvory
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimaryIvory
        )
        TextButton(onClick = onActionClick) {
            Text(text = actionLabel, color = VipChampagneGold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = actionLabel,
                tint = VipChampagneGold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// =========================================================================
// 7. MOVIES SCREEN (GET /movies, GET /movies/categories)
// =========================================================================

@Composable
fun MoviesScreen(
    moviesState: RemoteDataState<List<Movie>>,
    selectedCategory: String?,
    unreadNotifications: Int,
    onLoadMovies: (String?) -> Unit,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (moviesState is RemoteDataState.Idle) {
            onLoadMovies(selectedCategory)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("movies_screen")
    ) {
        HaniXTopBar(
            title = "Urdu & Dubbed Dramas",
            subtitle = if (selectedCategory == null) "All Dramas" else "Category: $selectedCategory",
            unreadNotifications = unreadNotifications,
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotificationsClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onLoadMovies(null) },
                    label = { Text("All Dramas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VipChampagneGold,
                        selectedLabelColor = OledBlack
                    )
                )
                OutlinedButton(
                    onClick = onCategoriesClick,
                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("movies_browse_categories_button")
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Categories", color = VipGoldLight)
                }
            }

            if (moviesState is RemoteDataState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VipChampagneGold, strokeWidth = 2.5.dp)
                }
            } else if (moviesState is RemoteDataState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CinemaCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = moviesState.message.ifBlank { "Could not load dramas. Please try again." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryIvory,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onLoadMovies(selectedCategory) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val moviesToDisplay: List<Movie> = when (moviesState) {
                is RemoteDataState.Success -> moviesState.data
                is RemoteDataState.Offline -> moviesState.cachedData ?: emptyList()
                else -> emptyList()
            }

            if (moviesToDisplay.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(moviesToDisplay, key = { it.resolvedId }) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.resolvedId) }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 8. SERIES SCREEN (GET /series -> series.php)
// =========================================================================

@Composable
fun SeriesScreen(
    seriesState: RemoteDataState<List<Series>>,
    selectedCategory: String?,
    unreadNotifications: Int,
    onLoadSeries: (String?) -> Unit,
    onSeriesClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (seriesState is RemoteDataState.Idle) {
            onLoadSeries(selectedCategory)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("series_screen")
    ) {
        HaniXTopBar(
            title = "Series & Seasons",
            subtitle = if (selectedCategory == null) "Turkish & Historical Series" else "Category: $selectedCategory",
            unreadNotifications = unreadNotifications,
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotificationsClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onLoadSeries(null) },
                    label = { Text("All Series") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VipChampagneGold,
                        selectedLabelColor = OledBlack
                    )
                )
                OutlinedButton(
                    onClick = onCategoriesClick,
                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("series_browse_categories_button")
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Categories", color = VipGoldLight)
                }
            }

            if (seriesState is RemoteDataState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VipChampagneGold, strokeWidth = 2.5.dp)
                }
            } else if (seriesState is RemoteDataState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CinemaCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = seriesState.message.ifBlank { "Could not load series. Please try again." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryIvory,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onLoadSeries(selectedCategory) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val seriesToDisplay: List<Series> = when (seriesState) {
                is RemoteDataState.Success -> seriesState.data
                is RemoteDataState.Offline -> seriesState.cachedData ?: emptyList()
                else -> emptyList()
            }

            if (seriesToDisplay.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(seriesToDisplay, key = { it.resolvedId }) { series ->
                        SeriesPosterCard(
                            series = series,
                            onClick = { onSeriesClick(series.resolvedId) }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 9. CATEGORIES SCREEN (GET /home/categories & GET /movies/categories)
// =========================================================================

@Composable
fun CategoriesScreen(
    categoriesState: RemoteDataState<List<CategoryItem>>,
    onLoadCategories: () -> Unit,
    onSelectCategoryForMovies: (String) -> Unit,
    onSelectCategoryForSeries: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (categoriesState is RemoteDataState.Idle) {
            onLoadCategories()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("categories_screen")
    ) {
        HaniXTopBar(
            title = "Content Categories",
            subtitle = "Browse by Category",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (categoriesState is RemoteDataState.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VipChampagneGold, strokeWidth = 2.5.dp)
                    }
                }
            } else if (categoriesState is RemoteDataState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CinemaCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = categoriesState.message.ifBlank { "Could not load categories." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryIvory,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = onLoadCategories, colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (categoriesState is RemoteDataState.Success) {
                items(categoriesState.data, key = { it.id }) { category ->
                    VvipGlassCard(
                        borderColor = VipChampagneGold.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                if (category.itemCount != null) {
                                    Text(
                                        text = "${category.itemCount} titles available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySilver
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onSelectCategoryForMovies(category.id) }) {
                                    Text("Dramas", color = VipChampagneGold)
                                }
                                OutlinedButton(onClick = { onSelectCategoryForSeries(category.id) }) {
                                    Text("Series", color = VipGoldLight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 10. SEARCH SCREEN
// =========================================================================

@Composable
fun SearchScreen(
    searchState: RemoteDataState<SearchPayload>,
    onExecuteSearch: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("search_screen")
    ) {
        HaniXTopBar(
            title = "Search HaniX Catalog",
            subtitle = "Search Dramas & Series",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.trim().length >= 2) {
                            onExecuteSearch(it)
                        }
                    },
                    placeholder = { Text("Search Ibn Sina, Mehmed, Rumi, Seljuks...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = VipChampagneGold)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                onExecuteSearch("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondarySilver)
                            }
                        }
                    },
                    singleLine = true,
                    colors = hanixTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_query_input")
                )

                Button(
                    onClick = { onExecuteSearch(query) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0000),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("search_submit_button")
                ) {
                    Text("Search", fontWeight = FontWeight.Bold)
                }
            }

            if (searchState is RemoteDataState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VipChampagneGold, strokeWidth = 2.5.dp)
                }
            } else if (searchState is RemoteDataState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CinemaCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = searchState.message.ifBlank { "Search failed. Please try again." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryIvory,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { if (query.isNotBlank()) onExecuteSearch(query) }, colors = ButtonDefaults.buttonColors(containerColor = CinemaCrimson)) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (searchState is RemoteDataState.Success) {
                val movies = searchState.data.movies.orEmpty()
                val series = searchState.data.series.orEmpty()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (series.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matching Series (${series.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold
                            )
                        }
                        items(series, key = { "s_${it.resolvedId}" }) { s ->
                            SeriesPosterCard(
                                series = s,
                                onClick = { onSeriesClick(s.resolvedId) }
                            )
                        }
                    }

                    if (movies.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matching Dramas & Episodes (${movies.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold
                            )
                        }
                        items(movies, key = { "m_${it.resolvedId}" }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = {
                                    if (movie.resolvedIsSeriesItem) {
                                        onSeriesClick(movie.seriesName ?: movie.resolvedId)
                                    } else {
                                        onMovieClick(movie.resolvedId)
                                    }
                                }
                            )
                        }
                    }

                    if (movies.isEmpty() && series.isEmpty()) {
                        item {
                            Text(
                                text = "No matching movies or series returned for \"$query\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondarySilver
                            )
                        }
                    }
                }
            } else if (searchState is RemoteDataState.Idle) {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Live HaniX Urdu Search",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Type at least 2 characters (e.g. \"Sina\", \"Mehmed\", \"Rumi\", \"Seljuks\") to search real series and episodes from hanixurdu.com/search.php.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }
    }
}
