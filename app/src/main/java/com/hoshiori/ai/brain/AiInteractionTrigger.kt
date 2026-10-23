package com.hoshiori.ai.brain

import com.hoshiori.ai.data.Character
import com.hoshiori.ai.data.GroupChatMessage
import kotlin.random.Random

/**
 * AI互相对话触发器
 * 
 * 功能：
 * 1. 检测AI消息是否触发其他AI回复
 * 2. 智能判断是否需要AI之间的互动
 * 3. 优化token消耗，避免无限循环
 */
class AiInteractionTrigger {
    
    companion object {
        private const val TAG = "AiInteractionTrigger"
        
        // AI互动概率配置
        private const val BASE_INTERACTION_PROBABILITY = 0.3  // 基础互动概率30%
        private const val MENTION_INTERACTION_PROBABILITY = 0.8  // 被@时互动概率80%
        private const val QUESTION_INTERACTION_PROBABILITY = 0.6  // 被提问时互动概率60%
        
        // 防止无限循环
        private const val MAX_AI_CHAIN_LENGTH = 3  // 最多连续3次AI互动
        private const val MIN_INTERVAL_MS = 3000L  // 最小间隔3秒
    }
    
    /**
     * 检查是否应该触发AI互动
     * 
     * @param lastMessage 最后一条消息
     * @param allCharacters 所有角色
     * @param recentMessages 最近的消息历史
     * @return 应该回复的角色列表（可能为空）
     */
    fun shouldTriggerInteraction(
        lastMessage: GroupChatMessage,
        allCharacters: List<Character>,
        recentMessages: List<GroupChatMessage>
    ): List<Character> {
        // 1. 如果最后一条消息是用户发的，不触发AI互动
        if (lastMessage.senderId == "user") {
            return emptyList()
        }
        
        // 2. 检查是否超过最大连续AI互动次数
        val recentAiChainLength = countRecentAiChain(recentMessages)
        if (recentAiChainLength >= MAX_AI_CHAIN_LENGTH) {
            android.util.Log.d(TAG, "AI连续互动次数过多($recentAiChainLength)，暂停触发")
            return emptyList()
        }
        
        // 3. 检查时间间隔
        val timeSinceLastMessage = System.currentTimeMillis() - lastMessage.timestamp
        if (timeSinceLastMessage < MIN_INTERVAL_MS) {
            android.util.Log.d(TAG, "消息间隔过短，暂停触发")
            return emptyList()
        }
        
        // 4. 获取可能被触发的角色
        val triggeredCharacters = mutableListOf<Character>()
        val otherCharacters = allCharacters.filter { it.id != lastMessage.senderId }
        
        for (character in otherCharacters) {
            val triggerProbability = calculateTriggerProbability(
                character = character,
                message = lastMessage,
                recentMessages = recentMessages
            )
            
            android.util.Log.d(TAG, "${character.name}被触发概率: $triggerProbability")
            
            // 根据概率决定是否触发
            if (Random.nextDouble() < triggerProbability) {
                triggeredCharacters.add(character)
                android.util.Log.d(TAG, "✅ ${character.name}被触发回复")
            }
        }
        
        // 5. 最多只触发1个AI回复（避免token消耗过大）
        return triggeredCharacters.take(1)
    }
    
    /**
     * 计算触发概率
     */
    private fun calculateTriggerProbability(
        character: Character,
        message: GroupChatMessage,
        recentMessages: List<GroupChatMessage>
    ): Double {
        var probability = 0.0
        
        // 1. 检查是否被@或提到名字
        if (isMentioned(character, message.content)) {
            probability = MENTION_INTERACTION_PROBABILITY
            android.util.Log.d(TAG, "${character.name}被提到，概率提升到$probability")
            return probability
        }
        
        // 2. 检查是否是问题（针对该角色）
        if (isQuestionForCharacter(character, message.content)) {
            probability = QUESTION_INTERACTION_PROBABILITY
            android.util.Log.d(TAG, "${character.name}被提问，概率提升到$probability")
            return probability
        }
        
        // 3. 检查话题相关性
        val topicRelevance = analyzeTopicRelevance(character, message.content)
        if (topicRelevance > 0.7) {
            probability = BASE_INTERACTION_PROBABILITY * 1.5
            android.util.Log.d(TAG, "${character.name}话题相关度高($topicRelevance)，概率提升到$probability")
            return probability
        }
        
        // 4. 检查最近发言频率（发言少的角色更容易被触发）
        val recentSpeakCount = recentMessages.takeLast(5).count { it.senderId == character.id }
        if (recentSpeakCount == 0) {
            probability = BASE_INTERACTION_PROBABILITY * 1.2
            android.util.Log.d(TAG, "${character.name}最近未发言，概率提升到$probability")
            return probability
        }
        
        // 5. 基础概率
        probability = BASE_INTERACTION_PROBABILITY * 0.5  // 降低基础概率，避免过度互动
        
        return probability.coerceIn(0.0, 1.0)
    }
    
    /**
     * 检查角色是否被提到
     */
    private fun isMentioned(character: Character, content: String): Boolean {
        val lowerContent = content.lowercase()
        val lowerName = character.name.lowercase()
        
        // 检查@符号
        if (lowerContent.contains("@$lowerName") || lowerContent.contains("@ $lowerName")) {
            return true
        }
        
        // 检查直接提到名字
        if (lowerContent.contains(lowerName)) {
            return true
        }
        
        return false
    }
    
    /**
     * 检查是否是针对该角色的问题
     */
    private fun isQuestionForCharacter(character: Character, content: String): Boolean {
        val lowerContent = content.lowercase()
        val lowerName = character.name.lowercase()
        
        // 检查是否包含问号
        val hasQuestionMark = content.contains("?") || content.contains("？")
        
        // 检查是否提到角色名字
        val mentionsCharacter = lowerContent.contains(lowerName)
        
        return hasQuestionMark && mentionsCharacter
    }
    
    /**
     * 分析话题相关性
     */
    private fun analyzeTopicRelevance(character: Character, content: String): Double {
        val lowerContent = content.lowercase()
        val rolePrompt = character.rolePrompt.lowercase()
        
        // 提取关键词
        val keywords = extractKeywords(lowerContent)
        
        // 计算匹配度
        val matchCount = keywords.count { rolePrompt.contains(it) }
        
        return if (keywords.isNotEmpty()) {
            matchCount.toDouble() / keywords.size
        } else {
            0.0
        }
    }
    
    /**
     * 统计最近的AI连续互动次数
     */
    private fun countRecentAiChain(messages: List<GroupChatMessage>): Int {
        var count = 0
        
        // 从最后一条消息开始往前数
        for (i in messages.size - 1 downTo 0) {
            if (messages[i].senderId == "user") {
                break  // 遇到用户消息就停止
            }
            count++
        }
        
        return count
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): List<String> {
        return text.split(Regex("[\\s，。！？、；：,.!?;:]+"))
            .filter { it.length >= 2 }
            .take(10)
    }
    
    /**
     * 生成AI互动的简化Prompt
     * 用于AI之间的对话，减少token消耗
     */
    fun generateAiInteractionPrompt(
        currentCharacter: Character,
        triggeringMessage: GroupChatMessage,
        allMembers: List<Character>,
        recentMessages: List<GroupChatMessage>
    ): String {
        val triggeringCharacter = allMembers.find { it.id == triggeringMessage.senderId }
        val otherMembers = allMembers.filter { it.id != currentCharacter.id }
        
        // 简化的上下文（只包含最近3条消息）
        val recentContext = recentMessages.takeLast(3)
            .joinToString("\n") { "${it.senderName}: ${it.content}" }
        
        return """
【群聊互动】
你是 ${currentCharacter.name}，${triggeringCharacter?.name ?: "某人"}刚刚说了一句话，你想回应一下。

群聊成员：${otherMembers.joinToString(", ") { it.name }}

【最近对话】
$recentContext

【${triggeringCharacter?.name}刚刚说】
${triggeringMessage.content}

【你的回应】
请以 ${currentCharacter.name} 的身份，给出简短自然的回应（1句话，10-20字即可）。
- 保持你的性格特点
- 回应要自然，不要生硬
- 可以适当表达情绪或观点
- 不要重复别人说过的话

💡 提示：这是AI之间的互动，要简洁有趣，避免冗长。
""".trimIndent()
    }
}
