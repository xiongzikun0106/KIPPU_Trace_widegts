package com.kippu.trace.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 右侧边缘淡出效果
fun Modifier.fadeRightEdge(
    fadeWidth: Dp = 48.dp // 增加 提高可见度
): Modifier = this.graphicsLayer {
    // 离屏渲染 确保混合模式生效
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val fadeWidthPx = fadeWidth.toPx()
    val width = size.width
    
    if (width > fadeWidthPx) {
        // 固定视口
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = width - fadeWidthPx,
                endX = width
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

// 仅对多行文本右下角应用淡出效果

fun Modifier.fadeLastLineEdge(
    fadeWidth: Dp = 48.dp,
    lastLineHeightFraction: Float = 0.25f 
): Modifier = this.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    
    val width = size.width
    val height = size.height
    val lastLineStart = height * (1f - lastLineHeightFraction)
    val fadeWidthPx = fadeWidth.toPx()

    // 保持顶部 100%
    drawRect(
        color = Color.Black,
        size = androidx.compose.ui.geometry.Size(width, lastLineStart),
        blendMode = BlendMode.DstIn
    )

    // 最后一行淡出
    if (width > fadeWidthPx) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = width - fadeWidthPx,
                endX = width
            ),
            topLeft = androidx.compose.ui.geometry.Offset(0f, lastLineStart),
            size = androidx.compose.ui.geometry.Size(width, height - lastLineStart),
            blendMode = BlendMode.DstIn
        )
    } else {
        drawRect(
            color = Color.Black,
            topLeft = androidx.compose.ui.geometry.Offset(0f, lastLineStart),
            size = androidx.compose.ui.geometry.Size(width, height - lastLineStart),
            blendMode = BlendMode.DstIn
        )
    }
}
