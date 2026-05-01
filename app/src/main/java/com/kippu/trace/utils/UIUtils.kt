package com.kippu.trace.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Applies a horizontal fade-to-transparent effect on the right edge of the content.
 */
fun Modifier.fadeRightEdge(
    fadeWidthFraction: Float = 0.1f // Reduced from 0.15f
): Modifier = this.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.horizontalGradient(
            0.0f to Color.Black,
            (1f - fadeWidthFraction) to Color.Black,
            1.0f to Color.Transparent
        ),
        blendMode = BlendMode.DstIn
    )
}

/**
 * Applies a fade-out effect ONLY to the bottom-right corner of a multi-line text block.
 * This ensures only the last line (near the truncation point) fades out.
 */
fun Modifier.fadeLastLineEdge(
    fadeWidthFraction: Float = 0.15f, // Reduced from 0.25f
    lastLineHeightFraction: Float = 0.25f 
): Modifier = this.graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    
    val width = size.width
    val height = size.height
    val lastLineStart = height * (1f - lastLineHeightFraction)
    val fadeStart = width * (1f - fadeWidthFraction)

    // First, preserve the top portion of the text
    drawRect(
        color = Color.Black,
        size = androidx.compose.ui.geometry.Size(width, lastLineStart),
        blendMode = BlendMode.DstIn
    )

    // Second, apply horizontal fade to the bottom portion (last line)
    drawRect(
        brush = Brush.horizontalGradient(
            0.0f to Color.Black,
            (1f - fadeWidthFraction) to Color.Black,
            1.0f to Color.Transparent,
            startX = 0f,
            endX = width
        ),
        topLeft = androidx.compose.ui.geometry.Offset(0f, lastLineStart),
        size = androidx.compose.ui.geometry.Size(width, height - lastLineStart),
        blendMode = BlendMode.DstIn
    )
}
