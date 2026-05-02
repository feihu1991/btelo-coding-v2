package com.btelo.coding.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `relay update response should deserialize snake case fields`() {
        val json = """
            {
              "success": true,
              "update_available": true,
              "apk_available": true,
              "version_code": 4,
              "version_name": "1.0.3",
              "file_name": "Yami-Coding-1.0.3.apk",
              "size_bytes": 41000000,
              "download_url": "https://example.test/app/apk"
            }
        """.trimIndent()

        val response = Gson().fromJson(json, RelayUpdateResponse::class.java)

        assertTrue(response.success)
        assertTrue(response.updateAvailable)
        assertTrue(response.apkAvailable)
        assertEquals(4, response.versionCode)
        assertEquals("1.0.3", response.versionName)
        assertEquals("Yami-Coding-1.0.3.apk", response.fileName)
        assertEquals(41000000L, response.sizeBytes)
        assertEquals("https://example.test/app/apk", response.downloadUrl)
    }
}
