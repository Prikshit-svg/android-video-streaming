package com.example.videostreamingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videostreamingapp.camera.CameraPreviewScreen
import com.example.videostreamingapp.ui.theme.VideoStreamingAppTheme
import com.example.videostreamingapp.viewModels.StreamingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoStreamingAppTheme {
                val streamingViewModel: StreamingViewModel = viewModel()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera preview fills the entire screen
                    CameraPreviewScreen(viewModel = streamingViewModel)

                    // Streaming controls overlaid at the bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        StreamingScreen(viewModel = streamingViewModel)
                    }
                }
            }
        }
    }
}
