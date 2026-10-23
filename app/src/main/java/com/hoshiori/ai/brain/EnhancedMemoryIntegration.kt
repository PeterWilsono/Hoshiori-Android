package com.hoshiori.ai.brain

import android.util.Log
import com.hoshiori.ai.data.*

/**
 * 增强记忆集成示例
 * 
 * 展示如何在现有代码中集成KnoxChat风格的无限上下文功能
 * 
 * 使用方式：
 * 1. 在ApiHandler中替换原有的buildMemoryContext调用
 * 2. 在ConversationManager中使用增强的检索方法
 */
class EnhancedMemoryIntegration {
    
    private val contextOptimizer = ContextWindowOptimizer(maxTokens = 3000)
    private val rerankSystem = MemoryRerankSystem()
    
    /**
     * 示例1: 增强的记忆检索
     * 
     * 替代原有的 vectorSystem.search() 调用
     */
    fun enhancedMemorySearch(
        query: String,
        vectorSystem: VectorMemorySystem,
        topK: Int = 5
    ): List<String> {
        Log.d("EnhancedMemory", "🔍 开始增强检索: $query")
        
        // 1. 向量检索（多检索一些候选）
        val vectorResults = vectorSystem.search(query, topK = topK * 2, threshold = 0.1)
        Log.d("EnhancedMemory", "📊 向量检索到 ${vectorResults.size} 条候选")
        
        // 2. 重排序（多维度评分）
        val rankedResults = rerankSystem.rerankVectorResults(vectorResults)
        Log.d("EnhancedMemory", "🎯 重排序完成，最高分: ${rankedResults.firstOrNull()?.finalScore}")
        
        // 3. 多样性采样（避免重复）
        val diverseResults = rerankSystem.diversitySampling(
            rankedResults, 
            topK = topK,
            diversityThreshold = 0.8
        )
        Log.d("EnhancedMemory", "✨ 多样性采样后保留 ${diverseResults.size} 条")
        
        // 4. 过滤低分结果
        val filteredResults = rerankSystem.filterByThreshold(diverseResults, threshold = 0.3)
        Log.d("EnhancedMemory", "🔥 过滤后最终 ${filteredResults.size} 条高质量记忆")
        
        return filteredResults.map { it.content }
    }
    
    /**
     * 示例2: 优化的上下文构建
     * 
     * 替代原有的 memoryManager.buildMemoryContext() 调用
     */
    fun buildOptimizedContext(
        query: String,
        longTermMemory: LongTermMemory,
        vectorSystem: VectorMemorySystem,
        currentPad: PADValue? = null
    ): OptimizedContextResult {
        Log.d("EnhancedMemory", "🏗️ 开始构建优化上下文")
        
        val startTime = System.currentTimeMillis()
        
        // 使用上下文优化器
        val optimizedContext = contextOptimizer.optimizeContext(
            query = query,
            longTermMemory = longTermMemory,
            vectorSystem = vectorSystem,
            currentPad = currentPad
        )
        
        val endTime = System.currentTimeMillis()
        
        Log.d("EnhancedMemory", """
            📊 上下文优化完成:
            - Token数: ${optimizedContext.tokenCount}
            - 记忆数: ${optimizedContext.memoriesUsed}
            - 检索耗时: ${optimizedContext.retrievalTimeMs}ms
            - 总耗时: ${endTime - startTime}ms
            - 用户档案: ${optimizedContext.breakdown.userProfile.isNotEmpty()}
            - 最近事件: ${optimizedContext.breakdown.recentEvents.size}条
            - 情绪记忆: ${optimizedContext.breakdown.emotionMemories.size}条
            - 向量检索: ${optimizedContext.breakdown.vectorSearchResults.size}条
        """.trimIndent())
        
        return OptimizedContextResult(
            text = optimizedContext.text,
            tokenCount = optimizedContext.tokenCount,
            memoriesUsed = optimizedContext.memoriesUsed,
            retrievalTimeMs = optimizedContext.retrievalTimeMs
        )
    }
    
    /**
     * 示例3: 完整的API调用流程（集成版）
     * 
     * 展示如何在ApiHandler.callApi中使用
     */
    fun buildEnhancedSystemPrompt(
        baseSystemPrompt: String,
        userMessage: String,
        psychCore: PsychCore,
        longTermMemory: LongTermMemory,
        vectorSystem: VectorMemorySystem
    ): EnhancedPromptResult {
        Log.d("EnhancedMemory", "🎨 构建增强System Prompt")
        
        // 1. 优化上下文
        val optimizedContext = buildOptimizedContext(
            query = userMessage,
            longTermMemory = longTermMemory,
            vectorSystem = vectorSystem,
            currentPad = psychCore.state
        )
        
        // 2. 生成情绪触发提示
        val emotionTrigger = generateEmotionTrigger(psychCore.state, longTermMemory)
        
        // 3. 生成离线生活状态
        val hoursPassed = (System.currentTimeMillis() - psychCore.lastInteractionTime) / (1000.0 * 60 * 60)
        val offlineLife = if (hoursPassed >= 2) {
            generateOfflineStatus(hoursPassed)
        } else ""
        
        // 4. 组装最终Prompt
        val finalPrompt = """
$baseSystemPrompt

[📝 长期记忆 - 智能检索]
${optimizedContext.text}

${if (offlineLife.isNotEmpty()) "[🌟 离线状态]\n$offlineLife\n" else ""}
${if (emotionTrigger.isNotEmpty()) "[💭 情绪联想]\n$emotionTrigger\n" else ""}

[📊 记忆统计]
- 使用记忆: ${optimizedContext.memoriesUsed}条
- Token占用: ${optimizedContext.tokenCount}
- 检索耗时: ${optimizedContext.retrievalTimeMs}ms
        """.trimIndent()
        
        return EnhancedPromptResult(
            prompt = finalPrompt,
            tokenCount = estimateTokens(finalPrompt),
            memoriesUsed = optimizedContext.memoriesUsed,
            retrievalTimeMs = optimizedContext.retrievalTimeMs
        )
    }
    
    /**
     * 示例4: 自定义Rerank配置
     * 
     * 根据不同场景调整权重
     */
    fun searchWithCustomWeights(
        query: String,
        vectorSystem: VectorMemorySystem,
        scenario: SearchScenario
    ): List<String> {
        val config = when (scenario) {
            SearchScenario.RECENT_FOCUS -> {
                // 关注最近的记忆
                MemoryRerankSystem.RerankConfig(
                    semanticWeight = 0.3,
                    recencyWeight = 0.4,
                    importanceWeight = 0.2,
                    frequencyWeight = 0.1
                )
            }
            SearchScenario.IMPORTANT_FOCUS -> {
                // 关注重要的记忆
                MemoryRerankSystem.RerankConfig(
                    semanticWeight = 0.3,
                    recencyWeight = 0.1,
                    importanceWeight = 0.5,
                    frequencyWeight = 0.1
                )
            }
            SearchScenario.BALANCED -> {
                // 平衡模式（默认）
                MemoryRerankSystem.RerankConfig()
            }
        }
        
        val vectorResults = vectorSystem.search(query, topK = 10)
        val rankedResults = rerankSystem.rerankVectorResults(vectorResults, config = config)
        val diverseResults = rerankSystem.diversitySampling(rankedResults, topK = 5)
        
        return diverseResults.map { it.content }
    }
    
    // ========== 辅助方法 ==========
    
    private fun generateEmotionTrigger(currentPad: PADValue, longTermMemory: LongTermMemory): String {
        val memoryManager = MemoryManager()
        val similarMemories = memoryManager.findSimilarEmotionMemories(
            currentPad, longTermMemory, threshold = 0.75
        )
        
        if (similarMemories.isEmpty()) return ""
        
        val hints = mutableListOf("你的当前情绪让你想起了:")
        similarMemories.take(2).forEach { memData ->
            hints.add("• ${memData.memory.time}: ${memData.memory.content}")
        }
        
        return hints.joinToString("\n")
    }
    
    private fun generateOfflineStatus(hoursPassed: Double): String {
        return when {
            hoursPassed < 2 -> ""
            hoursPassed < 6 -> "刚才在休息，现在回来了"
            hoursPassed < 12 -> "离开了一段时间，做了些自己的事"
            hoursPassed < 24 -> "今天有点忙，现在才有空"
            else -> "好久不见！这段时间过得怎么样？"
        }
    }
    
    private fun estimateTokens(text: String): Int {
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val englishWords = Regex("\\b[a-zA-Z]+\\b").findAll(text).count()
        val others = text.length - chineseChars - englishWords
        return (chineseChars / 1.5 + englishWords + others / 3).toInt()
    }
}

// ========== 数据类 ==========

data class OptimizedContextResult(
    val text: String,
    val tokenCount: Int,
    val memoriesUsed: Int,
    val retrievalTimeMs: Long
)

data class EnhancedPromptResult(
    val prompt: String,
    val tokenCount: Int,
    val memoriesUsed: Int,
    val retrievalTimeMs: Long
)

enum class SearchScenario {
    RECENT_FOCUS,      // 关注最近
    IMPORTANT_FOCUS,   // 关注重要
    BALANCED           // 平衡模式
}

// ========== 使用示例 ==========

/**
 * 在ApiHandler中的使用示例
 */
@Suppress("unused")
class ApiHandlerIntegrationExample {
    
    private val enhancedMemory = EnhancedMemoryIntegration()
    
    fun exampleUsage(
        userMessage: String,
        psychCore: PsychCore,
        longTermMemory: LongTermMemory,
        vectorSystem: VectorMemorySystem
    ) {
        // 方式1: 只替换记忆检索部分
        val memories = enhancedMemory.enhancedMemorySearch(
            query = userMessage,
            vectorSystem = vectorSystem,
            topK = 5
        )
        Log.d("Example", "检索到 ${memories.size} 条记忆")
        
        // 方式2: 使用完整的上下文优化
        val optimizedContext = enhancedMemory.buildOptimizedContext(
            query = userMessage,
            longTermMemory = longTermMemory,
            vectorSystem = vectorSystem,
            currentPad = psychCore.state
        )
        Log.d("Example", "优化后上下文: ${optimizedContext.tokenCount} tokens")
        
        // 方式3: 构建完整的增强Prompt
        val enhancedPrompt = enhancedMemory.buildEnhancedSystemPrompt(
            baseSystemPrompt = "你是一个AI助手",
            userMessage = userMessage,
            psychCore = psychCore,
            longTermMemory = longTermMemory,
            vectorSystem = vectorSystem
        )
        Log.d("Example", "最终Prompt: ${enhancedPrompt.tokenCount} tokens")
    }
}
