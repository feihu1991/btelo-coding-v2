package com.btelo.coding.data.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.btelo.coding.BuildConfig
import com.btelo.coding.MainActivity
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// GitHub API response models
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("content_type") val contentType: String?
)

// Resolved update info for the app
data class AppUpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val fileName: String,
    val releaseNotes: String?
)

// Download state sealed class
sealed class DownloadState {
    data class Progress(val percent: Int) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

class AppUpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos"
        private const val UPDATES_DIR = "updates"
    }

    /**
     * Check GitHub Releases for updates.
     * Returns AppUpdateInfo if update available, null otherwise
     */
    suspend fun checkForUpdate(): AppUpdateInfo? {
        return try {
            val url = "$GITHUB_API_URL/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Logger.w(TAG, "GitHub API request failed: ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            val release = gson.fromJson(body, GitHubRelease::class.java)

            // Find APK asset
            val apkAsset = release.assets.find { asset ->
                asset.name.endsWith(".apk", ignoreCase = true)
            }

            if (apkAsset == null) {
                Logger.w(TAG, "No APK asset found in release ${release.tagName}")
                return null
            }

            // Check if this is a newer version
            val remoteVersion = release.tagName.removePrefix("v")
            val localVersion = BuildConfig.VERSION_NAME

            if (!isNewerVersion(remoteVersion, localVersion)) {
                Logger.d(TAG, "Local version $localVersion is up to date")
                return null
            }

            Logger.i(TAG, "Update available: $remoteVersion (current: $localVersion)")

            AppUpdateInfo(
                versionName = remoteVersion,
                downloadUrl = apkAsset.browserDownloadUrl,
                sizeBytes = apkAsset.size,
                fileName = apkAsset.name,
                releaseNotes = release.body
            )
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to check for update: ${e.message}")
            null
        }
    }

    /**
     * Download APK with progress tracking
     * Returns Flow of DownloadState
     */
    fun downloadApk(updateInfo: AppUpdateInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Progress(0))

        // Create updates directory in cache
        val updatesDir = File(context.cacheDir, UPDATES_DIR)
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }

        val outputFile = File(updatesDir, updateInfo.fileName)

        // Use OkHttpClient with longer timeout for downloads
        val downloadClient = okHttpClient.newBuilder()
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .build()

        val request = Request.Builder()
            .url(updateInfo.downloadUrl)
            .build()

        val response = downloadClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Download failed: ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    if (totalBytes > 0) {
                        val percent = (downloadedBytes * 100 / totalBytes).toInt()
                        emit(DownloadState.Progress(percent))
                    }
                }
                output.flush()
            }
        }

        Logger.i(TAG, "Download completed: ${outputFile.absolutePath}")
        emit(DownloadState.Completed(outputFile))
    }.catch { e ->
        Logger.e(TAG, "Download failed", e)
        emit(DownloadState.Failed(e.message ?: "Download failed"))
    }.flowOn(Dispatchers.IO)

    /**
     * Create intent to install APK
     */
    fun createInstallIntent(apkFile: File): Intent {
        val uri = getApkUri(apkFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get content URI for APK file via FileProvider
     */
    fun getApkUri(apkFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
    }

    /**
     * Check if app has permission to install unknown apps
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Create intent to request install permission
     */
    fun createInstallPermissionIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Create intent to restart the app after installation
     */
    fun createRestartIntent(): Intent {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
    }

    /**
     * Create PendingIntent for delayed restart (fallback)
     */
    fun createRestartPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Compare version strings
     * Returns true if remote version is newer than local
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        // Strip leading "v" if present
        val remoteClean = remote.removePrefix("v")
        val localClean = local.removePrefix("v")

        // Split by "-" to separate version from suffix (e.g., "1.0.1-mvp")
        val remoteParts = remoteClean.split("-", limit = 2)
        val localParts = localClean.split("-", limit = 2)

        // Compare version numbers
        val remoteSegments = remoteParts[0].split(".").map { it.toIntOrNull() ?: 0 }
        val localSegments = localParts[0].split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(remoteSegments.size, localSegments.size)) {
            val r = remoteSegments.getOrElse(i) { 0 }
            val l = localSegments.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        // If versions are equal, check suffix
        // Release without suffix is considered newer than with suffix (e.g., "1.0.1" > "1.0.1-mvp")
        val remoteSuffix = remoteParts.getOrNull(1) ?: ""
        val localSuffix = localParts.getOrNull(1) ?: ""

        return remoteSuffix.isEmpty() && localSuffix.isNotEmpty()
    }

    /**
     * Clean up old downloaded APKs
     */
    fun cleanupOldDownloads() {
        try {
            val updatesDir = File(context.cacheDir, UPDATES_DIR)
            if (updatesDir.exists()) {
                val files = updatesDir.listFiles()
                files?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to cleanup old downloads: ${e.message}")
        }
    }
}
