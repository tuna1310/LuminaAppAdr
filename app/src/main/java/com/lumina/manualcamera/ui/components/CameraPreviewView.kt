package com.lumina.manualcamera.ui.components

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.lumina.manualcamera.camera.CameraController

@Composable
fun CameraPreviewView(
    cameraController: CameraController,
    modifier: Modifier = Modifier,
    onTapFocus: (x: Float, y: Float, width: Float, height: Float) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    AndroidView(
        factory = {
            cameraController.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView
            )

            // Bắt sự kiện Tap to focus
            previewView.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    onTapFocus(event.x, event.y, view.width.toFloat(), view.height.toFloat())
                    view.performClick()
                }
                true
            }

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}
