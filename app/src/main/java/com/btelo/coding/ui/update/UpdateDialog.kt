package com.btelo.coding.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btelo.coding.ui.theme.AppBackground
import com.btelo.coding.ui.theme.BubbleGradientStart
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onRetryInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = state.updateInfo ?: return
    val sizeText = info.apkSize?.let { bytes ->
        val mb = bytes / 1024f / 1024f
        String.format("%.1f MB", mb)
    } ?: "APK"

    AlertDialog(
        onDismissRequest = {
            if (!state.isDownloading) onDismiss()
        },
        title = {
            Text(
                text = "Update available",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Current ${state.currentVersion} -> ${info.versionName}",
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${info.apkName} · $sizeText",
                    color = TextSecondary
                )
                if (state.isDownloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { state.downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = BubbleGradientStart
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(state.downloadProgress * 100).toInt()}%",
                        color = TextSecondary
                    )
                }
                state.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(text = it, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (state.downloadedApk != null) onRetryInstall() else onInstall()
                },
                enabled = !state.isDownloading && !state.isChecking
            ) {
                Text(
                    text = if (state.downloadedApk != null) "Install" else "Download and install",
                    color = BubbleGradientStart
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isDownloading
            ) {
                Text("Later", color = TextSecondary)
            }
        },
        containerColor = AppBackground
    )
}
