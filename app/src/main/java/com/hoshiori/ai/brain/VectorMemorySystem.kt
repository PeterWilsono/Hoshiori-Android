package com.hoshiori.ai.brain

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * 向量记忆系统 - 基于TF-IDF的语义检索
 * 完整移植自 Python vector_memory.py
 * 
 * 核心功能：
 * 1. 文本向量化（TF-IDF）
 * 2. 语义相似度搜索
 * 3. 遗忘曲线（时间衰减）
 * 4. 访问频率增强
 */
class SimpleEmbedding(private val dim: Int = 128) {
    
    private val vocab = mutableMapOf<String, Int>()
    private val idf = mutableMapOf<String, Double>()
    
    /**
     * 添加词到 IDF（如果不存在）
     */
    fun ensureWord(word: String) {
        if (word !in idf) {
            idf[word] = 1.0
        }
    }
    
    /**
     * 中文分词（简化版）
     */
    private fun tokenize(text: String): List<String> {
        val words = mutableListOf<String>()
        
        // 提取中文词汇（2-4字）
        val chinesePattern = Regex("[\\u4e00-\\u9fff]{2,4}")
        words.addAll(chinesePattern.findAll(text).map { it.value })
        
        // 提取英文单词
        val englishPattern = Regex("[a-zA-Z]+")
        words.addAll(englishPattern.findAll(text).map { it.value.lowercase() })
        
        return words
    }
    
    /**
     * 构建词汇表和IDF
     */
    fun fit(texts: List<String>) {
        // 统计词频
        val docFreq = mutableMapOf<String, Int>()
        for (text in texts) {
            val words = tokenize(text).toSet()
            for (word in words) {
                docFreq[word] = (docFreq[word] ?: 0) + 1
            }
        }
        
        // 构建词汇表（取前dim个高频词）
        val sortedWords = docFreq.entries.sortedByDescending { it.value }
        vocab.clear()
        sortedWords.take(dim).forEachIndexed { index, entry ->
            vocab[entry.key] = index
        }
        
        // 计算IDF
        val nDocs = texts.size.toDouble()
        idf.clear()
        for ((word, freq) in docFreq) {
            idf[word] = ln(nDocs / (freq + 1))
        }
    }
    
    /**
     * 将文本编码为向量
     */
    fun encode(text: String): List<Double> {
        val vector = MutableList(dim) { 0.0 }
        val words = tokenize(text)
        
        // 计算TF
        val tf = mutableMapOf<String, Int>()
        for (word in words) {
            tf[word] = (tf[word] ?: 0) + 1
        }
        
        // TF-IDF
        for ((word, count) in tf) {
            vocab[word]?.let { idx ->
                vector[idx] = count * (idf[word] ?: 1.0)
            }
        }
        
        // 归一化
        val norm = sqrt(vector.sumOf { it * it })
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        
        return vector
    }
    
    fun hasVocab(): Boolean = vocab.isNotEmpty()
}

/**
 * 向量记忆系统
 */
class VectorMemorySystem(private val dim: Int = 128) {
    
    val embedder = SimpleEmbedding(dim)
    private val memoryVectors = mutableListOf<MemoryVector>()
    private val memoryIndex = mutableMapOf<Int, Int>()
    private var nextId = 0
    
    /**
     * 添加记忆
     */
    fun addMemory(content: String, metadata: Map<String, Any> = emptyMap()): Int {
        // 如果embedder还没有词汇表，先用当前文本初始化
        if (!embedder.hasVocab()) {
            embedder.fit(listOf(content))
        }
        
        val vector = embedder.encode(content)
        val memoryId = nextId++
        
        val memoryData = VectorMemoryData(
            id = memoryId,
            content = content,
            metadata = metadata,
            timestamp = System.currentTimeMillis(),
            accessCount = 0,
            strength = 1.0
        )
        
        val idx = memoryVectors.size
        memoryVectors.add(MemoryVector(vector, memoryData, System.currentTimeMillis()))
        memoryIndex[memoryId] = idx
        
        return memoryId
    }
    
    /**
     * 语义搜索
     */
    fun search(query: String, topK: Int = 5, threshold: Double = 0.1): List<SearchResult> {
        if (memoryVectors.isEmpty()) return emptyList()
        
        // 确保query中的词在词汇表中
        val queryWords = tokenize(query)
        for (word in queryWords) {
            embedder.ensureWord(word)
        }
        
        val queryVector = embedder.encode(query)
        
        // 计算相似度
        val similarities = mutableListOf<Triple<Int, Double, VectorMemoryData>>()
        
        for ((idx, memoryVector) in memoryVectors.withIndex()) {
            val similarity = cosineSimilarity(queryVector, memoryVector.vector)
            
            // 应用遗忘曲线
            val timeDecay = calculateTimeDecay(memoryVector.timestamp)
            val accessBoost = 1.0 + 0.1 * memoryVector.data.accessCount
            
            val finalScore = similarity * timeDecay * accessBoost * memoryVector.data.strength
            
            if (finalScore >= threshold) {
                similarities.add(Triple(idx, finalScore, memoryVector.data))
            }
        }
        
        // 排序并返回top_k
        similarities.sortByDescending { it.second }
        
        val results = mutableListOf<SearchResult>()
        for ((idx, score, memoryData) in similarities.take(topK)) {
            // 更新访问计数
            memoryVectors[idx].data.accessCount++
            memoryVectors[idx].data.strength = minOf(1.0, memoryData.strength * 1.1)
            
            results.add(SearchResult(
                content = memoryData.content,
                metadata = memoryData.metadata,
                similarity = score,
                memoryId = memoryData.id
            ))
        }
        
        return results
    }
    
    /**
     * 余弦相似度
     */
    private fun cosineSimilarity(v1: List<Double>, v2: List<Double>): Double {
        val dotProduct = v1.zip(v2).sumOf { (a, b) -> a * b }
        return dotProduct.coerceIn(0.0, 1.0)
    }
    
    /**
     * 遗忘曲线（时间衰减）
     */
    private fun calculateTimeDecay(timestamp: Long): Double {
        val hoursPassed = (System.currentTimeMillis() - timestamp) / (1000.0 * 60 * 60)
        val halfLife = 168.0 // 7天半衰期
        return exp(-0.693 * hoursPassed / halfLife)
    }
    
    /**
     * 从JSON记忆中构建向量索引
     */
    fun updateFromJson(longTermMemory: LongTermMemory) {
        val allTexts = mutableListOf<String>()
        
        // 收集所有文本
        for (event in longTermMemory.emotionalEvents) {
            allTexts.add(event.content)
        }
        
        for (event in longTermMemory.importantEvents) {
            allTexts.add(event.content)
        }
        
        longTermMemory.userProfile?.preferences?.forEach { pref ->
            allTexts.add(pref.content)
        }
        
        // 训练embedder
        if (allTexts.isNotEmpty()) {
            embedder.fit(allTexts)
            
            // 添加记忆
            for (event in longTermMemory.emotionalEvents) {
                addMemory(
                    event.content,
                    mapOf("type" to "emotional_event", "importance" to event.importance)
                )
            }
            
            for (event in longTermMemory.importantEvents) {
                addMemory(
                    event.content,
                    mapOf("type" to "important_event", "importance" to event.importance)
                )
            }
        }
    }
    
    /**
     * 获取记忆数量
     */
    fun getMemoryCount(): Int = memoryVectors.size
    
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
data class MemoryVector(
    val vector: List<Double>,
    val data: VectorMemoryData,
    val timestamp: Long
)

data class VectorMemoryData(
    val id: Int,
    val content: String,
    val metadata: Map<String, Any>,
    val timestamp: Long,
    var accessCount: Int,
    var strength: Double
)

data class SearchResult(
    val content: String,
    val metadata: Map<String, Any>,
    val similarity: Double,
    val memoryId: Int
)
