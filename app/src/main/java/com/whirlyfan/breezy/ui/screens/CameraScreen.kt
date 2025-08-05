package com.whirlyfan.breezy.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.whirlyfan.breezy.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.text.get
import kotlin.toString

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Request camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Handle permission denied
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Square camera preview in the center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val previewView = PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER // Crop to fill the square
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))

                    previewView
                }
            )
        }

        // Close button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Capture button
        Button(
            onClick = {
                isLoading = true
                takePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    executor = ContextCompat.getMainExecutor(context),
                    onPhotoCaptured = { uri ->
                        isLoading = false
                        val encodedUri = java.net.URLEncoder.encode(uri.toString(), "UTF-8")
                        navController.navigate("createPost/$encodedUri")
                    },
                    onError = { error ->
                        isLoading = false
                        Log.e("CameraScreen", "Photo capture failed: $error")
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(64.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: Executor,
    onPhotoCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val imageCapture = imageCapture ?: return

    // Create a temporary file for the image
    val photoFile = File(
        context.cacheDir,
        "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = Uri.fromFile(photoFile)
                onPhotoCaptured(uri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Unknown error")
            }
        }
    )
}