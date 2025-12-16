package com.melongamesinc.letterlyapp_testwork.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.melongamesinc.letterlyapp_testwork.utils.parseNoteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.UUID

class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        fun enqueue(context: Context, filePath: String): UUID {
            val uploadData = workDataOf(KEY_FILE_PATH to filePath)
            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(uploadData)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(uploadRequest)
            return uploadRequest.id
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val filePath = inputData.getString(KEY_FILE_PATH)
            ?: return@withContext Result.failure()

        val file = File(filePath)
        if (!file.exists()) return@withContext Result.failure()

        //val dataStore = provideUploadDataStore(applicationContext)
        val resultStore = UploadResultStore(applicationContext)

        val client = OkHttpClient()
        val mediaType = "audio/mpeg".toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mediaType)
            )
            .build()

        val request = Request.Builder()
            .url("https://api2.letterly.app/v3/notes/transcribe")
            .header(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTc2NTc5NzUzNCwianRpIjoiMDI3NTRlZTItZTM5Ny00MDhjLWIyNWQtZTViOGEwNWVhYjA3IiwidHlwZSI6ImFjY2VzcyIsInN1YiI6OTc0NiwibmJmIjoxNzY1Nzk3NTM0LCJjc3JmIjoiNWYxNzI0MjItZDVkMS00NGVmLThmZjktMWUwMDFlOGE2ZWQ5IiwiZXhwIjoxNzk3MzMzNTM0fQ.ugNOVbFgCCBBDDfYIHhgGFngFmPm26ZAgDCyTCUfX-Y"
            )
            .header("X-Device-Id", "12345")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string().orEmpty()
                val noteId = parseNoteId(json)

                resultStore.saveSuccess(noteId)
                Result.success()
            } else {
                resultStore.saveError("Backend responded with ${response.code}")
                Result.failure()
            }
        } catch (e: IOException) {
            resultStore.saveError("Network error: ${e.message}")
            Result.retry()
        } finally {
            file.delete()
        }
    }

}