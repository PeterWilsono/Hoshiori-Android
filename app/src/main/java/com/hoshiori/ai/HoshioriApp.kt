package com.hoshiori.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.hoshiori.ai.data.DataManager

class HoshioriApp : Application() {
    
    lateinit var dataManager: DataManager
        private set
    
    // ⭐ 统一记忆管理器
    lateinit var unifiedMemoryManager: com.hoshiori.ai.data.UnifiedMemoryManager
        private set
    
    // ⭐ 在线状态管理器
    lateinit var onlineStatusManager: com.hoshiori.ai.brain.OnlineStatusManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Application onCreate started ===")
        
        try {
            instance = this
            Log.d(TAG, "Instance set successfully")
            
            dataManager = DataManager(this)
            Log.d(TAG, "DataManager initialized successfully")
            
            // ⭐ 初始化统一记忆管理器
            unifiedMemoryManager = com.hoshiori.ai.data.UnifiedMemoryManager(this)
            Log.d(TAG, "UnifiedMemoryManager initialized successfully")
            
            // ⭐ 初始化在线状态管理器
            onlineStatusManager = com.hoshiori.ai.brain.OnlineStatusManager(dataManager)
            Log.d(TAG, "OnlineStatusManager initialized successfully")
            
            // ⭐ 更新所有角色的在线状态
            onlineStatusManager.updateAllOnlineStatus()
            Log.d(TAG, "All characters online status updated")
            
            // 复制默认头像
            copyDefaultAvatars()
            Log.d(TAG, "Default avatars copied")
            
            createNotificationChannel()
            Log.d(TAG, "Notification channel created successfully")
            
            // 启动主动消息服务
            scheduleProactiveMessages()
            Log.d(TAG, "Proactive message service scheduled")
            
            // ⭐⭐⭐ 第三阶段：启动每日日记任务
            scheduleDailyDiary()
            Log.d(TAG, "Daily diary task scheduled")
            
            Log.d(TAG, "=== Application onCreate completed successfully ===")
        } catch (e: Exception) {
            Log.e(TAG, "!!! FATAL ERROR in Application onCreate !!!", e)
            e.printStackTrace()
            // 不要抛出异常，让应用继续运行
        }
    }
    
    /**
     * 复制默认头像到内部存储
     */
    private fun copyDefaultAvatars() {
        try {
            val avatarDir = java.io.File(filesDir, "avatars")
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            
            // 复制秋岚的头像
            val qiulanAvatarFile = java.io.File(avatarDir, "avatar_default_qiulan.jpg")
            if (!qiulanAvatarFile.exists()) {
                assets.open("avatars/qiulan.jpg").use { input ->
                    qiulanAvatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Qiulan avatar copied to: ${qiulanAvatarFile.absolutePath}")
            }
            
            // 更新秋岚角色的头像路径（无论文件是否刚复制）
            val qiulan = dataManager.getCharacter("default_qiulan")
            if (qiulan != null) {
                // 如果头像路径不是绝对路径，更新为绝对路径
                if (qiulan.avatar == "default_qiulan" || qiulan.avatar == null || !qiulan.avatar!!.startsWith("/")) {
                    qiulan.avatar = qiulanAvatarFile.absolutePath
                    dataManager.saveCharacter(qiulan)
                    Log.d(TAG, "Qiulan avatar path updated to: ${qiulanAvatarFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying default avatars", e)
        }
    }
    
    private fun scheduleProactiveMessages() {
        try {
            val settings = dataManager.getSettings()
            if (settings.proactiveMessageEnabled) {
                // 使用用户设置的间隔时间调度
                com.hoshiori.ai.service.ProactiveMessageWorker.schedule(this, settings.proactiveMessageInterval)
                Log.d(TAG, "Proactive messages enabled and scheduled with ${settings.proactiveMessageInterval}分钟 interval")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling proactive messages", e)
        }
    }
    
    /**
     * ⭐⭐⭐ 第三阶段：调度每日日记任务
     */
    private fun scheduleDailyDiary() {
        try {
            com.hoshiori.ai.service.DailyDiaryWorker.schedule(this)
            Log.d(TAG, "Daily diary task scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily diary", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "星织消息",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "来自星织的消息通知"
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "HoshioriApp"
        const val CHANNEL_ID = "hoshiori_messages"
        
        lateinit var instance: HoshioriApp
            private set
    }
}
