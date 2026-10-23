package com.hoshiori.ai.brain

import java.util.*
import kotlin.random.Random

/**
 * 自主智能层 - Phase 3
 * 包含：双系统思考、真实离线行为、行为输出分离
 * 完整移植自 Python autonomous_system.py
 */
class DualSystemThinking {
    
    // 系统1：快速规则库 - 只保留最基本的问候，其他交给AI处理
    private val fastRules = mapOf(
        // 只保留最简单的问候，避免固定模板
        "你好" to listOf("你好~", "嗨~", "Hi~"),
        "再见" to listOf("拜拜~", "再见~"),
        "谢谢" to listOf("不客气~", "嘻嘻"),
        "晚安" to listOf("晚安~", "好梦~"),
        "早安" to listOf("早安~", "早~")
    )
    
    // 情绪快速响应 - 改为提示而非固定回复
    private val emotionPatterns = mapOf(
        "夸奖" to EmotionPattern(
            keywords = listOf("棒", "厉害", "优秀", "聪明", "可爱", "好看", "漂亮"),
            responses = emptyList(), // 不再使用固定回复
            shouldUseFast = false // 让AI根据角色性格回应
        ),
        "安慰" to EmotionPattern(
            keywords = listOf("难过", "伤心", "痛苦", "不开心", "烦恼", "郁闷"),
            responses = emptyList(),
            shouldUseFast = false
        )
    )
    
    // 需要深度推理的标志
    private val slowThinkingTriggers = listOf(
        "为什么", "怎么办", "如何", "解释", "分析",
        "建议", "帮我", "教我", "复杂", "详细"
    )
    
    /**
     * 处理输入，决定使用快速通道还是慢速通道
     */
    fun process(userInput: String): ThinkingResult {
        // 1. 检查精确匹配（只有最简单的问候）
        fastRules[userInput]?.let { responses ->
            return ThinkingResult(
                useFast = true,
                response = responses.random(),
                confidence = 1.0,
                reason = "exact_match"
            )
        }
        
        // 2. 检查情绪模式 - 不再使用固定回复，只是标记情绪类型
        for ((emotion, config) in emotionPatterns) {
            if (config.keywords.any { userInput.contains(it) }) {
                // 不使用快速通道，让AI根据角色性格回应
                return ThinkingResult(
                    useFast = false,
                    response = null,
                    confidence = 0.8,
                    reason = "emotion_$emotion"
                )
            }
        }
        
        // 3. 检查是否需要慢速思考
        val needsSlow = slowThinkingTriggers.any { userInput.contains(it) }
        
        if (needsSlow) {
            return ThinkingResult(
                useFast = false,
                response = null,
                confidence = 0.0,
                reason = "complex_query"
            )
        }
        
        // 4. 默认走慢速通道
        return ThinkingResult(
            useFast = false,
            response = null,
            confidence = 0.0,
            reason = "normal_conversation"
        )
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun addFastRule(pattern: String, responses: List<String>) {
        // 动态添加快速规则（可扩展）
        // TODO: 实现动态规则添加
    }
}

/**
 * 真实离线行为模拟器
 */
class OfflineActivitySimulator {
    
    // 活动类型
    private val activityTypes = mapOf(
        "news_reading" to ActivityConfig("阅读新闻", 6, true),
        "research" to ActivityConfig("研究问题", 2, true),
        "memory_consolidation" to ActivityConfig("整理记忆", 12, true),
        "learning" to ActivityConfig("学习新知识", 4, true)
    )
    
    /**
     * 生成离线生活状态 - 只提供提示，让AI根据角色设定自由发挥
     */
    fun generateOfflineLife(hoursPassed: Double, currentTime: Calendar): String {
        if (hoursPassed < 2) return ""
        
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        
        // 根据离线时长决定提示类型
        if (hoursPassed >= 12) {
            return """
💡 离线生活提示（${hoursPassed.toInt()}小时）：
- 你可以自然地提及这段时间的生活状态
- 根据你的角色性格，可能在做什么？（学习、休息、思考、娱乐等）
- 不要使用固定模板，要符合你的角色设定
- 可以提及，也可以不提及，自然就好
""".trimIndent()
        }
        
        // 确定时间段
        val timePeriod = when (hour) {
            in 6..10 -> "早晨"
            in 11..13 -> "中午"
            in 14..17 -> "下午"
            in 18..21 -> "晚上"
            else -> "深夜"
        }
        
        return """
💡 离线生活提示（${hoursPassed.toInt()}小时，现在是$timePeriod）：
- 如果合适，可以自然地提及你在这段时间的状态
- 根据你的角色性格和当前时间段，你可能在做什么？
- 不要使用固定模板，要符合你的角色设定
- 可以提及，也可以不提及，自然就好
""".trimIndent()
    }
    
    /**
     * 模拟离线期间的活动
     */
    fun simulateOfflineActivities(hoursOffline: Double, userContext: UserContext): List<OfflineActivity> {
        val activities = mutableListOf<OfflineActivity>()
        
        // 1. 新闻阅读
        if (hoursOffline >= 6 && activityTypes["news_reading"]?.enabled == true) {
            simulateNewsReading(userContext)?.let { activities.add(it) }
        }
        
        // 2. 问题研究
        if (userContext.unresolvedQuestions.isNotEmpty() && hoursOffline >= 2) {
            simulateResearch(userContext.unresolvedQuestions.first())?.let { activities.add(it) }
        }
        
        // 3. 记忆整理
        if (hoursOffline >= 12) {
            simulateMemoryConsolidation(userContext)?.let { activities.add(it) }
        }
        
        return activities
    }
    
    private fun simulateNewsReading(userContext: UserContext): OfflineActivity? {
        if (userContext.userInterests.isEmpty()) return null
        
        val topic = userContext.userInterests.random()
        
        return OfflineActivity(
            type = "news_reading",
            topic = topic,
            content = "💡 你可以提及看到了关于${topic}的内容（根据你的角色性格自由发挥）",
            shareMessage = "💡 如果合适，可以自然地分享关于${topic}的发现"
        )
    }
    
    private fun simulateResearch(question: String): OfflineActivity? {
        return OfflineActivity(
            type = "research",
            topic = question,
            content = "💡 用户之前问过：'$question'，你可以提及你思考过这个问题",
            shareMessage = "💡 如果合适，可以自然地分享你对'$question'的思考"
        )
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun simulateMemoryConsolidation(userContext: UserContext): OfflineActivity? {
        return OfflineActivity(
            type = "memory_consolidation",
            topic = "记忆整理",
            content = "💡 你可以提及回顾了你们的对话",
            shareMessage = "💡 如果合适，可以自然地提及你回顾了你们的对话"
        )
    }
    
    fun generateShareMessage(activities: List<OfflineActivity>): String {
        if (activities.isEmpty()) return ""
        return activities.first().shareMessage
    }
}

/**
 * 自主智能系统 - 整合所有自主功能
 */
class AutonomousIntelligenceSystem {
    
    val dualThinking = DualSystemThinking()
    val offlineSimulator = OfflineActivitySimulator()
    
    /**
     * 处理输入
     */
    @Suppress("UNUSED_PARAMETER")
    fun processInput(userInput: String, inputType: String = "text"): ProcessResult {
        val thinkingResult = dualThinking.process(userInput)
        
        return ProcessResult(
            useFastPath = thinkingResult.useFast,
            fastResponse = thinkingResult.response,
            confidence = thinkingResult.confidence,
            reason = thinkingResult.reason
        )
    }
    
    /**
     * 模拟离线生活
     */
    fun simulateOfflineLife(hoursOffline: Double, userContext: UserContext): OfflineLifeResult {
        val activities = offlineSimulator.simulateOfflineActivities(hoursOffline, userContext)
        val shareMessage = offlineSimulator.generateShareMessage(activities)
        
        return OfflineLifeResult(
            activities = activities,
            shareMessage = shareMessage,
            hasContent = activities.isNotEmpty()
        )
    }
    
    /**
     * 生成离线状态描述
     */
    fun generateOfflineStatus(hoursPassed: Double): String {
        return offlineSimulator.generateOfflineLife(hoursPassed, Calendar.getInstance())
    }
}

// 数据类
data class EmotionPattern(
    val keywords: List<String>,
    val responses: List<String>,
    val shouldUseFast: Boolean = true
)

data class ActivityConfig(
    val name: String,
    val minHours: Int,
    val enabled: Boolean
)

data class ThinkingResult(
    val useFast: Boolean,
    val response: String?,
    val confidence: Double,
    val reason: String
)

data class UserContext(
    val userInterests: List<String> = emptyList(),
    val unresolvedQuestions: List<String> = emptyList(),
    val userSharedLinks: List<String> = emptyList()
)

data class OfflineActivity(
    val type: String,
    val topic: String,
    val content: String,
    val shareMessage: String
)

data class ProcessResult(
    val useFastPath: Boolean,
    val fastResponse: String?,
    val confidence: Double,
    val reason: String
)

data class OfflineLifeResult(
    val activities: List<OfflineActivity>,
    val shareMessage: String,
    val hasContent: Boolean
)
