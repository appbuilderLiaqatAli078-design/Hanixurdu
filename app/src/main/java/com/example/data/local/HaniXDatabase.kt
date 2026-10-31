package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Season
import com.example.data.model.Series
import kotlinx.coroutines.flow.Flow

// =========================================================================
// ROOM ENTITIES (Local Cache Only — Never Authority for VIP or Playback)
// =========================================================================

@Entity(tableName = "cached_movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val poster: String?,
    val category: String?,
    val isPremium: Boolean,
    val duration: String?,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomain() = Movie(
        id = id,
        title = title,
        description = description,
        poster = poster,
        category = category,
        isPremium = isPremium,
        duration = duration
    )
}

fun Movie.toEntity() = MovieEntity(
    id = resolvedId,
    title = title,
    description = description,
    poster = resolvedPoster,
    category = category,
    isPremium = resolvedIsVip,
    duration = duration
)

@Entity(tableName = "cached_series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val poster: String?,
    val category: String?,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomain() = Series(
        id = id,
        title = title,
        description = description,
        poster = poster,
        category = category
    )
}

fun Series.toEntity() = SeriesEntity(
    id = resolvedId,
    title = resolvedTitle,
    description = description,
    poster = resolvedPoster,
    category = category
)

@Entity(tableName = "cached_seasons")
data class SeasonEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonName: String?
) {
    fun toDomain() = Season(id = id, seriesId = seriesId, seasonName = seasonName)
}

fun Season.toEntity() = SeasonEntity(
    id = resolvedId,
    seriesId = resolvedSeriesKey,
    seasonName = resolvedSeasonName
)

@Entity(tableName = "cached_episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seasonId: String,
    val title: String,
    val description: String?,
    val poster: String?,
    val isPremium: Boolean,
    val duration: String?,
    val playbackAvailability: String?
) {
    fun toDomain() = Episode(
        id = id,
        seasonId = seasonId,
        title = title,
        description = description,
        poster = poster,
        isPremium = isPremium,
        duration = duration,
        playbackAvailability = playbackAvailability
    )
}

fun Episode.toEntity() = EpisodeEntity(
    id = id,
    seasonId = seasonId.ifBlank { seasonName ?: "1" },
    title = title,
    description = description,
    poster = resolvedPoster,
    isPremium = resolvedIsVip,
    duration = duration,
    playbackAvailability = playbackAvailability
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val historyKey: String, // "${contentType}_${contentId}"
    val contentId: String,
    val contentType: String, // "movie" or "episode"
    val title: String,
    val subtitle: String?,
    val poster: String?,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Stores metadata for app-private, Android Keystore AES-GCM encrypted downloads (VIP only).
 * Never persists unrestricted raw Bunny Stream video URLs.
 */
@Entity(tableName = "encrypted_downloads")
data class EncryptedDownloadEntity(
    @PrimaryKey val downloadId: String,
    val contentId: String,
    val contentType: String,
    val title: String,
    val poster: String?,
    val privateEncryptedFilePath: String,
    val encryptionIvBase64: String,
    val fileSizeBytes: Long,
    val licenseExpiresAtEpochSeconds: Long,
    val requiresVipEntitlement: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun isLicenseExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        return nowEpochSeconds >= licenseExpiresAtEpochSeconds
    }
}

@Entity(tableName = "app_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // "SYSTEM", "VIP", "CONTENT", "BACKEND_STATUS"
    val isRead: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

// =========================================================================
// ROOM DAO
// =========================================================================

@Dao
interface HaniXDao {

    // Movies Cache
    @Query("SELECT * FROM cached_movies ORDER BY cachedAtEpochMs DESC")
    fun observeCachedMovies(): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM cached_movies WHERE id = :movieId LIMIT 1")
    suspend fun getCachedMovieById(movieId: String): MovieEntity?

    // Series Cache
    @Query("SELECT * FROM cached_series ORDER BY cachedAtEpochMs DESC")
    fun observeCachedSeries(): Flow<List<SeriesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM cached_series WHERE id = :seriesId LIMIT 1")
    suspend fun getCachedSeriesById(seriesId: String): SeriesEntity?

    // Seasons & Episodes Cache
    @Query("SELECT * FROM cached_seasons WHERE seriesId = :seriesId")
    suspend fun getCachedSeasonsForSeries(seriesId: String): List<SeasonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonEntity>)

    @Query("SELECT * FROM cached_episodes WHERE seasonId = :seasonId")
    suspend fun getCachedEpisodesForSeason(seasonId: String): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    // Watch History & Continue Watching
    @Query("SELECT * FROM watch_history ORDER BY updatedAtEpochMs DESC")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE isCompleted = 0 AND positionMs > 0 ORDER BY updatedAtEpochMs DESC")
    fun observeContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE historyKey = :historyKey")
    suspend fun deleteWatchHistoryEntry(historyKey: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()

    // Encrypted Downloads
    @Query("SELECT * FROM encrypted_downloads ORDER BY createdAtEpochMs DESC")
    fun observeEncryptedDownloads(): Flow<List<EncryptedDownloadEntity>>

    @Query("SELECT * FROM encrypted_downloads WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getEncryptedDownloadById(downloadId: String): EncryptedDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEncryptedDownload(download: EncryptedDownloadEntity)

    @Query("DELETE FROM encrypted_downloads WHERE downloadId = :downloadId")
    suspend fun deleteEncryptedDownloadById(downloadId: String)

    // Notifications
    @Query("SELECT * FROM app_notifications ORDER BY createdAtEpochMs DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()

    @Query("SELECT COUNT(*) FROM app_notifications")
    suspend fun getNotificationCount(): Int
}

@Database(
    entities = [
        MovieEntity::class,
        SeriesEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        WatchHistoryEntity::class,
        EncryptedDownloadEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HaniXDatabase : RoomDatabase() {
    abstract fun hanixDao(): HaniXDao

    companion object {
        @Volatile
        private var INSTANCE: HaniXDatabase? = null

        fun getInstance(context: Context): HaniXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HaniXDatabase::class.java,
                    "hanix_urdu_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
