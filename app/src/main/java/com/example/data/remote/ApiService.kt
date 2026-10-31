package com.example.data.remote

import com.example.data.model.ApiResponse
import com.example.data.model.LoginRequest
import com.example.data.model.Movie
import com.example.data.model.RegisterRequest
import com.example.data.model.Series
import com.example.data.remote.dto.CategoryListResponse
import com.example.data.remote.dto.DownloadRequest
import com.example.data.remote.dto.DownloadResponse
import com.example.data.remote.dto.EpisodeListResponse
import com.example.data.remote.dto.HomeResponse
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.MembershipStatusResponse
import com.example.data.remote.dto.MovieListResponse
import com.example.data.remote.dto.PasswordResetRequest
import com.example.data.remote.dto.PasswordVerifyRequest
import com.example.data.remote.dto.PasswordVerifyResponse
import com.example.data.remote.dto.PlaybackRequest
import com.example.data.remote.dto.PlaybackResponse
import com.example.data.remote.dto.RegisterResponse
import com.example.data.remote.dto.SearchResponse
import com.example.data.remote.dto.SeasonListResponse
import com.example.data.remote.dto.SeriesListResponse
import com.example.data.remote.dto.TokenRefreshRequest
import com.example.data.remote.dto.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API Service matching the exact Part 2 & Part 4 PHP REST API v1 contract (`https://hanixurdu.com/api/v1/`).
 */
interface ApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<RegisterResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: TokenRefreshRequest
    ): ApiResponse<LoginResponse>

    @POST("auth/forgot-password/verify")
    suspend fun verifyForgotPassword(
        @Body request: PasswordVerifyRequest
    ): ApiResponse<PasswordVerifyResponse>

    @POST("auth/forgot-password/reset")
    suspend fun resetForgotPassword(
        @Body request: PasswordResetRequest
    ): ApiResponse<Unit>

    @GET("user/profile")
    suspend fun getUserProfile(): ApiResponse<UserProfileResponse>

    @GET("home")
    suspend fun getHome(): ApiResponse<HomeResponse>

    @GET("home/trending")
    suspend fun getTrending(): ApiResponse<HomeResponse>

    @GET("categories")
    suspend fun getCategories(): ApiResponse<CategoryListResponse>

    @GET("movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null
    ): ApiResponse<MovieListResponse>

    @GET("movies/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int
    ): ApiResponse<Movie>

    @GET("series")
    suspend fun getSeries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<SeriesListResponse>

    @GET("series/{id}")
    suspend fun getSeriesDetail(
        @Path("id") id: Int
    ): ApiResponse<Series>

    @GET("series/{id}/seasons")
    suspend fun getSeasons(
        @Path("id") id: Int
    ): ApiResponse<SeasonListResponse>

    @GET("series/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: Int,
        @Query("season") season: String
    ): ApiResponse<EpisodeListResponse>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<SearchResponse>

    @GET("membership/status")
    suspend fun getMembershipStatus(): ApiResponse<MembershipStatusResponse>

    @POST("playback/authorize")
    suspend fun authorizePlayback(
        @Body request: PlaybackRequest
    ): ApiResponse<PlaybackResponse>

    @POST("downloads/authorize")
    suspend fun authorizeDownload(
        @Body request: DownloadRequest
    ): ApiResponse<DownloadResponse>
}
