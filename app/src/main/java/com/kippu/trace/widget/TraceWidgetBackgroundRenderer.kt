package com.kippu.trace.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.kippu.trace.model.DateEvent
import java.io.File
import kotlin.math.max

object TraceWidgetBackgroundRenderer {
    private const val CORNER_RADIUS_FRACTION = 0.075f

    fun render(context: Context, event: DateEvent?, widgetSize: TraceWidgetSize): Bitmap {
        val width = widgetSize.backgroundWidthPx
        val height = widgetSize.backgroundHeightPx
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val cornerRadius = max(width, height) * CORNER_RADIUS_FRACTION
        val clipPath = Path().apply {
            addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawColor(Color.rgb(30, 30, 30))

        val background = event?.backgroundUri?.let {
            decodeSampledBitmap(it, width, height)
        }
        if (background != null) {
            drawCenterCrop(canvas, background, bounds)
            background.recycle()
        }

        val overlayAlpha = ((event?.maskOpacity ?: 0.15f).coerceIn(0.25f, 0.65f) * 255).toInt()
        canvas.drawColor(Color.argb(overlayAlpha, 0, 0, 0))
        canvas.restore()

        return output
    }

    private fun decodeSampledBitmap(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1
        val halfWidth = width / 2
        val halfHeight = height / 2

        while ((halfWidth / inSampleSize) >= targetWidth && (halfHeight / inSampleSize) >= targetHeight) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, bounds: RectF) {
        val scale = max(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = bounds.left + (bounds.width() - scaledWidth) / 2f
        val top = bounds.top + (bounds.height() - scaledHeight) / 2f
        val target = RectF(left, top, left + scaledWidth, top + scaledHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, target, paint)
    }
}
