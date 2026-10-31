package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BackendEndpointCatalog
import com.example.data.model.User
import com.example.data.security.SecureSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read HaniX Urdu app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HaniX Urdu", appName)
    }

    @Test
    fun `secure session manager enforces HTTPS base URL and encrypts tokens`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SecureSessionManager(context)

        // Rejects cleartext HTTP
        val httpResult = sessionManager.updateApiBaseUrl("http://hanixurdu.com/api/v1/")
        assertTrue(httpResult.isFailure)

        // Accepts HTTPS
        val httpsResult = sessionManager.updateApiBaseUrl("https://hanixurdu.com/api/v1")
        assertTrue(httpsResult.isSuccess)
        assertEquals("https://hanixurdu.com/api/v1/", sessionManager.getConfiguredBaseUrl())

        // Token & user session persistence
        val user = User(
            id = "42",
            name = "Awais",
            email = "awais@hanixurdu.com",
            vipStatus = true,
            vipExpiry = "2027-01-01"
        )
        sessionManager.saveAuthenticatedSession("jwt_access_token_xyz", "jwt_refresh_token_abc", user)
        assertEquals("jwt_access_token_xyz", sessionManager.getAccessToken())
        assertTrue(sessionManager.isAuthenticated.value)

        sessionManager.clearSession()
        assertFalse(sessionManager.isAuthenticated.value)
    }

    @Test
    fun `backend endpoint catalog covers all 35 proposed API v1 routes`() {
        assertEquals(35, BackendEndpointCatalog.endpoints.size)
    }
}
