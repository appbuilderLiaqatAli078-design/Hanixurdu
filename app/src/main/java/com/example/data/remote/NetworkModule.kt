package com.example.data.remote

import com.example.BuildConfig
import com.example.data.security.SecureSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides HTTPS-only OkHttpClient, Moshi serializer, and Retrofit client for HaniXApiService.
 * Never disables TLS certificate validation and automatically injects Bearer authorization tokens.
 */
object NetworkModule {

    val moshi: Moshi = Moshi.Builder()
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

            val accessToken = sessionManager.getAccessToken()
            if (!accessToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $accessToken")
            }

            val response = chain.proceed(requestBuilder.build())
            if (response.code == 401 && !originalRequest.url.encodedPath.endsWith("/auth/login")) {
                sessionManager.clearSession("Your session expired or requires re-authentication.")
            }
            response
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .addInterceptor(dynamicHostAndAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.HANIX_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HaniXApiService::class.java)
    }
}
