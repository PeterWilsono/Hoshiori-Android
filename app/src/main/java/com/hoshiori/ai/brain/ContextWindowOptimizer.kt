package com.hoshiori.ai.brain

import com.hoshiori.ai.data.*

class ContextWindowOptimizer(
    private val maxTokens: Int = 3000,
    private val safeThreshold: Double = 0.7
) {
    private val rerankSystem = MemoryRerankSystem()
    
    fun optimizeContext(
        query: String,
        longTermMemory: LongTermMemory,
        vectorSystem: VectorMemorySystem,
        currentPad: PADValue? = null
    ): OptimizedContext {
        val startTime = System.currentTimeMillis()
        val tokenBudget = calculateTokenBudget()
        val allocation = allocateTokenBudget(tokenBudget)
        val retrievedMemories = retrieveRelevantMemories(query, longTermMemory, vectorSystem, allocation, currentPad)
        val optimizedText = buildOptimizedContext(retrievedMemories, allocation)
        val endTime = System.currentTimeMillis()
        return OptimizedContext(
            text = optimizedText,
            tokenCount = estimateTokens(optimizedText),
            retrievalTimeMs = endTime - startTime,
            memoriesUsed = retrievedMemories.totalCount,
            breakdown = retrievedMemories
        )
    }
    
    private fun calculateTokenBudget(): Int = (maxTokens * safeThreshold).toInt()
    
    private fun allocateTokenBudget(totalBudget: Int): TokenAllocation {
        return TokenAllocation(
            userProfile = (totalBudget * 0.20).toInt(),
            recentEvents = (totalBudget * 0.25).toInt(),
            emotionMemories = (totalBudget * 0.20).toInt(),
            vectorSearch = (totalBudget * 0.25).toInt(),
            knowledgeGraph = (totalBudget * 0.10).toInt()
        )
    }

    private fun retrieveRelevantMemories(
        query: String,
        longTermMemory: LongTermMemory,
        vectorSystem: VectorMemorySystem,
        allocation: TokenAllocation,
        currentPad: PADValue?
    ): RetrievedMemories {
        val memories = RetrievedMemories()
        longTermMemory.userProfile?.let { profile ->
            memories.userProfile = buildUserProfileText(profile, allocation.userProfile)
        }
        memories.recentEvents = selectRecentEvents(longTermMemory.importantEvents, allocation.recentEvents)
        if (currentPad != null) {
            memories.emotionMemories = selectEmotionMemories(longTermMemory, currentPad, allocation.emotionMemories)
        }
        val vectorResults = vectorSystem.search(query, topK = 10, threshold = 0.1)
        val rankedResults = rerankSystem.rerankVectorResults(vectorResults)
        val diverseResults = rerankSystem.diversitySampling(rankedResults, topK = 5)
        memories.vectorSearchResults = diverseResults.map { it.content }
        return memories
    }
    
    private fun buildUserProfileText(profile: UserProfile, maxTokens: Int): String {
        val parts = mutableListOf<String>()
        profile.name?.let { parts.add("姓名：$it") }
        profile.birthday?.let { parts.add("生日：$it") }
        profile.occupation?.let { parts.add("职业：$it") }
        if (profile.profileNotes.isNotEmpty()) {
            parts.add("基本信息：")
            profile.profileNotes.takeLast(3).forEach { parts.add(" $it") }
        }
        if (profile.preferences.isNotEmpty()) {
            parts.add("喜好偏好：")
            profile.preferences.sortedByDescending { it.importance }.take(5).forEach {
                parts.add(" ${it.content}")
            }
        }
        val text = parts.joinToString("\n")
        return truncateToTokens(text, maxTokens)
    }

    private fun selectRecentEvents(events: List<MemoryEvent>, maxTokens: Int): List<String> {
        val selected = events.sortedByDescending { it.importance }.take(5).map { "${it.time}: ${it.content}" }
        return fitToTokenBudget(selected, maxTokens)
    }
    
    private fun selectEmotionMemories(longTermMemory: LongTermMemory, currentPad: PADValue, maxTokens: Int): List<String> {
        val memoryManager = MemoryManager()
        val similarMemories = memoryManager.findSimilarEmotionMemories(currentPad, longTermMemory, threshold = 0.7)
        val selected = similarMemories.take(3).map { memData ->
            val memory = memData.memory
            "${memory.time}: ${memory.content}"
        }
        return fitToTokenBudget(selected, maxTokens)
    }
    
    private fun buildOptimizedContext(memories: RetrievedMemories, allocation: TokenAllocation): String {
        val parts = mutableListOf<String>()
        if (memories.userProfile.isNotEmpty()) {
            parts.add("【用户档案】")
            parts.add(memories.userProfile)
        }
        if (memories.recentEvents.isNotEmpty()) {
            parts.add("\n【最近重要事件】")
            memories.recentEvents.forEach { parts.add(" $it") }
        }
        if (memories.emotionMemories.isNotEmpty()) {
            parts.add("\n【情绪联想记忆】")
            memories.emotionMemories.forEach { parts.add(" $it") }
        }
        if (memories.vectorSearchResults.isNotEmpty()) {
            parts.add("\n【相关记忆】")
            memories.vectorSearchResults.forEach { parts.add(" $it") }
        }
        return parts.joinToString("\n")
    }

    private fun fitToTokenBudget(texts: List<String>, maxTokens: Int): List<String> {
        val result = mutableListOf<String>()
        var currentTokens = 0
        for (text in texts) {
            val tokens = estimateTokens(text)
            if (currentTokens + tokens <= maxTokens) {
                result.add(text)
                currentTokens += tokens
            } else break
        }
        return result
    }
    
    private fun truncateToTokens(text: String, maxTokens: Int): String {
        val currentTokens = estimateTokens(text)
        if (currentTokens <= maxTokens) return text
        val ratio = maxTokens.toDouble() / currentTokens
        val targetLength = (text.length * ratio).toInt()
        return text.take(targetLength) + "..."
    }
    
    private fun estimateTokens(text: String): Int {
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val englishWords = Regex("\\b[a-zA-Z]+\\b").findAll(text).count()
        val others = text.length - chineseChars - englishWords
        return (chineseChars / 1.5 + englishWords + others / 3).toInt()
    }
}

data class TokenAllocation(
    val userProfile: Int,
    val recentEvents: Int,
    val emotionMemories: Int,
    val vectorSearch: Int,
    val knowledgeGraph: Int
)

data class RetrievedMemories(
    var userProfile: String = "",
    var recentEvents: List<String> = emptyList(),
    var emotionMemories: List<String> = emptyList(),
    var vectorSearchResults: List<String> = emptyList(),
    var knowledgeGraphResults: List<String> = emptyList()
) {
    val totalCount: Int
        get() = recentEvents.size + emotionMemories.size + vectorSearchResults.size + knowledgeGraphResults.size
}

data class OptimizedContext(
    val text: String,
    val tokenCount: Int,
    val retrievalTimeMs: Long,
    val memoriesUsed: Int,
    val breakdown: RetrievedMemories
)
