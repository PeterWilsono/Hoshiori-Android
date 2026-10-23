package com.hoshiori.ai.brain

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * 认知增强层 - Phase 2
 * 包含：认知一致性检查、内在动机系统、个性化学习
 * 完整移植自 Python cognitive_system.py
 */
class CognitiveConsistencyChecker {
    
    // 信念库
    private val beliefs = mutableListOf<Belief>()
    
    // 矛盾检测规则
    private val contradictionPatterns = listOf(
        "喜欢" to "不喜欢",
        "爱" to "讨厌",
        "能" to "不能",
        "会" to "不会",
        "是" to "不是"
    )
    
    /**
     * 从文本中提取关键信念
     */
    fun extractBeliefs(text: String): List<Belief> {
        val beliefs = mutableListOf<Belief>()
        
        val patterns = listOf(
            Triple(Regex("我(不)?(喜欢|爱|讨厌|害怕)(.{2,15})"), { m: MatchResult ->
                Belief(
                    subject = "我",
                    predicate = m.groupValues[2],
                    obj = m.groupValues[3].trim(),
                    polarity = if (m.groupValues[1].isNotEmpty()) "negative" else "positive",
                    time = getCurrentTime(),
                    text = m.value
                )
            }, Unit),
            Triple(Regex("我(不)?(能|会|可以)(.{2,15})"), { m: MatchResult ->
                Belief(
                    subject = "我",
                    predicate = m.groupValues[2],
                    obj = m.groupValues[3].trim(),
                    polarity = if (m.groupValues[1].isNotEmpty()) "negative" else "positive",
                    time = getCurrentTime(),
                    text = m.value
                )
            }, Unit),
            Triple(Regex("我(不)?是(.{2,15})"), { m: MatchResult ->
                Belief(
                    subject = "我",
                    predicate = "是",
                    obj = m.groupValues[2].trim(),
                    polarity = if (m.groupValues[1].isNotEmpty()) "negative" else "positive",
                    time = getCurrentTime(),
                    text = m.value
                )
            }, Unit)
        )
        
        for ((pattern, extractor, _) in patterns) {
            pattern.findAll(text).forEach { match ->
                try {
                    val belief = extractor(match)
                    if (belief.obj.length >= 2) {
                        beliefs.add(belief)
                    }
                } catch (e: Exception) { }
            }
        }
        
        return beliefs
    }
    
    /**
     * 检查新信念是否与历史信念矛盾
     */
    fun checkContradiction(newBelief: Belief, historyBeliefs: List<Belief>): Pair<Boolean, ContradictionInfo?> {
        for (oldBelief in historyBeliefs) {
            if (newBelief.subject == oldBelief.subject && 
                isSimilarObject(newBelief.obj, oldBelief.obj)) {
                
                if (isContradictoryPredicate(
                    newBelief.predicate, newBelief.polarity,
                    oldBelief.predicate, oldBelief.polarity
                )) {
                    val timeDiff = calculateTimeDiff(newBelief.time, oldBelief.time)
                    
                    return Pair(true, ContradictionInfo(
                        oldBelief = oldBelief,
                        newBelief = newBelief,
                        severity = calculateSeverity(timeDiff, newBelief, oldBelief),
                        timeDiffDays = timeDiff
                    ))
                }
            }
        }
        return Pair(false, null)
    }
    
    private fun isSimilarObject(obj1: String, obj2: String): Boolean {
        if (obj1 == obj2) return true
        if (obj1 in obj2 || obj2 in obj1) return true
        
        val words1 = obj1.toSet()
        val words2 = obj2.toSet()
        val overlap = (words1 intersect words2).size.toDouble() / maxOf(words1.size, words2.size)
        
        return overlap > 0.5
    }
    
    private fun isContradictoryPredicate(pred1: String, pol1: String, pred2: String, pol2: String): Boolean {
        if (pred1 == pred2 && pol1 != pol2) return true
        
        for ((pos, neg) in contradictionPatterns) {
            if ((pred1 == pos && pred2 == neg) || (pred1 == neg && pred2 == pos)) {
                if (pol1 == pol2) return true
            }
        }
        return false
    }
    
    private fun calculateTimeDiff(time1: String, time2: String): Double {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val t1 = format.parse(time1)
            val t2 = format.parse(time2)
            abs((t1!!.time - t2!!.time) / (1000 * 60 * 60 * 24)).toDouble()
        } catch (e: Exception) { 0.0 }
    }
    
    private fun calculateSeverity(timeDiff: Double, newBelief: Belief, oldBelief: Belief): Double {
        val timeFactor = when {
            timeDiff < 7 -> 1.0
            timeDiff < 30 -> 0.5
            else -> 0.3
        }
        val polarityFactor = if (newBelief.polarity != oldBelief.polarity) 1.0 else 0.7
        return timeFactor * polarityFactor
    }
    
    /**
     * 生成矛盾解决提示
     */
    fun generateResolutionPrompt(info: ContradictionInfo): String {
        val old = info.oldBelief
        val new = info.newBelief
        val days = info.timeDiffDays.toInt()
        
        return when {
            info.severity > 0.8 -> """
                ⚠️ 检测到严重矛盾：
                - 之前说过：${old.text} (${days}天前)
                - 现在说：${new.text}
                
                建议回复策略：
                1. 承认变化："我之前确实说过${old.obj}，但现在..."
                2. 解释原因："后来我发现..." 或 "经过思考..."
            """.trimIndent()
            info.severity > 0.5 -> """
                💭 检测到观点变化：
                - 之前：${old.text} (${days}天前)
                - 现在：${new.text}
                
                建议：可以自然提及"我改变想法了"或"现在想想..."
            """.trimIndent()
            else -> """
                ℹ️ 注意到细微差异：
                - 之前：${old.text} (${days}天前)
                - 现在：${new.text}
                
                可能是不同语境，如果用户质疑，可以澄清具体情况。
            """.trimIndent()
        }
    }
    
    fun addBelief(belief: Belief) {
        beliefs.add(belief)
        if (beliefs.size > 100) {
            beliefs.removeAt(0)
        }
    }
    
    fun getBeliefs(): List<Belief> = beliefs.toList()
    
    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }
}

/**
 * 内在动机系统 - 基于自我决定理论（SDT）
 */
class MotivationSystem {
    
    // 三大基本需求
    val needs = mutableMapOf(
        "autonomy" to 50,      // 自主性
        "competence" to 50,    // 胜任感
        "relatedness" to 50    // 归属感
    )
    
    // 用户兴趣模型
    private val userInterests = mutableMapOf<String, Int>()
    
    // 对话轮次计数
    private var turnsSinceLastInitiative = 0
    
    /**
     * 根据对话上下文更新需求值
     */
    fun updateNeeds(context: ConversationContext) {
        // 1. 自主性检测
        if (context.recentTurns.size >= 10) {
            val passiveCount = context.recentTurns.takeLast(10).count { !it.aiInitiated }
            if (passiveCount >= 8) {
                needs["autonomy"] = (needs["autonomy"]!! - 5).coerceIn(0, 100)
            }
            if (context.recentTurns.takeLast(5).any { it.aiInitiated }) {
                needs["autonomy"] = (needs["autonomy"]!! + 10).coerceIn(0, 100)
            }
        }
        
        // 2. 胜任感检测
        if (context.userSharedKnowledge) {
            needs["competence"] = (needs["competence"]!! + 10).coerceIn(0, 100)
            context.topic?.let { userInterests[it] = (userInterests[it] ?: 0) + 2 }
        }
        if (context.failedResponses > 0) {
            needs["competence"] = (needs["competence"]!! - 5 * context.failedResponses).coerceIn(0, 100)
        }
        
        // 3. 归属感检测
        if (context.userSharedPrivate) {
            needs["relatedness"] = (needs["relatedness"]!! + 10).coerceIn(0, 100)
        }
        if (context.conversationFrequency < 0.5) {
            needs["relatedness"] = (needs["relatedness"]!! - 5).coerceIn(0, 100)
        }
    }
    
    /**
     * 检查是否需要主动行为
     */
    fun checkAndGenerateInitiative(context: ConversationContext): InitiativeResult {
        turnsSinceLastInitiative++
        
        val lowestNeed = needs.minByOrNull { it.value }!!
        
        if (lowestNeed.value < 30 && shouldTrigger(context)) {
            val message = generateInitiativeMessage(lowestNeed.key, context)
            if (message.isNotEmpty()) {
                turnsSinceLastInitiative = 0
                return InitiativeResult(
                    shouldSend = true,
                    need = lowestNeed.key,
                    message = message,
                    type = "initiative"
                )
            }
        }
        
        return InitiativeResult(shouldSend = false)
    }
    
    private fun shouldTrigger(context: ConversationContext): Boolean {
        // 沉默期
        if (context.silenceDuration > 30) return true
        
        // 话题冷场
        if (context.recentTurns.size >= 3) {
            val shortReplies = context.recentTurns.takeLast(3).count { it.userMessage.length < 5 }
            if (shortReplies >= 2) return true
        }
        
        // 强制周期
        if (turnsSinceLastInitiative >= 20) return true
        
        return false
    }
    
    private fun generateInitiativeMessage(needName: String, context: ConversationContext): String {
        return when (needName) {
            "autonomy" -> {
                val topics = userInterests.entries.sortedByDescending { it.value }
                if (topics.isNotEmpty()) {
                    val topic = topics.first().key
                    listOf(
                        "对了，${topic}最近怎么样了？",
                        "说起来，你上次提到的${topic}，后来有进展吗？",
                        "我突然想起来，你之前说的${topic}，我一直很好奇呢~"
                    ).random()
                } else {
                    "对了，最近有什么有趣的事吗？我想听你分享~"
                }
            }
            "competence" -> {
                if (context.recentTopics.isNotEmpty()) {
                    val topic = context.recentTopics.last()
                    "诶，你刚才说的'${topic}'是什么意思呀？我不太懂🤔"
                } else {
                    "你最近在学什么新东西吗？可以教教我吗？"
                }
            }
            "relatedness" -> {
                if (context.unresolvedIssues.isNotEmpty()) {
                    val issue = context.unresolvedIssues.last()
                    "对了，${issue.daysAgo}天前你说${issue.content}，现在怎么样了？我有点担心..."
                } else {
                    "这几天感觉你好像有点忙，还好吗？我在这里陪你~"
                }
            }
            else -> ""
        }
    }
    
    fun addUserInterest(topic: String, score: Int = 1) {
        userInterests[topic] = (userInterests[topic] ?: 0) + score
    }
}

/**
 * 个性化学习系统
 */
class PersonalizationLearner {
    
    val userProfile = UserStyleProfile()
    private val satisfactionHistory = mutableListOf<SatisfactionRecord>()
    
    /**
     * 分析用户隐式反馈
     */
    fun analyzeUserFeedback(userMessage: String, context: FeedbackContext): UserFeedback {
        val feedback = UserFeedback()
        
        // 回复速度分析
        feedback.responseSpeed = when {
            context.responseTime < 10 -> 1.0
            context.responseTime > 60 -> -1.0
            else -> 0.0
        }
        
        // 回复长度分析
        feedback.responseLength = when {
            userMessage.length > 50 -> 1.0
            userMessage.length < 5 -> -1.0
            else -> 0.0
        }
        
        // 表情使用分析
        val emojiCount = Regex("[😀-🙏]").findAll(userMessage).count()
        feedback.emojiUsage = if (emojiCount > 0) 1.0 else 0.0
        
        // 话题延续分析
        feedback.topicInterest = if (context.topicContinued) 1.0 else -1.0
        
        // 总体满意度
        feedback.overallSatisfaction = 
            0.3 * feedback.responseSpeed +
            0.2 * feedback.responseLength +
            0.2 * feedback.emojiUsage +
            0.3 * feedback.topicInterest
        
        return feedback
    }
    
    /**
     * 根据反馈更新偏好
     */
    fun updatePreferences(feedback: UserFeedback, currentTopic: String?) {
        currentTopic?.let { topic ->
            val currentScore = userProfile.topicPreferences[topic] ?: 0.5
            userProfile.topicPreferences[topic] = 
                (currentScore + feedback.overallSatisfaction * 0.1).coerceIn(0.0, 1.0)
        }
        
        satisfactionHistory.add(SatisfactionRecord(
            satisfaction = feedback.overallSatisfaction,
            topic = currentTopic,
            time = System.currentTimeMillis()
        ))
        
        if (satisfactionHistory.size > 100) {
            satisfactionHistory.removeAt(0)
        }
    }
    
    /**
     * 生成个性化适配提示
     */
    fun generateAdaptationPrompt(): String {
        val parts = mutableListOf("【用户偏好适配】")
        
        val topTopics = userProfile.topicPreferences.entries
            .sortedByDescending { it.value }
            .take(5)
        
        if (topTopics.isNotEmpty()) {
            parts.add("用户感兴趣的话题: ${topTopics.joinToString(", ") { it.key }}")
        }
        
        val style = userProfile.stylePreferences
        if (style.humor > 0.6) parts.add("- 用户喜欢幽默风格，多用俏皮话")
        if (style.detail > 0.6) parts.add("- 用户喜欢详细解释，不要回复太简短")
        else if (style.detail < 0.4) parts.add("- 用户喜欢简洁回复，避免长篇大论")
        
        when (userProfile.emojiPreference) {
            "frequent" -> parts.add("- 用户喜欢表情符号，可以多用")
            "none" -> parts.add("- 用户不太用表情，可以少用")
        }
        
        val lowTopics = topTopics.filter { it.value < 0.3 }.map { it.key }
        if (lowTopics.isNotEmpty()) {
            parts.add("- 避免: ${lowTopics.take(3).joinToString(", ")}")
        }
        
        return parts.joinToString("\n")
    }
    
    fun getAverageSatisfaction(recentN: Int = 10): Double {
        if (satisfactionHistory.isEmpty()) return 0.5
        val recent = satisfactionHistory.takeLast(recentN)
        val avg = recent.map { it.satisfaction }.average()
        return (avg + 1) / 2
    }
}

/**
 * 认知增强系统 - 整合所有认知功能
 */
class CognitiveEnhancementSystem {
    
    val consistencyChecker = CognitiveConsistencyChecker()
    val motivationSystem = MotivationSystem()
    val personalizationLearner = PersonalizationLearner()
    
    @Suppress("UNUSED_PARAMETER")
    fun processAiResponse(aiResponse: String, context: Map<String, Any>): ConsistencyResult {
        val newBeliefs = consistencyChecker.extractBeliefs(aiResponse)
        
        val result = ConsistencyResult()
        
        for (belief in newBeliefs) {
            val (hasContradiction, info) = consistencyChecker.checkContradiction(
                belief, consistencyChecker.getBeliefs()
            )
            
            if (hasContradiction && info != null) {
                result.hasContradiction = true
                result.contradictions.add(info)
                result.suggestions.add(consistencyChecker.generateResolutionPrompt(info))
            }
            
            consistencyChecker.addBelief(belief)
        }
        
        return result
    }
    
    fun checkInitiativeNeeded(context: ConversationContext): InitiativeResult {
        motivationSystem.updateNeeds(context)
        return motivationSystem.checkAndGenerateInitiative(context)
    }
    
    fun learnFromInteraction(userMessage: String, context: FeedbackContext) {
        val feedback = personalizationLearner.analyzeUserFeedback(userMessage, context)
        personalizationLearner.updatePreferences(feedback, context.currentTopic)
    }
    
    fun getPersonalizationPrompt(): String = personalizationLearner.generateAdaptationPrompt()
    
    fun getMotivationStatus(): Map<String, Any> = mapOf(
        "needs" to motivationSystem.needs.toMap(),
        "lowestNeed" to motivationSystem.needs.minByOrNull { it.value }!!
    )
}

// 数据类
data class Belief(
    val subject: String,
    val predicate: String,
    val obj: String,
    val polarity: String,
    val time: String,
    val text: String
)

data class ContradictionInfo(
    val oldBelief: Belief,
    val newBelief: Belief,
    val severity: Double,
    val timeDiffDays: Double
)

data class ConversationContext(
    val recentTurns: List<TurnInfo> = emptyList(),
    val userSharedKnowledge: Boolean = false,
    val userSharedPrivate: Boolean = false,
    val failedResponses: Int = 0,
    val conversationFrequency: Double = 1.0,
    val silenceDuration: Int = 0,
    val topic: String? = null,
    val recentTopics: List<String> = emptyList(),
    val unresolvedIssues: List<UnresolvedIssue> = emptyList()
)

data class TurnInfo(
    val userMessage: String,
    val aiInitiated: Boolean = false
)

data class UnresolvedIssue(
    val content: String,
    val daysAgo: Int
)

data class InitiativeResult(
    val shouldSend: Boolean,
    val need: String = "",
    val message: String = "",
    val type: String = ""
)

data class FeedbackContext(
    val responseTime: Int = 30,
    val topicContinued: Boolean = false,
    val currentTopic: String? = null
)

data class UserFeedback(
    var responseSpeed: Double = 0.0,
    var responseLength: Double = 0.0,
    var emojiUsage: Double = 0.0,
    var topicInterest: Double = 0.0,
    var overallSatisfaction: Double = 0.0
)

data class UserStyleProfile(
    var responseSpeedPreference: String = "normal",
    var responseLengthPreference: String = "normal",
    var emojiPreference: String = "moderate",
    val topicPreferences: MutableMap<String, Double> = mutableMapOf(),
    val stylePreferences: StylePreferences = StylePreferences()
)

data class StylePreferences(
    var humor: Double = 0.5,
    var formality: Double = 0.3,
    var detail: Double = 0.5
)

data class SatisfactionRecord(
    val satisfaction: Double,
    val topic: String?,
    val time: Long
)

data class ConsistencyResult(
    var hasContradiction: Boolean = false,
    val contradictions: MutableList<ContradictionInfo> = mutableListOf(),
    val suggestions: MutableList<String> = mutableListOf()
)
