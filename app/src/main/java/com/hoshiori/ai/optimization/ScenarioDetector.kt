package com.hoshiori.ai.optimization

import com.hoshiori.ai.data.ChatMessage
import com.hoshiori.ai.data.PsychCore

/**
 * 场景检测器
 * 
 * 根据用户消息和上下文，智能识别对话场景
 * 
 * @author Kiro AI
 * @since 2026-01-13
 */
class ScenarioDetector {
    
    /**
     * 检测对话场景
     */
    fun detectScenario(
        userMessage: String,
        recentMessages: List<ChatMessage>,
        psychCore: PsychCore?
    ): ConversationScenario {
        
        // 1. 初次见面检测
        if (recentMessages.size < 3) {
            android.util.Log.d("ScenarioDetector", "🎯 场景: 初次见面")
            return ConversationScenario.FIRST_MEETING
        }
        
        // 2. 情感支持检测
        if (isEmotionalSupport(userMessage, psychCore)) {
            android.util.Log.d("ScenarioDetector", "🎯 场景: 情感支持")
            return ConversationScenario.EMOTIONAL_SUPPORT
        }
        
        // 3. 记忆回忆检测
        if (isMemoryRecall(userMessage)) {
            android.util.Log.d("ScenarioDetector", "🎯 场景: 记忆回忆")
            return ConversationScenario.MEMORY_RECALL
        }
        
        // 4. 深度讨论检测
        if (isDeepDiscussion(userMessage, recentMessages)) {
            android.util.Log.d("ScenarioDetector", "🎯 场景: 深度讨论")
            return ConversationScenario.DEEP_DISCUSSION
        }
        
        // 5. 默认：闲聊
        android.util.Log.d("ScenarioDetector", "🎯 场景: 闲聊")
        return ConversationScenario.CASUAL_CHAT
    }
    
    /**
     * 是否为情感支持场景
     */
    private fun isEmotionalSupport(userMessage: String, psychCore: PsychCore?): Boolean {
        // 情感关键词
        val emotionalKeywords = listOf(
            "难过", "开心", "生气", "担心", "害怕", "激动",
            "郁闷", "烦", "累", "压力", "焦虑", "紧张",
            "伤心", "失落", "沮丧", "无助", "孤独",
            "兴奋", "高兴", "快乐", "幸福"
        )
        
        // 检查关键词
        val hasEmotionalKeyword = emotionalKeywords.any { userMessage.contains(it) }
        
        // 检查AI的情绪状态（如果AI情绪激动，也可能需要情感支持）
        val aiEmotionallyAroused = psychCore?.state?.arousal?.let { kotlin.math.abs(it) > 0.6 } ?: false
        
        return hasEmotionalKeyword || aiEmotionallyAroused
    }
    
    /**
     * 是否为记忆回忆场景
     */
    private fun isMemoryRecall(userMessage: String): Boolean {
        val memoryKeywords = listOf(
            "记得", "还记得", "之前", "那时候", "上次",
            "以前", "曾经", "当时", "那次", "回忆"
        )
        
        return memoryKeywords.any { userMessage.contains(it) }
    }
    
    /**
     * 是否为深度讨论场景
     */
    private fun isDeepDiscussion(userMessage: String, recentMessages: List<ChatMessage>): Boolean {
        // 深度讨论关键词
        val deepKeywords = listOf(
            "为什么", "怎么办", "如何", "建议", "想法",
            "觉得", "认为", "应该", "可以", "能不能",
            "帮我", "分析", "看法", "意见"
        )
        
        // 检查关键词
        val hasDeepKeyword = deepKeywords.any { userMessage.contains(it) }
        
        // 检查消息长度（长消息通常是深度讨论）
        val isLongMessage = userMessage.length > 100
        
        // 检查是否是连续讨论（最近3条消息都比较长）
        val isContinuousDiscussion = recentMessages.takeLast(3)
            .count { it.content.length > 50 } >= 2
        
        return hasDeepKeyword || isLongMessage || isContinuousDiscussion
    }
}
