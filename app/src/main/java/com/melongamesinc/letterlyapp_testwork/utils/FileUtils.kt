package com.melongamesinc.letterlyapp_testwork.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    // 30 MB
    private const val MAX_SIZE_BYTES = 30 * 1024 * 1024
    // 90 minutes
    private const val MAX_DURATION_MS = 90 * 60 * 1000L

    fun validateAndCacheFile(context: Context, uri: Uri): Result<File> {
        val contentResolver = context.contentResolver

        val size = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        } ?: return Result.Error("Cannot determine file size.")

        if (size > MAX_SIZE_BYTES) {
            return Result.Error("Audiofile exceeds 30 mb")
        }

        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            return Result.Error("Cannot read audio file metadata.")
        } finally {
            retriever.release()
        }

        if (durationMs > MAX_DURATION_MS) {
            return Result.Error("Audiofile duration exceeds 90 minutes")
        }

        return try {
            val cacheFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.Success(cacheFile)
        } catch (e: Exception) {
            Result.Error("Failed to cache file: ${e.message}")
        }
    }
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}