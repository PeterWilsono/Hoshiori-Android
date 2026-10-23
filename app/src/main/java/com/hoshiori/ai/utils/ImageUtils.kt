package com.hoshiori.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 图片处理工具类
 */
object ImageUtils {

    /**
     * 将图片转换为Base64编码
     * @param context 上下文
     * @param uri 图片URI
     * @param maxSize 最大尺寸（宽或高），默认1024
     * @param quality 压缩质量，默认85
     * @return Base64编码的字符串
     */
    fun imageToBase64(
        context: Context,
        uri: Uri,
        maxSize: Int = 1024,
        quality: Int = 85
    ): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 压缩图片
            val resizedBitmap = resizeBitmap(bitmap, maxSize)
            
            // 修正图片方向
            val rotatedBitmap = fixImageOrientation(context, uri, resizedBitmap)

            // 转换为Base64
            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            
            rotatedBitmap.recycle()
            if (rotatedBitmap != resizedBitmap) {
                resizedBitmap.recycle()
            }
            if (resizedBitmap != bitmap) {
                bitmap.recycle()
            }

            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "图片转Base64失败", e)
            return null
        }
    }

    /**
     * 压缩图片到指定尺寸
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 修正图片方向
     */
    private fun fixImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "修正图片方向失败", e)
            return bitmap
        }
    }

    /**
     * 保存图片到本地
     * @param context 上下文
     * @param uri 图片URI
     * @param characterId 角色ID
     * @return 保存后的文件路径
     */
    fun saveImageToLocal(context: Context, uri: Uri, characterId: String): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 创建保存目录
            val imageDir = File(context.filesDir, "images/$characterId")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            // 生成文件名
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val imageFile = File(imageDir, fileName)

            // 保存图片
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            return imageFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "保存图片失败", e)
            return null
        }
    }

    /**
     * 从文件路径加载Bitmap
     */
    fun loadBitmapFromPath(path: String, maxSize: Int = 512): Bitmap? {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)

            // 计算缩放比例
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
            options.inJustDecodeBounds = false

            return BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "加载图片失败", e)
            return null
        }
    }

    /**
     * 计算图片缩放比例
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    /**
     * 保存角色头像
     * 自动裁剪为正方形并压缩到合适尺寸
     * @param context 上下文
     * @param uri 图片URI
     * @param characterId 角色ID
     * @return 保存后的文件路径
     */
    fun saveAvatarImage(context: Context, uri: Uri, characterId: String): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 修正图片方向
            bitmap = fixImageOrientation(context, uri, bitmap)

            // 裁剪为正方形（居中裁剪）
            val squareBitmap = cropToSquare(bitmap)
            if (squareBitmap != bitmap) {
                bitmap.recycle()
            }

            // 压缩到512x512
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)
            if (resizedBitmap != squareBitmap) {
                squareBitmap.recycle()
            }

            // 创建保存目录
            val avatarDir = File(context.filesDir, "avatars")
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }

            // 生成文件名（使用角色ID，确保唯一）
            val fileName = "avatar_$characterId.jpg"
            val avatarFile = File(avatarDir, fileName)

            // 保存图片
            val outputStream = FileOutputStream(avatarFile)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            resizedBitmap.recycle()

            return avatarFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "保存头像失败", e)
            return null
        }
    }

    /**
     * 裁剪图片为正方形（居中裁剪）
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width == height) {
            return bitmap
        }

        val size = minOf(width, height)
        val x = (width - size) / 2
        val y = (height - size) / 2

        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * 加载头像图片
     * @param path 头像文件路径
     * @return Bitmap对象
     */
    fun loadAvatarBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "加载头像失败", e)
            null
        }
    }

    /**
     * 删除头像文件
     * @param path 头像文件路径
     * @return 是否删除成功
     */
    fun deleteAvatarImage(path: String): Boolean {
        return try {
            val file = File(path)
            file.delete()
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "删除头像失败", e)
            false
        }
    }
}
