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

// 使用阶梯式渐变解决半透明问题
fun Modifier.fadeRightEdge(
    fadeWidth: Dp = 48.dp 
): Modifier = this.graphicsLayer {
    // 强制离屏渲染 确保混合模式正确
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val fadeWidthPx = fadeWidth.toPx()
    val width = size.width
    
    if (width > fadeWidthPx) {
        // 使用阶梯函数 强制透明度骤降
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to Color.Black,
                    0.6f to Color.Black, // 保持 60% 区域不透明
                    0.7f to Color.Transparent, // 10% 距离内快速消失
                    1.0f to Color.Transparent  // 剩余区域全透明
                ),
                startX = width - fadeWidthPx,
                endX = width
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

// 仅对多行文本右下角淡出
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

    // 保留非最后一行区域
    drawRect(
        color = Color.Black,
        size = androidx.compose.ui.geometry.Size(width, lastLineStart),
        blendMode = BlendMode.DstIn
    )

    if (width > fadeWidthPx) {
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to Color.Black,
                    0.6f to Color.Black,
                    0.7f to Color.Transparent,
                    1.0f to Color.Transparent
                ),
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
