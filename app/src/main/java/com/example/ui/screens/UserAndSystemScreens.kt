package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.BackendEndpointCatalog
import com.example.data.model.RemoteDataState
import com.example.data.model.User
import com.example.data.model.VipPlan
import com.example.data.model.VipStatusPayload
import com.example.ui.components.ContractStateBanner
import com.example.ui.components.HaniXTopBar
import com.example.ui.components.SecurityAssuranceStrip
import com.example.ui.components.VvipGlassCard
import com.example.ui.theme.AmberPending
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
// 16. VIP MEMBERSHIP SCREEN (GET /vip/status, GET /vip/plans, POST /vip/verify)
// =========================================================================

@Composable
fun VipMembershipScreen(
    vipStatusState: RemoteDataState<VipStatusPayload>,
    vipPlansState: RemoteDataState<List<VipPlan>>,
    currentUser: User?,
    onLoadVipData: () -> Unit,
    onVerifyVip: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var selectedPlanId by rememberSaveable { mutableStateOf("vvip_monthly") }
    var voucherOrReference by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (vipStatusState is RemoteDataState.Idle) {
            onLoadVipData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("vip_membership_screen")
    ) {
        HaniXTopBar(
            title = "VVIP Membership Lounge",
            subtitle = "GET /vip/status • POST /vip/verify (unlock_vip.php)",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                VvipGlassCard(borderColor = VipChampagneGold) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = VipChampagneGold,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Server-Verified VVIP Entitlement",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VipGoldLight
                                )
                                Text(
                                    text = if (currentUser?.vipStatus == true) {
                                        "Active until ${currentUser.vipExpiry ?: "Verified"}"
                                    } else {
                                        "Not verified yet — checked via check_premium.php / GET /vip/status"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySilver
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "VIP authorization is strictly performed by the Hostinger PHP backend. Client-side overrides are never trusted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }

            item {
                ContractStateBanner(
                    state = vipStatusState,
                    onRetry = onLoadVipData,
                    onOpenChecklist = onOpenChecklist
                )
            }

            if (vipPlansState is RemoteDataState.Success && vipPlansState.data.isNotEmpty()) {
                items(vipPlansState.data, key = { it.id }) { plan ->
                    VvipGlassCard(
                        onClick = { selectedPlanId = plan.id },
                        borderColor = if (selectedPlanId == plan.id) VipChampagneGold else GlassBorderSubtle
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
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "${plan.durationDays} days • ${plan.features?.joinToString(", ") ?: "All VVIP Movies & Series"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySilver
                                )
                            }
                            Text(
                                text = plan.priceFormatted,
                                style = MaterialTheme.typography.titleLarge,
                                color = VipChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Server-side VIP Verification Form (POST /vip/verify -> unlock_vip.php)
            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Verify VIP Code / Payment Reference",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Submits to `POST /vip/verify` (bridging `unlock_vip.php` & `add_premium.php`) for backend entitlement validation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = voucherOrReference,
                            onValueChange = {
                                voucherOrReference = it
                                inputError = null
                            },
                            label = { Text("VIP Voucher or Transaction Reference") },
                            singleLine = true,
                            colors = hanixTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vip_reference_input")
                        )

                        if (inputError != null) {
                            Text(
                                text = inputError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = CinemaCrimson,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (voucherOrReference.isBlank()) {
                                    inputError = "Enter a VIP code or transaction reference to verify with the backend."
                                } else {
                                    onVerifyVip(selectedPlanId, voucherOrReference)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("vip_verify_submit_button")
                        ) {
                            Text("Verify with Hostinger Backend", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 17. USER PROFILE SCREEN (GET /user/profile, PUT /user/profile)
// =========================================================================

@Composable
fun UserProfileScreen(
    currentUser: User?,
    isAuthenticated: Boolean,
    profileUpdateState: RemoteDataState<User>,
    unreadNotifications: Int,
    onUpdateProfile: (String, String?) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToVip: () -> Unit,
    onNavigateToWatchHistory: () -> Unit,
    onNavigateToContinueWatching: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var displayName by rememberSaveable(currentUser?.name) {
        mutableStateOf(currentUser?.name ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("user_profile_screen")
    ) {
        HaniXTopBar(
            title = "User Profile & Account",
            subtitle = "GET / PUT /user/profile",
            unreadNotifications = unreadNotifications,
            onVipClick = onNavigateToVip,
            onNotificationsClick = onNavigateToNotifications
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = VipGoldDark.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Avatar",
                                    tint = VipChampagneGold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.name ?: "Unauthenticated Visitor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = currentUser?.email ?: "Sign in to sync your hanixurdu.com account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySilver
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentUser?.vipStatus == true) "VVIP Member (Expires: ${currentUser.vipExpiry ?: "Active"})" else "Standard Tier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VipChampagneGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isAuthenticated) {
                            Button(
                                onClick = onNavigateToLogin,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VipChampagneGold,
                                    contentColor = OledBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_signin_button")
                            ) {
                                Text("Sign In / Register HaniX Account", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name (PUT /user/profile)") },
                                singleLine = true,
                                colors = hanixTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onUpdateProfile(displayName, currentUser?.profileImage) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VipChampagneGold,
                                        contentColor = OledBlack
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_save_button")
                                ) {
                                    Text("Save Profile", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onLogout,
                                    border = BorderStroke(1.dp, CinemaCrimson),
                                    modifier = Modifier.testTag("profile_logout_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = CinemaCrimson)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Logout", color = CinemaCrimson)
                                }
                            }
                        }
                    }
                }
            }

            if (profileUpdateState !is RemoteDataState.Idle) {
                item {
                    ContractStateBanner(
                        state = profileUpdateState,
                        onRetry = { onUpdateProfile(displayName, currentUser?.profileImage) },
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            // Navigation Hub to all User & System Screens
            item {
                ProfileMenuRow(
                    icon = Icons.Default.WorkspacePremium,
                    title = "VVIP Membership & Plans",
                    subtitle = "GET /vip/status • GET /vip/plans • POST /vip/verify",
                    testTag = "profile_nav_vip",
                    onClick = onNavigateToVip
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Continue Watching",
                    subtitle = "Resume in-progress movies and episodes",
                    testTag = "profile_nav_continue_watching",
                    onClick = onNavigateToContinueWatching
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.History,
                    title = "Watch History",
                    subtitle = "GET / DELETE /user/history & local Room sync",
                    testTag = "profile_nav_watch_history",
                    onClick = onNavigateToWatchHistory
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Download,
                    title = "My Encrypted Downloads",
                    subtitle = "POST /downloads/authorize • Keystore AES-256-GCM",
                    testTag = "profile_nav_downloads",
                    onClick = onNavigateToDownloads
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "System & backend integration alerts",
                    testTag = "profile_nav_notifications",
                    onClick = onNavigateToNotifications
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Settings,
                    title = "App Settings & API Base URL",
                    subtitle = "GET /app/settings • GET /app/version • HTTPS config",
                    testTag = "profile_nav_settings",
                    onClick = onNavigateToSettings
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Info,
                    title = "About HaniX Urdu & Backend Checklist",
                    subtitle = "31 Proposed PHP REST API v1 endpoints & shahkot.php compatibility",
                    testTag = "profile_nav_about",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    VvipGlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = VipChampagneGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryIvory
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySilver
                )
            }
        }
    }
}

// =========================================================================
// 18. WATCH HISTORY SCREEN (GET /user/history, DELETE /user/history)
// =========================================================================

@Composable
fun WatchHistoryScreen(
    historyList: List<WatchHistoryEntity>,
    onSyncHistoryFromServer: () -> Unit,
    onClearAllHistory: () -> Unit,
    onDeleteEntry: (String) -> Unit,
    onResumeItem: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    LaunchedEffect(Unit) {
        onSyncHistoryFromServer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("watch_history_screen")
    ) {
        HaniXTopBar(
            title = "Watch History",
            subtitle = "GET / DELETE /user/history • Room Cache",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (historyList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onClearAllHistory,
                            border = BorderStroke(1.dp, CinemaCrimson),
                            modifier = Modifier.testTag("clear_watch_history_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = CinemaCrimson, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All History", color = CinemaCrimson)
                        }
                    }
                }

                items(historyList, key = { it.historyKey }) { item ->
                    VvipGlassCard(
                        onClick = { onResumeItem(item.contentType, item.contentId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "${item.contentType.uppercase()} #${item.contentId} • ${(item.progressFraction * 100).toInt()}% watched",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VipChampagneGold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { item.progressFraction },
                                    color = VipChampagneGold,
                                    trackColor = GlassCardElevated,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(onClick = { onDeleteEntry(item.historyKey) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = TextSecondarySilver)
                            }
                        }
                    }
                }
            } else {
                item {
                    VvipGlassCard {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Your Watch History is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Movies and episodes you stream are automatically recorded in Room and synced with GET/POST /playback/progress and GET /user/history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySilver
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 19. CONTINUE WATCHING SCREEN
// =========================================================================

@Composable
fun ContinueWatchingScreen(
    continueWatchingList: List<WatchHistoryEntity>,
    onResumeItem: (String, String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("continue_watching_screen")
    ) {
        HaniXTopBar(
            title = "Continue Watching",
            subtitle = "POST /playback/progress • Resume Queue",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (continueWatchingList.isNotEmpty()) {
                items(continueWatchingList, key = { it.historyKey }) { item ->
                    VvipGlassCard(
                        onClick = { onResumeItem(item.contentType, item.contentId) },
                        borderColor = VipChampagneGold.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemoveItem(item.historyKey) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondarySilver)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { item.progressFraction },
                                color = VipChampagneGold,
                                trackColor = GlassCardElevated,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                item {
                    VvipGlassCard {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "No In-Progress Streams",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "When you pause a movie or episode mid-stream, your exact timestamp is saved here for instant resume.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySilver
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 20. MY DOWNLOADS SCREEN (POST /downloads/authorize, GET /downloads, DELETE /downloads/{id})
// =========================================================================

@Composable
fun MyDownloadsScreen(
    encryptedDownloads: List<EncryptedDownloadEntity>,
    downloadActionState: RemoteDataState<EncryptedDownloadEntity>,
    unreadNotifications: Int,
    onRequestTestDownloadAuth: () -> Unit,
    onDeleteDownload: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("my_downloads_screen")
    ) {
        HaniXTopBar(
            title = "Encrypted Downloads",
            subtitle = "App-Private Keystore AES-256-GCM Storage",
            unreadNotifications = unreadNotifications,
            onNotificationsClick = onNotificationsClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.45f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = EmeraldVerified
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Protected Offline Entitlement Vault",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloads are authorized via POST /downloads/authorize, encrypted with hardware-backed AES-256-GCM in filesDir/encrypted_downloads, and checked for VIP license expiry. Unrestricted Bunny video URLs are never exposed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRequestTestDownloadAuth,
                            border = BorderStroke(1.dp, VipChampagneGold),
                            modifier = Modifier.testTag("test_download_authorize_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test POST /downloads/authorize", color = VipGoldLight)
                        }
                    }
                }
            }

            if (downloadActionState !is RemoteDataState.Idle) {
                item {
                    ContractStateBanner(
                        state = downloadActionState,
                        onRetry = onRequestTestDownloadAuth,
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            if (encryptedDownloads.isNotEmpty()) {
                items(encryptedDownloads, key = { it.downloadId }) { dl ->
                    val expired = dl.isLicenseExpired()
                    VvipGlassCard(
                        borderColor = if (expired) CinemaCrimson else EmeraldVerified.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = if (expired) "Offline License Expired — Re-authorize online" else "AES-256-GCM Encrypted • License Valid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (expired) CinemaCrimson else EmeraldVerified
                                )
                            }
                            IconButton(onClick = { onDeleteDownload(dl.downloadId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete download", tint = CinemaCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 21. NOTIFICATIONS SCREEN
// =========================================================================

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("notifications_screen")
    ) {
        HaniXTopBar(
            title = "Notifications",
            subtitle = "System & Backend Status Alerts",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.any { !it.isRead }) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("mark_all_notifications_read_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark All Read", color = VipChampagneGold)
                        }
                    }
                }
            }

            items(notifications, key = { it.id }) { item ->
                VvipGlassCard(
                    onClick = { onMarkRead(item.id) },
                    borderColor = if (!item.isRead) VipChampagneGold else GlassBorderSubtle
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = VipChampagneGold
                            )
                            if (!item.isRead) {
                                Surface(
                                    color = CinemaCrimson,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "NEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimaryIvory,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 22. APP SETTINGS SCREEN (GET /app/settings, GET /app/version & HTTPS Base URL)
// =========================================================================

@Composable
fun AppSettingsScreen(
    apiBaseUrl: String,
    appSettingsState: RemoteDataState<AppSettingsPayload>,
    appVersionState: RemoteDataState<AppVersionPayload>,
    feedbackMessage: String?,
    onLoadRemoteSettings: () -> Unit,
    onUpdateApiBaseUrl: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var editedUrl by rememberSaveable(apiBaseUrl) { mutableStateOf(apiBaseUrl) }

    LaunchedEffect(Unit) {
        if (appSettingsState is RemoteDataState.Idle) {
            onLoadRemoteSettings()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("app_settings_screen")
    ) {
        HaniXTopBar(
            title = "App & API Settings",
            subtitle = "GET /app/settings • GET /app/version",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Hostinger PHP REST API Base URL",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Default contract URL: ${BuildConfig.HANIX_API_BASE_URL}. Only HTTPS URLs (https://...) are accepted; cleartext HTTP is blocked at both the application and NetworkSecurityConfig levels.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editedUrl,
                            onValueChange = { editedUrl = it },
                            label = { Text("HTTPS REST API v1 Base URL") },
                            singleLine = true,
                            colors = hanixTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_base_url_input")
                        )

                        if (!feedbackMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feedbackMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (feedbackMessage.startsWith("Security Policy")) CinemaCrimson else EmeraldVerified
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onUpdateApiBaseUrl(editedUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VipChampagneGold,
                                    contentColor = OledBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_save_url_button")
                            ) {
                                Text("Apply HTTPS URL", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    editedUrl = BuildConfig.HANIX_API_BASE_URL
                                    onUpdateApiBaseUrl(BuildConfig.HANIX_API_BASE_URL)
                                },
                                border = BorderStroke(1.dp, GlassBorderSubtle)
                            ) {
                                Text("Reset Default", color = TextSecondarySilver)
                            }
                        }
                    }
                }
            }

            item {
                ContractStateBanner(
                    state = appSettingsState,
                    onRetry = onLoadRemoteSettings,
                    onOpenChecklist = onOpenChecklist
                )
            }

            item {
                ContractStateBanner(
                    state = appVersionState,
                    onRetry = onLoadRemoteSettings,
                    onOpenChecklist = onOpenChecklist
                )
            }

            item {
                SecurityAssuranceStrip()
            }
        }
    }
}

// =========================================================================
// 23. ABOUT HANIX URDU & BACKEND INTEGRATION CHECKLIST SCREEN
// =========================================================================

@Composable
fun AboutHaniXScreen(
    apiBaseUrl: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("about_hanix_screen")
    ) {
        HaniXTopBar(
            title = "About HaniX Urdu & API Checklist",
            subtitle = "Hostinger PHP + MySQL & Bunny Stream Architecture",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VvipGlassCard(borderColor = VipChampagneGold) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "HaniX Urdu Native Android Client v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Designed to share your existing Hostinger MySQL database (Users, Movies, Series, Seasons, Episodes, Posters, and VIP memberships) with your existing PHP website without modifying or replacing `shahkot.php` or any existing web pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Configured Base URL: $apiBaseUrl",
                            style = MaterialTheme.typography.labelSmall,
                            color = VipChampagneGold
                        )
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = AmberPending.copy(alpha = 0.7f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Honest Security &DRM Disclosure (Section 10)",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberPending
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• HTTPS TLS 1.2/1.3 enforced via network_security_config.xml.\n" +
                                "• Zero MySQL database credentials, admin secrets, or Bunny Stream API keys are packaged in the APK.\n" +
                                "• Short-lived Bunny Stream playback tokens and VIP checks are enforced server-side.\n" +
                                "• Note: No software can claim 100% prevention against external camera recording or hardware-level capture.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Proposed PHP REST API v1 Endpoints (${BackendEndpointCatalog.endpoints.size} Routes)",
                    style = MaterialTheme.typography.titleLarge,
                    color = VipChampagneGold
                )
            }

            items(BackendEndpointCatalog.endpoints, key = { "${it.method}_${it.path}" }) { spec ->
                VvipGlassCard(borderColor = GlassBorderSubtle) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = VipGoldDark.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${spec.method} ${spec.path}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VipGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = spec.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Maps from existing PHP: ${spec.legacyPhpReference}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = spec.securityRequirement,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }
        }
    }
}
