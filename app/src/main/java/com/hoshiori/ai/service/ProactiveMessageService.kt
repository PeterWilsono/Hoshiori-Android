package com.hoshiori.ai.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.R
import com.hoshiori.ai.data.ChatMessage
import com.hoshiori.ai.ui.ChatActivity
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 主动消息服务 - 让AI像真人一样主动发消息
 */
class ProactiveMessageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val dataManager by lazy { HoshioriApp.instance.dataManager }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "ProactiveMessageWorker started")
        
        val settings = dataManager.getSettings()
        if (!settings.proactiveMessageEnabled) {
            Log.d(TAG, "Proactive messages disabled")
            return Result.success()
        }
        
        // ⭐ 只检查在线角色
        val onlineStatusManager = HoshioriApp.instance.onlineStatusManager
        val characters = onlineStatusManager.getOnlineCharacters()
        Log.d(TAG, "Checking ${characters.size} online characters for proactive messages")
        
        for (character in characters) {
            val hoursSinceLastMessage = (System.currentTimeMillis() - character.lastMessageAt) / (1000 * 60 * 60.0)
            
            Log.d(TAG, "Character ${character.name}: ${hoursSinceLastMessage}h since last message")
            
            // 如果超过设定的间隔时间（转换为小时），有30%概率发送主动消息
            if (hoursSinceLastMessage >= settings.proactiveMessageInterval / 60.0) {
                val shouldSend = Random.nextFloat() < 0.3
                
                if (shouldSend) {
                    val message = generateProactiveMessage(character.name, hoursSinceLastMessage)
                    Log.d(TAG, "Sending proactive message to ${character.name}: $message")
                    
                    // 保存消息
                    val chatMessage = ChatMessage(
                        characterId = character.id,
                        content = message,
                        isFromUser = false,
                        emotion = "anticipation"
                    )
                    dataManager.saveMessage(chatMessage)
                    
                    // 更新角色状态
                    character.unreadCount++
                    character.lastMessage = message
                    character.lastMessageAt = System.currentTimeMillis()
                    dataManager.saveCharacter(character)
                    
                    // 发送通知
                    if (settings.notificationEnabled) {
                        sendNotification(character.id, character.name, message)
                    }
                }
            }
        }
        
        return Result.success()
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun generateProactiveMessage(characterName: String, hoursPassed: Double): String {
        val messages = when {
            hoursPassed < 2 -> listOf(
                "在吗？",
                "嘿~",
                "刚才想到一件事想跟你说"
            )
            hoursPassed < 6 -> listOf(
                "有空聊聊吗？",
                "突然想起你了",
                "在忙吗？"
            )
            hoursPassed < 12 -> listOf(
                "好久没聊天了，想你了~",
                "有点想你了呢"
            )
            hoursPassed < 24 -> listOf(
                "一整天没见到你了，还好吗？",
                "今天过得怎么样？"
            )
            hoursPassed < 48 -> listOf(
                "好久不见！最近还好吗？",
                "这么久没联系，有点想你了"
            )
            else -> listOf(
                "好久不见呀~",
                "最近过得怎么样？"
            )
        }
        
        return messages.random()
    }
    
    private fun sendNotification(characterId: String, characterName: String, message: String) {
        val intent = Intent(applicationContext, ChatActivity::class.java).apply {
            putExtra("character_id", characterId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            characterId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, HoshioriApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_empty)
            .setContentTitle(characterName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(characterId.hashCode(), notification)
        
        Log.d(TAG, "Notification sent for $characterName")
    }
    
    companion object {
        private const val TAG = "ProactiveMessageWorker"
        private const val WORK_NAME = "proactive_message_work"
        
        /**
         * 调度主动消息检查任务
         * 
         * @param context 上下文
         * @param intervalMinutes 检查间隔（分钟），如果为null则从设置中读取
         */
        fun schedule(context: Context, intervalMinutes: Int? = null) {
            val dataManager = HoshioriApp.instance.dataManager
            val settings = dataManager.getSettings()
            
            // 使用传入的间隔或从设置中读取
            val userInterval = (intervalMinutes ?: settings.proactiveMessageInterval).toLong()
            
            // ⭐ 智能调整检查频率
            // 对于长周期，使用更短的检查间隔，但保持原有的发送逻辑
            val checkInterval = when {
                userInterval >= 360 -> 60L  // 6小时 → 每小时检查一次
                userInterval >= 180 -> 45L  // 3小时 → 每45分钟检查一次
                userInterval >= 120 -> 30L  // 2小时 → 每30分钟检查一次
                userInterval >= 60 -> 20L   // 1小时 → 每20分钟检查一次
                else -> userInterval.coerceAtLeast(15L)  // 最小15分钟
            }
            
            Log.d(TAG, "📱 用户设置间隔: ${userInterval}分钟")
            Log.d(TAG, "📱 实际检查间隔: ${checkInterval}分钟")
            Log.d(TAG, "📱 说明: 每${checkInterval}分钟检查一次，但只在距离上次消息${userInterval}分钟后才发送")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)  // ⭐ 不要求电量充足
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<ProactiveMessageWorker>(
                checkInterval, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)  // 首次延迟5分钟启动
                .setBackoffCriteria(  // ⭐ 添加退避策略
                    BackoffPolicy.LINEAR,
                    15, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,  // 使用UPDATE替换旧的任务
                workRequest
            )
            
            Log.d(TAG, "✅ 主动消息Worker已调度")
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Proactive message worker cancelled")
        }
    }
}
