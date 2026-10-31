package com.example.data.remote.dto

import com.example.data.model.AuthSessionPayload
import com.example.data.model.CategoryItem
import com.example.data.model.DownloadAuthorizationPayload
import com.example.data.model.DownloadAuthorizeRequest
import com.example.data.model.Episode
import com.example.data.model.ForgotPasswordResetRequest
import com.example.data.model.ForgotPasswordVerifyRequest
import com.example.data.model.ForgotPasswordVerifyResponse
import com.example.data.model.HomePayload
import com.example.data.model.LoginRequest
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackAuthorizeRequest
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.User
import com.example.data.model.VipStatusPayload

/**
 * Clean Architecture DTO type aliases matching the exact PHP REST API v1 contracts (Part 2 & Part 4).
 */
typealias LoginResponse = AuthSessionPayload
typealias RegisterResponse = AuthSessionPayload
typealias HomeResponse = HomePayload
typealias MovieListResponse = List<Movie>
typealias SeriesListResponse = List<Series>
typealias SeasonListResponse = List<Season>
typealias EpisodeListResponse = List<Episode>
typealias CategoryListResponse = List<CategoryItem>
typealias SearchResponse = SearchPayload
typealias MembershipStatusResponse = VipStatusPayload
typealias PlaybackRequest = PlaybackAuthorizeRequest
typealias PlaybackResponse = PlaybackAuthorizationPayload
typealias DownloadRequest = DownloadAuthorizeRequest
typealias DownloadResponse = DownloadAuthorizationPayload
typealias UserProfileResponse = User
typealias PasswordVerifyRequest = ForgotPasswordVerifyRequest
typealias PasswordVerifyResponse = ForgotPasswordVerifyResponse
typealias PasswordResetRequest = ForgotPasswordResetRequest
typealias TokenRefreshRequest = RefreshTokenRequest
