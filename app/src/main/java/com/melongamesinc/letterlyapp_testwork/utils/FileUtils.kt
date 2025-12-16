package com.melongamesinc.letterlyapp_testwork.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val MAX_SIZE_BYTES = 30 * 1024 * 1024
    private const val MAX_DURATION_MS = 90 * 60 * 1000L

    fun validateAndCacheFile(context: Context, uri: Uri): FileValidationResult<File> {
        val contentResolver = context.contentResolver

        val size = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        } ?: return FileValidationResult.Error("Cannot determine file size.")

        if (size > MAX_SIZE_BYTES) {
            return FileValidationResult.Error("Audiofile exceeds 30 mb")
        }

        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            return FileValidationResult.Error("Cannot read audio file metadata.")
        } finally {
            retriever.release()
        }

        if (durationMs > MAX_DURATION_MS) {
            return FileValidationResult.Error("Audiofile duration exceeds 90 minutes")
        }

        return try {
            val cacheFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileValidationResult.Success(cacheFile)
        } catch (e: Exception) {
            FileValidationResult.Error("Failed to cache file: ${e.message}")
        }
    }
}

sealed class FileValidationResult<out T> {
    data class Success<out T>(val data: T) : FileValidationResult<T>()
    data class Error(val message: String) : FileValidationResult<Nothing>()
}