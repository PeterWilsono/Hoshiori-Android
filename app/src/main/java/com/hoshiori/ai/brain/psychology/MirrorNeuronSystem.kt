package com.hoshiori.ai.brain.psychology

/**
 * 镜像神经元系统 (Mirror Neuron System)
 * 
 * 理论基础：镜像神经元理论
 * 核心思想：人类会无意识地模仿对方的行为和情绪
 * 
 * 应用场景：
 * - AI模仿用户的语言风格
 * - AI模仿用户的情绪节奏
 * - 快速建立亲密感和共鸣
 */
class MirrorNeuronSystem {
    
    /**
     * 语言风格特征
     */
    data class LanguageStyle(
        val avgSentenceLength: Double,    // 平均句长
        val emojiFrequency: Double,       // 表情频率（每条消息）
        val punctuationStyle: PunctuationStyle,  // 标点风格
        val formalityLevel: Double,       // 正式程度（0-1）
        val responsePattern: ResponsePattern,    // 回复模式
        val expressionIntensity: Double   // 表达强度（0-1）
    )
    
    /**
     * 标点风格
     */
    enum class PunctuationStyle {
        ELLIPSIS,    // 省略号风格：...
        PERIOD,      // 句号风格：。
        EXCLAMATION, // 感叹号风格：！
        MIXED        // 混合风格
    }
    
    /**
     * 回复模式
     */
    enum class ResponsePattern {
        QUICK_SHORT,  // 快速短句
        THOUGHTFUL,   // 深思熟虑
        CASUAL,       // 随意聊天
        FORMAL        // 正式交流
    }
    
    /**
     * 分析用户的语言风格
     */
    fun analyzeLanguageStyle(messages: List<String>): LanguageStyle {
        if (messages.isEmpty()) {
            return getDefaultStyle()
        }
        
        return LanguageStyle(
            avgSentenceLength = calculateAvgLength(messages),
            emojiFrequency = calculateEmojiFrequency(messages),
            punctuationStyle = detectPunctuationStyle(messages),
            formalityLevel = assessFormality(messages),
            responsePattern = detectResponsePattern(messages),
            expressionIntensity = assessExpressionIntensity(messages)
        )
    }
    
    /**
     * 计算平均句长
     */
    private fun calculateAvgLength(messages: List<String>): Double {
        return messages.map { it.length }.average()
    }
    
    /**
     * 计算表情频率
     */
    private fun calculateEmojiFrequency(messages: List<String>): Double {
        val totalEmojis = messages.sumOf { countEmojis(it) }
        return totalEmojis.toDouble() / messages.size
    }
    
    /**
     * 统计表情数量
     */
    private fun countEmojis(text: String): Int {
        return Regex("[😀-🙏🌀-🗿]").findAll(text).count()
    }
    
    /**
     * 检测标点风格
     */
    private fun detectPunctuationStyle(messages: List<String>): PunctuationStyle {
        val ellipsisCount = messages.sumOf { it.count { c -> c == '…' || it.contains("...") } }
        val periodCount = messages.sumOf { it.count { c -> c == '。' || c == '.' } }
        val exclamationCount = messages.sumOf { it.count { c -> c == '！' || c == '!' } }
        
        val total = ellipsisCount + periodCount + exclamationCount
        if (total == 0) return PunctuationStyle.MIXED
        
        return when {
            ellipsisCount.toDouble() / total > 0.5 -> PunctuationStyle.ELLIPSIS
            exclamationCount.toDouble() / total > 0.4 -> PunctuationStyle.EXCLAMATION
            periodCount.toDouble() / total > 0.5 -> PunctuationStyle.PERIOD
            else -> PunctuationStyle.MIXED
        }
    }
    
    /**
     * 评估正式程度
     */
    private fun assessFormality(messages: List<String>): Double {
        val formalIndicators = listOf("您", "请", "谢谢", "不好意思", "打扰了")
        val casualIndicators = listOf("哈哈", "嘻嘻", "啊", "呀", "嘛", "啦", "哦", "诶")
        
        val formalCount = messages.sumOf { message ->
            formalIndicators.count { message.contains(it) }
        }
        val casualCount = messages.sumOf { message ->
            casualIndicators.count { message.contains(it) }
        }
        
        val total = formalCount + casualCount
        if (total == 0) return 0.5
        
        return formalCount.toDouble() / total
    }
    
    /**
     * 检测回复模式
     */
    private fun detectResponsePattern(messages: List<String>): ResponsePattern {
        val avgLength = calculateAvgLength(messages)
        val formalityLevel = assessFormality(messages)
        
        return when {
            avgLength < 15 -> ResponsePattern.QUICK_SHORT
            formalityLevel > 0.6 -> ResponsePattern.FORMAL
            avgLength > 50 -> ResponsePattern.THOUGHTFUL
            else -> ResponsePattern.CASUAL
        }
    }
    
    /**
     * 评估表达强度
     */
    private fun assessExpressionIntensity(messages: List<String>): Double {
        val intensityIndicators = listOf(
            "超级", "非常", "特别", "极其", "太", "巨", "爆",
            "！！", "!!!", "？？", "..."
        )
        
        val intensityCount = messages.sumOf { message ->
            intensityIndicators.count { message.contains(it) }
        }
        
        return (intensityCount.toDouble() / messages.size).coerceIn(0.0, 1.0)
    }
    
    /**
     * 生成镜像风格的Prompt（不覆盖角色设定）
     */
    fun generateMirrorPrompt(userStyle: LanguageStyle): String {
        return """
【镜像神经元 - 语言风格参考】

用户的语言风格：
- 平均句长：${userStyle.avgSentenceLength.toInt()}字
- 表情使用：${when {
    userStyle.emojiFrequency > 1.5 -> "频繁"
    userStyle.emojiFrequency > 0.5 -> "适中"
    userStyle.emojiFrequency > 0 -> "偶尔"
    else -> "不使用"
}}
- 正式程度：${when {
    userStyle.formalityLevel > 0.6 -> "正式"
    userStyle.formalityLevel > 0.3 -> "中等"
    else -> "随意"
}}

💡 参考建议（在不改变你角色设定的前提下）：
- 如果用户很正式，你可以稍微正式一点（但保持你的性格）
- 如果用户很随意，你也可以放松一点（但保持你的性格）
- 不要完全模仿用户，保持你自己的特点
- 你的角色设定优先级最高，不要被用户风格覆盖
        """.trimIndent()
    }
    
    /**
     * 获取默认风格
     */
    private fun getDefaultStyle(): LanguageStyle {
        return LanguageStyle(
            avgSentenceLength = 25.0,
            emojiFrequency = 0.5,
            punctuationStyle = PunctuationStyle.MIXED,
            formalityLevel = 0.5,
            responsePattern = ResponsePattern.CASUAL,
            expressionIntensity = 0.5
        )
    }
    
    /**
     * 检查风格是否发生变化
     */
    fun hasStyleChanged(oldStyle: LanguageStyle, newStyle: LanguageStyle): Boolean {
        return kotlin.math.abs(oldStyle.avgSentenceLength - newStyle.avgSentenceLength) > 10 ||
               kotlin.math.abs(oldStyle.emojiFrequency - newStyle.emojiFrequency) > 0.5 ||
               oldStyle.punctuationStyle != newStyle.punctuationStyle ||
               kotlin.math.abs(oldStyle.formalityLevel - newStyle.formalityLevel) > 0.3
    }
}
