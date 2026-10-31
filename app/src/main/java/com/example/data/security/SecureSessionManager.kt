package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.MembershipTier
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.hanixDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hanix_urdu_preferences"
)

/**
 * Manages authenticated session state, Android Keystore-encrypted Bearer tokens,
 * DataStore app preferences, and HTTPS API base URL configuration.
 *
 * Strict Membership Security (Section 4 & Section 14):
 * - Every unverified or newly registered user starts as FREE.
 * - Never grants VIP from hardcoded Android values.
 * - Local cached user data is strictly a UI display cache; expired VIP dates immediately
 *   downgrade to FREE, and all VIP playback/download operations are verified by the PHP backend.
 */
class SecureSessionManager(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        ioScope.launch {
            context.hanixDataStore.edit { store ->
                store[DS_ONBOARDING_DONE] = true
            }
        }
    }

    fun saveAuthenticatedSession(
        accessToken: String,
        refreshToken: String?,
        user: User
    ) {
        val normalizedUser = sanitizeUserMembership(user)
        val encAccess = cryptoManager.encryptString(accessToken)
        val encRefresh = refreshToken?.let { cryptoManager.encryptString(it) }
        prefs.edit().apply {
            putString(KEY_ENC_ACCESS_TOKEN, encAccess)
            if (encRefresh != null) {
                putString(KEY_ENC_REFRESH_TOKEN, encRefresh)
            }
            putString(KEY_USER_ID, normalizedUser.id)
            putString(KEY_USER_NAME, normalizedUser.name)
            putString(KEY_USER_EMAIL, normalizedUser.email)
            putString(KEY_USER_AVATAR, normalizedUser.resolvedProfileImage)
            putString(KEY_USER_MEMBERSHIP, normalizedUser.effectiveMembershipTier().name)
            putBoolean(KEY_USER_VIP_STATUS, normalizedUser.isActiveVip)
            putString(KEY_USER_VIP_EXPIRY, normalizedUser.resolvedExpiryDate)
            if (normalizedUser.vipExpiryEpochSeconds != null) {
                putLong(KEY_USER_VIP_EXPIRY_EPOCH, normalizedUser.vipExpiryEpochSeconds)
            } else {
                remove(KEY_USER_VIP_EXPIRY_EPOCH)
            }
            apply()
        }
        _currentUser.value = normalizedUser
        _isAuthenticated.value = true
        _sessionExpiredMessage.value = null
    }

    fun updateRotatedTokens(accessToken: String, refreshToken: String?) {
        val encAccess = cryptoManager.encryptString(accessToken)
        prefs.edit().apply {
            putString(KEY_ENC_ACCESS_TOKEN, encAccess)
            if (!refreshToken.isNullOrBlank()) {
                putString(KEY_ENC_REFRESH_TOKEN, cryptoManager.encryptString(refreshToken))
            }
            apply()
        }
        _isAuthenticated.value = true
    }

    fun updateCurrentUser(user: User) {
        val normalizedUser = sanitizeUserMembership(user)
        prefs.edit().apply {
            putString(KEY_USER_ID, normalizedUser.id)
            putString(KEY_USER_NAME, normalizedUser.name)
            putString(KEY_USER_EMAIL, normalizedUser.email)
            putString(KEY_USER_AVATAR, normalizedUser.resolvedProfileImage)
            putString(KEY_USER_MEMBERSHIP, normalizedUser.effectiveMembershipTier().name)
            putBoolean(KEY_USER_VIP_STATUS, normalizedUser.isActiveVip)
            putString(KEY_USER_VIP_EXPIRY, normalizedUser.resolvedExpiryDate)
            if (normalizedUser.vipExpiryEpochSeconds != null) {
                putLong(KEY_USER_VIP_EXPIRY_EPOCH, normalizedUser.vipExpiryEpochSeconds)
            } else {
                remove(KEY_USER_VIP_EXPIRY_EPOCH)
            }
            apply()
        }
        _currentUser.value = normalizedUser
    }

    fun applyServerVipStatus(tier: MembershipTier, expiryDate: String?, expiryEpochSeconds: Long?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            membership = tier.name,
            vipStatus = (tier == MembershipTier.VIP),
            vipExpiry = expiryDate,
            vipExpiryEpochSeconds = expiryEpochSeconds
        )
        updateCurrentUser(updated)
    }

    private fun sanitizeUserMembership(user: User): User {
        val effectiveTier = user.effectiveMembershipTier()
        return user.copy(
            membership = effectiveTier.name,
            vipStatus = (effectiveTier == MembershipTier.VIP),
            profileImage = user.resolvedProfileImage,
            vipExpiry = user.resolvedExpiryDate
        )
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
            remove(KEY_USER_MEMBERSHIP)
            remove(KEY_USER_VIP_STATUS)
            remove(KEY_USER_VIP_EXPIRY)
            remove(KEY_USER_VIP_EXPIRY_EPOCH)
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
        ioScope.launch {
            context.hanixDataStore.edit { store ->
                store[DS_API_BASE_URL] = normalized
            }
        }
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
        // Never restore a user profile if no valid encrypted access token exists
        if (getAccessToken() == null) return null
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val expiryStr = prefs.getString(KEY_USER_VIP_EXPIRY, null)
        val expiryEpoch = if (prefs.contains(KEY_USER_VIP_EXPIRY_EPOCH)) {
            prefs.getLong(KEY_USER_VIP_EXPIRY_EPOCH, 0L).takeIf { it > 0L }
        } else null
        val rawMembership = prefs.getString(KEY_USER_MEMBERSHIP, "FREE")
        val rawVipFlag = prefs.getBoolean(KEY_USER_VIP_STATUS, false)

        val resolvedTier = MembershipTier.resolve(
            rawMembership = rawMembership,
            vipStatusFlag = rawVipFlag,
            vipExpiryEpochSeconds = expiryEpoch,
            vipExpiryDateStr = expiryStr
        )

        return User(
            id = id,
            name = prefs.getString(KEY_USER_NAME, null),
            email = prefs.getString(KEY_USER_EMAIL, null),
            profileImage = prefs.getString(KEY_USER_AVATAR, null),
            membership = resolvedTier.name,
            vipStatus = (resolvedTier == MembershipTier.VIP),
            vipExpiry = expiryStr,
            vipExpiryEpochSeconds = expiryEpoch
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
        private const val KEY_USER_MEMBERSHIP = "user_membership"
        private const val KEY_USER_VIP_STATUS = "user_vip_status"
        private const val KEY_USER_VIP_EXPIRY = "user_vip_expiry"
        private const val KEY_USER_VIP_EXPIRY_EPOCH = "user_vip_expiry_epoch"
        private const val KEY_API_BASE_URL = "api_base_url"

        private val DS_ONBOARDING_DONE = booleanPreferencesKey("ds_onboarding_done")
        private val DS_API_BASE_URL = stringPreferencesKey("ds_api_base_url")
    }
}
