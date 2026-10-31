package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.EncryptedDownloadEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.AppSettingsPayload
import com.example.data.model.AppVersionPayload
import com.example.data.model.AuditionUploadPayload
import com.example.data.model.BackendEndpointCatalog
import com.example.data.model.MembershipTier
import com.example.data.model.MindGamePayload
import com.example.data.model.MindGameQuestionBank
import com.example.data.model.PaymentInfoDto
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
// 16. VIP MEMBERSHIP SCREEN — STRICTLY TWO PLANS: FREE and VIP
//     Includes JazzCash (03294806457 — Muhammad Hasnain) & WhatsApp Activation
// =========================================================================

@Composable
fun VipMembershipScreen(
    vipStatusState: RemoteDataState<VipStatusPayload>,
    vipPlansState: RemoteDataState<List<VipPlan>>,
    currentUser: User?,
    onLoadVipData: () -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val context = LocalContext.current
    val effectiveTier = currentUser?.effectiveMembershipTier() ?: MembershipTier.FREE
    var copiedFeedback by rememberSaveable { mutableStateOf(false) }

    val paymentInfo: PaymentInfoDto = (vipStatusState as? RemoteDataState.Success)?.data?.paymentInfo
        ?: PaymentInfoDto()

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
            title = "Membership — FREE & VIP",
            subtitle = "Upgrade to VIP for unlimited drama streaming",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Server-Verified Membership Status Card
            item {
                VvipGlassCard(
                    borderColor = if (effectiveTier == MembershipTier.VIP) VipChampagneGold else GlassBorderSubtle,
                    modifier = Modifier.testTag("current_membership_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (effectiveTier == MembershipTier.VIP) VipChampagneGold else EmeraldVerified,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Plan: ${effectiveTier.name}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (effectiveTier == MembershipTier.VIP) VipGoldLight else TextPrimaryIvory,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (effectiveTier == MembershipTier.VIP) {
                                        "VIP Active • Expires: ${currentUser?.resolvedExpiryDate ?: "Verified by Backend"}"
                                    } else {
                                        "FREE Plan (Default) — Browse & watch all FREE movies and episodes"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySilver
                                )
                            }
                            OutlinedButton(
                                onClick = onLoadVipData,
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier.testTag("refresh_vip_status_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh status", tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify", color = VipGoldLight)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Only FREE and VIP plans exist. Every new account starts as FREE. Upgrade to VIP via JazzCash to unlock all VIP episodes, HD servers, and ad-free viewing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }

            // JazzCash Payment & WhatsApp Activation Card (matching index.php & series.php)
            item {
                VvipGlassCard(
                    borderColor = VipChampagneGold,
                    modifier = Modifier.testTag("jazzcash_payment_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "JazzCash Payment",
                                tint = VipChampagneGold,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "JazzCash & WhatsApp VIP Activation",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VipGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Official payment details for instant VIP activation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySilver
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = CharcoalSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Method: ${paymentInfo.method}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VipChampagneGold
                                    )
                                    Text(
                                        text = "Account Name: ${paymentInfo.accountName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Account Number: ${paymentInfo.accountNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldVerified,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("JazzCash Account", paymentInfo.accountNumber)
                                        )
                                        copiedFeedback = true
                                    },
                                    border = BorderStroke(1.dp, VipChampagneGold),
                                    modifier = Modifier.testTag("copy_jazzcash_number_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy JazzCash", tint = VipChampagneGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copiedFeedback) "Copied" else "Copy", color = VipGoldLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = paymentInfo.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    runCatching {
                                        val emailPart = currentUser?.email ?: "my HaniX account"
                                        val msg = Uri.encode("Assalam-o-Alaikum HaniX Urdu! I want to activate VIP Membership for $emailPart.")
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("${paymentInfo.whatsappSupportUrl}?text=$msg"))
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldVerified,
                                    contentColor = OledBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vip_whatsapp_activate_button")
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message on WhatsApp", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(paymentInfo.whatsappChannelUrl))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vip_whatsapp_channel_button")
                            ) {
                                Text("WhatsApp Channel", color = VipGoldLight)
                            }
                        }
                    }
                }
            }

            // Plan 1: FREE Plan Specification
            item {
                VvipGlassCard(
                    borderColor = if (effectiveTier == MembershipTier.FREE) EmeraldVerified else GlassBorderSubtle,
                    modifier = Modifier.testTag("plan_card_free")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FREE Membership",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimaryIvory,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = CharcoalSurface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, EmeraldVerified)
                            ) {
                                Text(
                                    text = "FREE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Included automatically with every registered account\n" +
                                "• Browse and watch all FREE movies and FREE series episodes\n" +
                                "• Play Mind Games Quiz & submit Dubbing Studio auditions\n" +
                                "• Cannot watch VIP-only movies/episodes or access VIP downloads",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }

            // Plan 2: VIP Plan Specification (with real hanixurdu.com pricing tiers)
            item {
                VvipGlassCard(
                    borderColor = VipChampagneGold,
                    modifier = Modifier.testTag("plan_card_vip")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIP Membership",
                                style = MaterialTheme.typography.titleLarge,
                                color = VipGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = VipChampagneGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OledBlack,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• All FREE features included\n" +
                                "• Watch all VIP dramas and series episodes\n" +
                                "• Official VIP Durations: 1 Month (Rs. 250) • 6 Months (Rs. 599) • 1 Year (Rs. 999)\n" +
                                "• Instant VIP activation across all your devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
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
                    VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${plan.name} (${plan.planType.uppercase()})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory
                                )
                                Text(
                                    text = "${plan.durationDays} days • ${plan.features?.joinToString(", ") ?: "VIP Movies & Series"}",
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
        }
    }
}

// =========================================================================
// 17. USER PROFILE SCREEN (GET /user/profile, PUT /user/profile)
//     Also includes Mind Games Quiz (mind_game.php), Dubbing Studio Audition
//     Upload (studio.php & upload.php), and Official Help & Social Support Links
// =========================================================================

@Composable
fun UserProfileScreen(
    currentUser: User?,
    isAuthenticated: Boolean,
    profileUpdateState: RemoteDataState<User>,
    mindGameState: RemoteDataState<MindGamePayload> = RemoteDataState.Idle,
    auditionUploadState: RemoteDataState<AuditionUploadPayload> = RemoteDataState.Idle,
    unreadNotifications: Int,
    onUpdateProfile: (String, String?) -> Unit,
    onLoadMindGame: () -> Unit = {},
    onUploadAudition: (String, String, String, Uri) -> Unit = { _, _, _, _ -> },
    onResetAuditionFeedback: () -> Unit = {},
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
    val context = LocalContext.current
    var displayName by rememberSaveable(currentUser?.name) {
        mutableStateOf(currentUser?.name ?: "")
    }
    val effectiveTier = currentUser?.effectiveMembershipTier() ?: MembershipTier.FREE

    // Interactive Mind Games Quiz state (from mind_game.php)
    var showMindGameQuiz by rememberSaveable { mutableStateOf(false) }
    var quizQuestions by remember { mutableStateOf(MindGameQuestionBank.drawTenRandomQuestions()) }
    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    var quizSubmitted by rememberSaveable { mutableStateOf(false) }
    var localAuditionError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(mindGameState) {
        if (mindGameState is RemoteDataState.Success && mindGameState.data.questions.isNotEmpty()) {
            quizQuestions = mindGameState.data.questions
        }
    }

    // Zero-permission system file picker for Dubbing Studio Auditions (studio.php & upload.php)
    val auditionFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            localAuditionError = null
            val uploaderName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "HaniX Member"
            onUploadAudition(uploaderName, "+923160450074", "Voice Actor", uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("user_profile_screen")
    ) {
        HaniXTopBar(
            title = "My Profile",
            subtitle = "Account, Membership & Dubbing Studio",
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
                                    text = if (effectiveTier == MembershipTier.VIP) {
                                        "Membership: VIP (Expires: ${currentUser?.resolvedExpiryDate ?: "Active"})"
                                    } else {
                                        "Membership: FREE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (effectiveTier == MembershipTier.VIP) VipChampagneGold else EmeraldVerified
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
                                label = { Text("Display Name") },
                                singleLine = true,
                                colors = hanixTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onUpdateProfile(displayName, currentUser?.resolvedProfileImage) },
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
                        onRetry = { onUpdateProfile(displayName, currentUser?.resolvedProfileImage) },
                        onOpenChecklist = onOpenChecklist
                    )
                }
            }

            // Mind Games Quiz Card (from mind_game.php)
            item {
                VvipGlassCard(
                    borderColor = VipChampagneGold.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("mind_game_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Quiz,
                                    contentDescription = "Mind Games Quiz",
                                    tint = VipChampagneGold,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "HaniX Mind Games Quiz",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "10 Drama Trivia Questions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VipGoldLight
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    showMindGameQuiz = !showMindGameQuiz
                                    if (showMindGameQuiz) {
                                        onLoadMindGame()
                                        quizQuestions = MindGameQuestionBank.drawTenRandomQuestions()
                                        selectedAnswers.clear()
                                        quizSubmitted = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VipChampagneGold,
                                    contentColor = OledBlack
                                ),
                                modifier = Modifier.testTag("toggle_mind_game_button")
                            ) {
                                Text(if (showMindGameQuiz) "Hide Quiz" else "Play Quiz", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showMindGameQuiz) {
                            Spacer(modifier = Modifier.height(14.dp))
                            quizQuestions.forEachIndexed { idx, q ->
                                val correctOption = q.options.getOrNull(q.answerIndex)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Q${idx + 1}. ${q.question}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimaryIvory,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    q.options.forEach { option ->
                                        val isSelected = selectedAnswers[idx] == option
                                        val isCorrect = option == correctOption
                                        val borderColor = when {
                                            quizSubmitted && isCorrect -> EmeraldVerified
                                            quizSubmitted && isSelected && !isCorrect -> CinemaCrimson
                                            isSelected -> VipChampagneGold
                                            else -> GlassBorderSubtle
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (!quizSubmitted) {
                                                    selectedAnswers[idx] = option
                                                }
                                            },
                                            border = BorderStroke(1.dp, borderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = option,
                                                color = when {
                                                    quizSubmitted && isCorrect -> EmeraldVerified
                                                    isSelected -> VipGoldLight
                                                    else -> TextSecondarySilver
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            if (!quizSubmitted) {
                                Button(
                                    onClick = { quizSubmitted = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VipChampagneGold,
                                        contentColor = OledBlack
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_mind_game_button")
                                ) {
                                    Text("Submit Quiz Answers", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val score = quizQuestions.indices.count { i ->
                                    val expected = quizQuestions[i].options.getOrNull(quizQuestions[i].answerIndex)
                                    selectedAnswers[i] == expected
                                }
                                Surface(
                                    color = CharcoalSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, EmeraldVerified),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Your Score: $score / ${quizQuestions.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.Bold
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                quizQuestions = MindGameQuestionBank.drawTenRandomQuestions()
                                                selectedAnswers.clear()
                                                quizSubmitted = false
                                            },
                                            border = BorderStroke(1.dp, VipChampagneGold)
                                        ) {
                                            Text("Play Again", color = VipGoldLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dubbing Studio Audition Upload Card (from studio.php & upload.php)
            item {
                VvipGlassCard(
                    borderColor = VipChampagneGold.copy(alpha = 0.55f),
                    modifier = Modifier.testTag("dubbing_studio_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dubbing Studio",
                                tint = VipChampagneGold,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HaniX Dubbing Studio Auditions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Voice Over & Dubbing Submissions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VipGoldLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload your Urdu voice-over or dubbing sample (MP3, WAV, OGG, M4A, MP4 up to 10 MB). Our casting team reviews submissions regularly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        if (localAuditionError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = localAuditionError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = CinemaCrimson
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onResetAuditionFeedback()
                                if (!isAuthenticated) {
                                    onNavigateToLogin()
                                } else {
                                    auditionFileLauncher.launch("*/*")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VipChampagneGold,
                                contentColor = OledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_studio_audition_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAuthenticated) "Select Audio / Video Audition (Max 10 MB)" else "Sign In to Upload Audition",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (auditionUploadState !is RemoteDataState.Idle) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ContractStateBanner(
                                state = auditionUploadState,
                                onRetry = { auditionFileLauncher.launch("*/*") },
                                onOpenChecklist = onOpenChecklist
                            )
                        }
                    }
                }
            }

            // Official Help & Social Support Channels (from help.php & footer.php)
            item {
                VvipGlassCard(
                    borderColor = EmeraldVerified.copy(alpha = 0.5f),
                    modifier = Modifier.testTag("help_social_links_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Help & Social Channels",
                                tint = EmeraldVerified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Official Help & Social Support",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIvory,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "WhatsApp: +92 316 0450074 • Email: hannnix88@gmail.com",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySilver
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/923160450074"))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, EmeraldVerified),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp Support", color = EmeraldVerified, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q"))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, VipChampagneGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WA Channel", color = VipGoldLight, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@Hanixurdu"))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("YouTube", color = TextPrimaryIvory, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hanixurdu"))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Instagram", color = TextPrimaryIvory, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@hanixurdu"))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TikTok", color = TextPrimaryIvory, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Default.WorkspacePremium,
                    title = "FREE & VIP Membership Status",
                    subtitle = "Check active plan and upgrade options",
                    testTag = "profile_nav_vip",
                    onClick = onNavigateToVip
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Continue Watching",
                    subtitle = "Resume in-progress dramas and episodes",
                    testTag = "profile_nav_continue_watching",
                    onClick = onNavigateToContinueWatching
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.History,
                    title = "Watch History",
                    subtitle = "Recently viewed dramas and episodes",
                    testTag = "profile_nav_watch_history",
                    onClick = onNavigateToWatchHistory
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Download,
                    title = "My VIP Downloads",
                    subtitle = "Offline encrypted drama vault",
                    testTag = "profile_nav_downloads",
                    onClick = onNavigateToDownloads
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Drama releases and announcements",
                    testTag = "profile_nav_notifications",
                    onClick = onNavigateToNotifications
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Settings,
                    title = "App Settings",
                    subtitle = "Streaming server, cache and playback options",
                    testTag = "profile_nav_settings",
                    onClick = onNavigateToSettings
                )
            }
            item {
                ProfileMenuRow(
                    icon = Icons.Default.Info,
                    title = "About HaniX Urdu",
                    subtitle = "App version, legal information & community",
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
            subtitle = "Your recently watched dramas and episodes",
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
            subtitle = "Pick up right where you left off",
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
                                text = "When you pause a drama or episode mid-stream, your exact timestamp is saved here for instant resume.",
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
    onPlayDownloadedMedia: (String, String) -> Unit = { _, _ -> },
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
            title = "App-Private Offline Vault",
            subtitle = "AES-256-GCM Encrypted • Hidden from Gallery",
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
                                text = "Private Offline Vault (No Gallery Save)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIvory
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All downloaded movies and episodes are encrypted with hardware-backed AES-256-GCM inside HaniX's private vault with .nomedia protection. Videos are strictly hidden from your phone's Gallery and external file apps. Logged-in users can download FREE content, and VIP members can download VIP content.",
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
                            Text("Save Featured Stream to Vault", color = VipGoldLight)
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
                        onClick = {
                            if (!expired) {
                                onPlayDownloadedMedia(dl.contentType, dl.contentId)
                            }
                        },
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
                                    color = TextPrimaryIvory,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (expired) {
                                        "License Expired — Playback Disabled"
                                    } else {
                                        "AES-256-GCM Private Vault • Hidden from Gallery"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (expired) CinemaCrimson else EmeraldVerified
                                )
                            }
                            if (!expired) {
                                Button(
                                    onClick = { onPlayDownloadedMedia(dl.contentType, dl.contentId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CinemaCrimson,
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
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
            subtitle = "Drama releases and announcements",
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
            title = "App Settings",
            subtitle = "Streaming server & preferences",
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
                            text = "Streaming Gateway Server URL",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Configured streaming gateway: ${BuildConfig.HANIX_API_BASE_URL}. Secure HTTPS streaming is enforced for all media traffic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editedUrl,
                            onValueChange = { editedUrl = it },
                            label = { Text("Streaming Server Gateway URL") },
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
            title = "About HaniX Urdu",
            subtitle = "Official Urdu Dubbed Streaming Platform",
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
                            text = "HaniX Urdu Android v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryIvory
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The premier destination for high quality Turkish, Korean, and Chinese dramas dubbed and subtitled in Urdu. Stream smoothly across mobile and web with multi-server cinema playback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondarySilver
                        )
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.6f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Key Features",
                            style = MaterialTheme.typography.titleMedium,
                            color = VipChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Multi-Server HD Cinema Player (Main Server, Server 2 & Direct Web Player)\n" +
                                "• Professional Urdu Dubbing & Authentic Subtitles\n" +
                                "• VIP Membership with Instant JazzCash Activation\n" +
                                "• App-Private Encrypted Offline Vault for Downloaded Dramas\n" +
                                "• Seamless Watch History & Resume Playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }

            item {
                VvipGlassCard(borderColor = EmeraldVerified.copy(alpha = 0.6f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Community & Support",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldVerified
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Official Website: hanixurdu.com\nWhatsApp Support: +92 316 0450074\nEmail: hannnix88@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySilver
                        )
                    }
                }
            }
        }
    }
}
