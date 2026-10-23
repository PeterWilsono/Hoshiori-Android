package com.hoshiori.ai.brain

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 智能记忆管理系统 - 情绪驱动增强版
 * 完整移植自 Python memory_manager.py
 */
class MemoryManager(private val maxTokens: Int = 3000) {
    
    private val safeThreshold = (maxTokens * 0.65).toInt()
    private val compressTarget = (maxTokens * 0.40).toInt()
    
    // 记忆权重配置
    private val importanceWeights = mapOf(
        "profile" to 10,
        "preference" to 8,
        "event" to 9,
        "emotional_event" to 7,
        "milestone" to 9,
        "user_info" to 10,
        "normal" to 3
    )
    
    // 情绪阈值配置
    private val emotionThresholds = mapOf(
        "extreme_happiness" to mapOf("pleasure" to 0.7, "arousal" to 0.6),
        "extreme_sadness" to mapOf("pleasure" to -0.7, "arousal" to -0.3),
        "extreme_anger" to mapOf("pleasure" to -0.6, "arousal" to 0.7, "dominance" to 0.5),
        "burnout" to mapOf("arousal" to 0.8),
        "depression" to mapOf("pleasure" to -0.5, "arousal" to -0.4)
    )
    
    /**
     * 计算当前情绪与记忆情绪的相似度
     * 使用欧几里得距离的归一化版本
     */
    fun calculateEmotionSimilarity(currentPad: PADValue, memoryPad: PADValue): Double {
        val pDiff = (currentPad.pleasure - memoryPad.pleasure).let { it * it }
        val aDiff = (currentPad.arousal - memoryPad.arousal).let { it * it }
        val dDiff = (currentPad.dominance - memoryPad.dominance).let { it * it }
        
        val distance = sqrt(pDiff + aDiff + dDiff)
        val maxDistance = sqrt(12.0) // sqrt(3 * 2^2)
        
        return (1 - (distance / maxDistance)).coerceIn(0.0, 1.0)
    }
    
    /**
     * 根据情绪相似度检索记忆
     * 心理学原理：情绪一致性效应(Mood Congruence Effect)
     */
    fun findSimilarEmotionMemories(
        currentPad: PADValue,
        longTermMemory: LongTermMemory,
        threshold: Double = 0.75
    ): List<SimilarMemory> {
        val similarMemories = mutableListOf<SimilarMemory>()
        
        // 检索情感事件
        for (event in longTermMemory.emotionalEvents) {
            event.padSnapshot?.let { memoryPad ->
                val similarity = calculateEmotionSimilarity(currentPad, memoryPad)
                if (similarity >= threshold) {
                    similarMemories.add(SimilarMemory(
                        memory = event,
                        similarity = similarity,
                        type = "emotional_event"
                    ))
                }
            }
        }
        
        // 检索重要事件
        for (event in longTermMemory.importantEvents) {
            event.padSnapshot?.let { memoryPad ->
                val similarity = calculateEmotionSimilarity(currentPad, memoryPad)
                if (similarity >= threshold) {
                    similarMemories.add(SimilarMemory(
                        memory = event,
                        similarity = similarity,
                        type = "important_event"
                    ))
                }
            }
        }
        
        return similarMemories.sortedByDescending { it.similarity }.take(5)
    }
    
    /**
     * 检测极端情绪状态
     */
    fun detectExtremeEmotion(padState: PADValue, history: List<PADValue>? = null): ExtremeEmotionResult {
        val p = padState.pleasure
        val a = padState.arousal
        val d = padState.dominance
        
        // 检测极度开心
        if (p > 0.7 && a > 0.6) {
            return ExtremeEmotionResult(
                state = "extreme_happiness",
                severity = minOf(1.0, (p + a) / 2),
                suggestion = "情绪高涨,可以更活泼表达"
            )
        }
        
        // 检测极度悲伤
        if (p < -0.7 && a < -0.3) {
            return ExtremeEmotionResult(
                state = "extreme_sadness",
                severity = minOf(1.0, abs(p)),
                suggestion = "深度共情,提供情绪支持,不要说教"
            )
        }
        
        // 检测愤怒
        if (p < -0.6 && a > 0.7 && d > 0.5) {
            return ExtremeEmotionResult(
                state = "extreme_anger",
                severity = minOf(1.0, (abs(p) + a) / 2),
                suggestion = "情绪激动,需要冷静,可以适度辩护但不要对抗"
            )
        }
        
        // 检测情绪过载(Burnout)
        if (abs(a) > 0.8 && history != null) {
            val highArousalCount = history.takeLast(3).count { abs(it.arousal) > 0.7 }
            if (highArousalCount >= 2) {
                return ExtremeEmotionResult(
                    state = "burnout",
                    severity = abs(a),
                    suggestion = "建议换个轻松话题,避免继续刺激"
                )
            }
        }
        
        // 检测低落期
        if (p < -0.5 && abs(a) < 0.3) {
            return ExtremeEmotionResult(
                state = "depression",
                severity = abs(p),
                suggestion = "低能量状态,回复可以简短,陪伴即可"
            )
        }
        
        return ExtremeEmotionResult(
            state = "normal",
            severity = 0.0,
            suggestion = null
        )
    }
    
    /**
     * 构建记忆上下文（情绪驱动增强版）
     */
    fun buildMemoryContext(
        longTermMemory: LongTermMemory,
        recentSummary: String = "",
        currentPad: PADValue? = null
    ): String {
        val contextParts = mutableListOf<String>()
        
        // 1. 用户档案
        longTermMemory.userProfile?.let { profile ->
            contextParts.add("【你记住的用户信息】")
            profile.name?.let { contextParts.add("- 姓名：$it") }
            profile.birthday?.let { contextParts.add("- 生日：$it") }
            profile.occupation?.let { contextParts.add("- 职业：$it") }
            
            if (profile.profileNotes.isNotEmpty()) {
                contextParts.add("\n基本信息：")
                profile.profileNotes.takeLast(5).forEach {
                    contextParts.add("• $it")
                }
            }
            
            if (profile.preferences.isNotEmpty()) {
                contextParts.add("\n喜好偏好：")
                profile.preferences.sortedByDescending { it.importance }.take(10).forEach {
                    contextParts.add("• ${it.content}")
                }
            }
        }
        
        // 2. 情绪驱动的记忆检索
        currentPad?.let { pad ->
            val similarMemories = findSimilarEmotionMemories(pad, longTermMemory, 0.7)
            if (similarMemories.isNotEmpty()) {
                contextParts.add("\n【💭 类似心情下的往事】")
                contextParts.add("(你的当前情绪让你想起了这些...)")
                
                similarMemories.take(3).forEach { memData ->
                    val memory = memData.memory
                    val similarity = memData.similarity
                    val timeStr = memory.time.split(" ").firstOrNull() ?: ""
                    val eventText = memory.content
                    
                    val emoji = when {
                        similarity > 0.9 -> "🔥"
                        similarity > 0.8 -> "⚡"
                        else -> "✨"
                    }
                    contextParts.add("$emoji $timeStr: $eventText")
                }
            }
        }
        
        // 3. 讨论过的话题
        if (longTermMemory.discussedTopics.isNotEmpty()) {
            contextParts.add("\n【你们讨论过的话题】${longTermMemory.discussedTopics.take(10).joinToString(", ")}")
        }
        
        // 4. 重要事件
        if (longTermMemory.importantEvents.isNotEmpty()) {
            contextParts.add("\n【重要的共同回忆】")
            val highImportance = longTermMemory.importantEvents.filter { it.importance >= 7 }
            val selected = if (highImportance.isNotEmpty()) highImportance.take(5) 
                          else longTermMemory.importantEvents.take(5)
            
            selected.forEach { event ->
                val timeStr = event.time.split(" ").firstOrNull() ?: ""
                contextParts.add("- $timeStr: ${event.content}")
            }
        }
        
        // 5. 情感事件
        val highEmotionEvents = longTermMemory.emotionalEvents.filter { it.importance >= 6 }
        if (highEmotionEvents.isNotEmpty()) {
            contextParts.add("\n【情感记忆】")
            highEmotionEvents.takeLast(3).forEach { event ->
                val timeStr = event.time.split(" ").firstOrNull() ?: ""
                contextParts.add("- $timeStr: ${event.content}")
            }
        }
        
        // 6. 关系历程
        if (longTermMemory.relationshipHistory.isNotEmpty()) {
            contextParts.add("\n【我们之间的关系历程】")
            longTermMemory.relationshipHistory.takeLast(3).forEach {
                contextParts.add("- $it")
            }
        }
        
        // 7. 历史摘要
        if (longTermMemory.longTermSummaries.isNotEmpty()) {
            contextParts.add("\n【更早的对话回顾】")
            longTermMemory.longTermSummaries.takeLast(3).forEach {
                contextParts.add("• $it")
            }
        }
        
        // 8. 最近摘要
        if (recentSummary.isNotEmpty()) {
            contextParts.add("\n【最近对话】\n$recentSummary")
        }
        
        return contextParts.joinToString("\n")
    }
    
    /**
     * 估算Token数量
     */
    fun estimateTokens(text: String): Int {
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val englishWords = Regex("\\b[a-zA-Z]+\\b").findAll(text).count()
        val others = text.length - chineseChars - englishWords
        return (chineseChars / 1.5 + englishWords + others / 3).toInt()
    }
    
    /**
     * 从用户消息中提取关键信息
     */
    fun extractKeyInformation(message: String, aiResponse: String): ExtractedInfo {
        val extracted = ExtractedInfo()
        val combinedText = "$message $aiResponse"
        
        // 提取用户姓名
        val namePatterns = listOf(
            Regex("(?:我是|我叫|叫我)(?!(?:说|如果|想|要|觉得|认为))(.{2,6})(?=$|[，。！？])"),
            Regex("我的名字[是叫](.{2,6})(?=$|[，。！？])"),
            Regex("你可以叫我(.{2,6})(?=$|[，。！？])")
        )
        for (pattern in namePatterns) {
            pattern.find(message)?.let { match ->
                val name = match.groupValues[1].trim()
                val stopNames = setOf("小明", "小红", "什么", "谁", "你", "我", "他", "她", "它")
                if (name !in stopNames && name.length >= 2) {
                    extracted.userName = name
                }
            }
        }
        
        // 提取喜好
        val preferencePatterns = listOf(
            Regex("我[很]?喜欢(.{2,20})") to "like",
            Regex("我[很]?爱(.{2,20})") to "like",
            Regex("我[很]?讨厌(.{2,20})") to "dislike",
            Regex("我[很]?不喜欢(.{2,20})") to "dislike",
            Regex("我[很]?害怕(.{2,20})") to "dislike"
        )
        for ((pattern, sentiment) in preferencePatterns) {
            pattern.findAll(message).forEach { match ->
                var pref = match.groupValues[1].trim()
                pref = pref.replace(Regex("[的了吗呢啊,，。！？]"), "")
                if (pref.length >= 2 && pref !in setOf("什么", "谁", "哪个")) {
                    val prefText = if (sentiment == "like") "喜欢$pref" else "不喜欢$pref"
                    extracted.preferences.add(prefText)
                }
            }
        }
        
        // 识别话题
        val topicKeywords = mapOf(
            "工作" to listOf("工作", "上班", "加班", "同事", "老板", "项目", "公司"),
            "学习" to listOf("学习", "考试", "作业", "课程", "老师", "学校"),
            "感情" to listOf("喜欢", "爱", "恋爱", "男朋友", "女朋友", "分手", "表白"),
            "家庭" to listOf("爸爸", "妈妈", "父母", "家人", "兄弟", "姐妹"),
            "健康" to listOf("生病", "医院", "身体", "健康", "难受", "疼"),
            "美食" to listOf("好吃", "美食", "餐厅", "做饭", "菜", "饭"),
            "娱乐" to listOf("电影", "游戏", "音乐", "看剧", "综艺", "动漫")
        )
        for ((topic, keywords) in topicKeywords) {
            if (keywords.any { combinedText.contains(it) }) {
                extracted.topics.add(topic)
            }
        }
        
        // 识别情感事件
        val emotionalKeywords = mapOf(
            "开心" to listOf("开心", "高兴", "快乐", "幸福", "兴奋"),
            "难过" to listOf("难过", "伤心", "痛苦", "悲伤", "失落"),
            "生气" to listOf("生气", "愤怒", "气愤", "恼火"),
            "焦虑" to listOf("焦虑", "担心", "紧张", "害怕", "不安"),
            "感动" to listOf("感动", "温暖", "暖心", "感激")
        )
        for ((emotion, keywords) in emotionalKeywords) {
            if (keywords.any { combinedText.contains(it) }) {
                val eventDesc = if (message.length > 50) message.take(50) + "..." else message
                extracted.emotionalEvents.add(EmotionalEventInfo(
                    event = "[$emotion] $eventDesc",
                    emotion = emotion,
                    importance = if (emotion in listOf("开心", "感动")) 6 else 7
                ))
                break
            }
        }
        
        return extracted
    }
    
    /**
     * 计算记忆重要性
     */
    fun calculateImportance(eventType: String, padState: PADValue): Int {
        var baseImportance = importanceWeights[eventType] ?: 3
        
        val arousal = abs(padState.arousal)
        val pleasure = padState.pleasure
        
        if (arousal > 0.7 && abs(pleasure) > 0.5) {
            baseImportance = minOf(10, baseImportance + 2)
        }
        
        return baseImportance
    }
    
    fun shouldCompress(currentTokens: Int): Boolean = currentTokens > safeThreshold
}

// 数据类
data class SimilarMemory(
    val memory: MemoryEvent,
    val similarity: Double,
    val type: String
)

data class ExtremeEmotionResult(
    val state: String,
    val severity: Double,
    val suggestion: String?
)

data class ExtractedInfo(
    var userName: String? = null,
    val preferences: MutableList<String> = mutableListOf(),
    val topics: MutableList<String> = mutableListOf(),
    val emotionalEvents: MutableList<EmotionalEventInfo> = mutableListOf()
)

data class EmotionalEventInfo(
    val event: String,
    val emotion: String,
    val importance: Int
)

data class MemoryEvent(
    val time: String,
    val content: String,
    val emotion: String? = null,
    val importance: Int = 5,
    val padSnapshot: PADValue? = null
)

data class UserProfile(
    var name: String? = null,
    var birthday: String? = null,
    var occupation: String? = null,
    val profileNotes: MutableList<String> = mutableListOf(),
    val preferences: MutableList<PreferenceItem> = mutableListOf()
)

data class PreferenceItem(
    val content: String,
    val time: String,
    val importance: Int = 7
)

data class LongTermMemory(
    var userProfile: UserProfile? = UserProfile(),
    val emotionalEvents: MutableList<MemoryEvent> = mutableListOf(),
    val importantEvents: MutableList<MemoryEvent> = mutableListOf(),
    val discussedTopics: MutableList<String> = mutableListOf(),
    val relationshipHistory: MutableList<String> = mutableListOf(),
    val longTermSummaries: MutableList<String> = mutableListOf()
)
