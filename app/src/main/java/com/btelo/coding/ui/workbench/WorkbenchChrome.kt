package com.btelo.coding.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.BorderDefault
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary

enum class WorkbenchTab(
    val label: String,
    val icon: ImageVector
) {
    Agents("Agents", Icons.Default.Forum),
    Files("Files", Icons.Default.Folder),
    Browser("Browser", Icons.Default.Explore),
    Devices("Devices", Icons.Default.Computer)
}

@Composable
fun WorkbenchBottomBar(
    selectedTab: WorkbenchTab,
    onTabSelected: (WorkbenchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(CardSurface)
            .border(1.dp, BorderDefault, RoundedCornerShape(34.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkbenchTab.values().forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selected) TextPrimary.copy(alpha = 0.14f) else CardSurface)
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) AccentBlue else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = if (selected) AccentBlue else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StatusDot(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun SubtleText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 14.sp,
        modifier = modifier
    )
}
