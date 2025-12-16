package com.melongamesinc.letterlyapp_testwork.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.melongamesinc.letterlyapp_testwork.data.UploadResultStore
import com.melongamesinc.letterlyapp_testwork.data.UploadWorker
import com.melongamesinc.letterlyapp_testwork.data.provideUploadDataStore
import com.melongamesinc.letterlyapp_testwork.utils.FileUtils
import com.melongamesinc.letterlyapp_testwork.utils.FileValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class UiState {
    data object Idle : UiState()
    data class Error(val message: String) : UiState()
    data class Loading(val status: String) : UiState()
    data class Uploading(val workId: UUID) : UiState()
    data class Result(val json: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val store = UploadResultStore(appContext)
    private val workManager = WorkManager.getInstance(appContext)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentUploadWorkId: UUID? = null
    private var workObserverJob: Job? = null

    init {
        observePersistedResult()
    }
    private fun processFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading("Validating audio file...")

            val validationResult = withContext(Dispatchers.IO) {
                FileUtils.validateAndCacheFile(appContext, uri)
            }

            when (validationResult) {
                is FileValidationResult.Error -> {
                    _uiState.value = UiState.Error(validationResult.message)
                }

                is FileValidationResult.Success -> {
                    startUpload(validationResult.data)
                }
            }
        }
    }

    private fun startUpload(file: File) {
        workObserverJob?.cancel()

        val workId = UploadWorker.enqueue(appContext, file.absolutePath)
        currentUploadWorkId = workId

        _uiState.value = UiState.Loading("Upload scheduled...")

        workObserverJob = viewModelScope.launch {
            workManager
                .getWorkInfoByIdFlow(workId)
                .filterNotNull()
                .distinctUntilChangedBy { it.state }
                .collect { info ->
                    when (info.state) {
                        WorkInfo.State.ENQUEUED -> {
                            _uiState.value = UiState.Loading("Waiting for network...")
                        }

                        WorkInfo.State.RUNNING -> {
                            _uiState.value = UiState.Loading("Uploading...")
                        }

                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.FAILED -> {
                            cancel()
                        }

                        else -> Unit
                    }
                }
        }
    }

    private fun observePersistedResult() {
        viewModelScope.launch {
            store.getResult().collect { resultJson ->
                if (resultJson != null) {
                    _uiState.value = UiState.Result(resultJson)
                }
            }
        }
    }

    fun processFileFromPicker(uri: Uri) {
        processFile(uri)
    }

    fun reset() {
        viewModelScope.launch {
            store.clear()
            _uiState.value = UiState.Idle
        }
    }
}