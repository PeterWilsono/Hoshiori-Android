package com.hoshiori.ai.brain

import com.hoshiori.ai.data.ChatMessage

/**
 * 群聊记忆提取器
 * 智能提取角色的长期记忆，而不是简单的"最近N条"
 */
class GroupChatMemoryExtractor {
    
    /**
     * 提取群聊所需的智能记忆
     * 
     * @param longTermMemory 角色的长期记忆
     * @param recentMessages 最近的聊天记录（用于补充上下文）
     * @param characterName 角色名称
     * @param maxTokens 最大token限制（默认800，约400-500字）
     * @return 格式化的记忆文本
     */
    fun extractSmartMemory(
        longTermMemory: LongTermMemory,
        recentMessages: List<ChatMessage>,
        characterName: String,
        maxTokens: Int = 800
    ): String {
        val memoryParts = mutableListOf<String>()
        var estimatedTokens = 0
        
        // 1. 用户档案信息（最重要，优先级最高）
        val profileMemory = extractUserProfile(longTermMemory)
        if (profileMemory.isNotEmpty()) {
            memoryParts.add(profileMemory)
            estimatedTokens += estimateTokens(profileMemory)
        }
        
        // 2. 用户偏好（喜好、讨厌的东西）
        if (estimatedTokens < maxTokens * 0.6) {
            val preferenceMemory = extractPreferences(longTermMemory)
            if (preferenceMemory.isNotEmpty()) {
                memoryParts.add(preferenceMemory)
                estimatedTokens += estimateTokens(preferenceMemory)
            }
        }
        
        // 3. 重要事件（里程碑、约定等）
        if (estimatedTokens < maxTokens * 0.7) {
            val importantEvents = extractImportantEvents(longTermMemory)
            if (importantEvents.isNotEmpty()) {
                memoryParts.add(importantEvents)
                estimatedTokens += estimateTokens(importantEvents)
            }
        }
        
        // 4. 情感事件（开心、难过的重要时刻）
        if (estimatedTokens < maxTokens * 0.8) {
            val emotionalEvents = extractEmotionalEvents(longTermMemory)
            if (emotionalEvents.isNotEmpty()) {
                memoryParts.add(emotionalEvents)
                estimatedTokens += estimateTokens(emotionalEvents)
            }
        }
        
        // 5. 讨论过的话题
        if (estimatedTokens < maxTokens * 0.85) {
            val topics = extractTopics(longTermMemory)
            if (topics.isNotEmpty()) {
                memoryParts.add(topics)
                estimatedTokens += estimateTokens(topics)
            }
        }
        
        // 6. 关系历程
        if (estimatedTokens < maxTokens * 0.9) {
            val relationship = extractRelationship(longTermMemory)
            if (relationship.isNotEmpty()) {
                memoryParts.add(relationship)
                estimatedTokens += estimateTokens(relationship)
            }
        }
        
        // 7. 最近对话（只在还有空间时添加）
        if (estimatedTokens < maxTokens * 0.95 && recentMessages.isNotEmpty()) {
            val recentContext = extractRecentContext(recentMessages, characterName, maxTokens - estimatedTokens)
            if (recentContext.isNotEmpty()) {
                memoryParts.add(recentContext)
            }
        }
        
        return if (memoryParts.isEmpty()) {
            "（暂无私聊记忆）"
        } else {
            memoryParts.joinToString("\n\n")
        }
    }
    
    /**
     * 提取用户档案信息
     */
    private fun extractUserProfile(memory: LongTermMemory): String {
        val profile = memory.userProfile ?: return ""
        val parts = mutableListOf<String>()
        
        parts.add("【用户基本信息】")
        
        // 姓名
        profile.name?.let { 
            parts.add("• 姓名：$it")
        }
        
        // 生日
        profile.birthday?.let {
            parts.add("• 生日：$it")
        }
        
        // 职业
        profile.occupation?.let {
            parts.add("• 职业：$it")
        }
        
        // 其他档案笔记（最多3条）
        if (profile.profileNotes.isNotEmpty()) {
            profile.profileNotes.takeLast(3).forEach {
                parts.add("• $it")
            }
        }
        
        return if (parts.size > 1) parts.joinToString("\n") else ""
    }
    
    /**
     * 提取用户偏好
     */
    private fun extractPreferences(memory: LongTermMemory): String {
        val profile = memory.userProfile ?: return ""
        if (profile.preferences.isEmpty()) return ""
        
        val parts = mutableListOf<String>()
        parts.add("【用户喜好】")
        
        // 按重要性排序，取前8个
        profile.preferences
            .sortedByDescending { it.importance }
            .take(8)
            .forEach {
                parts.add("• ${it.content}")
            }
        
        return parts.joinToString("\n")
    }
    
    /**
     * 提取重要事件
     */
    private fun extractImportantEvents(memory: LongTermMemory): String {
        if (memory.importantEvents.isEmpty()) return ""
        
        val parts = mutableListOf<String>()
        parts.add("【重要的共同回忆】")
        
        // 优先选择重要性高的事件
        val highImportance = memory.importantEvents.filter { it.importance >= 7 }
        val selected = if (highImportance.isNotEmpty()) {
            highImportance.takeLast(5)
        } else {
            memory.importantEvents.takeLast(5)
        }
        
        selected.forEach { event ->
            val timeStr = event.time.split(" ").firstOrNull() ?: ""
            parts.add("• $timeStr: ${event.content}")
        }
        
        return parts.joinToString("\n")
    }
    
    /**
     * 提取情感事件
     */
    private fun extractEmotionalEvents(memory: LongTermMemory): String {
        if (memory.emotionalEvents.isEmpty()) return ""
        
        val parts = mutableListOf<String>()
        parts.add("【情感记忆】")
        
        // 优先选择重要性高的情感事件
        val highEmotion = memory.emotionalEvents.filter { it.importance >= 6 }
        val selected = if (highEmotion.isNotEmpty()) {
            highEmotion.takeLast(4)
        } else {
            memory.emotionalEvents.takeLast(4)
        }
        
        selected.forEach { event ->
            val timeStr = event.time.split(" ").firstOrNull() ?: ""
            val emotionEmoji = when (event.emotion) {
                "开心", "快乐" -> "😊"
                "难过", "伤心" -> "😢"
                "生气", "愤怒" -> "😠"
                "感动" -> "🥺"
                "焦虑", "担心" -> "😰"
                else -> "💭"
            }
            parts.add("$emotionEmoji $timeStr: ${event.content}")
        }
        
        return parts.joinToString("\n")
    }
    
    /**
     * 提取讨论过的话题
     */
    private fun extractTopics(memory: LongTermMemory): String {
        if (memory.discussedTopics.isEmpty()) return ""
        
        val topics = memory.discussedTopics.takeLast(10).joinToString("、")
        return "【你们讨论过的话题】\n$topics"
    }
    
    /**
     * 提取关系历程
     */
    private fun extractRelationship(memory: LongTermMemory): String {
        if (memory.relationshipHistory.isEmpty()) return ""
        
        val parts = mutableListOf<String>()
        parts.add("【我们之间的关系历程】")
        
        memory.relationshipHistory.takeLast(3).forEach {
            parts.add("• $it")
        }
        
        return parts.joinToString("\n")
    }
    
    /**
     * 提取最近对话上下文（智能选择相关的）
     */
    private fun extractRecentContext(
        messages: List<ChatMessage>,
        characterName: String,
        remainingTokens: Int
    ): String {
        if (messages.isEmpty() || remainingTokens < 100) return ""
        
        val parts = mutableListOf<String>()
        parts.add("【最近的私聊片段】")
        
        // 只取最近3-5条，避免占用太多token
        val recentCount = when {
            remainingTokens > 300 -> 5
            remainingTokens > 200 -> 4
            else -> 3
        }
        
        messages.takeLast(recentCount).forEach { msg ->
            val speaker = if (msg.isFromUser) "用户" else characterName
            val content = if (msg.content.length > 50) {
                msg.content.take(50) + "..."
            } else {
                msg.content
            }
            parts.add("$speaker: $content")
        }
        
        return parts.joinToString("\n")
    }
    
    /**
     * 估算Token数量（简化版）
     */
    private fun estimateTokens(text: String): Int {
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val englishWords = Regex("\\b[a-zA-Z]+\\b").findAll(text).count()
        val others = text.length - chineseChars - englishWords
        return (chineseChars / 1.5 + englishWords + others / 3).toInt()
    }
    
    /**
     * 生成记忆摘要（用于调试）
     */
    fun generateMemorySummary(memory: LongTermMemory): String {
        val parts = mutableListOf<String>()
        
        memory.userProfile?.let { profile ->
            parts.add("用户档案: ${if (profile.name != null) "有姓名" else "无姓名"}")
            parts.add("偏好数量: ${profile.preferences.size}")
        }
        
        parts.add("重要事件: ${memory.importantEvents.size}条")
        parts.add("情感事件: ${memory.emotionalEvents.size}条")
        parts.add("讨论话题: ${memory.discussedTopics.size}个")
        parts.add("关系历程: ${memory.relationshipHistory.size}条")
        
        return parts.joinToString(", ")
    }
}
