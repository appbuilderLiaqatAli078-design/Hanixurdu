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
import com.example.data.model.PlaybackAuthorizationPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BunnyPlaybackState {
    data object Idle : BunnyPlaybackState
    data object AuthorizingWithBackend : BunnyPlaybackState
    data class ReadyToPlay(
        val sessionId: String,
        val authorizedHlsUrl: String,
        val tokenExpiresAtEpochSeconds: Long?
    ) : BunnyPlaybackState
    data class DeniedVipRequired(val reason: String) : BunnyPlaybackState
    data class VideoProcessing(val status: String) : BunnyPlaybackState
    data class BackendPendingIntegration(val endpoint: String, val details: String) : BunnyPlaybackState
    data class PlaybackFailure(val message: String) : BunnyPlaybackState
}

/**
 * Bunny Stream HLS Adaptive Playback Manager built on AndroidX Media3 ExoPlayer.
 *
 * Security & Architecture Compliance (Sections 3, 5, 8, 10):
 * - Never embeds Bunny Stream API keys or library admin secrets inside the APK.
 * - Never invents a fake or hardcoded Bunny playback URL.
 * - Exclusively consumes short-lived signed HLS URLs returned by `POST /playback/authorize`
 *   on the Hostinger PHP backend after user authentication and VIP permission checks.
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
     * Validates the PHP backend's `PlaybackAuthorizationPayload` and prepares the HLS adaptive media source.
     */
    @OptIn(UnstableApi::class)
    fun initializeFromBackendAuthorization(payload: PlaybackAuthorizationPayload): ExoPlayer? {
        if (payload.requiresVip && !payload.isAuthorized) {
            releasePlayer()
            _playbackState.value = BunnyPlaybackState.DeniedVipRequired(
                "VIP Membership is required to stream this title. Entitlement check enforced by backend."
            )
            return null
        }

        if (!payload.isAuthorized) {
            releasePlayer()
            _playbackState.value = BunnyPlaybackState.PlaybackFailure(
                "Playback authorization denied by backend for content ID ${payload.contentId}."
            )
            return null
        }

        if (!payload.processingStatus.equals("READY", ignoreCase = true)) {
            releasePlayer()
            _playbackState.value = BunnyPlaybackState.VideoProcessing(payload.processingStatus)
            return null
        }

        val hlsUrl = payload.authorizedHlsUrl?.trim()
        if (hlsUrl.isNullOrBlank() || !hlsUrl.startsWith("https://", ignoreCase = true)) {
            releasePlayer()
            _playbackState.value = BunnyPlaybackState.PlaybackFailure(
                "Backend did not return a valid HTTPS signed Bunny Stream HLS playlist (.m3u8)."
            )
            return null
        }

        val nowEpochSeconds = System.currentTimeMillis() / 1000L
        if (payload.tokenExpiresAtEpochSeconds != null && payload.tokenExpiresAtEpochSeconds <= nowEpochSeconds) {
            releasePlayer()
            _playbackState.value = BunnyPlaybackState.PlaybackFailure(
                "Short-lived Bunny Stream playback token has already expired. Please re-authorize."
            )
            return null
        }

        releasePlayer()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(false) // Enforce HTTPS-only redirects
            .setUserAgent("HaniXUrdu-Android-HLS/1.0")

        if (!payload.customHeaders.isNullOrEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(payload.customHeaders)
        }

        val hlsMediaSource = HlsMediaSource.Factory(httpDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(Uri.parse(hlsUrl)))

        val player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(hlsMediaSource)
            prepare()
            val resumeMs = payload.resumePositionMs ?: 0L
            if (resumeMs > 0L) {
                seekTo(resumeMs)
            }
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    _isPlaying.value = isPlayingNow
                    _currentPositionMs.value = currentPosition.coerceAtLeast(0L)
                    _durationMs.value = duration.coerceAtLeast(0L)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _currentPositionMs.value = currentPosition.coerceAtLeast(0L)
                    _durationMs.value = duration.coerceAtLeast(0L)
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackState.value = BunnyPlaybackState.PlaybackFailure(
                        "Bunny HLS Stream Error (${error.errorCodeName}): ${error.localizedMessage ?: "Playback interrupted"}"
                    )
                }
            })
        }

        exoPlayer = player
        _playbackState.value = BunnyPlaybackState.ReadyToPlay(
            sessionId = payload.playbackSessionId,
            authorizedHlsUrl = hlsUrl,
            tokenExpiresAtEpochSeconds = payload.tokenExpiresAtEpochSeconds
        )
        return player
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
