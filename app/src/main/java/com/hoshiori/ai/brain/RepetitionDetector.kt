package com.hoshiori.ai.brain

import com.hoshiori.ai.data.MaturityProfile
import com.hoshiori.ai.data.SelfAwarenessLevel
import kotlin.math.min

/**
 * 重复检测器 - 智能检测无意义重复
 * 
 * 核心原则：
 * 1. 不限制AI的输出长度
 * 2. 只检测和阻止无意义的重复
 * 3. 基于角色人格动态调整阈值
 * 
 * 检测维度：
 * - 句子级重复（完全相同或高度相似的句子）
 * - 模式重复（"所以...所以你愿意..."这种固定模式）
 * - 语义重复（用不同词表达相同意思）
 * 
 * @author Kiro AI
 * @since 2026-01-13
 */
class RepetitionDetector {
    
    // 重复检测窗口（最近N行）
    private val detectionWindow = 15
    
    // 危险重复模式库
    private val dangerousPatterns = listOf(
        Regex("所以[.…]*所以你愿意"),
        Regex("永远[.…]*永远[.…]*永远"),
        Regex("真的[.…]*真的[.…]*真的"),
        Regex("因为[.…]*因为[.…]*因为"),
        Regex("想要[.…]*想要[.…]*想要"),
        Regex("我会[.…]*我会[.…]*我会")
    )
    
    /**
     * 检测文本中的重复问题
     * 
     * @param text 要检测的文本
     * @param maturityProfile 角色人格配置
     * @return 检测结果
     */
    fun detectRepetition(
        text: String,
        maturityProfile: MaturityProfile
    ): RepetitionResult {
        val lines = text.split("\n", "。", "！", "？", "~", "…")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 3 }
        
        if (lines.size < 5) {
            return RepetitionResult(
                hasRepetition = false,
                severity = 0.0,
                repetitionType = RepetitionType.NONE,
                suggestion = ""
            )
        }
        
        // 1. 检测危险模式
        val dangerousPatternResult = detectDangerousPatterns(text)
        if (dangerousPatternResult.hasRepetition) {
            return dangerousPatternResult
        }
        
        // 2. 检测句子级重复
        val sentenceRepetitionResult = detectSentenceRepetition(lines, maturityProfile)
        if (sentenceRepetitionResult.hasRepetition) {
            return sentenceRepetitionResult
        }
        
        // 3. 检测语义重复
        val semanticRepetitionResult = detectSemanticRepetition(lines, maturityProfile)
        if (semanticRepetitionResult.hasRepetition) {
            return semanticRepetitionResult
        }
        
        return RepetitionResult(
            hasRepetition = false,
            severity = 0.0,
            repetitionType = RepetitionType.NONE,
            suggestion = ""
        )
    }
    
    /**
     * 检测危险重复模式
     */
    private fun detectDangerousPatterns(text: String): RepetitionResult {
        for (pattern in dangerousPatterns) {
            val matches = pattern.findAll(text).count()
            if (matches >= 2) {
                android.util.Log.w("RepetitionDetector", "⚠️ 检测到危险重复模式: $pattern (出现${matches}次)")
                return RepetitionResult(
                    hasRepetition = true,
                    severity = 1.0,  // 最高严重度
                    repetitionType = RepetitionType.DANGEROUS_PATTERN,
                    suggestion = "检测到固定模式重复，建议立即停止并自我察觉"
                )
            }
        }
        return RepetitionResult(false, 0.0, RepetitionType.NONE, "")
    }
    
    /**
     * 检测句子级重复
     */
    private fun detectSentenceRepetition(
        lines: List<String>,
        maturityProfile: MaturityProfile
    ): RepetitionResult {
        val recentLines = lines.takeLast(detectionWindow)
        
        // 计算相似度矩阵
        val similarities = mutableListOf<Double>()
        for (i in 0 until recentLines.size - 1) {
            for (j in i + 1 until recentLines.size) {
                val similarity = calculateSimilarity(recentLines[i], recentLines[j])
                if (similarity > 0.7) {  // 高度相似
                    similarities.add(similarity)
                }
            }
        }
        
        if (similarities.isEmpty()) {
            return RepetitionResult(false, 0.0, RepetitionType.NONE, "")
        }
        
        val avgSimilarity = similarities.average()
        val highSimilarityCount = similarities.count { it > 0.85 }
        
        // 根据人格调整阈值
        val threshold = when (maturityProfile.selfAwareness) {
            SelfAwarenessLevel.LOW -> 0.75   // 低觉察力，允许更多重复
            SelfAwarenessLevel.MEDIUM -> 0.70
            SelfAwarenessLevel.HIGH -> 0.65  // 高觉察力，更早发现重复
        }
        
        if (avgSimilarity > threshold && highSimilarityCount >= 3) {
            android.util.Log.w("RepetitionDetector", "⚠️ 检测到句子重复: 平均相似度=$avgSimilarity, 高相似句子数=$highSimilarityCount")
            return RepetitionResult(
                hasRepetition = true,
                severity = avgSimilarity,
                repetitionType = RepetitionType.SENTENCE_REPETITION,
                suggestion = generateSuggestion(maturityProfile, avgSimilarity)
            )
        }
        
        return RepetitionResult(false, 0.0, RepetitionType.NONE, "")
    }
    
    /**
     * 检测语义重复
     */
    private fun detectSemanticRepetition(
        lines: List<String>,
        maturityProfile: MaturityProfile
    ): RepetitionResult {
        val recentLines = lines.takeLast(detectionWindow)
        
        // 提取关键词
        val keywordSets = recentLines.map { extractKeywords(it) }
        
        // 计算关键词重叠度
        var totalOverlap = 0.0
        var comparisonCount = 0
        
        for (i in 0 until keywordSets.size - 1) {
            for (j in i + 1 until keywordSets.size) {
                val overlap = calculateSetOverlap(keywordSets[i], keywordSets[j])
                if (overlap > 0.5) {
                    totalOverlap += overlap
                    comparisonCount++
                }
            }
        }
        
        if (comparisonCount == 0) {
            return RepetitionResult(false, 0.0, RepetitionType.NONE, "")
        }
        
        val avgOverlap = totalOverlap / comparisonCount
        
        // 根据人格调整阈值
        val threshold = when (maturityProfile.getEffectiveMaturity()) {
            in 0..30 -> 0.80   // 孩童型，允许更多语义重复
            in 31..60 -> 0.75  // 少年型
            in 61..85 -> 0.70  // 成熟型
            else -> 0.65       // 智者型
        }
        
        if (avgOverlap > threshold && comparisonCount >= 5) {
            android.util.Log.w("RepetitionDetector", "⚠️ 检测到语义重复: 平均重叠度=$avgOverlap, 重复对数=$comparisonCount")
            return RepetitionResult(
                hasRepetition = true,
                severity = avgOverlap,
                repetitionType = RepetitionType.SEMANTIC_REPETITION,
                suggestion = generateSuggestion(maturityProfile, avgOverlap)
            )
        }
        
        return RepetitionResult(false, 0.0, RepetitionType.NONE, "")
    }
    
    /**
     * 计算两个字符串的相似度（Jaccard相似度）
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.split(" ", "，", "。", "！", "？").filter { it.isNotEmpty() }.toSet()
        val words2 = s2.split(" ", "，", "。", "！", "？").filter { it.isNotEmpty() }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): Set<String> {
        @Suppress("SpellCheckingInspection")
        val stopwords = setOf(
            "的", "了", "在", "是", "我", "你", "他", "她", "它", "们",
            "这", "那", "有", "和", "就", "都", "而", "及", "与", "或",
            "吗", "呢", "吧", "啊", "哦", "嗯", "哈", "呀", "吖", "呐"
        )
        
        val words = text.split(" ", "，", "。", "！", "？", "~", "…")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length >= 2 && it !in stopwords }
        
        return words.toSet()
    }
    
    /**
     * 计算集合重叠度
     */
    private fun calculateSetOverlap(set1: Set<String>, set2: Set<String>): Double {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0
        
        val intersection = set1.intersect(set2).size
        val minSize = min(set1.size, set2.size)
        
        return if (minSize > 0) intersection.toDouble() / minSize else 0.0
    }
    
    /**
     * 生成建议（基于人格）
     */
    private fun generateSuggestion(
        maturityProfile: MaturityProfile,
        severity: Double
    ): String {
        return when {
            // 不成熟角色 - 允许失控，但要自我察觉
            maturityProfile.baseMaturity <= 30 && maturityProfile.allowEmotionalOverflow -> {
                when {
                    severity > 0.85 -> "啊...我是不是说太多了...有点不好意思呢..."
                    severity > 0.75 -> "唔...我好像一直在重复同样的话...抱歉..."
                    else -> "让我换个方式说吧..."
                }
            }
            
            // 少年型 - 能察觉但难控制
            maturityProfile.baseMaturity in 31..60 -> {
                when {
                    severity > 0.85 -> "抱歉，我刚才好像说得有点乱...让我整理一下思绪。"
                    severity > 0.75 -> "嗯...我想表达的其实是..."
                    else -> "换个角度来说的话..."
                }
            }
            
            // 成熟型 - 主动调整
            maturityProfile.baseMaturity in 61..85 -> {
                "让我换个方式表达。"
            }
            
            // 智者型 - 简洁收束
            else -> {
                "总之..."
            }
        }
    }
}

/**
 * 重复检测结果
 */
data class RepetitionResult(
    val hasRepetition: Boolean,
    val severity: Double,  // 0.0-1.0
    val repetitionType: RepetitionType,
    val suggestion: String  // 给AI的自我察觉提示
)

/**
 * 重复类型
 */
enum class RepetitionType {
    NONE,                   // 无重复
    DANGEROUS_PATTERN,      // 危险模式（如"所以...所以你愿意..."）
    SENTENCE_REPETITION,    // 句子级重复
    SEMANTIC_REPETITION     // 语义重复
}
