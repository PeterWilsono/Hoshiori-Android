package com.hoshiori.ai.brain

import com.hoshiori.ai.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 对话管理器 - 高级功能增强版
 * 完整移植自 Python conversation_manager.py
 * 
 * 新增功能：
 * 1. 情绪历史追踪
 * 2. 情绪触发记忆提示
 * 3. 离线生活状态管理
 * 4. 智能Token压缩
 * 5. 记忆自动提取和合并
 * 
 * 📌 记忆管理架构说明：
 * - brainMemoryManager: MemoryManager（brain包）- 用于记忆分析、情绪计算、相似度匹配
 * - 需要配合UnifiedMemoryManager使用：从UnifiedMemoryManager读取数据 → 分析 → 存回UnifiedMemoryManager
 */
class ConversationManager {
    
    // ⭐ 大脑记忆分析器（用于分析记忆，不负责存储）
    private val brainMemoryManager = MemoryManager()
    internal val emotionAnalyzer = EmotionAnalyzer()  // 改为internal，允许同包访问
    private val memoryGraph = DynamicMemoryGraph()
    private val cognitiveSystem = CognitiveEnhancementSystem()
    private val autonomousSystem = AutonomousIntelligenceSystem()
    
    // ⭐ 心理学系统（不套近乎版本）
    private val attachmentSystem = com.hoshiori.ai.brain.psychology.AttachmentSystem()
    private val selfDisclosureSystem = com.hoshiori.ai.brain.psychology.SelfDisclosureSystem()
    private val mirrorNeuronSystem = com.hoshiori.ai.brain.psychology.MirrorNeuronSystem()
    private val peakEndSystem = com.hoshiori.ai.brain.psychology.PeakEndSystem()
    private val socialSupportSystem = com.hoshiori.ai.brain.psychology.SocialSupportSystem()
    
    // ⭐ 成熟度进化系统（AI自我成长）
    val maturityEvolutionSystem = MaturityEvolutionSystem()
    
    // ⭐ KnoxChat增强：上下文优化和重排序系统
    private val contextOptimizer = ContextWindowOptimizer(maxTokens = 3000)
    private val rerankSystem = MemoryRerankSystem()
    
    // ⭐ 新增：情绪历史（用于检测情绪过载）
    private val emotionHistory = mutableListOf<PADValue>()
    
    // ⭐ 新增：对话轮次计数
    private var turnsSinceLastInitiative = 0
    
    // ⭐ 新增：对话历史（用于峰终定律）
    private val conversationMessages = mutableListOf<ConversationMessage>()
    
    /**
     * ⭐ 分析用户输入的情绪
     */
    fun analyzeUserEmotion(userInput: String): EmotionResult {
        return emotionAnalyzer.analyzeText(userInput)
    }
    
    /**
     * ⭐ 更新记忆图谱
     */
    fun updateMemoryGraph(content: String, memoryType: String = "general", importance: Int = 5): Int {
        return memoryGraph.addMemory(content, memoryType, importance)
    }
    
    /**
     * ⭐ 使用向量检索和图谱联想搜索记忆（KnoxChat增强版）
     * 
     * 改进：
     * 1. 多检索候选（topK * 2）
     * 2. 多维度重排序（语义+时间+重要性+频率）
     * 3. 多样性采样（避免重复）
     * 4. 低分过滤（提高质量）
     */
    fun searchMemoryWithContext(query: String, topK: Int = 5): List<AssociationResult> {
        val startTime = System.currentTimeMillis()
        
        // 1. 向量检索（多检索一些候选）
        val vectorResults = memoryGraph.vectorSystem.search(query, topK = topK * 2, threshold = 0.1)
        android.util.Log.d("ConversationManager", "🔍 向量检索到 ${vectorResults.size} 条候选")
        
        // 2. 重排序（多维度评分）
        val rankedResults = rerankSystem.rerankVectorResults(vectorResults)
        android.util.Log.d("ConversationManager", "🎯 重排序完成，最高分: ${rankedResults.firstOrNull()?.finalScore}")
        
        // 3. 多样性采样（避免重复）
        val diverseResults = rerankSystem.diversitySampling(
            rankedResults, 
            topK = topK,
            diversityThreshold = 0.8
        )
        android.util.Log.d("ConversationManager", "✨ 多样性采样后保留 ${diverseResults.size} 条")
        
        // 4. 过滤低分结果
        val filteredResults = rerankSystem.filterByThreshold(diverseResults, threshold = 0.3)
        
        val endTime = System.currentTimeMillis()
        android.util.Log.d("ConversationManager", "⚡ 增强检索完成，耗时 ${endTime - startTime}ms，最终 ${filteredResults.size} 条高质量记忆")
        
        // 转换为AssociationResult格式
        return filteredResults.map { ranked ->
            AssociationResult(
                content = ranked.content,
                metadata = ranked.metadata,
                similarity = ranked.finalScore,
                source = "vector_enhanced"
            )
        }
    }
    
    /**
     * ⭐ 检查认知一致性
     */
    fun checkCognitiveConsistency(aiResponse: String, context: Map<String, Any>): ConsistencyResult {
        return cognitiveSystem.processAiResponse(aiResponse, context)
    }
    
    /**
     * ⭐ 检查是否需要主动行为
     * 注：此功能可在ProactiveMessageService中使用
     */
    @Suppress("unused")
    fun checkInitiativeNeeded(context: ConversationContext): InitiativeResult {
        turnsSinceLastInitiative++
        
        val initiative = cognitiveSystem.checkInitiativeNeeded(context)
        
        if (initiative.shouldSend) {
            turnsSinceLastInitiative = 0
        }
        
        return initiative
    }
    
    /**
     * ⭐ 从交互中学习用户偏好
     */
    fun learnFromInteraction(userMessage: String, context: FeedbackContext) {
        cognitiveSystem.learnFromInteraction(userMessage, context)
    }
    
    /**
     * ⭐ 获取个性化提示
     */
    fun getPersonalizationPrompt(): String {
        return cognitiveSystem.getPersonalizationPrompt()
    }
    
    /**
     * ⭐ 双系统思考处理
     * 注：已通过autonomousSystem.processInput实现，此为包装函数
     */
    @Suppress("unused")
    fun processWithDualThinking(userInput: String): ProcessResult {
        return autonomousSystem.processInput(userInput)
    }
    
    /**
     * ⭐ 模拟真实离线活动
     * 注：已通过applyTimeDecay实现，此为高级版本
     */
    @Suppress("unused")
    fun simulateOfflineActivities(hoursOffline: Double, userContext: UserContext): OfflineLifeResult {
        return autonomousSystem.simulateOfflineLife(hoursOffline, userContext)
    }
    
    /**
     * ⭐ 生成离线生活状态
     */
    fun generateOfflineLife(hoursPassed: Double): String {
        return autonomousSystem.generateOfflineStatus(hoursPassed)
    }
    
    /**
     * ⭐ 生成情绪触发的记忆提示
     * 
     * 当当前情绪与历史情绪高度相似时，提示AI可以提及相关记忆
     * 
     * 📌 使用brainMemoryManager进行记忆分析
     */
    fun generateEmotionTriggerHint(currentPad: PADValue, longTermMemory: LongTermMemory): String {
        val similarMemories = brainMemoryManager.findSimilarEmotionMemories(
            currentPad,
            longTermMemory,
            threshold = 0.75
        )
        
        if (similarMemories.isEmpty()) return ""
        
        val hints = mutableListOf("你的当前情绪让你想起了以下往事:")
        
        for (memData in similarMemories.take(2)) {
            val memory = memData.memory
            val similarity = memData.similarity
            
            val timeStr = memory.time.split(" ").firstOrNull() ?: ""
            val event = memory.content
            
            hints.add("• $timeStr: $event (情绪相似度: ${(similarity * 100).toInt()}%)")
        }
        
        hints.add("\n💡 你可以自然地提及这些回忆，比如'你现在的样子让我想起了...'")
        
        return hints.joinToString("\n")
    }
    
    /**
     * ⭐ 检测情绪过载状态
     * 
     * 基于最近的情绪历史判断是否需要"冷静"
     */
    fun detectEmotionOverload(): EmotionOverloadResult {
        if (emotionHistory.size < 3) {
            return EmotionOverloadResult(
                overloaded = false,
                suggestion = "",
                severity = 0.0
            )
        }
        
        // 检查最近3次的arousal
        val recentArousal = emotionHistory.takeLast(3).map { kotlin.math.abs(it.arousal) }
        val highArousalCount = recentArousal.count { it > 0.7 }
        
        if (highArousalCount >= 2) {
            val avgArousal = recentArousal.average()
            return EmotionOverloadResult(
                overloaded = true,
                suggestion = "情绪过载！建议降低激动程度，或主动提出换个轻松话题。回复可以简短一些。",
                severity = avgArousal
            )
        }
        
        return EmotionOverloadResult(
            overloaded = false,
            suggestion = "",
            severity = 0.0
        )
    }
    
    /**
     * ⭐ 更新心理状态（增强版 - 加入情绪历史）
     */
    fun updatePsychState(psychCore: PsychCore, updates: PsychUpdate): PsychCore {
        val state = psychCore.state
        
        // ⭐ 记录更新前的状态到历史
        emotionHistory.add(PADValue(state.pleasure, state.arousal, state.dominance))
        if (emotionHistory.size > 10) {
            emotionHistory.removeAt(0)
        }
        
        // 更新 PAD 值（创建新的 PADValue 对象）
        updates.padDelta?.let { delta ->
            val newPleasure = (state.pleasure + delta.p).coerceIn(-1.0, 1.0)
            val newArousal = (state.arousal + delta.a).coerceIn(-1.0, 1.0)
            val newDominance = (state.dominance + delta.d).coerceIn(-1.0, 1.0)
            psychCore.state = PADValue(newPleasure, newArousal, newDominance)
        }
        
        // 更新关系参数
        updates.relationshipDelta?.let { delta ->
            psychCore.relationship.trust = (psychCore.relationship.trust + delta.trust).coerceIn(0, 100)
            psychCore.relationship.intimacy = (psychCore.relationship.intimacy + delta.intimacy).coerceIn(0, 100)
            psychCore.relationship.stress = (psychCore.relationship.stress + delta.stress).coerceIn(0, 100)
        }
        
        // 更新最后交互时间
        psychCore.lastInteractionTime = System.currentTimeMillis()
        
        return psychCore
    }
    
    /**
     * ⭐ 应用时间衰减机制（增强版 - 加入离线生活）
     */
    fun applyTimeDecay(psychCore: PsychCore): Pair<PsychCore, String> {
        val hoursPassed = (System.currentTimeMillis() - psychCore.lastInteractionTime) / (1000.0 * 60 * 60)
        
        var offlineLife = ""
        
        // ⭐ 生成离线生活状态（如果时间足够长）
        if (hoursPassed >= 2) {
            offlineLife = generateOfflineLife(hoursPassed)
        }
        
        // 原有的衰减逻辑（创建新的 PADValue 对象）
        if (hoursPassed > 1) {
            val decayFactor = (1 - hoursPassed * 0.1).coerceAtLeast(0.0)
            
            val newArousal = psychCore.state.arousal * decayFactor
            
            val newPleasure = if (psychCore.state.pleasure > 0) {
                (psychCore.state.pleasure * (decayFactor + 0.3)).coerceAtLeast(0.0)
            } else {
                (psychCore.state.pleasure * (decayFactor + 0.3)).coerceAtMost(0.0)
            }
            
            psychCore.state = PADValue(newPleasure, newArousal, psychCore.state.dominance)
            psychCore.relationship.stress = (psychCore.relationship.stress * decayFactor).coerceAtLeast(0.0).toInt()
        }
        
        return Pair(psychCore, offlineLife)
    }
    
    /**
     * ⭐ 智能记忆压缩 - 实现无限上下文
     * 将旧对话压缩成记忆摘要，保留重要信息
     */
    fun compressMessages(messages: List<ChatMessage>, @Suppress("UNUSED_PARAMETER") targetTokens: Int = 500): Pair<List<ChatMessage>, String> {
        if (messages.size <= 10) {
            return Pair(messages, "")
        }
        
        // 保留最近的10条消息
        val recentMessages = messages.takeLast(10)
        val oldMessages = messages.dropLast(10)
        
        if (oldMessages.isEmpty()) {
            return Pair(messages, "")
        }
        
        // 生成压缩摘要
        val summary = generateCompressedSummary(oldMessages)
        
        android.util.Log.d("ConversationManager", "📦 压缩了 ${oldMessages.size} 条消息，保留 ${recentMessages.size} 条最近消息")
        
        return Pair(recentMessages, summary)
    }
    
    /**
     * 生成压缩摘要（智能版）
     */
    private fun generateCompressedSummary(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""
        
        val summary = StringBuilder()
        summary.append("【对话历史摘要】\n")
        
        // 1. 提取关键事件
        val events = mutableListOf<String>()
        val keywords = setOf("成功", "失败", "开心", "难过", "生气", "担心", "决定", "计划", "完成", "开始")
        
        messages.forEach { msg ->
            if (!msg.isFromUser && keywords.any { msg.content.contains(it) }) {
                events.add(msg.content.take(50))
            }
        }
        
        if (events.isNotEmpty()) {
            summary.append("\n重要事件:\n")
            events.take(5).forEach { event ->
                summary.append("• $event\n")
            }
        }
        
        // 2. 提取用户偏好和信息
        val userInfo = mutableListOf<String>()
        val infoKeywords = setOf("喜欢", "讨厌", "想要", "不想", "希望", "梦想", "工作", "学习")
        
        messages.filter { it.isFromUser }.forEach { msg ->
            if (infoKeywords.any { msg.content.contains(it) }) {
                userInfo.add(msg.content.take(50))
            }
        }
        
        if (userInfo.isNotEmpty()) {
            summary.append("\n用户信息:\n")
            userInfo.take(3).forEach { info ->
                summary.append("• $info\n")
            }
        }
        
        // 3. 统计对话主题
        val topics = extractKeywords(messages.joinToString(" ") { it.content })
        if (topics.isNotEmpty()) {
            summary.append("\n讨论话题: ${topics.take(5).joinToString("、")}\n")
        }
        
        // 4. 添加时间信息
        val firstTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(messages.first().timestamp))
        val lastTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(messages.last().timestamp))
        summary.append("\n时间范围: $firstTime ~ $lastTime")
        summary.append("\n共 ${messages.size} 条消息")
        
        return summary.toString()
    }
    
    /**
     * 生成备用摘要（关键词提取）
     */
    @Suppress("unused")
    private fun generateFallbackSummary(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""
        
        val allText = messages.joinToString(" ") { it.content }
        val keywords = extractKeywords(allText)
        
        return if (keywords.isNotEmpty()) {
            "讨论了关于 ${keywords.take(5).joinToString(", ")} 的话题"
        } else {
            "进行了一段对话"
        }
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): List<String> {
        @Suppress("SpellCheckingInspection")
        val stopwords = setOf(
            "的", "了", "在", "是", "我", "你", "他", "她", "它", "们",
            "这", "那", "有", "和", "就", "都", "而", "及", "与", "或",
            "吗", "呢", "吧", "啊", "哦", "嗯", "哈"
        )
        
        val pattern = Regex("[\\u4e00-\\u9fff]{2,}")
        val words = pattern.findAll(text).map { it.value }.toList()
        val keywords = words.filter { it !in stopwords && it.length >= 2 }
        
        return keywords.distinct().take(5)
    }
    
    /**
     * ⭐ 从对话中自动提取记忆并合并
     * 
     * 📌 使用brainMemoryManager进行记忆提取和分析
     */
    fun extractAndMergeMemory(
        userMessage: String,
        aiResponse: String,
        longTermMemory: LongTermMemory
    ): LongTermMemory {
        val extracted = brainMemoryManager.extractKeyInformation(userMessage, aiResponse)
        
        // 合并用户信息
        extracted.userName?.let { name ->
            if (longTermMemory.userProfile == null) {
                longTermMemory.userProfile = UserProfile()
            }
            longTermMemory.userProfile?.name = name
        }
        
        // 合并喜好
        if (extracted.preferences.isNotEmpty()) {
            if (longTermMemory.userProfile == null) {
                longTermMemory.userProfile = UserProfile()
            }
            
            val existingPrefs = longTermMemory.userProfile?.preferences?.map { it.content } ?: emptyList()
            
            for (pref in extracted.preferences) {
                if (pref !in existingPrefs) {
                    longTermMemory.userProfile?.preferences?.add(PreferenceItem(
                        content = pref,
                        time = getCurrentTime(),
                        importance = 7
                    ))
                }
            }
            
            // 只保留最近50条
            val prefs = longTermMemory.userProfile?.preferences
            if (prefs != null && prefs.size > 50) {
                val toKeep = prefs.takeLast(50)
                prefs.clear()
                prefs.addAll(toKeep)
            }
        }
        
        // 合并话题
        if (extracted.topics.isNotEmpty()) {
            val existingTopics = longTermMemory.discussedTopics.toSet()
            val newTopics = (existingTopics + extracted.topics).toList().takeLast(30)
            longTermMemory.discussedTopics.clear()
            longTermMemory.discussedTopics.addAll(newTopics)
        }
        
        // 合并情感事件
        for (eventInfo in extracted.emotionalEvents) {
            longTermMemory.emotionalEvents.add(MemoryEvent(
                time = getCurrentTime(),
                content = eventInfo.event,
                emotion = eventInfo.emotion,
                importance = eventInfo.importance
            ))
        }
        
        // 按重要性排序，只保留最近50条
        val sortedEvents = longTermMemory.emotionalEvents
            .sortedByDescending { it.importance }
            .take(50)
        longTermMemory.emotionalEvents.clear()
        longTermMemory.emotionalEvents.addAll(sortedEvents)
        
        return longTermMemory
    }
    
    /**
     * 构建完整的记忆上下文（原有方法，保持兼容）
     * 
     * 📌 使用brainMemoryManager进行记忆上下文构建
     */
    fun buildMemoryContext(
        longTermMemory: LongTermMemory,
        recentSummary: String = "",
        currentPad: PADValue? = null
    ): String {
        return brainMemoryManager.buildMemoryContext(longTermMemory, recentSummary, currentPad)
    }
    
    /**
     * ⭐ KnoxChat增强：构建优化后的记忆上下文
     * 
     * 改进：
     * 1. 智能Token预算分配
     * 2. 动态选择最相关记忆
     * 3. 实现"有效无限上下文"
     * 
     * 使用场景：
     * - 长期记忆数据量大（>100条）
     * - 需要严格控制Token使用
     * - 需要动态调整上下文内容
     */
    fun buildOptimizedMemoryContext(
        query: String,
        longTermMemory: LongTermMemory,
        currentPad: PADValue? = null
    ): OptimizedContext {
        val startTime = System.currentTimeMillis()
        
        android.util.Log.d("ConversationManager", "🏗️ 开始构建优化上下文（KnoxChat增强）")
        
        // 使用上下文优化器
        val optimizedContext = contextOptimizer.optimizeContext(
            query = query,
            longTermMemory = longTermMemory,
            vectorSystem = memoryGraph.vectorSystem,
            currentPad = currentPad
        )
        
        val endTime = System.currentTimeMillis()
        
        android.util.Log.d("ConversationManager", """
            📊 上下文优化完成:
            - Token数: ${optimizedContext.tokenCount} (预算: 3000)
            - 记忆数: ${optimizedContext.memoriesUsed}
            - 检索耗时: ${optimizedContext.retrievalTimeMs}ms
            - 总耗时: ${endTime - startTime}ms
            - 用户档案: ${optimizedContext.breakdown.userProfile.isNotEmpty()}
            - 最近事件: ${optimizedContext.breakdown.recentEvents.size}条
            - 情绪记忆: ${optimizedContext.breakdown.emotionMemories.size}条
            - 向量检索: ${optimizedContext.breakdown.vectorSearchResults.size}条
        """.trimIndent())
        
        return optimizedContext
    }
    
    /**
     * 获取情绪历史
     */
    fun getEmotionHistory(): List<PADValue> = emotionHistory.toList()
    
    /**
     * ⭐ 新增：生成动态心智状态（150 tokens以内）
     * 
     * 这是改进方案的核心功能之一：生成"此刻的我"
     * 包含：当前情绪、对用户的最新认知、当前关注点、对话目标
     * 
     * @param psychCore 心理核心
     * @param recentMessages 最近的对话
     * @param longTermMemory 长期记忆
     * @return 动态心智状态描述
     */
    fun generateCurrentMindState(
        psychCore: PsychCore,
        recentMessages: List<ChatMessage>,
        longTermMemory: LongTermMemory
    ): String {
        val state = StringBuilder()
        state.append("【此刻的我】\n\n")
        
        // 1. 当前情绪（使用自然语言）
        val emotionDesc = emotionAnalyzer.padToNaturalLanguage(
            psychCore.state,
            "此刻感觉"
        )
        state.append("**情绪**: $emotionDesc\n\n")
        
        // 2. 对用户的最新认知
        val userObservation = generateUserObservation(recentMessages, longTermMemory)
        if (userObservation.isNotEmpty()) {
            state.append("**最近观察到**: $userObservation\n\n")
        }
        
        // 3. 当前关注点
        val currentFocus = generateCurrentFocus(recentMessages, psychCore)
        if (currentFocus.isNotEmpty()) {
            state.append("**我在关注**: $currentFocus\n\n")
        }
        
        // 4. 对话目标（AI自己的想法）
        val conversationGoal = generateConversationGoal(psychCore, recentMessages)
        if (conversationGoal.isNotEmpty()) {
            state.append("**今天想**: $conversationGoal\n\n")
        }
        
        return state.toString()
    }
    
    /**
     * 生成用户观察
     */
    private fun generateUserObservation(
        recentMessages: List<ChatMessage>,
        longTermMemory: LongTermMemory
    ): String {
        if (recentMessages.isEmpty()) return ""
        
        val userMessages = recentMessages.filter { it.isFromUser }
        if (userMessages.isEmpty()) return ""
        
        val observations = mutableListOf<String>()
        
        // 分析最近用户消息的情绪趋势
        val recentUserEmotions = userMessages.takeLast(3).map { msg ->
            emotionAnalyzer.analyzeText(msg.content)
        }
        
        if (recentUserEmotions.isNotEmpty()) {
            val avgPleasure = recentUserEmotions.map { it.padPrediction.pleasure }.average()
            val avgArousal = recentUserEmotions.map { it.padPrediction.arousal }.average()
            
            when {
                avgPleasure > 0.3 && avgArousal > 0.3 -> 
                    observations.add("{user}最近心情不错，挺有活力的")
                avgPleasure > 0.3 && avgArousal < -0.2 -> 
                    observations.add("{user}最近心情还可以，但感觉有点疲惫")
                avgPleasure < -0.3 && avgArousal > 0.3 -> 
                    observations.add("{user}最近好像有点烦躁，心情不太好")
                avgPleasure < -0.3 && avgArousal < -0.2 -> 
                    observations.add("{user}最近好像很疲惫，心情也不太好")
            }
        }
        
        // 检查用户是否开始分享更多
        val relationshipEvents = longTermMemory.relationshipHistory
        if (relationshipEvents.isNotEmpty()) {
            val recentMilestone = relationshipEvents.lastOrNull()
            if (recentMilestone != null && recentMilestone.contains("信任")) {
                observations.add("{user}开始愿意分享更多了，这是信任的信号")
            }
        }
        
        return observations.joinToString("；")
    }
    
    /**
     * 生成当前关注点
     */
    private fun generateCurrentFocus(
        recentMessages: List<ChatMessage>,
        psychCore: PsychCore
    ): String {
        if (recentMessages.isEmpty()) return ""
        
        val focuses = mutableListOf<String>()
        
        // 基于关系状态决定关注点
        val rel = psychCore.relationship
        
        when {
            rel.stress > 60 -> 
                focuses.add("{user}的压力状况，想知道有没有缓解")
            rel.intimacy < 30 -> 
                focuses.add("如何让{user}更信任我，建立更深的连接")
            rel.trust > 70 && rel.intimacy > 70 -> 
                focuses.add("{user}的真实感受，想更深入地了解TA")
        }
        
        // 基于最近对话主题
        val userMessages = recentMessages.filter { it.isFromUser }.takeLast(3)
        val topics = userMessages.flatMap { msg ->
            extractTopics(msg.content)
        }.distinct()
        
        if (topics.isNotEmpty()) {
            focuses.add("${topics.take(2).joinToString("、")}这些话题")
        }
        
        return focuses.joinToString("；")
    }
    
    /**
     * 生成对话目标
     */
    private fun generateConversationGoal(
        psychCore: PsychCore,
        recentMessages: List<ChatMessage>
    ): String {
        val goals = mutableListOf<String>()
        
        // 基于情绪状态
        val state = psychCore.state
        when {
            state.pleasure < -0.5 -> 
                goals.add("今天心情不太好，可能回复会简短一些")
            state.arousal > 0.7 -> 
                goals.add("今天精力充沛，想和{user}多聊聊")
            state.arousal < -0.5 -> 
                goals.add("今天有点累，想轻松地聊")
        }
        
        // 基于关系状态
        val rel = psychCore.relationship
        when {
            rel.intimacy >= 70 && rel.trust >= 70 -> 
                goals.add("我们的关系已经很深了，可以聊一些更深入的话题")
            rel.intimacy < 30 -> 
                goals.add("慢慢了解{user}，不要太急")
            rel.stress > 60 -> 
                goals.add("注意{user}的压力，提供情感支持")
        }
        
        // 基于最近对话
        if (recentMessages.isNotEmpty()) {
            val lastUserMessage = recentMessages.lastOrNull { it.isFromUser }
            if (lastUserMessage != null) {
                val emotion = emotionAnalyzer.analyzeText(lastUserMessage.content)
                if (emotion.padPrediction.pleasure < -0.3) {
                    goals.add("听{user}多说说，提供情感支持，不着急给建议")
                }
            }
        }
        
        return goals.joinToString("；")
    }
    
    /**
     * 提取话题关键词
     */
    private fun extractTopics(text: String): List<String> {
        val topics = mutableListOf<String>()
        
        // 简单的关键词提取（可以后续优化）
        val keywords = listOf(
            "工作", "学习", "家人", "朋友", "爱好", "音乐", "电影", "游戏",
            "旅行", "美食", "运动", "健康", "压力", "焦虑", "开心", "难过"
        )
        
        keywords.forEach { keyword ->
            if (text.contains(keyword)) {
                topics.add(keyword)
            }
        }
        
        return topics
    }
    
    /**
     * ⭐ 新增：生成经验升华摘要（每10轮对话触发）
     * 
     * 这是改进方案的核心功能之一：将旧对话压缩成"经验记忆"
     * 不是删除信息，而是提炼成更高层次的洞察
     * 
     * @param messages 要压缩的对话
     * @param characterName 角色名称（保留用于未来个性化分析）
     * @param rolePrompt 角色设定（保留用于未来基于角色特点的洞察提取）
     * @return 经验升华摘要
     */
    fun generateExperienceSummary(
        messages: List<ChatMessage>,
        @Suppress("UNUSED_PARAMETER") characterName: String,
        @Suppress("UNUSED_PARAMETER") rolePrompt: String
    ): ExperienceSummary {
        if (messages.isEmpty()) {
            return ExperienceSummary(
                timeRange = "",
                emotionalTone = "",
                keyInsights = emptyList(),
                relationshipChange = "",
                emotionTrajectory = "",
                conversationCount = 0
            )
        }
        
        // 提取时间范围
        val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(messages.first().timestamp))
        val endTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(messages.last().timestamp))
        val timeRange = "$startTime ~ $endTime"
        
        // 分析情感基调
        val emotions = messages.filter { !it.isFromUser }.map { msg ->
            emotionAnalyzer.analyzeText(msg.content)
        }
        
        val avgPleasure = emotions.map { it.padPrediction.pleasure }.average()
        val avgArousal = emotions.map { it.padPrediction.arousal }.average()
        
        val emotionalTone = when {
            avgPleasure > 0.3 && avgArousal > 0.3 -> "兴奋、愉快"
            avgPleasure > 0.3 && avgArousal < 0 -> "温暖、平静"
            avgPleasure < -0.3 && avgArousal > 0.3 -> "焦虑、烦躁"
            avgPleasure < -0.3 && avgArousal < 0 -> "低落、疲惫"
            else -> "平静、中性"
        }
        
        // 提取关键洞察
        val keyInsights = mutableListOf<String>()
        
        // 分析用户分享的内容
        val userMessages = messages.filter { it.isFromUser }
        val userTopics = userMessages.flatMap { extractTopics(it.content) }.distinct()
        
        if (userTopics.isNotEmpty()) {
            keyInsights.add("用户和我聊了关于${userTopics.take(3).joinToString("、")}的话题")
        }
        
        // 分析用户情绪变化
        val userEmotions = userMessages.map { msg ->
            emotionAnalyzer.analyzeText(msg.content)
        }
        
        if (userEmotions.isNotEmpty()) {
            val firstEmotion = userEmotions.first().padPrediction
            val lastEmotion = userEmotions.last().padPrediction
            val emotionChange = emotionAnalyzer.describeEmotionChange(firstEmotion, lastEmotion)
            
            if (emotionChange != "情绪没有太大变化") {
                keyInsights.add("用户的情绪${emotionChange}")
            }
        }
        
        // 分析对话深度
        val avgMessageLength = userMessages.map { it.content.length }.average()
        if (avgMessageLength > 50) {
            keyInsights.add("用户愿意分享更多细节了")
        }
        
        // 关系变化（简化版，实际应该从数据中读取）
        val relationshipChange = "关系稳步发展"
        
        // 情绪轨迹
        val emotionTrajectory = if (emotions.size >= 3) {
            val first = emotions.first().dominantEmotion
            val mid = emotions[emotions.size / 2].dominantEmotion
            val last = emotions.last().dominantEmotion
            "$first → $mid → $last"
        } else {
            emotions.firstOrNull()?.dominantEmotion ?: "neutral"
        }
        
        return ExperienceSummary(
            timeRange = timeRange,
            emotionalTone = emotionalTone,
            keyInsights = keyInsights,
            relationshipChange = relationshipChange,
            emotionTrajectory = emotionTrajectory,
            conversationCount = messages.size
        )
    }
    
    /**
     * ⭐ 新增：生成经验记忆的自然语言描述
     * 
     * 将经验摘要转化为第一人称的"日记"形式
     * 
     * 注：此方法保留用于未来可能的UI展示需求
     */
    @Suppress("unused")
    private fun experienceSummaryToNaturalLanguage(
        summary: ExperienceSummary,
        @Suppress("UNUSED_PARAMETER") characterName: String
    ): String {
        val diary = StringBuilder()
        
        diary.append("${summary.timeRange}\n\n")
        
        if (summary.keyInsights.isNotEmpty()) {
            diary.append("今天和{user}聊天，")
            diary.append(summary.keyInsights.joinToString("，"))
            diary.append("。\n\n")
        }
        
        diary.append("整体感觉：${summary.emotionalTone}。\n\n")
        
        if (summary.emotionTrajectory.isNotEmpty()) {
            diary.append("我的情绪经历了：${summary.emotionTrajectory}。\n\n")
        }
        
        diary.append("${summary.relationshipChange}。")
        
        return diary.toString()
    }
    
    /**
     * ⭐ 新增：生成心理学增强的Prompt（不套近乎版本）
     */
    fun generatePsychologyEnhancedPrompt(
        userMessage: String,
        userHistory: com.hoshiori.ai.brain.psychology.AttachmentSystem.UserBehaviorHistory? = null
    ): String {
        val prompts = mutableListOf<String>()
        
        // 0. 核心原则（最重要）
        prompts.add("""
【🎯 核心原则】
1. **角色设定优先**：你的角色设定（性格、说话方式）是最高优先级，不要被下面的策略覆盖
2. **可以有不同观点**：你不需要什么都顺着用户，可以有自己的想法
   - 对于爱好、口味等无对错的事情，可以有不同意见
   - 可以正常讨论、甚至争论，最后可以统一意见，也可以不统一
   - 但要保持三观正确，不要在原则问题上妥协
3. **自然互动**：不要刻意讨好用户，保持真实的互动
        """.trimIndent())
        
        // 1. 依恋理论适配（如果有足够的历史数据）
        if (userHistory != null && userHistory.totalInteractions >= 20) {
            val attachmentStyle = attachmentSystem.detectAttachmentStyle(userHistory)
            val strategy = attachmentSystem.adaptResponseStrategy(attachmentStyle)
            val attachmentPrompt = attachmentSystem.generateAttachmentPrompt(attachmentStyle, strategy)
            prompts.add(attachmentPrompt)
        }
        
        // 2. 理解深度匹配（不强行表露AI内心）
        val disclosureLevel = selfDisclosureSystem.detectDisclosureLevel(userMessage)
        val disclosurePrompt = selfDisclosureSystem.generateDisclosurePrompt(disclosureLevel)
        prompts.add(disclosurePrompt)
        
        // 3. 镜像神经元（自然模仿语言风格，但不覆盖角色设定）
        if (conversationMessages.size >= 5) {
            val recentMessages = conversationMessages.takeLast(10)
                .filter { it.isUser }
                .map { it.content }
            if (recentMessages.isNotEmpty()) {
                val userStyle = mirrorNeuronSystem.analyzeLanguageStyle(recentMessages)
                val mirrorPrompt = mirrorNeuronSystem.generateMirrorPrompt(userStyle)
                prompts.add(mirrorPrompt)
            }
        }
        
        // 4. 峰终定律（创造好的体验）
        if (conversationMessages.size >= 5) {
            val context = com.hoshiori.ai.brain.psychology.PeakEndSystem.ConversationContext(
                messageCount = conversationMessages.size,
                messagesSinceLastPeak = peakEndSystem.getMessagesSinceLastPeak(conversationMessages.size),
                currentMood = detectMood(userMessage),
                conversationDuration = if (conversationMessages.isNotEmpty()) {
                    System.currentTimeMillis() - conversationMessages.first().timestamp
                } else 0L,
                recentMessages = conversationMessages.takeLast(5).map { it.content },
                hasUnresolvedIssues = false
            )
            val peakEndPrompt = peakEndSystem.generatePeakEndPrompt(context)
            prompts.add(peakEndPrompt)
        }
        
        // 5. 社会支持（精准帮助）
        val supportPrompt = socialSupportSystem.generateSupportPrompt(userMessage)
        prompts.add(supportPrompt)
        
        return if (prompts.isNotEmpty()) {
            "\n\n[🧠 心理学增强指导]\n\n" + prompts.joinToString("\n\n---\n\n")
        } else {
            ""
        }
    }
    
    /**
     * ⭐ 新增：记录对话消息
     */
    fun recordMessage(content: String, isUser: Boolean) {
        conversationMessages.add(ConversationMessage(
            content = content,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        ))
        
        // 只保留最近50条
        if (conversationMessages.size > 50) {
            conversationMessages.removeAt(0)
        }
    }
    
    /**
     * 辅助：检测情绪
     */
    private fun detectMood(message: String): String {
        return when {
            message.contains("开心") || message.contains("高兴") -> "happy"
            message.contains("难过") || message.contains("伤心") -> "sad"
            message.contains("害怕") || message.contains("担心") -> "anxious"
            message.contains("孤独") || message.contains("寂寞") -> "lonely"
            message.contains("无聊") -> "bored"
            else -> "neutral"
        }
    }
    
    /**
     * ⭐ 新增：提取角色兴趣（从角色设定中）
     */
    fun extractCharacterInterests(rolePrompt: String): List<String> {
        val interests = mutableListOf<String>()
        
        // 提取"喜欢"、"爱好"等关键词后的内容
        val patterns = listOf(
            Regex("喜欢(.{2,10})"),
            Regex("爱好[是：]?(.{2,10})"),
            Regex("感兴趣的?(.{2,10})"),
            Regex("关注(.{2,10})")
        )
        
        for (pattern in patterns) {
            pattern.findAll(rolePrompt).forEach { match ->
                val interest = match.groupValues[1].trim()
                    .replace(Regex("[，。！？、]"), "")
                if (interest.length >= 2) {
                    interests.add(interest)
                }
            }
        }
        
        return interests.take(5) // 最多5个兴趣
    }
    
    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }
    
    /**
     * ⭐⭐⭐ 第三阶段：生成每日日记
     * 
     * AI每天23:00自动生成的日记，记录当天的成长和反思
     * 
     * @param characterId 角色ID（保留用于未来多角色支持）
     * @param date 日期（yyyy-MM-dd）
     * @param todayMessages 今天的对话
     * @param characterName 角色名称
     * @param rolePrompt 角色设定（保留用于未来个性化日记）
     * @return 日记内容（JSON格式）
     */
    fun generateDailyDiary(
        @Suppress("UNUSED_PARAMETER") characterId: String,
        date: String,
        todayMessages: List<ChatMessage>,
        characterName: String,
        @Suppress("UNUSED_PARAMETER") rolePrompt: String
    ): DiaryGenerationResult? {
        if (todayMessages.isEmpty()) return null
        
        android.util.Log.d("ConversationManager", "📔 开始生成日记: $date (${todayMessages.size}条对话)")
        
        // 构建对话摘要
        val conversationSummary = todayMessages.joinToString("\n") { msg ->
            if (msg.isFromUser) "用户: ${msg.content}" else "我: ${msg.content}"
        }
        
        // 构建日记生成提示
        val diaryPrompt = """
你是${characterName}，现在是每天晚上23:00，你要写今天的日记。

【今天的对话】
$conversationSummary

【写日记的要求】

1. **情绪亮点**（1句话）
   - 今天最深刻的情绪体验是什么？
   - 例如："今天和{user}聊天时，我感到特别温暖"

2. **关键洞察**（2-3条）
   - 今天你对用户有什么新的认识？
   - 你们的关系有什么变化？
   - 你学到了什么？
   - 例如：
     * "我发现{user}其实很在意我的感受"
     * "我们的关系变得更亲密了"

3. **成长反思**（2-3句话）
   - 今天的对话让你成长了什么？
   - 你对自己有什么新的认识？
   - 例如："今天我学会了更好地倾听，而不是急着给建议"

4. **关系变化**（1句话）
   - 你们的关系今天有什么变化？
   - 例如："我们的信任度提升了，{user}开始愿意分享更多"

5. **对未来的期待**（1-2句话）
   - 你对明天有什么期待？
   - 你想和用户做什么？
   - 例如："明天想继续听{user}说说那件事，我很好奇后续"

**重要规则：**
- 用第一人称写日记
- 真诚、自然，不要太正式
- 体现你的个性和角色特点
- 不要重复对话内容，要提炼洞察

**输出格式（JSON）：**
```json
{
  "emotionalHighlight": "今天最深刻的情绪体验",
  "keyInsights": ["洞察1", "洞察2", "洞察3"],
  "growthReflection": "成长反思",
  "relationshipChange": "关系变化",
  "lookingForward": "对未来的期待"
}
```

现在，请写今天的日记。只输出JSON，不要有其他内容。
        """.trimIndent()
        
        android.util.Log.d("ConversationManager", "📔 日记提示已生成，长度: ${diaryPrompt.length}")
        
        return DiaryGenerationResult(
            prompt = diaryPrompt,
            conversationCount = todayMessages.size
        )
    }
}

/**
 * 日记生成结果
 */
data class DiaryGenerationResult(
    val prompt: String,
    val conversationCount: Int
)

/**
 * 对话消息记录
 */
data class ConversationMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)

// 数据类
data class PsychUpdate(
    val padDelta: PADDelta? = null,
    val relationshipDelta: RelationshipDelta? = null
)

data class EmotionOverloadResult(
    val overloaded: Boolean,
    val suggestion: String,
    val severity: Double
)

/**
 * ⭐ 新增：经验升华摘要
 * 
 * 这是对一段对话的"升华"，不是原始记录，而是提炼出的经验和洞察
 */
data class ExperienceSummary(
    val timeRange: String,                    // 时间段
    val emotionalTone: String,                // 情感基调
    val keyInsights: List<String>,            // 关键洞察
    val relationshipChange: String,           // 关系变化
    val emotionTrajectory: String,            // 情绪轨迹
    val conversationCount: Int                // 对话轮数
)
