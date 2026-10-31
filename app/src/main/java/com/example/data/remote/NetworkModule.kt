package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.ApiResponse
import com.example.data.model.AuthSessionPayload
import com.example.data.model.RefreshTokenRequest
import com.example.data.security.HaniXSecurityShield
import com.example.data.security.SecureSessionManager
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Tolerant Boolean adapter for PHP/MySQL responses where boolean columns (e.g. `is_premium`, `is_vip`)
 * may be serialized as `1`/`0`, `"1"`/`"0"`, `"true"`/`"false"`, or JSON booleans `true`/`false`.
 */
class FlexibleBooleanAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.STRING -> {
                val str = reader.nextString().trim()
                str == "1" || str.equals("true", ignoreCase = true) || str.equals("yes", ignoreCase = true)
            }
            JsonReader.Token.NULL -> reader.nextNull()
            else -> {
                reader.skipValue()
                false
            }
        }
    }

    @ToJson
    fun toJson(value: Boolean?): Boolean? = value
}

/**
 * Provides HTTPS-only OkHttpClient, Moshi serializer with PHP/MySQL boolean compatibility,
 * Bearer token injection, and automatic `POST /auth/refresh` token rotation on HTTP 401.
 */
object NetworkModule {

    val moshi: Moshi = Moshi.Builder()
        .add(FlexibleBooleanAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createApiService(sessionManager: SecureSessionManager): HaniXApiService {
        val dynamicHostAndAuthInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val configuredBase = sessionManager.getConfiguredBaseUrl().toHttpUrlOrNull()

            val targetUrl = if (configuredBase != null && configuredBase.isHttps) {
                val baseSegments = configuredBase.encodedPathSegments.filter { it.isNotEmpty() }
                val defaultSegments = BuildConfig.HANIX_API_BASE_URL
                    .toHttpUrlOrNull()
                    ?.encodedPathSegments
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                val originalSegments = originalRequest.url.encodedPathSegments.filter { it.isNotEmpty() }
                val relativeSegments = if (originalSegments.take(defaultSegments.size) == defaultSegments) {
                    originalSegments.drop(defaultSegments.size)
                } else {
                    originalSegments
                }

                originalRequest.url.newBuilder()
                    .scheme("https")
                    .host(configuredBase.host)
                    .port(configuredBase.port)
                    .encodedPath("/" + (baseSegments + relativeSegments).joinToString("/"))
                    .build()
            } else {
                originalRequest.url
            }

            val requestBuilder = originalRequest.newBuilder()
                .url(targetUrl)
                .header("Accept", "application/json")
                .header("X-Client-Platform", "HaniX-Urdu-Android")
                .header("X-Client-Version", BuildConfig.VERSION_NAME)

            // Only attach Authorization if not already set by Authenticator retry
            if (originalRequest.header("Authorization") == null) {
                val accessToken = sessionManager.getAccessToken()
                if (!accessToken.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $accessToken")
                }
            }

            chain.proceed(requestBuilder.build())
        }

        // Token Refresh Authenticator (Section 5: Token refresh & expired session handling)
        val tokenRefreshAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                val path = response.request.url.encodedPath
                if (path.endsWith("/auth/login") ||
                    path.endsWith("/auth/register") ||
                    path.endsWith("/auth/refresh")
                ) {
                    return null
                }

                // Prevent infinite retry loops
                if (responseCount(response) >= 2) {
                    sessionManager.clearSession("Your session has expired. Please sign in again.")
                    return null
                }

                val refreshToken = sessionManager.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    sessionManager.clearSession("Authentication required. Please sign in.")
                    return null
                }

                synchronized(this) {
                    val refreshUrl = (sessionManager.getConfiguredBaseUrl() + "auth/refresh").toHttpUrlOrNull()
                        ?: return null
                    val reqAdapter = moshi.adapter(RefreshTokenRequest::class.java)
                    val bodyJson = reqAdapter.toJson(RefreshTokenRequest(refreshToken))
                    val refreshRequest = Request.Builder()
                        .url(refreshUrl)
                        .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .header("Accept", "application/json")
                        .build()

                    val refreshClient = HaniXSecurityShield.configureSecureOkHttpClient(
                        OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                    ).build()

                    return try {
                        refreshClient.newCall(refreshRequest).execute().use { refreshResp ->
                            if (!refreshResp.isSuccessful) {
                                sessionManager.clearSession("Session expired. Please sign in again.")
                                return null
                            }
                            val rawBody = refreshResp.body?.string() ?: return null
                            val type = Types.newParameterizedType(
                                ApiResponse::class.java,
                                AuthSessionPayload::class.java
                            )
                            val adapter = moshi.adapter<ApiResponse<AuthSessionPayload>>(type)
                            val parsed = adapter.fromJson(rawBody)
                            val sessionData = parsed?.data
                            val rotatedAccess = sessionData?.resolvedAccessToken.orEmpty()
                            if (parsed?.success == true && sessionData != null && rotatedAccess.isNotBlank()) {
                                sessionManager.saveAuthenticatedSession(
                                    accessToken = rotatedAccess,
                                    refreshToken = sessionData.resolvedRefreshToken ?: refreshToken,
                                    user = sessionData.user
                                )
                                response.request.newBuilder()
                                    .header("Authorization", "Bearer $rotatedAccess")
                                    .build()
                            } else {
                                sessionManager.clearSession("Session expired. Please sign in again.")
                                null
                            }
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            private fun responseCount(response: Response): Int {
                var count = 1
                var prior = response.priorResponse
                while (prior != null) {
                    count++
                    prior = prior.priorResponse
                }
                return count
            }
        }

        // Zero API leak policy: never log URLs, tokens, cookies, or stream links to logcat
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        val okHttpClient = HaniXSecurityShield.configureSecureOkHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(dynamicHostAndAuthInterceptor)
                .authenticator(tokenRefreshAuthenticator)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
        ).build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.HANIX_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HaniXApiService::class.java)
    }
}
