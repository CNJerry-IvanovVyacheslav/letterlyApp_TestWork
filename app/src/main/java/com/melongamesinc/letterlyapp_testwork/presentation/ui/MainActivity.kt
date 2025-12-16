package com.melongamesinc.letterlyapp_testwork.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.melongamesinc.letterlyapp_testwork.presentation.theme.LetterlyApp_TestWorkTheme
import com.melongamesinc.letterlyapp_testwork.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val selectAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processFileFromPicker(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { nonNullIntent ->
            handleIntentIfNeeded(nonNullIntent)
        }

        setContent {
            LetterlyApp_TestWorkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel, onLaunchPicker = ::launchAudioPicker)
                }
            }
        }
    }

    fun launchAudioPicker() {
        selectAudioLauncher.launch("audio/*")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentIfNeeded(intent)
    }

    private fun handleIntentIfNeeded(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            val extras = intent.extras
            val uri = extras?.getParcelable<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                viewModel.processFileFromPicker(uri)
            } else {
                println("No audio file attached in share intent")
            }
        }
    }
}