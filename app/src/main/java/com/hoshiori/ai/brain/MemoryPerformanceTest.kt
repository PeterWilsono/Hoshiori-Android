package com.hoshiori.ai.brain

import android.util.Log

/**
 * 记忆性能测试
 * 
 * 对比原有实现和KnoxChat增强版的性能差异
 * 
 * 使用方式：
 * ```kotlin
 * val tester = MemoryPerformanceTest()
 * tester.runComparisonTest(vectorSystem, longTermMemory)
 * ```
 */
class MemoryPerformanceTest {
    
    private val TAG = "MemoryPerformanceTest"
    
    /**
     * 运行完整的对比测试
     */
    fun runComparisonTest(
        vectorSystem: VectorMemorySystem,
        longTermMemory: LongTermMemory
    ): TestReport {
        Log.d(TAG, "🧪 开始性能对比测试...")
        
        val testQueries = listOf(
            "你还记得我喜欢什么吗",
            "我们之前聊过什么",
            "我心情不好的时候",
            "上次我们讨论的那个话题",
            "你对我的了解有多少"
        )
        
        val report = TestReport()
        
        for (query in testQueries) {
            Log.d(TAG, "\n📝 测试查询: $query")
            
            // 测试原有方法
            val originalResult = testOriginalMethod(query, vectorSystem)
            report.originalResults.add(originalResult)
            
            // 测试增强方法
            val enhancedResult = testEnhancedMethod(query, vectorSystem, longTermMemory)
            report.enhancedResults.add(enhancedResult)
            
            // 对比
            val improvement = calculateImprovement(originalResult, enhancedResult)
            report.improvements.add(improvement)
            
            Log.d(TAG, """
                📊 对比结果:
                - 原有方法: ${originalResult.resultCount}条, ${originalResult.timeMs}ms
                - 增强方法: ${enhancedResult.resultCount}条, ${enhancedResult.timeMs}ms
                - 性能提升: ${improvement.speedImprovement}%
                - 质量提升: ${improvement.qualityImprovement}%
            """.trimIndent())
        }
        
        // 生成总结报告
        report.generateSummary()
        printReport(report)
        
        return report
    }
    
    /**
     * 测试原有方法
     */
    private fun testOriginalMethod(
        query: String,
        vectorSystem: VectorMemorySystem
    ): TestResult {
        val startTime = System.currentTimeMillis()
        
        // 原有的简单向量检索
        val results = vectorSystem.search(query, topK = 5, threshold = 0.1)
        
        val endTime = System.currentTimeMillis()
        
        return TestResult(
            method = "原有方法",
            query = query,
            resultCount = results.size,
            timeMs = endTime - startTime,
            avgScore = results.map { it.similarity }.average().takeIf { !it.isNaN() } ?: 0.0,
            results = results.map { it.content }
        )
    }
    
    /**
     * 测试增强方法
     */
    private fun testEnhancedMethod(
        query: String,
        vectorSystem: VectorMemorySystem,
        longTermMemory: LongTermMemory
    ): TestResult {
        val startTime = System.currentTimeMillis()
        
        val rerankSystem = MemoryRerankSystem()
        
        // 1. 向量检索
        val vectorResults = vectorSystem.search(query, topK = 10, threshold = 0.1)
        
        // 2. 重排序
        val rankedResults = rerankSystem.rerankVectorResults(vectorResults)
        
        // 3. 多样性采样
        val diverseResults = rerankSystem.diversitySampling(rankedResults, topK = 5)
        
        // 4. 过滤低分
        val filteredResults = rerankSystem.filterByThreshold(diverseResults, threshold = 0.3)
        
        val endTime = System.currentTimeMillis()
        
        return TestResult(
            method = "增强方法",
            query = query,
            resultCount = filteredResults.size,
            timeMs = endTime - startTime,
            avgScore = filteredResults.map { it.finalScore }.average().takeIf { !it.isNaN() } ?: 0.0,
            results = filteredResults.map { it.content }
        )
    }
    
    /**
     * 计算改进幅度
     */
    private fun calculateImprovement(
        original: TestResult,
        enhanced: TestResult
    ): Improvement {
        val speedImprovement = if (original.timeMs > 0) {
            ((original.timeMs - enhanced.timeMs).toDouble() / original.timeMs * 100)
        } else 0.0
        
        val qualityImprovement = if (original.avgScore > 0) {
            ((enhanced.avgScore - original.avgScore) / original.avgScore * 100)
        } else 0.0
        
        return Improvement(
            query = original.query,
            speedImprovement = speedImprovement,
            qualityImprovement = qualityImprovement,
            originalTime = original.timeMs,
            enhancedTime = enhanced.timeMs,
            originalScore = original.avgScore,
            enhancedScore = enhanced.avgScore
        )
    }
    
    /**
     * 打印测试报告
     */
    private fun printReport(report: TestReport) {
        Log.d(TAG, """
            
            ╔═══════════════════════════════════════════════════════════╗
            ║           📊 记忆系统性能对比测试报告                      ║
            ╚═══════════════════════════════════════════════════════════╝
            
            【测试概况】
            - 测试查询数: ${report.originalResults.size}
            - 测试时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
            
            【性能指标】
            - 平均检索时间 (原有): ${report.summary.avgOriginalTime}ms
            - 平均检索时间 (增强): ${report.summary.avgEnhancedTime}ms
            - 速度提升: ${String.format("%.1f", report.summary.avgSpeedImprovement)}%
            
            【质量指标】
            - 平均相关性分数 (原有): ${String.format("%.3f", report.summary.avgOriginalScore)}
            - 平均相关性分数 (增强): ${String.format("%.3f", report.summary.avgEnhancedScore)}
            - 质量提升: ${String.format("%.1f", report.summary.avgQualityImprovement)}%
            
            【结果数量】
            - 平均结果数 (原有): ${String.format("%.1f", report.summary.avgOriginalCount)}条
            - 平均结果数 (增强): ${String.format("%.1f", report.summary.avgEnhancedCount)}条
            
            【详细对比】
        """.trimIndent())
        
        report.improvements.forEachIndexed { index, improvement ->
            Log.d(TAG, """
                ${index + 1}. ${improvement.query}
                   速度: ${improvement.originalTime}ms → ${improvement.enhancedTime}ms (${String.format("%.1f", improvement.speedImprovement)}%)
                   质量: ${String.format("%.3f", improvement.originalScore)} → ${String.format("%.3f", improvement.enhancedScore)} (${String.format("%.1f", improvement.qualityImprovement)}%)
            """.trimIndent())
        }
        
        Log.d(TAG, """
            
            【结论】
            ${generateConclusion(report)}
            
            ═══════════════════════════════════════════════════════════
        """.trimIndent())
    }
    
    /**
     * 生成结论
     */
    private fun generateConclusion(report: TestReport): String {
        val speedUp = report.summary.avgSpeedImprovement
        val qualityUp = report.summary.avgQualityImprovement
        
        return when {
            speedUp > 20 && qualityUp > 20 -> 
                "✅ 增强方法在速度和质量上都有显著提升，强烈推荐使用！"
            speedUp > 10 && qualityUp > 10 -> 
                "✅ 增强方法表现良好，建议采用。"
            speedUp > 0 && qualityUp > 0 -> 
                "✅ 增强方法有一定改进，可以考虑使用。"
            else -> 
                "⚠️ 增强方法在当前数据集上改进不明显，建议调整参数。"
        }
    }
}

// ========== 数据类 ==========

data class TestResult(
    val method: String,
    val query: String,
    val resultCount: Int,
    val timeMs: Long,
    val avgScore: Double,
    val results: List<String>
)

data class Improvement(
    val query: String,
    val speedImprovement: Double,
    val qualityImprovement: Double,
    val originalTime: Long,
    val enhancedTime: Long,
    val originalScore: Double,
    val enhancedScore: Double
)

data class TestReport(
    val originalResults: MutableList<TestResult> = mutableListOf(),
    val enhancedResults: MutableList<TestResult> = mutableListOf(),
    val improvements: MutableList<Improvement> = mutableListOf(),
    var summary: TestSummary = TestSummary()
) {
    fun generateSummary() {
        summary = TestSummary(
            avgOriginalTime = originalResults.map { it.timeMs }.average(),
            avgEnhancedTime = enhancedResults.map { it.timeMs }.average(),
            avgOriginalScore = originalResults.map { it.avgScore }.average(),
            avgEnhancedScore = enhancedResults.map { it.avgScore }.average(),
            avgOriginalCount = originalResults.map { it.resultCount }.average(),
            avgEnhancedCount = enhancedResults.map { it.resultCount }.average(),
            avgSpeedImprovement = improvements.map { it.speedImprovement }.average(),
            avgQualityImprovement = improvements.map { it.qualityImprovement }.average()
        )
    }
}

data class TestSummary(
    val avgOriginalTime: Double = 0.0,
    val avgEnhancedTime: Double = 0.0,
    val avgOriginalScore: Double = 0.0,
    val avgEnhancedScore: Double = 0.0,
    val avgOriginalCount: Double = 0.0,
    val avgEnhancedCount: Double = 0.0,
    val avgSpeedImprovement: Double = 0.0,
    val avgQualityImprovement: Double = 0.0
)

// ========== 使用示例 ==========

/**
 * 在ChatActivity或测试代码中使用
 */
@Suppress("unused")
class PerformanceTestExample {
    
    fun runTest() {
        // 准备测试数据
        val vectorSystem = VectorMemorySystem()
        val longTermMemory = LongTermMemory()
        
        // 添加一些测试记忆
        vectorSystem.addMemory("用户喜欢吃火锅", mapOf("importance" to 8))
        vectorSystem.addMemory("用户是程序员", mapOf("importance" to 9))
        vectorSystem.addMemory("用户最近心情不好", mapOf("importance" to 7))
        vectorSystem.addMemory("我们讨论过AI技术", mapOf("importance" to 6))
        vectorSystem.addMemory("用户养了一只猫", mapOf("importance" to 5))
        
        // 运行测试
        val tester = MemoryPerformanceTest()
        val report = tester.runComparisonTest(vectorSystem, longTermMemory)
        
        // 查看结果
        Log.d("Test", "测试完成，平均速度提升: ${report.summary.avgSpeedImprovement}%")
    }
}
