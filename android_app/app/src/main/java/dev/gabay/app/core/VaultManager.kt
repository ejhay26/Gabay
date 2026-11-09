package dev.gabay.app.core

import android.content.Context
import android.os.Environment
import java.io.File

data class VaultFile(
    val filename: String,
    val displayName: String,
    val description: String,
    val expectedSizeBytes: Long,
    val isPresent: Boolean,
    val currentSizeBytes: Long
)

data class VaultStatus(
    val directory: File,
    val isReady: Boolean,
    val hasStoragePermission: Boolean,
    val files: List<VaultFile>,
    val missingFiles: List<VaultFile>,
    val ggufModels: List<File>
)

object VaultManager {

    private val REQUIRED_FILES = listOf(
        Triple("ph.gabay", "Road Graph", 250_000_000L),
        Triple("pois_fts.sqlite", "Search Index", 20_000_000L),
        Triple("philippines_vector.pmtiles", "Vector Map Tiles", 50_000_000L)
    )

    /**
     * Finds the Gabay data directory across external storage locations.
     */
    fun resolveVaultDir(context: Context): File {
        // 1. Primary: /sdcard/Documents/Gabay or /storage/emulated/0/Documents/Gabay
        val publicDocs = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Gabay"
        )
        if (publicDocs.exists() && publicDocs.isDirectory) {
            return publicDocs
        }

        // 2. Direct path fallback
        val sdcard = File("/sdcard/Documents/Gabay")
        if (sdcard.exists() && sdcard.isDirectory) {
            return sdcard
        }

        // 3. App-specific external storage (always readable without MANAGE_EXTERNAL_STORAGE)
        val appSpecific = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Gabay")
        if (appSpecific.exists() && appSpecific.isDirectory) {
            return appSpecific
        }

        // Default to public docs directory
        if (!publicDocs.exists()) {
            publicDocs.mkdirs()
        }
        return publicDocs
    }

    /**
     * Checks vault files and returns complete health status.
     */
    fun checkVault(context: Context, hasStoragePermission: Boolean): VaultStatus {
        val dir = resolveVaultDir(context)

        val files = REQUIRED_FILES.map { (name, label, minSize) ->
            val file = File(dir, name)
            val exists = file.exists() && file.isFile && file.length() >= minSize
            VaultFile(
                filename = name,
                displayName = label,
                description = "Required data file: $name",
                expectedSizeBytes = minSize,
                isPresent = exists,
                currentSizeBytes = if (file.exists()) file.length() else 0L
            )
        }

        val missing = files.filter { !it.isPresent }

        val ggufModels = dir.listFiles { f -> f.extension.equals("gguf", ignoreCase = true) }
            ?.toList() ?: emptyList()

        return VaultStatus(
            directory = dir,
            isReady = missing.isEmpty(),
            hasStoragePermission = hasStoragePermission,
            files = files,
            missingFiles = missing,
            ggufModels = ggufModels
        )
    }
}
