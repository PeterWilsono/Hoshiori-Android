package com.hoshiori.ai.brain

/**
 * 记忆重排序系统 - 借鉴KnoxChat的Rerank机制
 * 
 * 核心功能：
 * 1. 对检索结果进行二次排序，提高相关性
 * 2. 综合考虑：语义相似度、时间新鲜度、重要性、访问频率
 * 3. 支持多维度权重配置
 * 
 * 使用场景：
 * - 在向量检索后，对top-K结果进行精细化排序
 * - 在知识图谱扩展后，过滤噪声结果
 */
class MemoryRerankSystem {
    
    /**
     * 重排序配置
     */
    data class RerankConfig(
        val semanticWeight: Double = 0.4,      // 语义相似度权重
        val recencyWeight: Double = 0.25,      // 时间新鲜度权重
        val importanceWeight: Double = 0.2,    // 重要性权重
        val frequencyWeight: Double = 0.15     // 访问频率权重
    )
    
    private val defaultConfig = RerankConfig()
    
    /**
     * 对向量检索结果进行重排序
     * 
     * @param results 原始检索结果
     * @param currentTime 当前时间戳
     * @param config 重排序配置
     * @return 重排序后的结果
     */
    fun rerankVectorResults(
        results: List<SearchResult>,
        currentTime: Long = System.currentTimeMillis(),
        config: RerankConfig = defaultConfig
    ): List<RankedResult> {
        if (results.isEmpty()) return emptyList()
        
        val rankedResults = results.map { result ->
            // 1. 语义相似度分数（已归一化）
            val semanticScore = result.similarity
            
            // 2. 时间新鲜度分数
            val timestamp = result.metadata["timestamp"] as? Long ?: currentTime
            val recencyScore = calculateRecencyScore(timestamp, currentTime)
            
            // 3. 重要性分数
            val importance = result.metadata["importance"] as? Int ?: 5
            val importanceScore = importance / 10.0
            
            // 4. 访问频率分数
            val accessCount = result.metadata["access_count"] as? Int ?: 0
            val frequencyScore = calculateFrequencyScore(accessCount)
            
            // 综合得分
            val finalScore = 
                semanticScore * config.semanticWeight +
                recencyScore * config.recencyWeight +
                importanceScore * config.importanceWeight +
                frequencyScore * config.frequencyWeight
            
            RankedResult(
                content = result.content,
                metadata = result.metadata,
                originalScore = result.similarity,
                finalScore = finalScore,
                scoreBreakdown = ScoreBreakdown(
                    semantic = semanticScore,
                    recency = recencyScore,
                    importance = importanceScore,
                    frequency = frequencyScore
                )
            )
        }
        
        return rankedResults.sortedByDescending { it.finalScore }
    }
    
    /**
     * 对联想检索结果进行重排序
     * 
     * @param results 联想检索结果（向量+图谱）
     * @param currentTime 当前时间戳
     * @param config 重排序配置
     * @return 重排序后的结果
     */
    fun rerankAssociationResults(
        results: List<AssociationResult>,
        currentTime: Long = System.currentTimeMillis(),
        config: RerankConfig = defaultConfig
    ): List<RankedAssociationResult> {
        if (results.isEmpty()) return emptyList()
        
        val rankedResults = results.map { result ->
            // 1. 原始相似度分数
            val semanticScore = result.similarity
            
            // 2. 来源权重（向量检索 > 图谱扩展）
            val sourceBoost = if (result.source == "vector") 1.2 else 0.8
            
            // 3. 时间新鲜度
            val timestamp = result.metadata["timestamp"] as? Long ?: currentTime
            val recencyScore = calculateRecencyScore(timestamp, currentTime)
            
            // 4. 重要性
            val importance = result.metadata["importance"] as? Int ?: 5
            val importanceScore = importance / 10.0
            
            // 5. 图谱深度惩罚（深度越大，相关性越低）
            val depth = result.metadata["depth"] as? Int ?: 1
            val depthPenalty = 1.0 / depth
            
            // 综合得分
            val finalScore = (
                semanticScore * config.semanticWeight * sourceBoost +
                recencyScore * config.recencyWeight +
                importanceScore * config.importanceWeight
            ) * depthPenalty
            
            RankedAssociationResult(
                content = result.content,
                metadata = result.metadata,
                source = result.source,
                originalScore = result.similarity,
                finalScore = finalScore,
                scoreBreakdown = ScoreBreakdown(
                    semantic = semanticScore,
                    recency = recencyScore,
                    importance = importanceScore,
                    frequency = 0.0
                )
            )
        }
        
        return rankedResults.sortedByDescending { it.finalScore }
    }
    
    /**
     * 计算时间新鲜度分数
     * 使用指数衰减，半衰期为7天
     */
    private fun calculateRecencyScore(timestamp: Long, currentTime: Long): Double {
        val hoursPassed = (currentTime - timestamp) / (1000.0 * 60 * 60)
        val halfLifeHours = 168.0 // 7天
        return kotlin.math.exp(-0.693 * hoursPassed / halfLifeHours)
    }
    
    /**
     * 计算访问频率分数
     * 使用对数函数，避免过度偏向高频记忆
     */
    private fun calculateFrequencyScore(accessCount: Int): Double {
        if (accessCount == 0) return 0.0
        return kotlin.math.ln(accessCount.toDouble() + 1) / kotlin.math.ln(100.0)
    }
    
    /**
     * 过滤低分结果
     * 
     * @param results 排序后的结果
     * @param threshold 最低分数阈值
     * @return 过滤后的结果
     */
    fun filterByThreshold(
        results: List<RankedResult>,
        threshold: Double = 0.3
    ): List<RankedResult> {
        return results.filter { it.finalScore >= threshold }
    }
    
    /**
     * 多样性采样
     * 避免返回过于相似的记忆
     * 
     * @param results 排序后的结果
     * @param topK 返回数量
     * @param diversityThreshold 相似度阈值（低于此值认为不同）
     * @return 多样化的结果
     */
    fun diversitySampling(
        results: List<RankedResult>,
        topK: Int = 5,
        diversityThreshold: Double = 0.8
    ): List<RankedResult> {
        if (results.isEmpty()) return emptyList()
        
        val selected = mutableListOf<RankedResult>()
        selected.add(results.first()) // 添加最高分
        
        for (result in results.drop(1)) {
            // 检查与已选结果的相似度
            val isDiverse = selected.all { selected ->
                val similarity = calculateContentSimilarity(result.content, selected.content)
                similarity < diversityThreshold
            }
            
            if (isDiverse) {
                selected.add(result)
                if (selected.size >= topK) break
            }
        }
        
        return selected
    }
    
    /**
     * 简单的内容相似度计算（基于词重叠）
     */
    private fun calculateContentSimilarity(content1: String, content2: String): Double {
        val words1 = tokenize(content1).toSet()
        val words2 = tokenize(content2).toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union
    }
    
    private fun tokenize(text: String): List<String> {
        val words = mutableListOf<String>()
        val chinesePattern = Regex("[\\u4e00-\\u9fff]{2,4}")
        words.addAll(chinesePattern.findAll(text).map { it.value })
        val englishPattern = Regex("[a-zA-Z]+")
        words.addAll(englishPattern.findAll(text).map { it.value.lowercase() })
        return words
    }
}

// 数据类
data class RankedResult(
    val content: String,
    val metadata: Map<String, Any>,
    val originalScore: Double,
    val finalScore: Double,
    val scoreBreakdown: ScoreBreakdown
)

data class RankedAssociationResult(
    val content: String,
    val metadata: Map<String, Any>,
    val source: String,
    val originalScore: Double,
    val finalScore: Double,
    val scoreBreakdown: ScoreBreakdown
)

data class ScoreBreakdown(
    val semantic: Double,
    val recency: Double,
    val importance: Double,
    val frequency: Double
)
