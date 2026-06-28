package com.example.videostreamingapp.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videostreamingapp.permissions.CameraPermissionGate
import com.example.videostreamingapp.viewModels.CamViewModel
import com.example.videostreamingapp.viewModels.StreamingViewModel

@Composable
fun CameraPreviewScreen(
    viewModel: StreamingViewModel = viewModel(),
    viewModel1: CamViewModel = viewModel()
){
val lifecycleOwner= LocalLifecycleOwner.current
    val context = LocalContext.current
    val controller=remember  {CameraController(context,lifecycleOwner,viewModel1)}
    CameraPermissionGate {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {ctx->
                PreviewView(ctx).also{previewView->
                    controller.startCam(previewView){nv21,width,height->
                        viewModel.onFrameCaptured(nv21,width,height)
                    }
                }

            }

        )
    }
}