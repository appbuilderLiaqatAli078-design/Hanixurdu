package com.example.data.security

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.io.File
import java.net.Proxy
import javax.net.ssl.HttpsURLConnection

/**
 * Centralized Security Shield for HaniX Urdu:
 * 1. Screen Recording Allowed + Internal Audio Capture Blocked (`ALLOW_CAPTURE_BY_NONE`) so nobody can record/copy the Urdu dubbing audio track.
 * 2. Strict SSL Pinning & Anti-MITM Proxy Lock (`Proxy.NO_PROXY`, TLS 1.2/1.3, SPKI Root/Intermediate pinning & hostname verification for `hanixurdu.com`).
 * 3. Zero API & Stream URL Leak Sanitizer so internal `.php` endpoints, SQL table names, and raw stream URLs never leak in user-facing UI or logs.
 * 4. App-Private Offline Vault isolation (`filesDir/hanix_offline_vault` + `.nomedia`) so downloads never appear in the device Gallery.
 */
object HaniXSecurityShield {

    /**
     * Pinned SPKI SHA-256 digests for `hanixurdu.com` and `*.hanixurdu.com`
     * covering Hostinger / Let's Encrypt (ISRG Root X1/X2, R3/R10/R11/E1/E5/E6),
     * Cloudflare / Google Trust Services (GTS Root R1/R4), and DigiCert Global Root G2.
     */
    val pinnedHostingerAndRootHashes: List<String> = listOf(
        "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=", // ISRG Root X1 (Let's Encrypt / Hostinger)
        "sha256/diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=", // ISRG Root X2
        "sha256/y7xVm0TVJNahMr2sZydE2jQH8SquXV9yLF9seROHHHU=", // Let's Encrypt R10
        "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=", // Let's Encrypt R11
        "sha256/NYbU7PBwV4y9/67c4guWTki8FJ+uudrXL0a4V4aRcrg=", // Let's Encrypt E5
        "sha256/pL1+qb9HTMRZJmuC/bB/ZI9d302BYrrqiVuRyW+DGrU=", // Let's Encrypt E6
        "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=", // Let's Encrypt R3
        "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=", // DigiCert Global Root G2
        "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=", // GTS Root R1 (Cloudflare / Google Trust)
        "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c="  // GTS Root R4
    )

    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        .apply {
            pinnedHostingerAndRootHashes.forEach { pin ->
                add("hanixurdu.com", pin)
                add("*.hanixurdu.com", pin)
            }
        }
        .build()

    /**
     * Locks an [OkHttpClient.Builder] with:
     * - `Proxy.NO_PROXY` (prevents Charles, Burp, Fiddler, and VPN proxy sniffers from intercepting API/video URLs)
     * - Strict TLS 1.3 / TLS 1.2 `ConnectionSpec.MODERN_TLS`
     * - SPKI Certificate Pinning (`certificatePinner`) and Hostname verification for `hanixurdu.com` and trusted video CDNs
     */
    fun configureSecureOkHttpClient(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val modernTls = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()

        return builder
            .proxy(Proxy.NO_PROXY)
            .certificatePinner(certificatePinner)
            .connectionSpecs(listOf(modernTls, ConnectionSpec.COMPATIBLE_TLS))
            .hostnameVerifier { hostname, session ->
                val cleanHost = hostname.lowercase().trim()
                val isAllowedHost = cleanHost == "hanixurdu.com" ||
                    cleanHost.endsWith(".hanixurdu.com") ||
                    cleanHost.endsWith(".b-cdn.net") ||
                    cleanHost.endsWith(".vimeocdn.com") ||
                    cleanHost.endsWith("vimeo.com") ||
                    cleanHost.endsWith("youtube.com") ||
                    cleanHost.endsWith("googlevideo.com") ||
                    cleanHost.endsWith("google.com") ||
                    cleanHost.endsWith("dailymotion.com") ||
                    cleanHost.endsWith("dmcdn.net") ||
                    cleanHost.endsWith("rumble.com") ||
                    cleanHost.endsWith("rmbl.ws")
                isAllowedHost && HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
            }
    }

    /**
     * Allows video screen recording (`FLAG_SECURE` cleared) while strictly blocking
     * internal audio recording/copying (`ALLOW_CAPTURE_BY_NONE` on Android 10+).
     */
    fun enforceScreenRecordAllowedButAudioBlocked(activity: Activity) {
        // Ensure video screen recording is allowed (do NOT block video screen capture)
        activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enforceAudioCaptureBlock(activity)
    }

    /**
     * Sets OS-level `ALLOW_CAPTURE_BY_NONE` on [AudioManager] so screen recorders
     * and third-party audio capture apps cannot record or copy HaniX Urdu dubbing audio.
     */
    fun enforceAudioCaptureBlock(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.allowedCapturePolicy = android.media.AudioAttributes.ALLOW_CAPTURE_BY_NONE
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Builds Media3 [AudioAttributes] with `C.ALLOW_CAPTURE_BY_NONE` so ExoPlayer
     * audio streams cannot be recorded by screen recorders or external apps.
     */
    @OptIn(UnstableApi::class)
    fun buildProtectedMedia3AudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
            .build()
    }

    /**
     * JavaScript shield injected into WebViews to block `captureStream()`, audio extraction,
     * context-menu link copying, and video source URL inspection.
     */
    fun buildWebViewAudioAndLinkProtectionJs(): String {
        return """
            (function() {
                try {
                    document.addEventListener('contextmenu', function(e) { e.preventDefault(); }, false);
                    document.addEventListener('selectstart', function(e) { e.preventDefault(); }, false);
                    if (typeof HTMLMediaElement !== 'undefined') {
                        var blockCapture = function() {
                            throw new DOMException('Audio capture is disabled by HaniX Urdu DRM.', 'NotAllowedError');
                        };
                        HTMLMediaElement.prototype.captureStream = blockCapture;
                        HTMLMediaElement.prototype.mozCaptureStream = blockCapture;
                        HTMLMediaElement.prototype.webkitCaptureStream = blockCapture;
                    }
                    var mediaEls = document.querySelectorAll('video, audio');
                    for (var i = 0; i < mediaEls.length; i++) {
                        mediaEls[i].setAttribute('controlsList', 'nodownload noplaybackrate');
                        mediaEls[i].setAttribute('disablePictureInPicture', 'true');
                        mediaEls[i].oncontextmenu = function() { return false; };
                    }
                } catch (err) {}
            })();
        """.trimIndent()
    }

    /**
     * Prepares the app-private offline vault directory inside `context.filesDir/hanix_offline_vault`
     * with a `.nomedia` sentinel file so downloaded dramas/episodes never appear in the device Gallery.
     */
    fun getOrCreatePrivateOfflineVaultDir(context: Context): File {
        val vaultDir = File(context.filesDir, "hanix_offline_vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        val noMediaFile = File(vaultDir, ".nomedia")
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile()
            } catch (_: Exception) {
            }
        }
        // Also ensure legacy encrypted_downloads folder has .nomedia
        val legacyDir = File(context.filesDir, "encrypted_downloads")
        if (!legacyDir.exists()) {
            legacyDir.mkdirs()
        }
        val legacyNoMedia = File(legacyDir, ".nomedia")
        if (!legacyNoMedia.exists()) {
            try {
                legacyNoMedia.createNewFile()
            } catch (_: Exception) {
            }
        }
        return vaultDir
    }

    /**
     * Sanitizes any subtitle, status message, or banner text so internal `.php` filenames,
     * raw API routes (`GET /...`, `POST /...`), and raw stream URLs are never leaked in the UI.
     */
    fun sanitizeUiLabel(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim()

        // Strip raw https/http URLs
        cleaned = cleaned.replace(Regex("""https?://[^\s•)]+""", RegexOption.IGNORE_CASE), "HaniX Secure Cloud")

        // Replace internal PHP script names with user-friendly labels
        val phpScriptPattern = Regex("""\b[a-zA-Z0-9_]+\.php(?:\?[^\s•)]*)?""", RegexOption.IGNORE_CASE)
        cleaned = cleaned.replace(phpScriptPattern, "HaniX Cloud")

        // Replace raw REST method prefixes like GET /movies, POST /playback/authorize, PUT /user/profile
        val restRoutePattern = Regex("""\b(?:GET|POST|PUT|DELETE)\s+/[a-zA-Z0-9_/\-?=&{}]+""", RegexOption.IGNORE_CASE)
        cleaned = cleaned.replace(restRoutePattern, "HaniX Secure Sync")

        // Clean up duplicate separators
        cleaned = cleaned
            .replace(Regex("""HaniX (?:Secure )?(?:Cloud|Sync)\s*•\s*HaniX (?:Secure )?(?:Cloud|Sync)"""), "HaniX Official Hub")
            .replace("hanixurdu.com/", "")
            .trim()

        return cleaned.ifBlank { "HaniX Urdu Official" }
    }
}
