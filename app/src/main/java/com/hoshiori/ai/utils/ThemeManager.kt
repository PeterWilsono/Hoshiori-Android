package com.hoshiori.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.ColorUtils
import com.hoshiori.ai.data.ChatTheme
import com.hoshiori.ai.data.TextColorMode
import com.hoshiori.ai.data.ThemeType
import java.io.File
import java.io.FileOutputStream

/**
 * 主题管理器
 * 负责处理聊天背景、主题切换、颜色适配等
 */
object ThemeManager {
    
    private const val CUSTOM_BG_FILENAME = "custom_chat_background.jpg"
    
    /**
     * 保存自定义背景图片
     */
    fun saveCustomBackground(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // 压缩图片以节省空间
            val compressed = compressBackground(bitmap)
            
            // 保存到内部存储
            val file = File(context.filesDir, CUSTOM_BG_FILENAME)
            val outputStream = FileOutputStream(file)
            compressed.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to save custom background", e)
            null
        }
    }
    
    /**
     * 压缩背景图片
     * 限制最大尺寸为1080p，保持宽高比
     */
    private fun compressBackground(bitmap: Bitmap): Bitmap {
        val maxWidth = 1080
        val maxHeight = 1920
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 获取自定义背景图片
     */
    fun getCustomBackground(context: Context): Bitmap? {
        return try {
            val file = File(context.filesDir, CUSTOM_BG_FILENAME)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to load custom background", e)
            null
        }
    }
    
    /**
     * 删除自定义背景图片
     */
    fun deleteCustomBackground(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CUSTOM_BG_FILENAME)
            file.delete()
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to delete custom background", e)
            false
        }
    }
    
    /**
     * 根据主题类型获取预设背景资源ID
     */
    fun getPresetBackgroundResource(themeType: ThemeType): Int? {
        return when (themeType) {
            ThemeType.DEFAULT -> null  // 使用默认渐变
            ThemeType.STARRY_NIGHT -> com.hoshiori.ai.R.drawable.bg_theme_starry_night
            ThemeType.SAKURA -> com.hoshiori.ai.R.drawable.bg_theme_sakura
            ThemeType.OCEAN -> com.hoshiori.ai.R.drawable.bg_theme_ocean
            ThemeType.SUNSET -> com.hoshiori.ai.R.drawable.bg_theme_sunset
            ThemeType.CUSTOM -> null
        }
    }
    
    /**
     * 计算图片的平均亮度
     * 返回值范围：0.0 (黑) - 1.0 (白)
     */
    fun calculateBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10  // 采样间隔，提高性能
        
        var totalBrightness = 0.0
        var sampleCount = 0
        
        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // 使用感知亮度公式
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                totalBrightness += brightness
                sampleCount++
            }
        }
        
        return (totalBrightness / sampleCount).toFloat()
    }
    
    /**
     * 根据背景亮度自动选择文字颜色
     * @param brightness 背景亮度 (0.0-1.0)
     * @return 文字颜色 (黑色或白色)
     */
    fun getAutoTextColor(brightness: Float): Int {
        return if (brightness > 0.5f) {
            Color.BLACK  // 亮背景用深色文字
        } else {
            Color.WHITE  // 暗背景用浅色文字
        }
    }
    
    /**
     * 根据主题配置获取文字颜色
     */
    fun getTextColor(context: Context, theme: ChatTheme): Int {
        return when (theme.textColorMode) {
            TextColorMode.AUTO -> {
                // 自动模式：根据背景亮度决定
                val brightness = when (theme.themeType) {
                    ThemeType.DEFAULT -> 0.3f  // 默认渐变偏暗
                    ThemeType.STARRY_NIGHT -> 0.2f  // 星空很暗
                    ThemeType.SAKURA -> 0.7f  // 樱花偏亮
                    ThemeType.OCEAN -> 0.4f  // 海洋中等
                    ThemeType.SUNSET -> 0.5f  // 日落中等
                    ThemeType.CUSTOM -> {
                        // 自定义图片：计算实际亮度
                        val bitmap = getCustomBackground(context)
                        bitmap?.let { calculateBrightness(it) } ?: 0.5f
                    }
                }
                getAutoTextColor(brightness)
            }
            TextColorMode.LIGHT -> Color.WHITE
            TextColorMode.DARK -> Color.BLACK
            TextColorMode.CUSTOM -> theme.customTextColor
        }
    }
    
    /**
     * 应用透明度到颜色
     */
    fun applyOpacity(color: Int, opacity: Float): Int {
        val alpha = (opacity * 255).toInt().coerceIn(0, 255)
        return ColorUtils.setAlphaComponent(color, alpha)
    }
    
    /**
     * 获取主题名称（用于UI显示）
     */
    fun getThemeName(themeType: ThemeType): String {
        return when (themeType) {
            ThemeType.DEFAULT -> "默认渐变"
            ThemeType.STARRY_NIGHT -> "星空"
            ThemeType.SAKURA -> "樱花"
            ThemeType.OCEAN -> "海洋"
            ThemeType.SUNSET -> "日落"
            ThemeType.CUSTOM -> "自定义"
        }
    }
}
