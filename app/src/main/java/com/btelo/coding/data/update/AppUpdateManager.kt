package com.btelo.coding.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.btelo.coding.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int?,
    val releaseName: String,
    val releaseUrl: String,
    val apkName: String,
    val apkDownloadUrl: String,
    val apkSize: Long?
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    object NotAvailable : UpdateCheckResult()
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val latestReleaseUrl = "https://api.github.com/repos/feihu1991/btelo-coding-v2/releases/latest"

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestReleaseUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "BTELO-Coding/${BuildConfig.VERSION_NAME}")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext UpdateCheckResult.NotAvailable
            if (!response.isSuccessful) error("GitHub release check failed: HTTP ${response.code}")

            val body = response.body?.string().orEmpty()
            val release = gson.fromJson(body, JsonObject::class.java)
            val tagName = release.get("tag_name")?.asString.orEmpty()
            val versionName = tagName.removePrefix("v")
            if (versionName.isBlank() || !isNewerVersion(versionName, BuildConfig.VERSION_NAME)) {
                return@withContext UpdateCheckResult.NotAvailable
            }

            val assets = release.getAsJsonArray("assets") ?: return@withContext UpdateCheckResult.NotAvailable
            val apkAsset = assets.firstOrNull { asset ->
                asset.asJsonObject.get("name")?.asString?.endsWith(".apk", ignoreCase = true) == true
            }?.asJsonObject ?: return@withContext UpdateCheckResult.NotAvailable

            UpdateCheckResult.Available(
                UpdateInfo(
                    versionName = versionName,
                    versionCode = null,
                    releaseName = release.get("name")?.asString ?: tagName,
                    releaseUrl = release.get("html_url")?.asString.orEmpty(),
                    apkName = apkAsset.get("name")?.asString ?: "btelo-update.apk",
                    apkDownloadUrl = apkAsset.get("browser_download_url")?.asString.orEmpty(),
                    apkSize = apkAsset.get("size")?.asLong
                )
            )
        }
    }

    suspend fun downloadApk(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(info.apkDownloadUrl)
            .header("User-Agent", "BTELO-Coding/${BuildConfig.VERSION_NAME}")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("APK download failed: HTTP ${response.code}")
            val body = response.body ?: error("Empty APK response")
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, info.apkName.ifBlank { "btelo-${info.versionName}.apk" })

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val total = body.contentLength().takeIf { it > 0 } ?: info.apkSize ?: -1L
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        if (total > 0) onProgress(readTotal.toFloat() / total.toFloat())
                    }
                }
            }
            onProgress(1f)
            apkFile
        }
    }

    fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val max = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until max) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }
}
