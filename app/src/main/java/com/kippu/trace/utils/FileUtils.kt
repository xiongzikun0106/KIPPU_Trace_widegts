package com.kippu.trace.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {
    // 2K 分辨率上限 (QHD 标准长边 2560px，确保优于 1080p 且兼顾存储)
    private const val MAX_RESOLUTION = 2560

    // 将外部图片压缩至 2K 分辨率并转存至私有空间
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val outputDir = File(context.filesDir, "backgrounds")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // 预读取尺寸 计算采样率
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options) 
            }

            // 计算初步缩放比例
            options.inSampleSize = calculateInSampleSize(options)
            options.inJustDecodeBounds = false

            // 完整解码图片
            var bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 处理旋转
            bitmap = rotateImageIfRequired(context, bitmap, uri)

            // 精确缩放到 2K 以内
            val longEdge = if (bitmap.width > bitmap.height) bitmap.width else bitmap.height
            if (longEdge > MAX_RESOLUTION) {
                val scaleFactor = MAX_RESOLUTION.toFloat() / longEdge
                val targetWidth = (bitmap.width * scaleFactor).toInt()
                val targetHeight = (bitmap.height * scaleFactor).toInt()
                val scaledBitmap = bitmap.scale(targetWidth, targetHeight, filter = true)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = scaledBitmap
                }
            }

            // 保存为高保真压缩后的 JPEG
            val fileName = "bg_${UUID.randomUUID()}.jpg"
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            
            bitmap.recycle()
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if ((height > MAX_RESOLUTION) || (width > MAX_RESOLUTION)) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize >= MAX_RESOLUTION) && (halfWidth / inSampleSize >= MAX_RESOLUTION)) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        return try {
            val ei = ExifInterface(inputStream)
            when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (_: Exception) {
            bitmap
        } finally {
            inputStream.close()
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) {
            source.recycle()
        }
        return rotated
    }

    // 删除图片
    @Suppress("unused")
    fun deleteImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
