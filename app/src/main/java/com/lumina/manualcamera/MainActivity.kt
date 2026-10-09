package com.lumina.manualcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lumina.manualcamera.ui.CameraScreen
import com.lumina.manualcamera.ui.CameraViewModel
import com.lumina.manualcamera.ui.gallery.GalleryScreen
import com.lumina.manualcamera.ui.settings.SettingsScreen
import com.lumina.manualcamera.ui.theme.LuminaAmber
import com.lumina.manualcamera.ui.theme.LuminaTheme

enum class Screen {
    CAMERA,
    GALLERY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            LuminaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.CAMERA) }
                    var hasCameraPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
                    }

                    LaunchedEffect(Unit) {
                        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    // Xử lý nút Back của Android
                    if (currentScreen != Screen.CAMERA) {
                        BackHandler {
                            currentScreen = Screen.CAMERA
                        }
                    }

                    if (hasCameraPermission) {
                        when (currentScreen) {
                            Screen.CAMERA -> {
                                CameraScreen(
                                    viewModel = viewModel,
                                    onOpenGallery = { currentScreen = Screen.GALLERY },
                                    onOpenSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }
                            Screen.GALLERY -> {
                                GalleryScreen(
                                    onBackToCamera = { currentScreen = Screen.CAMERA }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.CAMERA }
                                )
                            }
                        }
                    } else {
                        PermissionDeniedView(
                            onRequestPermission = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedView(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.camera_permission_required),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = LuminaAmber)
            ) {
                Text(
                    text = stringResource(R.string.grant_permission),
                    color = Color.Black
                )
            }
        }
    }
}
