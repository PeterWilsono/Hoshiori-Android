package com.hoshiori.ai.brain

import java.util.Locale

/**
 * 情绪分析器 - 双通道架构
 * 完整移植自 Python emotion_analyzer.py
 */
class EmotionAnalyzer {
    
    // 情感关键词库（规则匹配通道）
    private val emotionKeywords = mapOf(
        "joy" to EmotionData(
            keywords = listOf("开心", "高兴", "快乐", "幸福", "兴奋", "欣喜", "愉快", "哈哈", "嘻嘻", "😊", "😄", "🎉"),
            pad = PADValue(0.6, 0.4, 0.2)
        ),
        "sadness" to EmotionData(
            keywords = listOf("难过", "伤心", "痛苦", "悲伤", "失落", "沮丧", "郁闷", "哭", "😢", "😭"),
            pad = PADValue(-0.6, -0.2, -0.3)
        ),
        "anger" to EmotionData(
            keywords = listOf("生气", "愤怒", "气愤", "恼火", "火大", "讨厌", "烦", "😠", "😡", "💢"),
            pad = PADValue(-0.5, 0.7, 0.5)
        ),
        "fear" to EmotionData(
            keywords = listOf("害怕", "恐惧", "担心", "紧张", "焦虑", "不安", "😰", "😨"),
            pad = PADValue(-0.4, 0.3, -0.5)
        ),
        "surprise" to EmotionData(
            keywords = listOf("惊讶", "震惊", "意外", "没想到", "竟然", "😲", "😮"),
            pad = PADValue(0.2, 0.6, 0.0)
        ),
        "disgust" to EmotionData(
            keywords = listOf("恶心", "厌恶", "反感", "🤢", "🤮"),
            pad = PADValue(-0.6, 0.2, 0.3)
        ),
        "trust" to EmotionData(
            keywords = listOf("相信", "信任", "依赖", "放心", "靠谱"),
            pad = PADValue(0.4, 0.1, -0.2)
        ),
        "anticipation" to EmotionData(
            keywords = listOf("期待", "盼望", "希望", "想要", "等待"),
            pad = PADValue(0.3, 0.3, 0.1)
        )
    )
    
    // 情绪强度修饰词
    private val intensityModifiers = mapOf(
        "very_high" to listOf("超级", "非常", "特别", "极其", "太", "巨", "爆", "超"),
        "high" to listOf("很", "挺", "蛮", "好"),
        "low" to listOf("有点", "稍微", "略微", "一点")
    )
    
    // Plutchik情绪轮 - 复杂情绪映射
    private val complexEmotions = mapOf(
        "love" to ComplexEmotion(listOf("joy", "trust"), 0.4),
        "guilt" to ComplexEmotion(listOf("fear", "sadness"), 0.4),
        "delight" to ComplexEmotion(listOf("joy", "surprise"), 0.4),
        "curiosity" to ComplexEmotion(listOf("trust", "surprise"), 0.3),
        "awe" to ComplexEmotion(listOf("fear", "surprise"), 0.4),
        "despair" to ComplexEmotion(listOf("fear", "sadness"), 0.5),
        "remorse" to ComplexEmotion(listOf("sadness", "disgust"), 0.4),
        "contempt" to ComplexEmotion(listOf("anger", "disgust"), 0.4),
        "optimism" to ComplexEmotion(listOf("anticipation", "joy"), 0.3),
        "anxiety" to ComplexEmotion(listOf("fear", "anticipation"), 0.4),
        "disappointment" to ComplexEmotion(listOf("surprise", "sadness"), 0.4)
    )
    
    /**
     * 分析文本情绪
     */
    fun analyzeText(text: String): EmotionResult {
        // 通道1: 关键词匹配
        val keywordResult = keywordMatching(text)
        
        // 通道2: 规则分析
        val ruleResult = ruleBasedAnalysis(text)
        
        // 融合两个通道
        val basicEmotions = mergeChannels(keywordResult, ruleResult)
        
        // 检测复杂情绪
        val complexEmotionsResult = detectComplexEmotions(basicEmotions)
        
        // 计算PAD值
        val padPrediction = calculatePAD(basicEmotions)
        
        // 确定主导情绪
        val dominantEmotion = basicEmotions.maxByOrNull { it.value }?.key ?: "neutral"
        
        // 计算置信度
        val confidence = calculateConfidence(basicEmotions)
        
        return EmotionResult(
            basicEmotions = basicEmotions,
            complexEmotions = complexEmotionsResult,
            padPrediction = padPrediction,
            dominantEmotion = dominantEmotion,
            confidence = confidence
        )
    }
    
    private fun keywordMatching(text: String): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        
        for ((emotion, data) in emotionKeywords) {
            var score = 0.0
            for (keyword in data.keywords) {
                if (text.contains(keyword)) {
                    var baseScore = 0.3
                    
                    // ⭐ 增强：检查强度修饰词（充分利用）
                    var foundModifier = false
                    for ((intensity, modifiers) in intensityModifiers) {
                        for (modifier in modifiers) {
                            // 检查修饰词在关键词前后
                            val beforePattern = "$modifier$keyword"
                            val afterPattern = "$keyword$modifier"
                            
                            if (text.contains(beforePattern) || text.contains(afterPattern)) {
                                baseScore = when (intensity) {
                                    "very_high" -> 0.9
                                    "high" -> 0.6
                                    "low" -> 0.2
                                    else -> baseScore
                                }
                                foundModifier = true
                                break
                            }
                        }
                        if (foundModifier) break
                    }
                    
                    score = maxOf(score, baseScore)
                }
            }
            if (score > 0) {
                scores[emotion] = score
            }
        }
        return scores
    }
    
    private fun ruleBasedAnalysis(text: String): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        
        // 检测感叹
        if (text.contains("!") || text.contains("！")) {
            scores["joy"] = (scores["joy"] ?: 0.0) + 0.2
            scores["surprise"] = (scores["surprise"] ?: 0.0) + 0.1
        }
        
        // 检测疑问
        if (text.contains("?") || text.contains("？") || 
            listOf("吗", "呢", "吧").any { text.contains(it) }) {
            scores["anticipation"] = (scores["anticipation"] ?: 0.0) + 0.1
        }
        
        // 检测极端词汇
        val extremePositive = listOf("太好了", "完美", "棒极了", "爱死了")
        val extremeNegative = listOf("糟糕", "完蛋", "崩溃", "绝望")
        
        if (extremePositive.any { text.contains(it) }) {
            scores["joy"] = 0.8
        }
        if (extremeNegative.any { text.contains(it) }) {
            scores["sadness"] = 0.7
            scores["fear"] = 0.5
        }
        
        return scores
    }
    
    private fun mergeChannels(
        keywordScores: Map<String, Double>,
        ruleScores: Map<String, Double>
    ): Map<String, Double> {
        val merged = mutableMapOf<String, Double>()
        val allEmotions = keywordScores.keys + ruleScores.keys
        
        for (emotion in allEmotions) {
            val keywordScore = keywordScores[emotion] ?: 0.0
            val ruleScore = ruleScores[emotion] ?: 0.0
            merged[emotion] = 0.7 * keywordScore + 0.3 * ruleScore
        }
        return merged
    }
    
    private fun detectComplexEmotions(basicEmotions: Map<String, Double>): Map<String, Double> {
        val complex = mutableMapOf<String, Double>()
        
        for ((complexEmotion, config) in complexEmotions) {
            val scores = config.primary.map { basicEmotions[it] ?: 0.0 }
            if (scores.all { it >= config.threshold }) {
                complex[complexEmotion] = scores.average()
            }
        }
        return complex
    }
    
    private fun calculatePAD(basicEmotions: Map<String, Double>): PADValue {
        if (basicEmotions.isEmpty()) return PADValue(0.0, 0.0, 0.0)
        
        var p = 0.0
        var a = 0.0
        var d = 0.0
        val totalWeight = basicEmotions.values.sum()
        
        for ((emotion, score) in basicEmotions) {
            emotionKeywords[emotion]?.let { data ->
                val weight = score / totalWeight
                p += data.pad.pleasure * weight
                a += data.pad.arousal * weight
                d += data.pad.dominance * weight
            }
        }
        
        return PADValue(
            pleasure = p.coerceIn(-1.0, 1.0),
            arousal = a.coerceIn(-1.0, 1.0),
            dominance = d.coerceIn(-1.0, 1.0)
        )
    }
    
    private fun calculateConfidence(basicEmotions: Map<String, Double>): Double {
        if (basicEmotions.isEmpty()) return 0.0
        val maxScore = basicEmotions.values.maxOrNull() ?: 0.0
        val highScores = basicEmotions.values.count { it >= maxScore * 0.8 }
        return if (highScores > 2) maxScore * 0.7 else maxScore
    }
    
    /**
     * ⭐ 增强：验证AI回复与当前情绪状态的一致性（细粒度控制）
     */
    fun verifyResponseConsistency(aiResponse: String, currentPad: PADValue): ConsistencyCheckResult {
        val responseEmotion = analyzeText(aiResponse)
        val responsePad = responseEmotion.padPrediction
        
        val pDiff = kotlin.math.abs(currentPad.pleasure - responsePad.pleasure)
        val aDiff = kotlin.math.abs(currentPad.arousal - responsePad.arousal)
        val dDiff = kotlin.math.abs(currentPad.dominance - responsePad.dominance)
        
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        // 检查Pleasure维度
        if (pDiff > 1.0) {
            if (currentPad.pleasure < -0.5 && responsePad.pleasure > 0.3) {
                issues.add("情绪极性不一致：当前悲伤(P=${String.format(Locale.US, "%.2f", currentPad.pleasure)})，但回复积极(P=${String.format(Locale.US, "%.2f", responsePad.pleasure)})")
                suggestions.add("建议添加情绪过渡语句，如'虽然我也想开心起来，但是...'")
            } else if (currentPad.pleasure > 0.5 && responsePad.pleasure < -0.3) {
                issues.add("情绪极性不一致：当前愉悦(P=${String.format(Locale.US, "%.2f", currentPad.pleasure)})，但回复消极(P=${String.format(Locale.US, "%.2f", responsePad.pleasure)})")
                suggestions.add("建议保持积极语气，或解释为什么突然变得消极")
            }
        }
        
        // 检查Arousal维度
        if (aDiff > 0.8) {
            if (currentPad.arousal < -0.3 && responsePad.arousal > 0.5) {
                issues.add("能量不匹配：当前低落(A=${String.format(Locale.US, "%.2f", currentPad.arousal)})，但回复激动(A=${String.format(Locale.US, "%.2f", responsePad.arousal)})")
                suggestions.add("建议降低能量，使用更平静的语气")
            } else if (currentPad.arousal > 0.7 && responsePad.arousal < 0) {
                issues.add("能量不匹配：当前激动(A=${String.format(Locale.US, "%.2f", currentPad.arousal)})，但回复平淡(A=${String.format(Locale.US, "%.2f", responsePad.arousal)})")
                suggestions.add("建议提高能量，使用更有活力的表达")
            }
        }
        
        // 检查Dominance维度
        if (dDiff > 0.6) {
            if (currentPad.dominance < -0.3 && responsePad.dominance > 0.3) {
                issues.add("掌控感不匹配：当前被动(D=${String.format(Locale.US, "%.2f", currentPad.dominance)})，但回复主动(D=${String.format(Locale.US, "%.2f", responsePad.dominance)})")
                suggestions.add("建议使用更温和、顺从的语气")
            }
        }
        
        val isConsistent = issues.isEmpty()
        val overallSuggestion = if (isConsistent) {
            "情绪一致性良好"
        } else {
            "检测到${issues.size}个情绪不一致问题：\n${issues.joinToString("\n")}\n\n修正建议：\n${suggestions.joinToString("\n")}"
        }
        
        return ConsistencyCheckResult(
            isConsistent = isConsistent,
            issues = issues,
            suggestions = suggestions,
            overallSuggestion = overallSuggestion,
            pDiff = pDiff,
            aDiff = aDiff,
            dDiff = dDiff
        )
    }
    
    /**
     * ⭐ 新增：情绪强度评估
     */
    fun assessEmotionIntensity(text: String): EmotionIntensity {
        val emotion = analyzeText(text)
        val pad = emotion.padPrediction
        
        val intensity = kotlin.math.sqrt(
            pad.pleasure * pad.pleasure +
            pad.arousal * pad.arousal +
            pad.dominance * pad.dominance
        ) / kotlin.math.sqrt(3.0)
        
        val level = when {
            intensity > 0.7 -> "极强"
            intensity > 0.5 -> "强"
            intensity > 0.3 -> "中等"
            intensity > 0.1 -> "弱"
            else -> "极弱"
        }
        
        return EmotionIntensity(
            intensity = intensity,
            level = level,
            dominantEmotion = emotion.dominantEmotion
        )
    }
    
    /**
     * ⭐ 新增：将PAD值转化为自然语言描述
     * 
     * 这是改进方案的核心功能之一：让情绪有温度，不是冰冷的数字
     * 
     * @param pad PAD值
     * @param context 上下文信息（可选），如"和用户聊天时"
     * @return 自然语言描述，如"比较开心，但不是那种兴奋的开心，更像是平静中带着一点愉悦"
     */
    fun padToNaturalLanguage(pad: PADValue, context: String = ""): String {
        val p = pad.pleasure
        val a = pad.arousal
        val d = pad.dominance
        
        // 基础情绪描述
        val pleasureDesc = when {
            p > 0.6 -> "很开心"
            p > 0.3 -> "比较开心"
            p > 0.1 -> "心情还不错"
            p > -0.1 -> "心情平静"
            p > -0.3 -> "有点不开心"
            p > -0.6 -> "比较难过"
            else -> "很难过"
        }
        
        // 激活度描述
        val arousalDesc = when {
            a > 0.6 -> "很兴奋"
            a > 0.3 -> "有点激动"
            a > 0.1 -> "稍微有点活力"
            a > -0.1 -> "平静"
            a > -0.3 -> "有点疲惫"
            a > -0.6 -> "比较低落"
            else -> "非常疲惫"
        }
        
        // 支配度描述
        val dominanceDesc = when {
            d > 0.5 -> "感觉很有掌控感"
            d > 0.2 -> "比较主动"
            d > -0.2 -> "不主动也不被动"
            d > -0.5 -> "比较被动"
            else -> "感觉很被动"
        }
        
        // 组合描述（更自然的表达）
        val description = StringBuilder()
        
        // 主要情绪
        description.append(pleasureDesc)
        
        // 添加激活度的细节
        if (kotlin.math.abs(a) > 0.2) {
            if (p > 0 && a > 0.5) {
                description.append("，不是那种平静的开心，而是那种兴奋的、激动的感觉")
            } else if (p > 0 && a < -0.2) {
                description.append("，但不是那种兴奋的开心，更像是平静中带着一点愉悦")
            } else if (p < 0 && a > 0.5) {
                description.append("，而且心里很烦躁，静不下来")
            } else if (p < 0 && a < -0.2) {
                description.append("，感觉很疲惫，提不起劲")
            }
        }
        
        // 添加支配度的细节
        if (kotlin.math.abs(d) > 0.3) {
            if (d > 0.3) {
                description.append("。感觉自己还挺有掌控感的")
            } else if (d < -0.3) {
                description.append("。感觉有点被动，不太想主动做什么")
            }
        }
        
        // 添加上下文
        if (context.isNotEmpty()) {
            description.insert(0, "$context，")
        }
        
        return description.toString()
    }
    
    /**
     * ⭐ 新增：生成情绪变化描述
     * 
     * 描述从一个情绪状态到另一个情绪状态的变化
     * 
     * @param fromPad 之前的PAD值
     * @param toPad 当前的PAD值
     * @return 变化描述，如"从平静变得有点激动了"
     */
    fun describeEmotionChange(fromPad: PADValue, toPad: PADValue): String {
        val pChange = toPad.pleasure - fromPad.pleasure
        val aChange = toPad.arousal - fromPad.arousal
        val dChange = toPad.dominance - fromPad.dominance
        
        val changes = mutableListOf<String>()
        
        // 愉悦度变化
        if (kotlin.math.abs(pChange) > 0.2) {
            if (pChange > 0.5) {
                changes.add("心情变好了很多")
            } else if (pChange > 0.2) {
                changes.add("心情变好了一些")
            } else if (pChange < -0.5) {
                changes.add("心情变差了很多")
            } else if (pChange < -0.2) {
                changes.add("心情变差了一些")
            }
        }
        
        // 激活度变化
        if (kotlin.math.abs(aChange) > 0.2) {
            if (aChange > 0.5) {
                changes.add("变得激动起来")
            } else if (aChange > 0.2) {
                changes.add("变得有点兴奋")
            } else if (aChange < -0.5) {
                changes.add("变得很疲惫")
            } else if (aChange < -0.2) {
                changes.add("平静了下来")
            }
        }
        
        // 支配度变化
        if (kotlin.math.abs(dChange) > 0.3) {
            if (dChange > 0.3) {
                changes.add("感觉更有掌控感了")
            } else if (dChange < -0.3) {
                changes.add("变得更被动了")
            }
        }
        
        return if (changes.isEmpty()) {
            "情绪没有太大变化"
        } else {
            changes.joinToString("，")
        }
    }
}

data class PADValue(
    val pleasure: Double,
    val arousal: Double,
    val dominance: Double
)

data class EmotionData(
    val keywords: List<String>,
    val pad: PADValue
)

data class ComplexEmotion(
    val primary: List<String>,
    val threshold: Double
)

data class EmotionResult(
    val basicEmotions: Map<String, Double>,
    val complexEmotions: Map<String, Double>,
    val padPrediction: PADValue,
    val dominantEmotion: String,
    val confidence: Double
)

/**
 * ⭐ 新增：情绪一致性检查结果（细粒度）
 */
data class ConsistencyCheckResult(
    val isConsistent: Boolean,
    val issues: List<String>,
    val suggestions: List<String>,
    val overallSuggestion: String,
    val pDiff: Double,
    val aDiff: Double,
    val dDiff: Double
)

/**
 * ⭐ 新增：情绪强度评估结果
 */
data class EmotionIntensity(
    val intensity: Double,
    val level: String,
    val dominantEmotion: String
)
