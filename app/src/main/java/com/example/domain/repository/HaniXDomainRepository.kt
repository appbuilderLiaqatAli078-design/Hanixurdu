package com.example.domain.repository

import com.example.data.model.AuthSessionPayload
import com.example.data.model.CategoryItem
import com.example.data.model.Episode
import com.example.data.model.HomePayload
import com.example.data.model.Movie
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.RemoteDataState
import com.example.data.model.SearchPayload
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.VipStatusPayload

/**
 * Clean Architecture Domain Repository Contract for HaniX Urdu (Part 4).
 */
interface HaniXDomainRepository {
    suspend fun login(email: String, password: String): RemoteDataState<AuthSessionPayload>
    suspend fun register(name: String, email: String, password: String): RemoteDataState<AuthSessionPayload>
    suspend fun logout()
    suspend fun fetchHomeDashboard(): RemoteDataState<HomePayload>
    suspend fun fetchMovies(category: String? = null): RemoteDataState<List<Movie>>
    suspend fun fetchMovieDetails(movieId: String): RemoteDataState<Movie>
    suspend fun fetchCategories(): RemoteDataState<List<CategoryItem>>
    suspend fun fetchSeries(category: String? = null): RemoteDataState<List<Series>>
    suspend fun fetchSeriesDetails(seriesId: String): RemoteDataState<Series>
    suspend fun fetchSeriesSeasons(seriesId: String): RemoteDataState<List<Season>>
    suspend fun fetchSeasonEpisodes(seasonId: String): RemoteDataState<List<Episode>>
    suspend fun searchContent(query: String): RemoteDataState<SearchPayload>
    suspend fun fetchVipStatus(): RemoteDataState<VipStatusPayload>
    suspend fun requestPlaybackAuthorization(contentType: String, contentId: String): RemoteDataState<PlaybackAuthorizationPayload>
}
