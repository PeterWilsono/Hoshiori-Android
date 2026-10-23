package com.hoshiori.ai.optimization

import com.hoshiori.ai.data.ChatMessage

/**
 * 对话历史优化器
 * 
 * 智能选择重要的历史消息，而不是简单保留最近N条
 * 
 * @author Kiro AI
 * @since 2026-01-13
 */
class ConversationHistoryOptimizer {
    
    /**
     * 优化对话历史
     * 
     * 策略：
     * 1. 保留最近5条（保证连贯性）
     * 2. 从历史中选择3条重要消息（保证上下文）
     * 3. 确保不超过Token限制
     */
    fun optimizeHistory(
        messages: List<ChatMessage>,
        maxTokens: Int = 2000
    ): List<ChatMessage> {
        if (messages.size <= 5) {
            android.util.Log.d("HistoryOptimizer", "📝 消息数≤5，无需优化")
            return messages
        }
        
        // 1. 保留最近5条
        val recent = messages.takeLast(5)
        
        // 2. 从历史中选择3条重要消息
        val historical = messages.dropLast(5)
        val important = selectImportantMessages(historical, count = 3)
        
        // 3. 合并并按时间排序
        val combined = (important + recent).sortedBy { it.timestamp }
        
        // 4. 确保不超过Token限制
        val result = limitByTokens(combined, maxTokens)
        
        android.util.Log.d("HistoryOptimizer", """
            📊 历史优化:
            - 原始: ${messages.size}条
            - 优化后: ${result.size}条
            - 节省: ${messages.size - result.size}条
        """.trimIndent())
        
        return result
    }
    
    /**
     * 选择重要消息
     */
    private fun selectImportantMessages(
        messages: List<ChatMessage>,
        count: Int
    ): List<ChatMessage> {
        return messages
            .map { it to calculateImportance(it) }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }
    
    /**
     * 计算消息重要性
     */
    private fun calculateImportance(message: ChatMessage): Double {
        var score = 1.0
        
        // 1. 情感强度（非中性情绪更重要）
        if (message.emotion != "neutral") {
            score += 0.5
        }
        
        // 2. 消息长度（长消息通常更重要）
        when {
            message.content.length > 100 -> score += 0.8
            message.content.length > 50 -> score += 0.4
        }
        
        // 3. 关键词检测
        val importantKeywords = listOf(
            "重要", "记住", "一定", "必须", "千万",
            "喜欢", "讨厌", "想要", "不想", "希望",
            "决定", "打算", "计划"
        )
        
        val keywordCount = importantKeywords.count { message.content.contains(it) }
        score += keywordCount * 0.3
        
        // 4. 用户主动分享（用户消息通常更重要）
        if (message.isFromUser) {
            score += 0.3
        }
        
        // 5. 包含问题（问答对话更重要）
        if (message.content.contains("？") || message.content.contains("?")) {
            score += 0.2
        }
        
        return score
    }
    
    /**
     * 按Token限制裁剪
     */
    private fun limitByTokens(
        messages: List<ChatMessage>,
        maxTokens: Int
    ): List<ChatMessage> {
        var totalTokens = 0
        val result = mutableListOf<ChatMessage>()
        
        for (msg in messages) {
            val msgTokens = estimateMessageTokens(msg)
            if (totalTokens + msgTokens <= maxTokens) {
                result.add(msg)
                totalTokens += msgTokens
            } else {
                // 超过限制，停止添加
                break
            }
        }
        
        // 如果结果太少（<3条），至少保留最近3条
        if (result.size < 3 && messages.size >= 3) {
            return messages.takeLast(3)
        }
        
        return result
    }
    
    /**
     * 估算消息Token数
     */
    private fun estimateMessageTokens(message: ChatMessage): Int {
        // 中文：1字≈1.5tokens
        // 角色标识：约10tokens
        val contentTokens = (message.content.length * 1.5).toInt()
        val roleTokens = 10
        return contentTokens + roleTokens
    }
}
