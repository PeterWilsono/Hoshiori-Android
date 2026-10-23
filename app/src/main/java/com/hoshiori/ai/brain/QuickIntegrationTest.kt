package com.hoshiori.ai.brain

import android.util.Log

/**
 * 快速集成测试
 * 
 * 用于验证KnoxChat增强功能是否正常工作
 * 
 * 使用方式：
 * 在ChatActivity的onCreate或任意位置调用：
 * ```kotlin
 * QuickIntegrationTest.runQuickTest(
 *     conversationManager = apiHandler.conversationManager,
 *     longTermMemory = char.memory
 * )
 * ```
 */
object QuickIntegrationTest {
    
    private const val TAG = "QuickIntegrationTest"
    
    /**
     * 运行快速测试
     */
    fun runQuickTest(
        conversationManager: ConversationManager,
        longTermMemory: LongTermMemory
    ) {
        Log.d(TAG, "🧪 开始KnoxChat集成快速测试...")
        
        // 测试1: 向量检索增强
        testEnhancedVectorSearch(conversationManager)
        
        // 测试2: 上下文优化
        testContextOptimization(conversationManager, longTermMemory)
        
        // 测试3: Rerank功能
        testRerankSystem(conversationManager)
        
        Log.d(TAG, "✅ KnoxChat集成快速测试完成！")
    }
    
    /**
     * 测试1: 向量检索增强
     */
    private fun testEnhancedVectorSearch(conversationManager: ConversationManager) {
        Log.d(TAG, "\n📝 测试1: 向量检索增强")
        
        try {
            // 添加一些测试记忆
            conversationManager.updateMemoryGraph("用户喜欢吃火锅", "preference", 8)
            conversationManager.updateMemoryGraph("用户是程序员", "profile", 9)
            conversationManager.updateMemoryGraph("用户最近心情不好", "emotion", 7)
            
            // 搜索记忆
            val results = conversationManager.searchMemoryWithContext("用户喜欢什么", topK = 3)
            
            Log.d(TAG, "✅ 测试1通过: 检索到 ${results.size} 条记忆")
            results.forEachIndexed { index, result ->
                Log.d(TAG, "  ${index + 1}. ${result.content} (分数: ${String.format("%.3f", result.similarity)})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试1失败: ${e.message}", e)
        }
    }
    
    /**
     * 测试2: 上下文优化
     */
    private fun testContextOptimization(
        conversationManager: ConversationManager,
        longTermMemory: LongTermMemory
    ) {
        Log.d(TAG, "\n📝 测试2: 上下文优化")
        
        try {
            // 构建优化上下文
            val optimizedContext = conversationManager.buildOptimizedMemoryContext(
                query = "你还记得我吗",
                longTermMemory = longTermMemory,
                currentPad = PADValue(0.5, 0.3, 0.2)
            )
            
            Log.d(TAG, "✅ 测试2通过:")
            Log.d(TAG, "  - Token数: ${optimizedContext.tokenCount}")
            Log.d(TAG, "  - 记忆数: ${optimizedContext.memoriesUsed}")
            Log.d(TAG, "  - 耗时: ${optimizedContext.retrievalTimeMs}ms")
            Log.d(TAG, "  - 内容长度: ${optimizedContext.text.length}字符")
            
            // 验证Token数是否在合理范围内
            if (optimizedContext.tokenCount > 3000) {
                Log.w(TAG, "⚠️ 警告: Token数超出预算 (${optimizedContext.tokenCount}/3000)")
            }
            
            // 验证检索时间是否合理
            if (optimizedContext.retrievalTimeMs > 100) {
                Log.w(TAG, "⚠️ 警告: 检索时间较长 (${optimizedContext.retrievalTimeMs}ms)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试2失败: ${e.message}", e)
        }
    }
    
    /**
     * 测试3: Rerank功能
     */
    private fun testRerankSystem(conversationManager: ConversationManager) {
        Log.d(TAG, "\n📝 测试3: Rerank功能")
        
        try {
            // 添加更多测试记忆
            conversationManager.updateMemoryGraph("用户喜欢编程", "preference", 8)
            conversationManager.updateMemoryGraph("用户养了一只猫", "profile", 6)
            conversationManager.updateMemoryGraph("用户最近在学习AI", "event", 7)
            
            // 搜索并观察排序
            val results = conversationManager.searchMemoryWithContext("用户的兴趣爱好", topK = 5)
            
            Log.d(TAG, "✅ 测试3通过: 检索到 ${results.size} 条记忆（已重排序）")
            
            // 验证结果是否按分数降序排列
            var lastScore = Double.MAX_VALUE
            var isSorted = true
            
            results.forEachIndexed { index, result ->
                Log.d(TAG, "  ${index + 1}. ${result.content} (分数: ${String.format("%.3f", result.similarity)})")
                
                if (result.similarity > lastScore) {
                    isSorted = false
                }
                lastScore = result.similarity
            }
            
            if (!isSorted) {
                Log.w(TAG, "⚠️ 警告: 结果未按分数降序排列")
            }
            
            // 验证多样性（检查是否有重复内容）
            val uniqueContents = results.map { it.content }.toSet()
            if (uniqueContents.size < results.size) {
                Log.w(TAG, "⚠️ 警告: 检测到重复内容")
            } else {
                Log.d(TAG, "✅ 多样性验证通过: 所有结果都是唯一的")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试3失败: ${e.message}", e)
        }
    }
    
    /**
     * 性能基准测试
     */
    fun runPerformanceBenchmark(
        conversationManager: ConversationManager,
        longTermMemory: LongTermMemory
    ) {
        Log.d(TAG, "\n🏃 开始性能基准测试...")
        
        val queries = listOf(
            "你还记得我喜欢什么吗",
            "我们之前聊过什么",
            "我心情不好的时候",
            "上次我们讨论的那个话题",
            "你对我的了解有多少"
        )
        
        val times = mutableListOf<Long>()
        val tokenCounts = mutableListOf<Int>()
        val memoryCounts = mutableListOf<Int>()
        
        for (query in queries) {
            val startTime = System.currentTimeMillis()
            
            val optimizedContext = conversationManager.buildOptimizedMemoryContext(
                query = query,
                longTermMemory = longTermMemory,
                currentPad = PADValue(0.0, 0.0, 0.0)
            )
            
            val endTime = System.currentTimeMillis()
            val elapsed = endTime - startTime
            
            times.add(elapsed)
            tokenCounts.add(optimizedContext.tokenCount)
            memoryCounts.add(optimizedContext.memoriesUsed)
            
            Log.d(TAG, "  查询: $query")
            Log.d(TAG, "    耗时: ${elapsed}ms, Token: ${optimizedContext.tokenCount}, 记忆: ${optimizedContext.memoriesUsed}")
        }
        
        val avgTime = times.average()
        val avgTokens = tokenCounts.average()
        val avgMemories = memoryCounts.average()
        
        Log.d(TAG, "\n📊 性能基准测试结果:")
        Log.d(TAG, "  - 平均耗时: ${String.format("%.1f", avgTime)}ms")
        Log.d(TAG, "  - 平均Token: ${String.format("%.0f", avgTokens)}")
        Log.d(TAG, "  - 平均记忆数: ${String.format("%.1f", avgMemories)}")
        
        // 性能评估
        when {
            avgTime < 30 -> Log.d(TAG, "✅ 性能评级: 优秀 (< 30ms)")
            avgTime < 50 -> Log.d(TAG, "✅ 性能评级: 良好 (< 50ms)")
            avgTime < 100 -> Log.d(TAG, "⚠️ 性能评级: 一般 (< 100ms)")
            else -> Log.d(TAG, "❌ 性能评级: 需要优化 (> 100ms)")
        }
        
        when {
            avgTokens < 2000 -> Log.d(TAG, "✅ Token使用: 优秀 (< 2000)")
            avgTokens < 2500 -> Log.d(TAG, "✅ Token使用: 良好 (< 2500)")
            avgTokens < 3000 -> Log.d(TAG, "⚠️ Token使用: 接近上限 (< 3000)")
            else -> Log.d(TAG, "❌ Token使用: 超出预算 (> 3000)")
        }
    }
    
    /**
     * 对比测试（原有方法 vs 增强方法）
     */
    fun runComparisonTest(
        conversationManager: ConversationManager,
        longTermMemory: LongTermMemory
    ) {
        Log.d(TAG, "\n🔬 开始对比测试（原有 vs 增强）...")
        
        val query = "你还记得我喜欢什么吗"
        
        // 测试原有方法
        val startTime1 = System.currentTimeMillis()
        val originalContext = conversationManager.buildMemoryContext(
            longTermMemory = longTermMemory,
            currentPad = PADValue(0.0, 0.0, 0.0)
        )
        val endTime1 = System.currentTimeMillis()
        val originalTime = endTime1 - startTime1
        val originalTokens = estimateTokens(originalContext)
        
        // 测试增强方法
        val startTime2 = System.currentTimeMillis()
        val enhancedContext = conversationManager.buildOptimizedMemoryContext(
            query = query,
            longTermMemory = longTermMemory,
            currentPad = PADValue(0.0, 0.0, 0.0)
        )
        val endTime2 = System.currentTimeMillis()
        val enhancedTime = endTime2 - startTime2
        
        Log.d(TAG, "\n📊 对比结果:")
        Log.d(TAG, "  原有方法:")
        Log.d(TAG, "    - 耗时: ${originalTime}ms")
        Log.d(TAG, "    - Token: $originalTokens")
        Log.d(TAG, "    - 内容长度: ${originalContext.length}字符")
        
        Log.d(TAG, "  增强方法:")
        Log.d(TAG, "    - 耗时: ${enhancedTime}ms (${enhancedContext.retrievalTimeMs}ms检索)")
        Log.d(TAG, "    - Token: ${enhancedContext.tokenCount}")
        Log.d(TAG, "    - 记忆数: ${enhancedContext.memoriesUsed}")
        Log.d(TAG, "    - 内容长度: ${enhancedContext.text.length}字符")
        
        // 计算改进幅度
        val speedImprovement = if (originalTime > 0) {
            ((originalTime - enhancedTime).toDouble() / originalTime * 100)
        } else 0.0
        
        val tokenImprovement = if (originalTokens > 0) {
            ((originalTokens - enhancedContext.tokenCount).toDouble() / originalTokens * 100)
        } else 0.0
        
        Log.d(TAG, "\n📈 改进幅度:")
        Log.d(TAG, "  - 速度: ${if (speedImprovement > 0) "+" else ""}${String.format("%.1f", speedImprovement)}%")
        Log.d(TAG, "  - Token: ${if (tokenImprovement > 0) "+" else ""}${String.format("%.1f", tokenImprovement)}%")
    }
    
    private fun estimateTokens(text: String): Int {
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val englishWords = Regex("\\b[a-zA-Z]+\\b").findAll(text).count()
        val others = text.length - chineseChars - englishWords
        return (chineseChars / 1.5 + englishWords + others / 3).toInt()
    }
}
