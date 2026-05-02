package com.btelo.coding.data.remote

import com.google.gson.annotations.SerializedName

data class AppUpdateInfo(
    val success: Boolean,
    @field:SerializedName("update_available") val updateAvailable: Boolean,
    @field:SerializedName("apk_available") val apkAvailable: Boolean = false,
    @field:SerializedName("version_code") val versionCode: Int,
    @field:SerializedName("version_name") val versionName: String,
    @field:SerializedName("file_name") val fileName: String? = null,
    @field:SerializedName("size_bytes") val sizeBytes: Long = 0L,
    val sha256: String? = null,
    @field:SerializedName("built_at") val builtAt: String? = null,
    @field:SerializedName("download_url") val downloadUrl: String? = null
)
