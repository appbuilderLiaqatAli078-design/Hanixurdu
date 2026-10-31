package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Movie
import com.example.data.model.RemoteDataState
import com.example.data.model.Series
import com.example.data.security.HaniXSecurityShield
import com.example.navigation.BottomNavTab
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextPrimaryIvory
import com.example.ui.theme.TextSecondarySilver
import com.example.ui.theme.VipChampagneGold
import com.example.ui.theme.VipGoldDark
import com.example.ui.theme.VipGoldLight

/**
 * Top Bar for primary HaniX Urdu screens with VIP shortcut, Search, and Notification badge.
 * Uses only FREE and VIP membership badges (Section 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaniXTopBar(
    title: String,
    subtitle: String? = null,
    unreadNotifications: Int = 0,
    onBackClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onVipClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OledBlack.copy(alpha = 0.94f),
            titleContentColor = TextPrimaryIvory
        ),
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = VipChampagneGold
                    )
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(VipChampagneGold, CinemaCrimson)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HX",
                        style = MaterialTheme.typography.labelLarge,
                        color = OledBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val cleanSubtitle = HaniXSecurityShield.sanitizeUiLabel(subtitle)
                    if (!cleanSubtitle.isNullOrBlank()) {
                        Text(
                            text = cleanSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = VipChampagneGold,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        actions = {
            if (onVipClick != null) {
                Surface(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onVipClick)
                        .testTag("top_bar_vip_badge"),
                    color = VipGoldDark.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, VipChampagneGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Membership",
                            tint = VipChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = VipGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (onSearchClick != null) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_bar_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search movies and series",
                        tint = TextPrimaryIvory
                    )
                }
            }
            if (onNotificationsClick != null) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("top_bar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(
                                    containerColor = CinemaCrimson,
                                    contentColor = TextPrimaryIvory
                                ) {
                                    Text(unreadNotifications.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimaryIvory
                        )
                    }
                }
            }
        }
    )
}

/**
 * Bottom Navigation Bar with OLED Obsidian finish and Gold active indicators.
 */
@Composable
fun HaniXBottomNavBar(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = CharcoalSurface,
        contentColor = TextPrimaryIvory,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(BorderStroke(0.5.dp, GlassBorderSubtle))
            .testTag("hanix_bottom_nav_bar")
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val icon = when (tab) {
                BottomNavTab.HOME -> Icons.Default.Home
                BottomNavTab.DRAMAS -> Icons.Default.VideoLibrary
                BottomNavTab.SERIES -> Icons.Default.LiveTv
                BottomNavTab.VIP -> Icons.Default.WorkspacePremium
                BottomNavTab.PROFILE -> Icons.Default.Person
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.testTag(tab.testTag),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OledBlack,
                    selectedTextColor = VipChampagneGold,
                    indicatorColor = VipChampagneGold,
                    unselectedIconColor = TextSecondarySilver,
                    unselectedTextColor = TextMutedSlate
                )
            )
        }
    }
}

/**
 * Glassmorphic container card with subtle metallic border and deep charcoal surface.
 */
@Composable
fun VvipGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorderSubtle,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

/**
 * Displays Loading, Empty, AuthRequired, PendingBackend, Offline, and Error states (Section 8).
 */
@Composable
fun <T> ContractStateBanner(
    state: RemoteDataState<T>,
    onRetry: () -> Unit,
    onOpenChecklist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        is RemoteDataState.Loading -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_loading"),
                borderColor = VipChampagneGold.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = VipChampagneGold,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Loading HaniX Urdu Streams...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                    }
                }
            }
        }

        is RemoteDataState.AuthRequired -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_auth_required"),
                borderColor = VipChampagneGold
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Authentication Required",
                            tint = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign-In Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipGoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        modifier = Modifier.testTag("auth_required_retry_button")
                    ) {
                        Text("Sign In / Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        is RemoteDataState.PendingBackend -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_pending_backend"),
                borderColor = AmberPending.copy(alpha = 0.7f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberPending.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refreshing content",
                                tint = AmberPending,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stream Refreshing",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberPending,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Synchronizing latest Urdu episodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = VipGoldLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This channel is refreshing its stream servers. Tap retry to reconnect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("retry_endpoint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry request",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Connection", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        is RemoteDataState.Offline -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_offline"),
                borderColor = CinemaCrimson.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = CinemaCrimson
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connection Offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        border = BorderStroke(1.dp, VipChampagneGold),
                        modifier = Modifier.testTag("offline_retry_button")
                    ) {
                        Text("Retry Connection", color = VipChampagneGold)
                    }
                }
            }
        }

        is RemoteDataState.Error -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_error"),
                borderColor = ErrorCoral.copy(alpha = 0.7f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "API Error",
                            tint = ErrorCoral
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notice",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorCoral
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = HaniXSecurityShield.sanitizeUiLabel(state.message) ?: state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        modifier = Modifier.testTag("error_retry_button")
                    ) {
                        Text("Retry Request")
                    }
                }
            }
        }

        is RemoteDataState.Empty -> {
            VvipGlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("contract_state_empty")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onRetry) {
                        Text("Refresh", color = VipChampagneGold)
                    }
                }
            }
        }

        else -> Unit
    }
}

/**
 * Poster grid card for a Movie from the Hostinger MySQL database.
 * Displays real posters (poster or poster_path) and explicit FREE or VIP labels (Section 4 & 8).
 */
@Composable
fun MoviePosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rankNumber: Int? = null,
    isLandscape: Boolean = false
) {
    val isVip = movie.resolvedIsVip
    val posterUrl = movie.resolvedPoster

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("movie_card_${movie.resolvedId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(
            width = if (isVip) 1.5.dp else 1.dp,
            color = if (isVip) Color(0xFFFFD700).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isLandscape) 16f / 9f else 0.68f)
                .background(
                    Brush.verticalGradient(
                        listOf(GlassCardElevated, OledBlack)
                    )
                )
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = movie.title,
                        tint = VipChampagneGold.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // Bottom gradient overlay for title & rank number (matching .poster-overlay in style.css)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // VIP Crown / FREE Badge on top-left or top-right
            if (isVip) {
                Surface(
                    color = Color(0xFFFFD700),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🔒 Unlock VIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Surface(
                    color = CharcoalSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "FREE",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldVerified,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Title at bottom-left
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = if (rankNumber != null) 42.dp else 10.dp, bottom = 10.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (movie.resolvedIsSeriesItem) {
                    Text(
                        text = "Series • Urdu",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF0000),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Large Rank Number at bottom-right (matching .poster-number in index.php)
            if (rankNumber != null) {
                Text(
                    text = rankNumber.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Poster grid card for a Series from the Hostinger MySQL database.
 */
@Composable
fun SeriesPosterCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rankNumber: Int? = null
) {
    val posterUrl = series.resolvedPoster
    val titleText = series.resolvedTitle
    val isVip = series.resolvedIsVip

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("series_card_${series.resolvedId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(
            width = if (isVip) 1.5.dp else 1.dp,
            color = if (isVip) Color(0xFFFFD700).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .background(
                    Brush.verticalGradient(
                        listOf(GlassCardElevated, OledBlack)
                    )
                )
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = titleText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = titleText,
                        tint = VipChampagneGold.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            if (isVip) {
                Surface(
                    color = Color(0xFFFFD700),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "👑 VIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = if (rankNumber != null) 40.dp else 10.dp, bottom = 10.dp)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Urdu Series",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD700),
                    maxLines = 1
                )
            }

            if (rankNumber != null) {
                Text(
                    text = rankNumber.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Security pill banner reminding users of hardware Keystore, HTTPS-only & Backend VIP enforcement.
 */
@Composable
fun SecurityAssuranceStrip(modifier: Modifier = Modifier) {
    val pulseColor by animateColorAsState(targetValue = EmeraldVerified, label = "security_pulse")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Verified",
                tint = pulseColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "HTTPS TLS 1.3 • Keystore AES-256 • FREE & VIP Backend Verified",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondarySilver
            )
        }
    }
}

/**
 * 3D Cinema Button Color Variants
 */
enum class Cinema3DButtonVariant {
    Crimson,
    Gold,
    Green,
    White,
    Dark
}

/**
 * Polished 3D Cinema Button with realistic top luminous highlight,
 * vertical depth gradient, elevated drop shadow, and tactile feel.
 * Replaces cartoon/flat buttons across all drama & player screens.
 */
@Composable
fun Cinema3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: Cinema3DButtonVariant = Cinema3DButtonVariant.Crimson,
    shape: Shape = RoundedCornerShape(12.dp),
    icon: (@Composable () -> Unit)? = null,
    testTag: String = "cinema_3d_button",
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
    enabled: Boolean = true
) {
    val (gradientColors, highlightColor, textColor) = when (variant) {
        Cinema3DButtonVariant.Crimson -> Triple(
            listOf(Color(0xFFFF3344), Color(0xFFDC143C), Color(0xFF8B0000)),
            Color(0xFFFF858E),
            Color.White
        )
        Cinema3DButtonVariant.Gold -> Triple(
            listOf(Color(0xFFFFE066), Color(0xFFD4AF37), Color(0xFF8C6D1F)),
            Color(0xFFFFF2B2),
            Color(0xFF140D00)
        )
        Cinema3DButtonVariant.Green -> Triple(
            listOf(Color(0xFF38E678), Color(0xFF25D366), Color(0xFF0F7A38)),
            Color(0xFF9DF5BA),
            Color.White
        )
        Cinema3DButtonVariant.White -> Triple(
            listOf(Color(0xFFFFFFFF), Color(0xFFE8E8E8), Color(0xFFC4C4C4)),
            Color(0xFFFFFFFF),
            Color(0xFF111111)
        )
        Cinema3DButtonVariant.Dark -> Triple(
            listOf(Color(0xFF323642), Color(0xFF22252F), Color(0xFF13151C)),
            Color(0xFF555B6E),
            Color.White
        )
    }

    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(shape)
            .background(Brush.verticalGradient(gradientColors))
            .border(width = 1.dp, color = highlightColor.copy(alpha = 0.5f), shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Glossy Highlight Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Floating Circular Red 3D Chat Button matching hanixurdu.com (bottom-left).
 */
@Composable
fun FloatingChatButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0xFFDC143C))
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFF3B4B),
                        Color(0xFFDC143C),
                        Color(0xFF8B0000)
                    )
                )
            )
            .border(1.5.dp, Color(0xFFFF8A94), CircleShape)
            .clickable(onClick = onClick)
            .testTag("floating_chat_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubble,
            contentDescription = "HaniX Chat Support",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Floating 3D Glossy Orange/Gold "Mind Games" Pill matching hanixurdu.com (bottom-right).
 */
@Composable
fun FloatingMindGamesPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50), spotColor = Color(0xFFFF6600))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFF8533),
                        Color(0xFFFF5500),
                        Color(0xFFCC3700)
                    )
                )
            )
            .border(1.dp, Color(0xFFFFB380), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("floating_mind_games_pill"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🧠",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Mind Games",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * Floating Green "Follow on WhatsApp" Pill matching hanixurdu.com
 */
@Composable
fun WhatsAppFollowPill(
    modifier: Modifier = Modifier,
    onFollowClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50), spotColor = Color(0xFF25D366))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF263238),
                        Color(0xFF1A2226),
                        Color(0xFF101618)
                    )
                )
            )
            .border(1.dp, Color(0xFF25D366).copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("whatsapp_follow_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onFollowClick)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💬",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Follow on WhatsApp",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Latest updates & episodes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            if (onDismissClick != null) {
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
