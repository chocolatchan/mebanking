package com.mebanking.app.ui.ekyc

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mebanking.app.ui.theme.PastelPurpleLight
import com.mebanking.app.ui.theme.PastelPurplePrimary

/**
 * Interactive Live Camera Viewfinder and Face Verification Dialog.
 *
 * Implements:
 * 1. Live CameraX viewfinder preview with front/back camera support.
 * 2. Fallback gracefully for tests/emulators if camera hardware is unavailable.
 * 3. Animated scanning beam & oval face alignment guide.
 * 4. Captures standardized biometric frame bytes for verification.
 */
@Composable
fun FaceCaptureDialog(
    title: String = "Xác thực khuôn mặt",
    subtitle: String = "Vui lòng giữ thẳng khuôn mặt trong khung hình, đảm bảo đủ ánh sáng",
    isProcessing: Boolean = false,
    errorMessage: String? = null,
    onFrameCaptured: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraUnavailable by remember { mutableStateOf(false) }
    var liveFaceState by remember { mutableStateOf(FaceDetectionLiveState.INITIALIZING) }
    var latestFaceBytes by remember { mutableStateOf<ByteArray?>(null) }
    var localWarning by remember { mutableStateOf<String?>(null) }

    val faceAnalyzer = remember {
        FaceFrameAnalyzer { status, bytes ->
            liveFaceState = status
            if (status == FaceDetectionLiveState.READY && bytes != null) {
                latestFaceBytes = bytes
                localWarning = null
            } else if (status != FaceDetectionLiveState.READY) {
                latestFaceBytes = null
            }
        }
    }

    val submitCapture: () -> Unit = {
        when {
            !hasCameraPermission -> {
                localWarning = "Cần cấp quyền Máy ảnh để xác thực khuôn mặt."
                onFrameCaptured("permission_denied".toByteArray(Charsets.ISO_8859_1))
            }
            cameraUnavailable -> {
                localWarning = "Camera trên thiết bị không khả dụng."
                onFrameCaptured(ByteArray(0))
            }
            liveFaceState == FaceDetectionLiveState.MULTIPLE_FACES -> {
                localWarning = "Phát hiện nhiều khuôn mặt. Vui lòng chỉ giữ 1 khuôn mặt."
                onFrameCaptured("multiple_faces".toByteArray(Charsets.ISO_8859_1))
            }
            liveFaceState == FaceDetectionLiveState.LOW_LIGHT -> {
                localWarning = "Ánh sáng quá yếu. Vui lòng di chuyển tới nơi đủ sáng."
                onFrameCaptured("low_light".toByteArray(Charsets.ISO_8859_1))
            }
            liveFaceState != FaceDetectionLiveState.READY || latestFaceBytes == null -> {
                localWarning = "Không tìm thấy khuôn mặt trong khung hình. Vui lòng đưa mặt vào khung oval."
                onFrameCaptured(ByteArray(0))
            }
            else -> {
                localWarning = null
                onFrameCaptured(latestFaceBytes!!)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            faceAnalyzer.close()
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                } else {
                    cameraProviderFuture.addListener({
                        try {
                            cameraProviderFuture.get().unbindAll()
                        } catch (_: Throwable) {
                            // Ignore if provider failed or already released
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            } catch (_: Throwable) {
                // Ignore if provider not initialized
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLine",
    )

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Viewfinder container
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1C1E))
                        .border(2.dp, PastelPurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasCameraPermission && !cameraUnavailable) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                                try {
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            if (
                                                lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED &&
                                                ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }
                                                val cameraSelector = when {
                                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                                    else -> null
                                                }
                                                if (cameraSelector != null) {
                                                    val imageAnalysis = ImageAnalysis.Builder()
                                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                        .build()
                                                        .also {
                                                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx), faceAnalyzer)
                                                        }
                                                    cameraProvider.unbindAll()
                                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                                                } else {
                                                    cameraUnavailable = true
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            cameraUnavailable = true
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                } catch (_: Throwable) {
                                    cameraUnavailable = true
                                }
                                previewView
                            },
                        )
                    } else if (hasCameraPermission && cameraUnavailable) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = PastelPurpleLight,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera không khả dụng\nChạm nút chụp để xác thực",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cần quyền Máy ảnh để nhận diện",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelPurplePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Cấp quyền", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Throwable) {}
                                    },
                                ) {
                                    Text("Cài đặt", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Face guide oval overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ovalWidth = size.width * 0.70f
                        val ovalHeight = size.height * 0.85f
                        val topLeft = Offset(
                            x = (size.width - ovalWidth) / 2f,
                            y = (size.height - ovalHeight) / 2f,
                        )

                        // Outer oval boundary
                        drawOval(
                            color = PastelPurpleLight,
                            topLeft = topLeft,
                            size = Size(ovalWidth, ovalHeight),
                            style = Stroke(width = 3.dp.toPx()),
                        )

                        // Animated scanning beam
                        val currentScanY = size.height * scanLineY
                        drawLine(
                            color = PastelPurpleLight.copy(alpha = 0.55f),
                            start = Offset(x = topLeft.x, y = currentScanY),
                            end = Offset(x = topLeft.x + ovalWidth, y = currentScanY),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }

                    // Viewfinder status chips
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            color = Color(0xCC000000),
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            val (statusText, statusColor) = when {
                                !hasCameraPermission -> "● Chưa có quyền Camera" to Color(0xFFFFB74D)
                                cameraUnavailable -> "● Camera không khả dụng" to Color(0xFFFFB74D)
                                liveFaceState == FaceDetectionLiveState.READY -> "● Đã nhận diện khuôn mặt" to Color(0xFF81C784)
                                liveFaceState == FaceDetectionLiveState.MULTIPLE_FACES -> "● Phát hiện nhiều khuôn mặt" to Color(0xFFE57373)
                                liveFaceState == FaceDetectionLiveState.LOW_LIGHT -> "● Ánh sáng quá yếu" to Color(0xFFFFB74D)
                                liveFaceState == FaceDetectionLiveState.NO_FACE -> "● Không tìm thấy khuôn mặt" to Color(0xFFE57373)
                                else -> "● Đang quét khuôn mặt..." to PastelPurpleLight
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }

                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        Surface(
                            color = Color(0xCC000000),
                            shape = CircleShape,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Text(
                                text = if (liveFaceState == FaceDetectionLiveState.READY) {
                                    "Ánh sáng: Tốt | Sẵn sàng chụp"
                                } else {
                                    "Đưa 1 khuôn mặt vào khung oval"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera Shutter Trigger Action
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isProcessing -> Color.Gray
                                liveFaceState == FaceDetectionLiveState.READY -> PastelPurplePrimary
                                else -> MaterialTheme.colorScheme.outline
                            },
                        )
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isProcessing) {
                            submitCapture()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Chụp ảnh khuôn mặt",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isProcessing) "Đang xác thực khuôn mặt..." else "Chạm vào nút để chụp ảnh",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val displayError = localWarning ?: errorMessage
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = displayError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isProcessing) {
                        submitCapture()
                    }
                },
                enabled = !isProcessing,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PastelPurplePrimary),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang xử lý...")
                } else {
                    Text("Chụp & Xác thực", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Hủy")
            }
        },
    )
}
