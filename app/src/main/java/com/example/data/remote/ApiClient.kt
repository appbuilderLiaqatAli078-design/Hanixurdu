package com.example.data.remote

import com.example.data.security.SecureSessionManager

/**
 * Provides configured Retrofit API clients (`HaniXApiService` and `ApiService`) backed by
 * `NetworkModule`'s HTTPS-only OkHttpClient, Moshi `FlexibleBooleanAdapter`, and 401 token refresh Authenticator.
 */
object ApiClient {

    fun createHaniXApiService(sessionManager: SecureSessionManager): HaniXApiService {
        return NetworkModule.createApiService(sessionManager)
    }
}
