package com.example.videostreamingapp.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.videostreamingapp.viewModels.CamViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: CamViewModel
    ) {
    private var camera: Camera?=null
    private var imageAnalysis: ImageAnalysis?=null
    private var cameraExecutor: ExecutorService= Executors.newSingleThreadExecutor()

    fun startCam(
        previewView : PreviewView,//shows camera preview
        onFrame:(ByteArray,Int,Int)-> Unit//callback for each frame data.Signature:(ByteArray, width, height)
    ){
        val cameraProviderFuture= ProcessCameraProvider.getInstance(context)//it returns Future<CameraProvider> meaning:"I'll provide you the camera when it's ready"
        cameraProviderFuture.addListener({
            val cameraProvider=cameraProviderFuture.get()//This object manages:opening cameras,closing cameras,binding use cases

            val preview= Preview.Builder().build()//Creates preview pipeline for camera(Although nothing is Shown yet)
                .also { it.surfaceProvider=previewView.surfaceProvider }//Attach the preview to the surface

            imageAnalysis= ImageAnalysis.Builder()//Creates another pipeline. Unlike Preview,it doesn't display anything.It only gives access to image frames.
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->//represents the  frame data which contains details such as width,height,rotation,YUV planes,timestamp
                        val nv21=imageProxy.toNv21ByteArray()
                        onFrame(nv21,imageProxy.width,imageProxy.height)
                        imageProxy.close()
                    }
                }
            val cameraSelector=if(viewModel.selectedCam.value) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                camera=cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

    }
            catch (e: Exception){
                Log.e("Camera Controller", "bind failed", e)

            }

            },
           ContextCompat.getMainExecutor(context))


    }
    fun stopCamera(){
        ProcessCameraProvider.getInstance(context).get().unbindAll()
        cameraExecutor.shutdown()
    }
}
