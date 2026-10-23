package com.hoshiori.ai.brain

import com.hoshiori.ai.data.*
import kotlin.math.min
import kotlin.math.max

/**
 * 情绪控制系统 - 学习控制情绪爆炸
 * 
 * 核心理念（重新设计）：
 * 1. 角色的基础性格不变（天真的就是天真的，成熟的就是成熟的）
 * 2. 但可以学会"控制情绪爆炸"，减少重复
 * 3. 对不同用户有不同的情绪控制能力
 * 4. 核心目标：解决重复问题，而不是改变性格
 * 
 * 评估维度：
 * - 重复频率：是否经常重复
 * - 自我察觉：是否能意识到重复并调整
 * - 恢复能力：失控后能否快速恢复
 * 
 * 注意：类名保持为 MaturityEvolutionSystem 以保持向后兼容
 * 
 * @author Kiro AI
 * @since 2026-01-13
 * @version 3.0 - 重新设计为情绪控制系统
 */
class MaturityEvolutionSystem(
    private val growthSpeed: GrowthSpeed = GrowthSpeed.NATURAL
) {
    
    // 情绪控制评估历史（最近50次对话）
    private val controlHistory = mutableListOf<ControlSnapshot>()
    
    // 重复事件记录
    private val repetitionEvents = mutableListOf<RepetitionEvent>()
    
    // 自我察觉事件记录
    private val selfAwarenessEvents = mutableListOf<SelfAwarenessEvent>()
    
    /**
     * 评估并更新情绪控制能力
     * 
     * 注意：只更新emotionalControl，不改变baseMaturity
     * 
     * @param character 角色
     * @param conversationContext 对话上下文
     * @return 更新后的情绪控制能力
     */
    fun evaluateAndUpdateControl(
        character: Character,
        conversationContext: EmotionalControlContext
    ): Int {
        val profile = character.psychCore.maturityProfile
        val currentControl = profile.emotionalControl
        
        // 简化评估：只关注3个维度
        // 1. 重复频率 (40%)
        val repetitionScore = evaluateRepetitionFrequency()
        
        // 2. 自我察觉 (40%)
        val awarenessScore = evaluateSelfAwareness()
        
        // 3. 恢复能力 (20%)
        val recoveryScore = evaluateRecovery(conversationContext)
        
        // 综合评分
        val overallScore = (
            repetitionScore * 0.4 +
            awarenessScore * 0.4 +
            recoveryScore * 0.2
        ).toInt()
        
        android.util.Log.d("EmotionalControl", """
            📊 情绪控制评估:
            - 重复频率: $repetitionScore
            - 自我察觉: $awarenessScore
            - 恢复能力: $recoveryScore
            - 综合评分: $overallScore
            - 当前控制力: $currentControl
            - 基础成熟度: ${profile.baseMaturity}（不变）
        """.trimIndent())
        
        // 计算新的情绪控制能力
        val newControl = calculateNewControl(currentControl, overallScore)
        
        // 记录快照
        recordSnapshot(character, overallScore, newControl)
        
        // 更新情绪控制能力
        profile.emotionalControl = newControl
        
        // 同步更新相关属性（基于情绪控制能力）
        updateControlRelatedAttributes(profile)
        
        android.util.Log.i("EmotionalControl", "✨ 情绪控制更新: $currentControl → $newControl (变化: ${newControl - currentControl})")
        
        return newControl
    }
    
    /**
     * 评估重复频率
     */
    private fun evaluateRepetitionFrequency(): Int {
        val recentRepetitions = repetitionEvents.filter { 
            System.currentTimeMillis() - it.timestamp < 7 * 24 * 60 * 60 * 1000 // 最近7天
        }
        
        return when {
            recentRepetitions.isEmpty() -> 90  // 无重复
            recentRepetitions.size <= 2 -> 75  // 偶尔重复
            recentRepetitions.size <= 5 -> 55  // 经常重复
            else -> 35  // 频繁重复
        }
    }
    
    /**
     * 评估自我察觉能力
     */
    private fun evaluateSelfAwareness(): Int {
        val recentAwareness = selfAwarenessEvents.filter {
            System.currentTimeMillis() - it.timestamp < 7 * 24 * 60 * 60 * 1000
        }
        
        val recentRepetitions = repetitionEvents.filter {
            System.currentTimeMillis() - it.timestamp < 7 * 24 * 60 * 60 * 1000
        }
        
        return when {
            recentRepetitions.isEmpty() -> 80  // 没有重复，无法评估
            recentAwareness.size >= recentRepetitions.size -> 90  // 每次都能察觉
            recentAwareness.size >= recentRepetitions.size / 2 -> 70  // 大部分能察觉
            recentAwareness.isNotEmpty() -> 50  // 偶尔能察觉
            else -> 30  // 从不察觉
        }
    }
    
    /**
     * 评估恢复能力
     */
    private fun evaluateRecovery(context: EmotionalControlContext): Int {
        // 如果本次对话有重复但能快速恢复 → 高分
        // 如果重复后持续失控 → 低分
        return if (context.hadRepetitionInThisConversation) {
            // 有重复，检查是否恢复
            if (context.aiResponse.length < 500) 70 else 50
        } else {
            80  // 无重复，正常
        }
    }
    
    /**
     * 计算新的情绪控制能力
     * 
     * 使用指数移动平均，缓慢变化
     */
    private fun calculateNewControl(currentControl: Int, overallScore: Int): Int {
        // 根据成长速度调整权重
        val alpha = when (growthSpeed) {
            GrowthSpeed.VERY_SLOW -> 0.05
            GrowthSpeed.SLOW -> 0.08
            GrowthSpeed.NATURAL -> 0.10
            GrowthSpeed.FAST -> 0.15
            GrowthSpeed.VERY_FAST -> 0.20
        }
        
        val newControl = (currentControl * (1 - alpha) + overallScore * alpha).toInt()
        
        // 根据成长速度调整单次变化幅度
        val maxChange = when (growthSpeed) {
            GrowthSpeed.VERY_SLOW -> 1
            GrowthSpeed.SLOW -> 1
            GrowthSpeed.NATURAL -> 2
            GrowthSpeed.FAST -> 3
            GrowthSpeed.VERY_FAST -> 5
        }
        
        val change = (newControl - currentControl).coerceIn(-maxChange, maxChange)
        
        // 添加惯性保护
        val trend = getRecentTrend()
        val finalChange = if (shouldApplyInertia(change, trend)) {
            (change * 0.5).toInt()
        } else {
            change
        }
        
        android.util.Log.d("EmotionalControl", """
            📊 情绪控制计算:
            - 当前: $currentControl
            - 评分: $overallScore
            - 成长速度: $growthSpeed
            - Alpha: $alpha
            - 原始变化: $change
            - 趋势: $trend
            - 最终变化: $finalChange
            - 新控制力: ${currentControl + finalChange}
        """.trimIndent())
        
        return (currentControl + finalChange).coerceIn(0, 100)
    }
    
    /**
     * 获取最近的趋势
     */
    private fun getRecentTrend(): Int {
        if (controlHistory.size < 5) return 0
        
        val recent = controlHistory.takeLast(5)
        val firstControl = recent.first().control
        val lastControl = recent.last().control
        
        return when {
            lastControl - firstControl > 3 -> 1   // 上升趋势
            lastControl - firstControl < -3 -> -1 // 下降趋势
            else -> 0  // 稳定
        }
    }
    
    /**
     * 判断是否应该应用惯性
     */
    private fun shouldApplyInertia(change: Int, trend: Int): Boolean {
        return (change > 0 && trend < 0) || (change < 0 && trend > 0)
    }
    
    /**
     * 更新与情绪控制相关的属性
     * 
     * 注意：不改变基础成熟度相关的属性（如expressionStyle）
     */
    private fun updateControlRelatedAttributes(profile: MaturityProfile) {
        val control = profile.emotionalControl
        
        // 自我觉察能力随情绪控制能力提升
        profile.selfAwareness = when {
            control >= 70 -> SelfAwarenessLevel.HIGH
            control >= 40 -> SelfAwarenessLevel.MEDIUM
            else -> SelfAwarenessLevel.LOW
        }
        
        // 恢复速度随情绪控制能力提升
        profile.overflowRecoverySpeed = when {
            control >= 70 -> RecoverySpeed.FAST
            control >= 40 -> RecoverySpeed.MEDIUM
            else -> RecoverySpeed.SLOW
        }
        
        // 注意：以下属性由baseMaturity决定，不在这里更新：
        // - expressionStyle（表达风格）
        // - emotionalThreshold（情感阈值）
        // - allowEmotionalOverflow（是否允许失控）
    }
    
    /**
     * 记录重复事件
     */
    fun recordRepetitionEvent(
        severity: Double,
        repetitionType: RepetitionType,
        wasInterrupted: Boolean
    ) {
        repetitionEvents.add(RepetitionEvent(
            timestamp = System.currentTimeMillis(),
            severity = severity,
            type = repetitionType,
            wasInterrupted = wasInterrupted
        ))
        
        // 只保留最近100条
        if (repetitionEvents.size > 100) {
            repetitionEvents.removeAt(0)
        }
        
        android.util.Log.d("MaturityEvolution", "📝 记录重复事件: $repetitionType (严重度: $severity)")
    }
    
    /**
     * 记录自我察觉事件
     */
    fun recordSelfAwarenessEvent(
        triggerType: String,
        responseQuality: Double
    ) {
        selfAwarenessEvents.add(SelfAwarenessEvent(
            timestamp = System.currentTimeMillis(),
            triggerType = triggerType,
            responseQuality = responseQuality
        ))
        
        // 只保留最近100条
        if (selfAwarenessEvents.size > 100) {
            selfAwarenessEvents.removeAt(0)
        }
        
        android.util.Log.d("MaturityEvolution", "✨ 记录自我察觉: $triggerType (质量: $responseQuality)")
    }
    
    /**
     * 记录快照
     */
    private fun recordSnapshot(character: Character, score: Int, newControl: Int) {
        controlHistory.add(ControlSnapshot(
            timestamp = System.currentTimeMillis(),
            characterId = character.id,
            control = newControl,
            evaluationScore = score,
            relationshipIntimacy = character.psychCore.relationship.intimacy,
            emotionArousal = character.psychCore.state.arousal
        ))
        
        // 只保留最近50条
        if (controlHistory.size > 50) {
            controlHistory.removeAt(0)
        }
    }
    
    /**
     * 获取情绪控制趋势
     */
    fun getControlTrend(): ControlTrend {
        if (controlHistory.size < 10) {
            return ControlTrend(
                direction = TrendDirection.STABLE,
                changeRate = 0.0,
                confidence = 0.0
            )
        }
        
        val recent = controlHistory.takeLast(10)
        val firstControl = recent.first().control
        val lastControl = recent.last().control
        val change = lastControl - firstControl
        
        val direction = when {
            change > 5 -> TrendDirection.GROWING
            change < -5 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
        
        val changeRate = change / 10.0  // 平均每次对话的变化
        val confidence = min(1.0, controlHistory.size / 50.0)  // 数据越多，置信度越高
        
        return ControlTrend(direction, changeRate, confidence)
    }
    
    /**
     * 获取情绪控制报告
     */
    fun getControlReport(character: Character): String {
        val profile = character.psychCore.maturityProfile
        val control = profile.emotionalControl
        val baseMaturity = profile.baseMaturity
        val effectiveMaturity = profile.getEffectiveMaturity()
        val trend = getControlTrend()
        
        val controlLevel = when {
            control >= 85 -> "优秀"
            control >= 65 -> "良好"
            control >= 35 -> "一般"
            else -> "需要提升"
        }
        
        val characterType = when {
            baseMaturity >= 85 -> "智者型"
            baseMaturity >= 65 -> "成熟型"
            baseMaturity >= 35 -> "少年型"
            else -> "孩童型"
        }
        
        val trendDesc = when (trend.direction) {
            TrendDirection.GROWING -> "正在提升 ↗"
            TrendDirection.DECLINING -> "有所下降 ↘"
            TrendDirection.STABLE -> "保持稳定 →"
        }
        
        return """
            【AI情绪控制报告】
            
            角色性格: $characterType (基础成熟度: $baseMaturity/100)
            情绪控制: $controlLevel ($control/100)
            表现成熟度: $effectiveMaturity/100
            发展趋势: $trendDesc
            变化速率: ${String.format("%.1f", trend.changeRate)}/次对话
            数据置信度: ${(trend.confidence * 100).toInt()}%
            
            最近表现:
            - 重复事件: ${repetitionEvents.filter { System.currentTimeMillis() - it.timestamp < 7 * 24 * 60 * 60 * 1000 }.size}次/周
            - 自我察觉: ${selfAwarenessEvents.filter { System.currentTimeMillis() - it.timestamp < 7 * 24 * 60 * 60 * 1000 }.size}次/周
            
            ${getControlAdvice(control, baseMaturity, trend)}
        """.trimIndent()
    }
    
    /**
     * 获取情绪控制建议
     */
    private fun getControlAdvice(control: Int, baseMaturity: Int, trend: ControlTrend): String {
        return when {
            control < 30 && trend.direction == TrendDirection.DECLINING -> 
                "💡 建议: AI正在经历情绪控制困难期，可能需要更多耐心和引导。"
            
            control < 30 && trend.direction == TrendDirection.GROWING -> 
                "✨ 进展: AI正在学习情绪控制，继续保持当前的交流方式。"
            
            control in 30..60 && trend.direction == TrendDirection.GROWING -> 
                "🌟 成长: AI的情绪控制能力正在提升，重复问题会逐渐减少。"
            
            control > 70 && trend.direction == TrendDirection.STABLE -> 
                "🎯 优秀: AI已经能够很好地控制情绪，很少出现重复问题。"
            
            control > 70 && trend.direction == TrendDirection.DECLINING -> 
                "⚠️ 注意: 情绪控制能力有所下降，可能是最近对话质量不佳。"
            
            baseMaturity < 30 && control > 50 -> 
                "🎭 平衡: 虽然角色性格天真，但已经学会了控制情绪爆炸。"
            
            else -> 
                "📊 正常: AI的情绪控制能力处于正常发展轨道。"
        }
    }
}

/**
 * 情绪控制上下文（用于情绪控制评估）
 */
data class EmotionalControlContext(
    val aiResponse: String,
    val internalMonologue: String,
    val currentEmotion: PADValue,
    val relationshipIntimacy: Int,
    val hadRepetitionInThisConversation: Boolean,
    val userMessageLength: Int
)

/**
 * 情绪控制快照
 */
data class ControlSnapshot(
    val timestamp: Long,
    val characterId: String,
    val control: Int,
    val evaluationScore: Int,
    val relationshipIntimacy: Int,
    val emotionArousal: Double
)

/**
 * 重复事件
 */
data class RepetitionEvent(
    val timestamp: Long,
    val severity: Double,
    val type: RepetitionType,
    val wasInterrupted: Boolean
)

/**
 * 自我察觉事件
 */
data class SelfAwarenessEvent(
    val timestamp: Long,
    val triggerType: String,
    val responseQuality: Double
)

/**
 * 情绪控制趋势
 */
data class ControlTrend(
    val direction: TrendDirection,
    val changeRate: Double,  // 每次对话的平均变化
    val confidence: Double   // 0.0-1.0
)

/**
 * 趋势方向
 */
enum class TrendDirection {
    GROWING,    // 成长中
    DECLINING,  // 退化中
    STABLE      // 稳定
}

/**
 * 成长里程碑
 */
data class GrowthMilestone(
    val fromLevel: String,
    val toLevel: String,
    val message: String
)

/**
 * 成长速度
 * 
 * 控制AI成熟度变化的快慢
 * 默认使用NATURAL（自然速度），让成长更真实
 */
enum class GrowthSpeed(
    val description: String,
    val conversationsNeeded: Int  // 需要多少次对话才能显著改变
) {
    VERY_SLOW("极慢 - 像真人一样需要很长时间", 20),
    SLOW("慢速 - 成长需要耐心培养", 12),
    NATURAL("自然 - 符合真实成长规律", 10),  // 默认
    FAST("快速 - 较快看到成长效果", 7),
    VERY_FAST("极快 - 仅用于测试", 5)
}
