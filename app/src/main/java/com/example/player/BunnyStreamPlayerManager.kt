package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.PlaybackAuthorizationPayload
import com.example.data.model.PlaybackServerDto
import com.example.data.security.HaniXSecurityShield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BunnyPlaybackState {
    data object Idle : BunnyPlaybackState
    data object AuthorizingWithBackend : BunnyPlaybackState
    data class ReadyToPlay(
        val sessionId: String,
        val authorizedHlsUrl: String,
        val membershipRequired: String,
        val tokenExpiresAtEpochSeconds: Long?,
        val playbackType: String = "hls", // "hls", "direct", or "embed"
        val selectedServerId: String = "main"
    ) : BunnyPlaybackState
    data class DeniedVipRequired(val reason: String) : BunnyPlaybackState
    data class ExpiredPlaybackToken(val reason: String) : BunnyPlaybackState
    data class VideoProcessing(val status: String) : BunnyPlaybackState
    data class BackendPendingIntegration(val endpoint: String, val details: String) : BunnyPlaybackState
    data class PlaybackFailure(val message: String) : BunnyPlaybackState
}

/**
 * Multi-Source & Multi-Server Video Playback Manager (`run_video.php` + Bunny Stream).
 *
 * Supports all 3 playback modes used across `run_video.php`:
 * 1. Bunny Stream signed HLS adaptive streams (`playback_type = "hls"` / `.m3u8`) via Media3 ExoPlayer
 * 2. Direct MP4/MKV streams (`video_path` with `playback_type = "direct"`) via Media3 ExoPlayer
 * 3. Embedded Provider Streams (`video_link` / `server1_link` / `server2_link` for YouTube, Rumble,
 *    Dailymotion, Google Drive `/preview`, or Server 2 iframe links with `playback_type = "embed"`)
 */
class BunnyStreamPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    private val _playbackState = MutableStateFlow<BunnyPlaybackState>(BunnyPlaybackState.Idle)
    val playbackState: StateFlow<BunnyPlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setAuthorizing() {
        _playbackState.value = BunnyPlaybackState.AuthorizingWithBackend
    }

    fun setBackendPending(endpoint: String, details: String) {
        releasePlayer()
        _playbackState.value = BunnyPlaybackState.BackendPendingIntegration(endpoint, details)
    }

    fun setError(message: String) {
        releasePlayer()
        _playbackState.value = BunnyPlaybackState.PlaybackFailure(message)
    }

    /**
     * Pure validation of backend playback authorization payload (`POST /playback/authorize`).
     */
    fun validateAuthorizationPayload(
        payload: PlaybackAuthorizationPayload,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): BunnyPlaybackState {
        val isVipContent = payload.requiresVip || payload.membershipRequired.equals("VIP", ignoreCase = true)
        val authorized = payload.resolvedIsAuthorized

        if (isVipContent && !authorized) {
            return BunnyPlaybackState.DeniedVipRequired(
                "VIP Membership is required to watch this content. Your current membership is FREE or your VIP access has expired."
            )
        }

        if (!authorized) {
            return BunnyPlaybackState.PlaybackFailure(
                "Playback authorization denied by backend for content ID ${payload.contentId.ifBlank { payload.contentIdSnake.orEmpty() }}."
            )
        }

        if (!payload.processingStatus.equals("READY", ignoreCase = true)) {
            return BunnyPlaybackState.VideoProcessing(payload.processingStatus)
        }

        val expiryEpoch = payload.resolvedExpiryEpochSeconds
        if (expiryEpoch != null && expiryEpoch <= nowEpochSeconds) {
            return BunnyPlaybackState.ExpiredPlaybackToken(
                "Short-lived playback authorization token has expired. Requesting fresh authorization is required."
            )
        }

        val streamUrl = payload.resolvedPlaybackUrl?.trim()
        if (streamUrl.isNullOrBlank() || !streamUrl.startsWith("https://", ignoreCase = true)) {
            return BunnyPlaybackState.PlaybackFailure(
                "Backend did not return a valid HTTPS playback URL."
            )
        }

        val resolvedType = classifyUrlAndType(streamUrl, payload.playbackType)

        return BunnyPlaybackState.ReadyToPlay(
            sessionId = payload.playbackSessionId.ifBlank { "session_${payload.contentId.ifBlank { payload.contentIdSnake.orEmpty() }}" },
            authorizedHlsUrl = streamUrl,
            membershipRequired = if (isVipContent) "VIP" else "FREE",
            tokenExpiresAtEpochSeconds = expiryEpoch,
            playbackType = resolvedType,
            selectedServerId = payload.selectedServer ?: "main"
        )
    }

    private fun classifyUrlAndType(url: String, declaredType: String): String {
        val lowerUrl = url.lowercase()
        val lowerType = declaredType.lowercase()
        if (lowerType == "webpage" || lowerUrl.contains("run_video.php")) {
            return "webpage"
        }
        if (lowerType == "embed" ||
            lowerUrl.contains("youtube.com/embed/") ||
            lowerUrl.contains("youtu.be/") ||
            lowerUrl.contains("rumble.com/embed/") ||
            lowerUrl.contains("dailymotion.com/embed/") ||
            lowerUrl.contains("player.vimeo.com/") ||
            lowerUrl.contains("drive.google.com/") ||
            lowerUrl.contains("/embed/") ||
            lowerUrl.contains("embed")
        ) {
            return "embed"
        }
        if (lowerType == "hls" || lowerUrl.contains(".m3u8")) {
            return "hls"
        }
        return lowerType.ifBlank { "direct" }
    }

    /**
     * Validates the PHP backend's `PlaybackAuthorizationPayload` and prepares Media3 ExoPlayer (for `hls`/`direct`)
     * or signals `ReadyToPlay(..., playbackType = "embed" | "webpage")` (for embedded YouTube/Drive/Rumble/Server 2/Web Player links).
     */
    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val newPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(HaniXSecurityShield.buildProtectedMedia3AudioAttributes(), true)
            .build()
        
        newPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
                _currentPositionMs.value = newPlayer.currentPosition.coerceAtLeast(0L)
                _durationMs.value = newPlayer.duration.coerceAtLeast(0L)
            }

            override fun onPlaybackStateChanged(state: Int) {
                _currentPositionMs.value = newPlayer.currentPosition.coerceAtLeast(0L)
                _durationMs.value = newPlayer.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: PlaybackException) {
                // Automatically fall back to the WebView HTML5 / Embed player if ExoPlayer fails
                val currentState = _playbackState.value
                if (currentState is BunnyPlaybackState.ReadyToPlay) {
                    releasePlayer()
                    _playbackState.value = currentState.copy(playbackType = "embed")
                }
            }
        })
        
        exoPlayer = newPlayer
        return newPlayer
    }

    /**
     * Validates the PHP backend's `PlaybackAuthorizationPayload` and prepares Media3 ExoPlayer (for `hls`/`direct`)
     * or signals `ReadyToPlay(..., playbackType = "embed" | "webpage")` (for embedded YouTube/Drive/Rumble/Server 2/Web Player links).
     */
    @OptIn(UnstableApi::class)
    fun initializeFromBackendAuthorization(
        payload: PlaybackAuthorizationPayload,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
        attachExoPlayer: Boolean = true
    ): ExoPlayer? {
        val validatedState = validateAuthorizationPayload(payload, nowEpochSeconds)
        if (validatedState !is BunnyPlaybackState.ReadyToPlay) {
            releasePlayer()
            _playbackState.value = validatedState
            return null
        }

        // We update the state but DO NOT release the player if we are going to reuse it
        _playbackState.value = validatedState

        // If the selected server is an iframe embed or Direct Web Player,
        // WebView handles rendering while ExoPlayer stays released.
        if (!attachExoPlayer ||
            validatedState.playbackType == "embed" ||
            validatedState.playbackType == "webpage"
        ) {
            releasePlayer()
            return null
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")

        if (!payload.customHeaders.isNullOrEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(payload.customHeaders)
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(validatedState.authorizedHlsUrl))
        val mediaSource = if (validatedState.playbackType == "hls") {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(httpDataSourceFactory)
                .createMediaSource(mediaItem)
        }

        HaniXSecurityShield.enforceAudioCaptureBlock(context)

        val player = getOrCreatePlayer()
        // Reusing the same instance: setMediaSource replaces current items.
        // We don't call stop() to avoid unnecessary surface clearing.
        player.setMediaSource(mediaSource)
        player.setPlaybackSpeed(_playbackSpeed.value)
        player.prepare()
        
        val resumeMs = payload.resumePositionMs ?: 0L
        if (resumeMs > 0L) {
            player.seekTo(resumeMs)
        }
        player.playWhenReady = true

        return player
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.5f)
        _playbackSpeed.value = clamped
        exoPlayer?.setPlaybackSpeed(clamped)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekByDeltaMs(deltaMs: Long) {
        val player = exoPlayer ?: return
        val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, dur)
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    fun seekToPositionMs(positionMs: Long) {
        val player = exoPlayer ?: return
        val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, dur)
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    /**
     * Switches between `Main Server` (`video_path`/`video_link`) and `Server 2` (`server1_link`) matching `run_video.php`.
     */
    fun switchServer(
        server: PlaybackServerDto,
        basePayload: PlaybackAuthorizationPayload,
        attachExoPlayer: Boolean = true,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ) {
        val updatedPayload = basePayload.copy(
            playbackType = server.type,
            playbackUrl = server.url,
            authorizedHlsUrl = server.url,
            selectedServer = server.id
        )
        initializeFromBackendAuthorization(
            payload = updatedPayload,
            nowEpochSeconds = nowEpochSeconds,
            attachExoPlayer = attachExoPlayer
        )
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun syncPlayerPosition(): Pair<Long, Long> {
        val player = exoPlayer ?: return 0L to 0L
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        _currentPositionMs.value = pos
        _durationMs.value = dur
        return pos to dur
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
    }
}
