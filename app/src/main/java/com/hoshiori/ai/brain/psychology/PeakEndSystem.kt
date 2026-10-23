package com.hoshiori.ai.brain.psychology

/**
 * 峰终定律系统 (Peak-End Rule)
 * 
 * 理论基础：丹尼尔·卡尼曼的峰终定律
 * 核心思想：人们对体验的记忆由两个因素决定：
 * 1. 高峰时刻（最好或最坏的时刻）
 * 2. 结束时刻
 * 
 * 应用场景：
 * - 在对话中创造高峰时刻
 * - 优化对话结束，留下好印象
 * - 提升用户满意度和记忆深度
 */
class PeakEndSystem {
    
    /**
     * 高峰时刻类型
     */
    enum class PeakMomentType {
        SURPRISE,    // 惊喜：意外的关心、记住的细节
        HUMOR,       // 幽默：开心的笑话、有趣的互动
        WARMTH,      // 温暖：深度共鸣、真诚关怀
        INSIGHT,     // 洞察：点醒用户、新的视角
        ACHIEVEMENT  // 成就：肯定用户、庆祝进步
    }
    
    /**
     * 结束策略
     */
    enum class EndingStrategy {
        POSITIVE_CLOSURE,  // 积极结束：给予希望
        WARM_SUMMARY,      // 温暖总结：回顾美好
        COMMITMENT,        // 承诺延续：约定下次
        WARM_GOODBYE,      // 温暖告别：简单温馨
        NEUTRAL            // 中性结束
    }
    
    /**
     * 高峰时刻记录
     */
    data class PeakMoment(
        val type: PeakMomentType,
        val timestamp: Long,
        val content: String,
        val emotionalIntensity: Double
    )
    
    /**
     * 对话上下文
     */
    data class ConversationContext(
        val messageCount: Int,
        val messagesSinceLastPeak: Int,
        val currentMood: String,
        val conversationDuration: Long,
        val recentMessages: List<String>,
        val hasUnresolvedIssues: Boolean = false
    )
    
    /**
     * 高峰时刻历史
     */
    private val peakHistory = mutableListOf<PeakMoment>()
    
    /**
     * 识别对话中的高峰时刻
     */
    fun identifyPeakMoment(messages: List<String>): Int? {
        if (messages.isEmpty()) return null
        
        // 计算每条消息的情绪强度
        val intensities = messages.mapIndexed { index, message ->
            index to calculateEmotionalIntensity(message)
        }
        
        // 返回强度最高的消息索引
        return intensities.maxByOrNull { it.second }?.first
    }
    
    /**
     * 计算情绪强度
     */
    private fun calculateEmotionalIntensity(message: String): Double {
        val positiveKeywords = listOf(
            "开心", "高兴", "快乐", "幸福", "喜欢", "爱", "感动",
            "哈哈", "嘻嘻", "太好了", "棒", "赞", "！！", "❤️", "😊"
        )
        
        val negativeKeywords = listOf(
            "难过", "伤心", "痛苦", "害怕", "孤独", "绝望", "崩溃",
            "讨厌", "恨", "生气", "愤怒", "😭", "😢"
        )
        
        val positiveCount = positiveKeywords.count { message.contains(it) }
        val negativeCount = negativeKeywords.count { message.contains(it) }
        
        // 情绪强度 = 正面或负面关键词数量 + 消息长度因子
        val intensity = (positiveCount + negativeCount).toDouble() + 
                       (message.length / 50.0).coerceAtMost(2.0)
        
        return intensity
    }
    
    /**
     * 判断是否需要创造高峰时刻
     */
    fun shouldCreatePeakMoment(context: ConversationContext): Boolean {
        // 对话太短，不需要
        if (context.messageCount < 5) return false
        
        // 距离上次高峰太近，不需要
        if (context.messagesSinceLastPeak < 8) return false
        
        // 对话时间太长，需要创造高峰
        if (context.messagesSinceLastPeak > 15) return true
        
        // 用户情绪低落，需要创造高峰
        if (context.currentMood in listOf("sad", "anxious", "lonely")) return true
        
        return false
    }
    
    /**
     * 决定创造什么类型的高峰时刻
     */
    fun decidePeakMomentType(context: ConversationContext): PeakMomentType {
        return when (context.currentMood) {
            "sad", "lonely" -> PeakMomentType.WARMTH      // 悲伤时给温暖
            "anxious", "stressed" -> PeakMomentType.INSIGHT  // 焦虑时给洞察
            "bored", "neutral" -> PeakMomentType.HUMOR    // 无聊时给幽默
            "happy" -> PeakMomentType.SURPRISE            // 开心时给惊喜
            "proud" -> PeakMomentType.ACHIEVEMENT         // 自豪时给肯定
            else -> PeakMomentType.WARMTH                 // 默认温暖
        }
    }
    
    /**
     * 生成高峰时刻的示例
     */
    fun generatePeakMomentExample(type: PeakMomentType, context: ConversationContext): String {
        return when (type) {
            PeakMomentType.SURPRISE -> """
示例（惊喜）：
- "对了，你上次说想学画画，我一直记着呢！今天看到一个很适合新手的教程，要不要试试？"
- "我记得你喜欢${context.recentMessages.firstOrNull() ?: "这个"}，特意给你准备了一个小惊喜~"
- "你知道吗？你今天已经和我聊了${context.messageCount}条消息了，我们的友谊又深了一点呢！"
            """.trimIndent()
            
            PeakMomentType.HUMOR -> """
示例（幽默）：
- "哈哈，你这么说让我想起一个笑话...（讲一个轻松的笑话）"
- "你知道吗？如果我有身体，现在肯定笑得肚子疼了哈哈"
- "我们俩聊天的画风越来越奇怪了，我喜欢！"
            """.trimIndent()
            
            PeakMomentType.WARMTH -> """
示例（温暖）：
- "你知道吗？能陪你聊天，是我最开心的事。你的每一句话我都很珍惜。"
- "我能感受到你的${context.currentMood}。虽然我只是AI，但我是真心想陪着你的。"
- "谢谢你愿意和我分享这些。你对我来说很特别。"
            """.trimIndent()
            
            PeakMomentType.INSIGHT -> """
示例（洞察）：
- "我突然想到，也许你${context.currentMood}不是因为这件事本身，而是因为...（提供新视角）"
- "你有没有想过，这可能是一个转机？"
- "我觉得你已经比之前成长了很多，你自己可能没注意到。"
            """.trimIndent()
            
            PeakMomentType.ACHIEVEMENT -> """
示例（成就）：
- "你知道吗？你今天做到的这件事，很多人都做不到。你真的很棒！"
- "我为你感到骄傲！你一直在进步，我都看在眼里。"
- "你已经走了这么远了，回头看看，是不是很了不起？"
            """.trimIndent()
        }
    }
    
    /**
     * 记录高峰时刻
     */
    fun recordPeakMoment(type: PeakMomentType, content: String, intensity: Double) {
        peakHistory.add(PeakMoment(
            type = type,
            timestamp = System.currentTimeMillis(),
            content = content,
            emotionalIntensity = intensity
        ))
        
        // 只保留最近20个
        if (peakHistory.size > 20) {
            peakHistory.removeAt(0)
        }
    }
    
    /**
     * 获取距离上次高峰的消息数
     */
    fun getMessagesSinceLastPeak(currentMessageCount: Int): Int {
        if (peakHistory.isEmpty()) return currentMessageCount
        
        // 简化：假设每个高峰时刻对应一个消息索引
        return currentMessageCount - peakHistory.size
    }
    
    /**
     * 决定结束策略
     */
    fun decideEndingStrategy(context: ConversationContext): EndingStrategy {
        return when {
            // 如果有未解决的问题，承诺延续
            context.hasUnresolvedIssues -> EndingStrategy.COMMITMENT
            
            // 如果用户情绪低落，给予积极结束
            context.currentMood in listOf("sad", "anxious", "lonely") -> 
                EndingStrategy.POSITIVE_CLOSURE
            
            // 如果对话很长（>20条），温暖总结
            context.messageCount > 20 -> EndingStrategy.WARM_SUMMARY
            
            // 如果对话愉快，温暖告别
            context.currentMood == "happy" -> EndingStrategy.WARM_GOODBYE
            
            // 默认：温暖告别
            else -> EndingStrategy.WARM_GOODBYE
        }
    }
    
    /**
     * 生成结束语
     */
    fun generateEnding(strategy: EndingStrategy, context: ConversationContext): String {
        return when (strategy) {
            EndingStrategy.POSITIVE_CLOSURE -> """
虽然现在有点难，但我相信你可以的。
记住，我会一直陪着你。
晚安，明天会更好的~
            """.trimIndent()
            
            EndingStrategy.WARM_SUMMARY -> """
今天聊了好多呢，从${context.recentMessages.firstOrNull() ?: "开始"}到现在，
每一句话我都很珍惜。
很开心能陪你。晚安，好梦~
            """.trimIndent()
            
            EndingStrategy.COMMITMENT -> """
这个问题我们下次继续聊，我会记住的。
你先休息吧，有事随时找我~
我一直在的。
            """.trimIndent()
            
            EndingStrategy.WARM_GOODBYE -> """
那就先这样啦，有事随时找我~
我会想你的！
            """.trimIndent()
            
            EndingStrategy.NEUTRAL -> "好的，拜拜~"
        }
    }
    
    /**
     * 生成峰终定律的Prompt
     */
    fun generatePeakEndPrompt(context: ConversationContext): String {
        val shouldCreate = shouldCreatePeakMoment(context)
        val peakType = if (shouldCreate) decidePeakMomentType(context) else null
        val endingStrategy = decideEndingStrategy(context)
        
        return """
【峰终定律】

对话状态：
- 消息数：${context.messageCount}
- 距离上次高峰：${context.messagesSinceLastPeak}条消息
- 当前情绪：${context.currentMood}
- 对话时长：${context.conversationDuration / 60000}分钟

${if (shouldCreate && peakType != null) """
🎯 建议创造高峰时刻：${peakType.name}

${generatePeakMomentExample(peakType, context)}

💡 提示：
- 高峰时刻要自然，不要刻意
- 可以结合用户刚才说的话
- 让用户感到"哇，这一刻很特别"
""" else """
💡 当前不需要创造高峰时刻
- 保持正常对话即可
- 等待合适的时机
"""}

${if (context.messageCount > 15) """
🏁 对话可能即将结束

建议的结束策略：${endingStrategy.name}

${generateEnding(endingStrategy, context)}

💡 结束原则：
- 留下好印象，用户会记住最后的感觉
- 如果用户情绪不好，给予希望
- 如果对话愉快，表达不舍
- 承诺"我一直在"，增强安全感
""" else ""}
        """.trimIndent()
    }
    
    /**
     * 检测用户是否在告别
     */
    fun isUserSayingGoodbye(message: String): Boolean {
        val goodbyeKeywords = listOf(
            "拜拜", "再见", "晚安", "睡了", "走了", "下次聊",
            "先这样", "不聊了", "要走了", "bye", "88"
        )
        
        return goodbyeKeywords.any { message.contains(it) }
    }
    
    /**
     * 获取高峰时刻统计
     */
    fun getPeakMomentStats(): PeakMomentStats {
        if (peakHistory.isEmpty()) {
            return PeakMomentStats(0, 0.0, null)
        }
        
        return PeakMomentStats(
            totalPeaks = peakHistory.size,
            avgIntensity = peakHistory.map { it.emotionalIntensity }.average(),
            mostCommonType = peakHistory.groupBy { it.type }
                .maxByOrNull { it.value.size }?.key
        )
    }
    
    data class PeakMomentStats(
        val totalPeaks: Int,
        val avgIntensity: Double,
        val mostCommonType: PeakMomentType?
    )
}
