package com.btelo.coding.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientEnd
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.RedError
import com.btelo.coding.ui.theme.TextOnBubble
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onConnected: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var showQrScanner by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }

    // Auto-navigate when connected with session
    LaunchedEffect(uiState.isConnected, uiState.sessionId) {
        if (uiState.isConnected && uiState.sessionId != null) {
            onConnected(uiState.sessionId!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Main connection page
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Server status card (shown when connected)
            if (uiState.isConnected) {
                ServerStatusCard(
                    serverAddress = uiState.serverAddress,
                    claudeVersion = uiState.claudeVersion,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Decorative logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BubbleGradientStart.copy(alpha = 0.2f),
                                BubbleGradientEnd.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BubbleGradientStart, BubbleGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C",
                        color = TextOnBubble,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "开始对话",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (uiState.isConnected) "连接已就绪" else "扫码或输入地址连接服务器",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            if (!uiState.isConnected) {
                // Scan QR button
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            showQrScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TextOnBubble
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "扫描二维码",
                        color = TextOnBubble,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual input button
                OutlinedButton(
                    onClick = { showManualInput = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "手动输入地址",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = RedError,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = RedError
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (uiState.isConnecting) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = BubbleGradientStart,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "连接中...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // QR Scanner overlay
        AnimatedVisibility(
            visible = showQrScanner,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            QrScannerOverlay(
                hasPermission = hasCameraPermission,
                onQrCodeDetected = { qrContent ->
                    showQrScanner = false
                    viewModel.onQrCodeScanned(qrContent)
                },
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onDismiss = { showQrScanner = false }
            )
        }

        // Manual input dialog
        AnimatedVisibility(
            visible = showManualInput,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ManualInputOverlay(
                onConnect = { url ->
                    showManualInput = false
                    viewModel.connectManually(url)
                },
                onDismiss = { showManualInput = false }
            )
        }
    }
}

@Composable
private fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .background(AppBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private val BorderSubtle = TextTertiary

@Composable
fun ServerStatusCard(
    serverAddress: String?,
    claudeVersion: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已连接",
                    color = GreenSuccess,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = serverAddress ?: "",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            claudeVersion?.let { version ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Claude Code v$version",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QrScannerOverlay(
    hasPermission: Boolean,
    onQrCodeDetected: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        if (hasPermission) {
            QrCameraScanner(
                onQrCodeDetected = onQrCodeDetected,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "扫描二维码",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Scanning frame
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, BubbleGradientStart, RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "将二维码放入框内，即可自动扫描",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "手动输入地址",
                    color = BubbleGradientStart,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onDismiss() }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            // No camera permission
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "需要相机权限才能扫描二维码",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart)
                ) {
                    Text("授予权限", color = TextOnBubble)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "返回手动输入",
                    color = BubbleGradientStart,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
fun ManualInputOverlay(
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "输入服务器地址",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "http://192.168.1.100:8080 或 btelo://...",
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BubbleGradientStart,
                        unfocusedBorderColor = TextTertiary,
                        cursorColor = BubbleGradientStart
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onConnect(inputText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleGradientStart),
                    enabled = inputText.isNotBlank()
                ) {
                    Text("连接", color = TextOnBubble, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun QrCameraScanner(
    onQrCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var lastScanned by remember { mutableStateOf("") }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImage(imageProxy, barcodeScanner) { qrContent ->
                                if (qrContent != null && qrContent != lastScanned) {
                                    lastScanned = qrContent
                                    onQrCodeDetected(qrContent)
                                }
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun processImage(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onResult: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && barcode.valueType == Barcode.TYPE_URL ||
                        rawValue != null && rawValue.startsWith("btelo://")
                    ) {
                        onResult(rawValue)
                        break
                    } else if (rawValue != null) {
                        onResult(rawValue)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
