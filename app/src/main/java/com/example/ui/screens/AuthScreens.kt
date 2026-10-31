package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AuthSessionPayload
import com.example.data.model.RemoteDataState
import com.example.ui.components.ContractStateBanner
import com.example.ui.components.HaniXTopBar
import com.example.ui.components.SecurityAssuranceStrip
import com.example.ui.components.VvipGlassCard
import com.example.ui.theme.CharcoalBackground
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
import kotlinx.coroutines.delay

// =========================================================================
// 1. SPLASH SCREEN
// =========================================================================

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    isAuthenticated: Boolean,
    onNavigateNext: (String) -> Unit
) {
    LaunchedEffect(onboardingCompleted, isAuthenticated) {
        delay(900)
        if (!onboardingCompleted) {
            onNavigateNext("onboarding")
        } else {
            onNavigateNext("home")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E172A), OledBlack),
                    radius = 900f
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VipChampagneGold, CinemaCrimson)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "HaniX Urdu Emblem",
                    tint = OledBlack,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "HaniX Urdu",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimaryIvory,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "VVIP URDU CINEMA & SERIES",
                style = MaterialTheme.typography.labelMedium,
                color = VipChampagneGold
            )
            Spacer(modifier = Modifier.height(28.dp))
            TextButton(
                onClick = {
                    if (!onboardingCompleted) onNavigateNext("onboarding") else onNavigateNext("home")
                },
                modifier = Modifier.testTag("splash_continue_button")
            ) {
                Text("Enter Cinema Lounge", color = VipGoldLight)
            }
        }
    }
}

// =========================================================================
// 2. ONBOARDING SCREEN
// =========================================================================

private data class OnboardingSlide(
    val badge: String,
    val title: String,
    val description: String,
    val phpReference: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    onSignInClick: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            badge = "UNIFIED HOSTINGER BACKEND",
            title = "One Account Across Web & Android",
            description = "Directly connects to your existing Hostinger MySQL database via the proposed /api/v1/ REST contract. Movies, Series, Seasons, Episodes, and VIP status stay synchronized with hanixurdu.com.",
            phpReference = "Shares data with index.php, series.php, view_series.php & shahkot.php"
        ),
        OnboardingSlide(
            badge = "BUNNY STREAM HLS ADAPTIVE",
            title = "Server-Signed Cinema Playback",
            description = "Every stream is authorized server-side via POST /playback/authorize. Short-lived Bunny Stream HLS tokens protect premium content with zero Bunny API secrets inside the APK.",
            phpReference = "Replaces direct URL exposure in run_video.php & check_premium.php"
        ),
        OnboardingSlide(
            badge = "ANDROID KEYSTORE SECURITY",
            title = "Encrypted Offline Downloads & VVIP",
            description = "Access tokens and offline downloads are encrypted with hardware-backed AES-256-GCM in Android Keystore, with server-verified VIP entitlement checks.",
            phpReference = "Integrates with add_premium.php & unlock_vip.php"
        )
    )

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSlide = slides[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("onboarding_screen")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(VipChampagneGold.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CinemaCrimson.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.75f),
                    radius = size.width * 0.65f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HaniX Urdu",
                    style = MaterialTheme.typography.titleLarge,
                    color = VipChampagneGold,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip to App", color = TextSecondarySilver)
                }
            }

            VvipGlassCard(
                borderColor = VipChampagneGold.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Surface(
                        color = VipGoldDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, VipChampagneGold)
                    ) {
                        Text(
                            text = currentSlide.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = VipGoldLight,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Icon(
                        imageVector = when (currentIndex) {
                            0 -> Icons.Default.MovieFilter
                            1 -> Icons.Default.WorkspacePremium
                            else -> Icons.Default.Security
                        },
                        contentDescription = currentSlide.title,
                        tint = VipChampagneGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentSlide.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimaryIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentSlide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentSlide.phpReference,
                        style = MaterialTheme.typography.labelSmall,
                        color = VipChampagneGold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    slides.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (idx == currentIndex) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == currentIndex) VipChampagneGold else TextMutedSlate.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentIndex < slides.lastIndex) {
                            currentIndex++
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VipChampagneGold,
                        contentColor = OledBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentIndex < slides.lastIndex) "Next Feature" else "Explore HaniX Urdu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continue"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSignInClick,
                    border = BorderStroke(1.dp, VipChampagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_signin_button")
                ) {
                    Text("Sign In with Existing HaniX Account", color = VipGoldLight)
                }
            }
        }
    }
}

// =========================================================================
// 3. LOGIN SCREEN (POST /auth/login -> login_process.php)
// =========================================================================

@Composable
fun LoginScreen(
    authState: RemoteDataState<AuthSessionPayload>,
    apiBaseUrl: String,
    onLoginSubmit: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBackOrContinue: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is RemoteDataState.Success) {
            onBackOrContinue()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(rememberScrollState())
            .testTag("login_screen")
    ) {
        HaniXTopBar(
            title = "Sign In to HaniX Urdu",
            subtitle = "POST /auth/login • Shared Hostinger Account",
            onBackClick = onBackOrContinue
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecurityAssuranceStrip()

            VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.45f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use your existing hanixurdu.com account credentials. Password verification is performed securely on the Hostinger PHP backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationError = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = VipChampagneGold)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondarySilver
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    AnimatedVisibility(visible = validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaCrimson,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            modifier = Modifier.testTag("login_forgot_password_button")
                        ) {
                            Text("Forgot Password?", color = VipChampagneGold)
                        }
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                validationError = "Please enter both your email address and password."
                            } else {
                                onLoginSubmit(email, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text(
                            text = "Authenticate with Hostinger API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ContractStateBanner(
                state = authState,
                onRetry = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLoginSubmit(email, password)
                    }
                },
                onOpenChecklist = onOpenChecklist
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New to HaniX Urdu?", color = TextSecondarySilver)
                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.testTag("login_to_register_button")
                ) {
                    Text("Create Account", color = VipChampagneGold, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Target API: $apiBaseUrl",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// 4. REGISTRATION SCREEN (POST /auth/register -> signup.php)
// =========================================================================

@Composable
fun RegisterScreen(
    authState: RemoteDataState<AuthSessionPayload>,
    onRegisterSubmit: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is RemoteDataState.Success) {
            onBackClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(rememberScrollState())
            .testTag("register_screen")
    ) {
        HaniXTopBar(
            title = "Create HaniX Account",
            subtitle = "POST /auth/register • Bridges signup.php",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.45f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Join HaniX Urdu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registers a shared user account in your Hostinger MySQL database for both web and Android access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Name", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationError = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Password (min 8 characters)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_confirm_password_input")
                    )

                    AnimatedVisibility(visible = validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaCrimson,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            when {
                                name.isBlank() || email.isBlank() || password.isBlank() ->
                                    validationError = "All fields are required."
                                password.length < 8 ->
                                    validationError = "Password must be at least 8 characters."
                                password != confirmPassword ->
                                    validationError = "Passwords do not match."
                                else ->
                                    onRegisterSubmit(name, email, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_submit_button")
                    ) {
                        Text(
                            text = "Register Account via API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ContractStateBanner(
                state = authState,
                onRetry = {
                    if (name.isNotBlank() && email.isNotBlank() && password.length >= 8) {
                        onRegisterSubmit(name, email, password)
                    }
                },
                onOpenChecklist = onOpenChecklist
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered on hanixurdu.com?", color = TextSecondarySilver)
                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("register_to_login_button")
                ) {
                    Text("Sign In", color = VipChampagneGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// 5. FORGOT PASSWORD SCREEN (POST /auth/forgot-password -> forgot_password.php)
// =========================================================================

@Composable
fun ForgotPasswordScreen(
    forgotPasswordState: RemoteDataState<Unit>,
    onSubmitEmail: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(rememberScrollState())
            .testTag("forgot_password_screen")
    ) {
        HaniXTopBar(
            title = "Password Recovery",
            subtitle = "POST /auth/forgot-password • forgot_password.php",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VvipGlassCard(borderColor = VipChampagneGold.copy(alpha = 0.45f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Reset Your HaniX Password",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimaryIvory
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter the email address linked to your hanixurdu.com account. The PHP backend will dispatch a time-limited recovery link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySilver
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localError = null
                        },
                        label = { Text("Registered Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = VipChampagneGold)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = hanixTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input")
                    )

                    if (localError != null) {
                        Text(
                            text = localError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaCrimson,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                localError = "Please enter a valid email address."
                            } else {
                                onSubmitEmail(email)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VipChampagneGold,
                            contentColor = OledBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("forgot_password_submit_button")
                    ) {
                        Text("Send Recovery Request", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (forgotPasswordState is RemoteDataState.Success) {
                VvipGlassCard(borderColor = EmeraldVerified) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldVerified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Password recovery instructions requested via POST /auth/forgot-password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryIvory
                        )
                    }
                }
            } else {
                ContractStateBanner(
                    state = forgotPasswordState,
                    onRetry = { if (email.isNotBlank()) onSubmitEmail(email) },
                    onOpenChecklist = onOpenChecklist
                )
            }
        }
    }
}

@Composable
fun hanixTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VipChampagneGold,
    unfocusedBorderColor = GlassBorderSubtle,
    focusedLabelColor = VipChampagneGold,
    unfocusedLabelColor = TextSecondarySilver,
    cursorColor = VipChampagneGold,
    focusedTextColor = TextPrimaryIvory,
    unfocusedTextColor = TextPrimaryIvory,
    focusedContainerColor = CharcoalBackground,
    unfocusedContainerColor = CharcoalBackground
)
