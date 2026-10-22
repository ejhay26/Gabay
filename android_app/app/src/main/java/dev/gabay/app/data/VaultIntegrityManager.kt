package dev.gabay.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class VaultFileItem(
    val filename: String,
    val displayName: String,
    val description: String,
    val expectedSizeBytes: Long,
    val downloadUrl: String,
    val currentSizeBytes: Long = 0L,
    val isPresent: Boolean = false
)

data class VaultStatus(
    val isReady: Boolean,
    val files: List<VaultFileItem>,
    val missingFiles: List<VaultFileItem>,
    val totalMissingBytes: Long
)

object VaultIntegrityManager {

    const val VAULT_DIRECTORY = "/sdcard/Documents/Gabay"
    const val GITHUB_RELEASE_BASE = "https://github.com/ejhay26/Gabay/releases/download/v0.1.0-vault"

    val REQUIRED_VAULT_FILES = listOf(
        VaultFileItem(
            filename = "ph.gabay",
            displayName = "Canonical Road Graph (ph.gabay)",
            description = "OpenStreetMap CSR directed road network graph with zero-threshold connectivity.",
            expectedSizeBytes = 273_840_000L, // ~273.8 MB
            downloadUrl = "$GITHUB_RELEASE_BASE/ph.gabay"
        ),
        VaultFileItem(
            filename = "pois_fts.sqlite",
            displayName = "Spatial Search Index (pois_fts.sqlite)",
            description = "Nationwide SQLite FTS5 database of gas stations, hospitals, barangays, and streets.",
            expectedSizeBytes = 106_100_000L, // ~106.1 MB
            downloadUrl = "$GITHUB_RELEASE_BASE/pois_fts.sqlite"
        ),
        VaultFileItem(
            filename = "philippines_vector.pmtiles",
            displayName = "Offline Vector Map (philippines_vector.pmtiles)",
            description = "Protomaps offline vector tile archive for MapLibre streets, coastlines, and topography.",
            expectedSizeBytes = 75_400_000L, // ~75.4 MB
            downloadUrl = "$GITHUB_RELEASE_BASE/philippines_vector.pmtiles"
        )
    )

    /**
     * Inspects vault directory and returns health status
     */
    fun checkVault(): VaultStatus {
        val dir = File(VAULT_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val checkedFiles = REQUIRED_VAULT_FILES.map { req ->
            val f = File(dir, req.filename)
            val exists = f.exists() && f.length() > 1_000_000L // At least 1 MB
            req.copy(
                currentSizeBytes = if (f.exists()) f.length() else 0L,
                isPresent = exists
            )
        }

        val missing = checkedFiles.filter { !it.isPresent }
        val missingBytes = missing.sumOf { it.expectedSizeBytes }

        return VaultStatus(
            isReady = missing.isEmpty(),
            files = checkedFiles,
            missingFiles = missing,
            totalMissingBytes = missingBytes
        )
    }

    /**
     * Download streaming implementation with progress callbacks
     */
    suspend fun streamDownloadFile(
        fileItem: VaultFileItem,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val destDir = File(VAULT_DIRECTORY)
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, fileItem.filename)
        val tempFile = File(destDir, "${fileItem.filename}.download")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(fileItem.downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext false
            }

            val totalLength = connection.contentLengthLong.let { if (it > 0) it else fileItem.expectedSizeBytes }
            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalLength)
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 1_000_000L) {
                if (destFile.exists()) destFile.delete()
                tempFile.renameTo(destFile)
                return@withContext true
            } else {
                tempFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }
}
