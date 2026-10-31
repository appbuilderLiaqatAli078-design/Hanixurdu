package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.CategoryItem
import com.example.data.model.HomePayload
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
// 6. HOME SCREEN (GET /home, /home/banners, /home/featured, /home/latest, /home/categories)
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
    onMovieSelected: (String) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onResumePlayback: (String, String) -> Unit
) {
    LaunchedEffect(Unit) {
        if (homeState is RemoteDataState.Idle) {
            onRefreshHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("home_screen")
    ) {
        HaniXTopBar(
            title = "HaniX Urdu",
            subtitle = if (currentUser != null) "Welcome, ${currentUser.name ?: "Member"}" else "VVIP Entertainment Lounge",
            unreadNotifications = unreadNotifications,
            onSearchClick = onNavigateToSearch,
            onVipClick = onNavigateToVip,
            onNotificationsClick = onNavigateToNotifications
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Cinematic Hero Spotlight Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    VipGoldDark.copy(alpha = 0.45f),
                                    CinemaCrimson.copy(alpha = 0.35f),
                                    CharcoalSurface
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = VipChampagneGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "HOSTINGER + BUNNY STREAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OledBlack,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (currentUser?.vipStatus == true) {
                                Surface(
                                    color = CinemaCrimson,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "VVIP ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "HaniX Urdu Native Streaming",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected to $apiBaseUrl. Shares accounts, Movies, Series, Seasons, Episodes, and VIP status with your existing Hostinger PHP + MySQL website.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (currentUser == null) {
                                Button(
                                    onClick = onNavigateToLogin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VipChampagneGold,
                                        contentColor = OledBlack
                                    ),
                                    modifier = Modifier.testTag("home_hero_login_button")
                                ) {
                                    Text("Sign In / Register", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onNavigateToVip,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VipChampagneGold,
                                        contentColor = OledBlack
                                    ),
                                    modifier = Modifier.testTag("home_hero_vip_button")
                                ) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VVIP Lounge", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = onNavigateToAbout,
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier.testTag("home_hero_checklist_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("API Checklist", color = VipGoldLight)
                            }
                        }
                    }
                }
            }

            // Quick Feature Access Chips (Categories, Continue Watching, Watch History, Settings)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            icon = Icons.Default.WorkspacePremium,
                            label = "VIP Plans",
                            testTag = "home_quick_vip",
                            onClick = onNavigateToVip
                        )
                    }
                    item {
                        QuickNavPill(
                            icon = Icons.Default.Settings,
                            label = "API Settings",
                            testTag = "home_quick_settings",
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }

            // Continue Watching Rail (if local Room has active progress items)
            if (continueWatchingList.isNotEmpty()) {
                item {
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
                                        color = VipChampagneGold,
                                        trackColor = GlassCardElevated,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live API Response / Pending Backend Status Banner
            item {
                ContractStateBanner(
                    state = homeState,
                    onRetry = onRefreshHome,
                    onOpenChecklist = onNavigateToAbout
                )
            }

            // When HomePayload succeeds from the PHP backend, render live Banners, Featured & Latest rails
            if (homeState is RemoteDataState.Success) {
                val payload = homeState.data

                if (!payload.featuredMovies.isNullOrEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "Featured Movies",
                            actionLabel = "All Movies",
                            onActionClick = onNavigateToMovies
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(payload.featuredMovies, key = { it.id }) { movie ->
                                MoviePosterCard(
                                    movie = movie,
                                    onClick = { onMovieSelected(movie.id) },
                                    modifier = Modifier.width(156.dp)
                                )
                            }
                        }
                    }
                }

                if (!payload.featuredSeries.isNullOrEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "Featured Series",
                            actionLabel = "All Series",
                            onActionClick = onNavigateToSeries
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(payload.featuredSeries, key = { it.id }) { series ->
                                SeriesPosterCard(
                                    series = series,
                                    onClick = { onSeriesSelected(series.id) },
                                    modifier = Modifier.width(156.dp)
                                )
                            }
                        }
                    }
                }

                if (!payload.latestMovies.isNullOrEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "Latest Uploads",
                            actionLabel = "Browse",
                            onActionClick = onNavigateToMovies
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(payload.latestMovies, key = { it.id }) { movie ->
                                MoviePosterCard(
                                    movie = movie,
                                    onClick = { onMovieSelected(movie.id) },
                                    modifier = Modifier.width(156.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Contract Inspection Cards so user can navigate to Movie/Series/Season/Episode/Player screens
            item {
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Catalog & Playback Contract Explorer",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Zero fake production data is bundled. Inspect how Movie Details, Series Seasons, Episodes, and Bunny Stream HLS Authorization interact with your Hostinger PHP endpoints:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onMovieSelected("1") },
                                border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inspect_movie_contract_button")
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GET /movies/1", color = TextPrimaryIvory, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onSeriesSelected("1") },
                                border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inspect_series_contract_button")
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GET /series/1", color = TextPrimaryIvory, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                SecurityAssuranceStrip()
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
            title = "Urdu & Dubbed Movies",
            subtitle = if (selectedCategory == null) "GET /movies" else "GET /movies?category=$selectedCategory",
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
                    label = { Text("All Movies") },
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
                    Text("Filter by Category", color = VipGoldLight)
                }
            }

            ContractStateBanner(
                state = moviesState,
                onRetry = { onLoadMovies(selectedCategory) },
                onOpenChecklist = onOpenChecklist
            )

            val moviesToDisplay: List<Movie> = when (moviesState) {
                is RemoteDataState.Success -> moviesState.data
                is RemoteDataState.Offline -> moviesState.cachedData ?: emptyList()
                else -> emptyList()
            }

            if (moviesToDisplay.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(moviesToDisplay, key = { it.id }) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { onMovieClick(movie.id) }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 8. SERIES SCREEN (GET /series)
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
            subtitle = if (selectedCategory == null) "GET /series • series.php" else "GET /series?category=$selectedCategory",
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

            ContractStateBanner(
                state = seriesState,
                onRetry = { onLoadSeries(selectedCategory) },
                onOpenChecklist = onOpenChecklist
            )

            val seriesToDisplay: List<Series> = when (seriesState) {
                is RemoteDataState.Success -> seriesState.data
                is RemoteDataState.Offline -> seriesState.cachedData ?: emptyList()
                else -> emptyList()
            }

            if (seriesToDisplay.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(seriesToDisplay, key = { it.id }) { series ->
                        SeriesPosterCard(
                            series = series,
                            onClick = { onSeriesClick(series.id) }
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
            subtitle = "GET /home/categories • GET /movies/categories",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ContractStateBanner(
                    state = categoriesState,
                    onRetry = onLoadCategories,
                    onOpenChecklist = onOpenChecklist
                )
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
                                        text = "${category.itemCount} titles in MySQL catalog",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySilver
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onSelectCategoryForMovies(category.name) }) {
                                    Text("Movies", color = VipChampagneGold)
                                }
                                OutlinedButton(onClick = { onSelectCategoryForSeries(category.name) }) {
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
// 10. SEARCH SCREEN (GET /search?q={query} -> search.php)
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
            subtitle = "GET /search?q={query} • Bridges search.php",
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
                    onValueChange = { query = it },
                    placeholder = { Text("Search Urdu movies, dubbed series, genres...") },
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
                        containerColor = VipChampagneGold,
                        contentColor = OledBlack
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("search_submit_button")
                ) {
                    Text("Search", fontWeight = FontWeight.Bold)
                }
            }

            ContractStateBanner(
                state = searchState,
                onRetry = { if (query.isNotBlank()) onExecuteSearch(query) },
                onOpenChecklist = onOpenChecklist
            )

            if (searchState is RemoteDataState.Success) {
                val movies = searchState.data.movies.orEmpty()
                val series = searchState.data.series.orEmpty()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (movies.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matching Movies (${movies.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold
                            )
                        }
                        items(movies, key = { it.id }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) }
                            )
                        }
                    }

                    if (series.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matching Series (${series.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = VipChampagneGold
                            )
                        }
                        items(series, key = { it.id }) { s ->
                            SeriesPosterCard(
                                series = s,
                                onClick = { onSeriesClick(s.id) }
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
                            text = "Hostinger MySQL Search Contract",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Queries `GET https://hanixurdu.com/api/v1/search?q={query}` using prepared PDO statements across your existing movies and series tables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }
    }
}
