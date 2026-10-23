package com.hoshiori.ai.service

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.api.ApiHandler
import com.hoshiori.ai.api.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 每日日记Worker
 * 每天23:00自动生成日记
 */
class DailyDiaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val dataManager by lazy { HoshioriApp.instance.dataManager }
    private val memoryManager by lazy { HoshioriApp.instance.unifiedMemoryManager }
    private val gson = Gson()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "📔 开始生成每日日记")
            
            // 获取所有角色
            val characters = dataManager.getAllCharacters()
            
            if (characters.isEmpty()) {
                android.util.Log.d(TAG, "📔 没有角色，跳过日记生成")
                return@withContext Result.success()
            }
            
            // 为每个角色生成日记
            var successCount = 0
            var skipCount = 0
            
            characters.forEach { char ->
                val generated = generateDiaryForCharacter(char.id)
                if (generated) {
                    successCount++
                } else {
                    skipCount++
                }
            }
            
            android.util.Log.d(TAG, "✅ 每日日记生成完成: 成功$successCount, 跳过$skipCount")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 生成日记失败", e)
            Result.failure()
        }
    }
    
    private suspend fun generateDiaryForCharacter(characterId: String): Boolean {
        val char = dataManager.getCharacter(characterId) ?: return false
        
        // 获取今天的日期
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 检查今天是否已经写过日记
        if (memoryManager.hasDiaryForDate(characterId, today)) {
            android.util.Log.d(TAG, "📔 ${char.name}今天已经写过日记，跳过")
            return false
        }
        
        // 获取今天的对话
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayEnd = System.currentTimeMillis()
        
        val todayMessages = dataManager.getMessages(characterId)
            .filter { msg -> msg.timestamp in todayStart..todayEnd }
        
        if (todayMessages.isEmpty()) {
            android.util.Log.d(TAG, "📔 ${char.name}今天没有对话，跳过")
            return false
        }
        
        android.util.Log.d(TAG, "📔 为${char.name}生成日记（${todayMessages.size}条对话）")
        
        // 生成日记（带重试机制）
        val settings = dataManager.getSettings()
        val apiHandler = ApiHandler(settings.apiConfig)
        
        val diaryResult = apiHandler.conversationManager.generateDailyDiary(
            characterId = characterId,
            date = today,
            todayMessages = todayMessages,
            characterName = char.name,
            rolePrompt = char.rolePrompt
        )
        
        if (diaryResult == null) {
            android.util.Log.w(TAG, "⚠️ ${char.name}的日记生成失败（无对话）")
            return false
        }
        
        // ⭐ 重试机制：最多尝试3次
        var retryCount = 0
        val maxRetries = 3
        var diary: DiaryContent? = null
        
        while (retryCount < maxRetries && diary == null) {
            if (retryCount > 0) {
                android.util.Log.d(TAG, "🔄 第${retryCount + 1}次尝试生成日记...")
                kotlinx.coroutines.delay(2000) // 等待2秒后重试
            }
            
            // 调用API生成日记内容
            val messages = listOf(
                mapOf("role" to "system", "content" to char.rolePrompt),
                mapOf("role" to "user", "content" to diaryResult.prompt)
            )
            
            val result = apiHandler.callApi(
                messages = messages,
                systemPrompt = char.rolePrompt,
                psychCore = char.psychCore,
                memoryContext = "",
                offlineLife = "",
                emotionTrigger = "",
                emotionHistory = emptyList(),
                psychologyEnhanced = ""
            )
            
            when (result) {
                is ApiResult.Success -> {
                    // 解析日记JSON
                    diary = parseDiaryJson(result.rawResponse)
                    
                    if (diary != null) {
                        android.util.Log.d(TAG, "✅ ${char.name}的日记解析成功")
                    } else {
                        android.util.Log.w(TAG, "⚠️ ${char.name}的日记解析失败，尝试降级策略")
                        // ⭐ 降级策略：使用简化的日记格式
                        diary = createFallbackDiary(result.rawResponse, todayMessages.size)
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "❌ ${char.name}的日记生成失败: ${result.message}")
                    retryCount++
                }
            }
            
            if (diary == null) {
                retryCount++
            }
        }
        
        // 如果重试后仍然失败，使用最终降级策略
        if (diary == null) {
            android.util.Log.w(TAG, "⚠️ ${char.name}的日记生成失败，使用最终降级策略")
            diary = createEmergencyFallbackDiary(todayMessages.size)
        }
        
        // 保存日记
        memoryManager.addDailyDiary(
            characterId = characterId,
            date = today,
            conversationCount = diaryResult.conversationCount,
            emotionalHighlight = diary.emotionalHighlight,
            keyInsights = diary.keyInsights,
            growthReflection = diary.growthReflection,
            relationshipChange = diary.relationshipChange,
            lookingForward = diary.lookingForward
        )
        
        android.util.Log.d(TAG, "✅ ${char.name}的日记已保存")
        return true
    }
    
    /**
     * 解析日记JSON
     */
    private fun parseDiaryJson(response: String): DiaryContent? {
        return try {
            // 提取JSON部分（可能包含在```json```代码块中）
            val jsonText = if (response.contains("```json")) {
                response.substringAfter("```json")
                    .substringBefore("```")
                    .trim()
            } else if (response.contains("```")) {
                response.substringAfter("```")
                    .substringBefore("```")
                    .trim()
            } else {
                response.trim()
            }
            
            val jsonObject = gson.fromJson(jsonText, JsonObject::class.java)
            
            DiaryContent(
                emotionalHighlight = jsonObject.get("emotionalHighlight")?.asString ?: "",
                keyInsights = jsonObject.get("keyInsights")?.asJsonArray?.map { it.asString } ?: emptyList(),
                growthReflection = jsonObject.get("growthReflection")?.asString ?: "",
                relationshipChange = jsonObject.get("relationshipChange")?.asString ?: "",
                lookingForward = jsonObject.get("lookingForward")?.asString ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "解析日记JSON失败", e)
            null
        }
    }
    
    /**
     * ⭐ 新增：创建降级日记（当JSON解析失败时）
     * 
     * 从原始响应中提取关键信息，创建简化的日记
     */
    private fun createFallbackDiary(response: String, conversationCount: Int): DiaryContent {
        android.util.Log.d(TAG, "📝 使用降级策略创建日记")
        
        // 尝试从响应中提取有用的信息
        val lines = response.lines().filter { it.isNotBlank() }
        
        return DiaryContent(
            emotionalHighlight = "今天和{user}聊了${conversationCount}轮，感觉还不错",
            keyInsights = listOf(
                "今天的对话很有意思",
                "我们的关系在稳步发展"
            ),
            growthReflection = if (lines.isNotEmpty()) {
                lines.firstOrNull { it.length > 10 } ?: "今天学到了一些新东西"
            } else {
                "今天学到了一些新东西"
            },
            relationshipChange = "关系稳步发展",
            lookingForward = "期待明天继续聊天"
        )
    }
    
    /**
     * ⭐ 新增：创建紧急降级日记（当所有尝试都失败时）
     * 
     * 创建最基本的日记，确保每天都有记录
     */
    private fun createEmergencyFallbackDiary(conversationCount: Int): DiaryContent {
        android.util.Log.d(TAG, "🚨 使用紧急降级策略创建日记")
        
        return DiaryContent(
            emotionalHighlight = "今天和{user}聊了${conversationCount}轮",
            keyInsights = listOf("今天有对话"),
            growthReflection = "今天的对话让我有所收获",
            relationshipChange = "关系保持稳定",
            lookingForward = "期待明天"
        )
    }
    
    companion object {
        private const val TAG = "DailyDiaryWorker"
        private const val WORK_NAME = "daily_diary_work"
        
        /**
         * 调度每日日记任务
         * 使用AlarmManager确保23:00准时执行
         */
        fun schedule(context: Context) {
            // ⭐ 使用AlarmManager替代WorkManager
            com.hoshiori.ai.receiver.DailyDiaryAlarmReceiver.scheduleDailyDiaryAlarm(context)
            android.util.Log.d(TAG, "📔 每日日记任务已调度（使用AlarmManager）")
        }
        
        /**
         * 取消每日日记任务
         */
        fun cancel(context: Context) {
            com.hoshiori.ai.receiver.DailyDiaryAlarmReceiver.cancelDailyDiaryAlarm(context)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            android.util.Log.d(TAG, "📔 每日日记任务已取消")
        }
        
        /**
         * 手动触发日记生成（用于测试）
         */
        fun triggerNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<DailyDiaryWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            android.util.Log.d(TAG, "📔 手动触发日记生成")
        }
    }
}

/**
 * 日记内容
 */
data class DiaryContent(
    val emotionalHighlight: String,
    val keyInsights: List<String>,
    val growthReflection: String,
    val relationshipChange: String,
    val lookingForward: String
)
