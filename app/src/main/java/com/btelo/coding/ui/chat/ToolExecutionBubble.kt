package com.btelo.coding.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btelo.coding.domain.model.ToolExecution
import com.btelo.coding.domain.model.ToolStatus
import com.btelo.coding.domain.model.ToolType
import com.btelo.coding.ui.theme.AccentBlue
import com.btelo.coding.ui.theme.CardSurface
import com.btelo.coding.ui.theme.GreenSuccess
import com.btelo.coding.ui.theme.SkillTagBorder
import com.btelo.coding.ui.theme.TextPrimary
import com.btelo.coding.ui.theme.TextSecondary
import com.btelo.coding.ui.theme.TextTertiary
import com.btelo.coding.ui.theme.ThinkingPurple

private val toolColors = mapOf(
    ToolType.BASH to GreenSuccess,
    ToolType.READ to AccentBlue,
    ToolType.EDIT to AccentBlue,
    ToolType.WRITE to GreenSuccess,
    ToolType.GREP to SkillTagBorder
)

private val toolIcons = mapOf(
    ToolType.BASH to Icons.Default.Terminal,
    ToolType.READ to Icons.Default.MenuBook,
    ToolType.EDIT to Icons.Default.Edit,
    ToolType.WRITE to Icons.Default.Check,
    ToolType.GREP to Icons.Default.Search
)

@Composable
fun ToolExecutionBubble(tool: ToolExecution) {
    val toolColor = toolColors[tool.type] ?: TextSecondary
    val toolIcon = toolIcons[tool.type] ?: Icons.Default.Terminal
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardSurface)
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tool icon with colored circle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(toolColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = toolIcon,
                    contentDescription = tool.type.name,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tool.type.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                color = toolColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Command preview (truncated)
        Text(
            text = tool.command,
            color = TextPrimary,
            fontSize = 13.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 40.dp, top = 2.dp)
        )

        // Expanded: full command in monospace
        if (expanded) {
            Text(
                text = "command: ${tool.command}",
                color = TextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
            )
            if (!tool.output.isNullOrBlank()) {
                Text(
                    text = tool.output,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 40.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ThinkingBubble(description: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(ThinkingPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧠",
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thinking...",
                color = ThinkingPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun CompletionStatusBar(
    toolCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$toolCount tool(s) used · Done",
            color = TextTertiary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (expanded) "▲" else "▼",
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}
