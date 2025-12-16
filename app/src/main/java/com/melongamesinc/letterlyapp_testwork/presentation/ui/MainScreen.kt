package com.melongamesinc.letterlyapp_testwork.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melongamesinc.letterlyapp_testwork.presentation.viewmodel.MainViewModel
import com.melongamesinc.letterlyapp_testwork.presentation.viewmodel.UiState

@Composable
fun MainScreen(viewModel: MainViewModel, onLaunchPicker: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is UiState.Idle -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Share audio file or select manually",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onLaunchPicker) {
                        Text("Select Audio File")
                    }
                }
            }

            is UiState.Loading -> {
                val status = (uiState as UiState.Loading).status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = status)
                }
            }

            is UiState.Error -> {
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Result -> {
                ResultView(json = (uiState as UiState.Result).json)
            }

            else -> Unit
        }
    }
}

@Composable
private fun ResultView(json: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Upload completed",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = json)
    }
}
