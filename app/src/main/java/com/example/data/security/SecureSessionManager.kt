package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages authenticated session state, Android Keystore-encrypted Bearer tokens,
 * configurable HTTPS REST API base URL, and onboarding completion.
 */
class SecureSessionManager(
    context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hanix_urdu_secure_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(loadSavedUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(getAccessToken() != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _apiBaseUrl = MutableStateFlow(getConfiguredBaseUrl())
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        _onboardingCompleted.value = true
    }

    fun saveAuthenticatedSession(
        accessToken: String,
        refreshToken: String?,
        user: User
    ) {
        val encAccess = cryptoManager.encryptString(accessToken)
        val encRefresh = refreshToken?.let { cryptoManager.encryptString(it) }
        prefs.edit().apply {
            putString(KEY_ENC_ACCESS_TOKEN, encAccess)
            if (encRefresh != null) {
                putString(KEY_ENC_REFRESH_TOKEN, encRefresh)
            }
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_AVATAR, user.profileImage)
            putBoolean(KEY_USER_VIP_STATUS, user.vipStatus == true)
            putString(KEY_USER_VIP_EXPIRY, user.vipExpiry)
            apply()
        }
        _currentUser.value = user
        _isAuthenticated.value = true
        _sessionExpiredMessage.value = null
    }

    fun updateCurrentUser(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_AVATAR, user.profileImage)
            putBoolean(KEY_USER_VIP_STATUS, user.vipStatus == true)
            putString(KEY_USER_VIP_EXPIRY, user.vipExpiry)
            apply()
        }
        _currentUser.value = user
    }

    fun getAccessToken(): String? {
        val encrypted = prefs.getString(KEY_ENC_ACCESS_TOKEN, null) ?: return null
        return cryptoManager.decryptString(encrypted)?.takeIf { it.isNotBlank() }
    }

    fun getRefreshToken(): String? {
        val encrypted = prefs.getString(KEY_ENC_REFRESH_TOKEN, null) ?: return null
        return cryptoManager.decryptString(encrypted)?.takeIf { it.isNotBlank() }
    }

    fun clearSession(expiredReason: String? = null) {
        prefs.edit().apply {
            remove(KEY_ENC_ACCESS_TOKEN)
            remove(KEY_ENC_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_AVATAR)
            remove(KEY_USER_VIP_STATUS)
            remove(KEY_USER_VIP_EXPIRY)
            apply()
        }
        _currentUser.value = null
        _isAuthenticated.value = false
        if (expiredReason != null) {
            _sessionExpiredMessage.value = expiredReason
        }
    }

    fun dismissSessionExpiredNotice() {
        _sessionExpiredMessage.value = null
    }

    /**
     * Strictly enforces HTTPS for the Hostinger PHP REST API base URL.
     * Rejects cleartext http:// URLs per Section 10 security requirements.
     */
    fun updateApiBaseUrl(rawUrl: String): Result<String> {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Security Policy: Only HTTPS API URLs (https://...) are permitted.")
            )
        }
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        prefs.edit().putString(KEY_API_BASE_URL, normalized).apply()
        _apiBaseUrl.value = normalized
        return Result.success(normalized)
    }

    fun getConfiguredBaseUrl(): String {
        val saved = prefs.getString(KEY_API_BASE_URL, null)
        if (!saved.isNullOrBlank() && saved.startsWith("https://", ignoreCase = true)) {
            return if (saved.endsWith("/")) saved else "$saved/"
        }
        return BuildConfig.HANIX_API_BASE_URL
    }

    private fun loadSavedUser(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        return User(
            id = id,
            name = prefs.getString(KEY_USER_NAME, null),
            email = prefs.getString(KEY_USER_EMAIL, null),
            profileImage = prefs.getString(KEY_USER_AVATAR, null),
            vipStatus = prefs.getBoolean(KEY_USER_VIP_STATUS, false),
            vipExpiry = prefs.getString(KEY_USER_VIP_EXPIRY, null)
        )
    }

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ENC_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_ENC_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_VIP_STATUS = "user_vip_status"
        private const val KEY_USER_VIP_EXPIRY = "user_vip_expiry"
        private const val KEY_API_BASE_URL = "api_base_url"
    }
}
