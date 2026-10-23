package com.hoshiori.ai.brain

import com.hoshiori.ai.data.MaturityProfile
import com.hoshiori.ai.data.ExpressionStyle
import com.hoshiori.ai.data.RecoverySpeed

/**
 * 情感表达控制器
 * 
 * 核心功能：
 * 1. 实时监控AI的输出流
 * 2. 检测重复并在适当时机干预
 * 3. 模拟人类的"自我察觉"过程
 * 4. 生成自然的收束语言
 * 5. ⭐ 记录事件用于成熟度评估
 * 
 * 工作流程：
 * 1. AI开始输出 → 正常流式传输
 * 2. 检测到重复 → 根据人格决定是否干预
 * 3. 触发干预 → 注入自我察觉提示
 * 4. AI自然收束 → 恢复正常状态
 * 5. ⭐ 记录事件 → 用于成熟度进化
 * 
 * @author Kiro AI
 * @since 2026-01-13
 */
class EmotionalExpressionController(
    private val maturityProfile: MaturityProfile,
    private val maturityEvolutionSystem: MaturityEvolutionSystem? = null  // 可选，用于记录事件
) {
    
    private val repetitionDetector = RepetitionDetector()
    
    // 当前输出状态
    private var currentOutput = StringBuilder()
    private var isInOverflowState = false
    private var overflowStartTime = 0L
    
    // 统计信息
    private var totalChunksProcessed = 0
    private var interventionCount = 0
    
    /**
     * 处理输出块（流式传输）
     * 
     * @param chunk 新的输出块
     * @param affectionLevel 当前好感度
     * @return 处理结果
     */
    fun processChunk(
        chunk: String,
        affectionLevel: Int
    ): ChunkProcessResult {
        totalChunksProcessed++
        currentOutput.append(chunk)
        
        // 每收到一定量的文本就检测一次
        if (currentOutput.length % 100 < chunk.length) {
            val detectionResult = repetitionDetector.detectRepetition(
                currentOutput.toString(),
                maturityProfile
            )
            
            if (detectionResult.hasRepetition) {
                return handleRepetitionDetected(detectionResult, affectionLevel)
            }
        }
        
        // 正常传输
        return ChunkProcessResult(
            shouldContinue = true,
            processedChunk = chunk,
            interventionMessage = null,
            state = ExpressionState.NORMAL
        )
    }
    
    /**
     * 处理检测到的重复
     */
    private fun handleRepetitionDetected(
        detectionResult: RepetitionResult,
        affectionLevel: Int
    ): ChunkProcessResult {
        interventionCount++
        
        android.util.Log.w("EmotionalExpressionController", """
            🚨 检测到重复！
            - 类型: ${detectionResult.repetitionType}
            - 严重度: ${detectionResult.severity}
            - 好感度: $affectionLevel
            - 基础成熟度: ${maturityProfile.baseMaturity}
            - 情绪控制: ${maturityProfile.emotionalControl}
            - 表现成熟度: ${maturityProfile.getEffectiveMaturity()}
        """.trimIndent())
        
        // ⭐ 记录重复事件（用于成熟度评估）
        maturityEvolutionSystem?.recordRepetitionEvent(
            severity = detectionResult.severity,
            repetitionType = detectionResult.repetitionType,
            wasInterrupted = true
        )
        
        // 根据重复类型和人格决定干预策略
        return when (detectionResult.repetitionType) {
            RepetitionType.DANGEROUS_PATTERN -> {
                // 危险模式 - 立即干预
                handleDangerousPattern(detectionResult)
            }
            
            RepetitionType.SENTENCE_REPETITION,
            RepetitionType.SEMANTIC_REPETITION -> {
                // 普通重复 - 根据人格决定
                handleNormalRepetition(detectionResult, affectionLevel)
            }
            
            RepetitionType.NONE -> {
                ChunkProcessResult(
                    shouldContinue = true,
                    processedChunk = "",
                    interventionMessage = null,
                    state = ExpressionState.NORMAL
                )
            }
        }
    }
    
    /**
     * 处理危险模式
     */
    private fun handleDangerousPattern(
        detectionResult: RepetitionResult
    ): ChunkProcessResult {
        isInOverflowState = true
        overflowStartTime = System.currentTimeMillis()
        
        val interventionMessage = generateInterventionMessage(
            detectionResult,
            isEmergency = true
        )
        
        android.util.Log.e("EmotionalExpressionController", "🛑 危险模式检测，强制干预: $interventionMessage")
        
        return ChunkProcessResult(
            shouldContinue = false,  // 停止继续生成
            processedChunk = "",
            interventionMessage = interventionMessage,
            state = ExpressionState.OVERFLOW_DETECTED
        )
    }
    
    /**
     * 处理普通重复
     */
    private fun handleNormalRepetition(
        detectionResult: RepetitionResult,
        affectionLevel: Int
    ): ChunkProcessResult {
        // 根据人格和好感度决定是否干预
        val shouldIntervene = shouldIntervene(detectionResult.severity, affectionLevel)
        
        if (!shouldIntervene && maturityProfile.allowEmotionalOverflow) {
            // 允许继续（不成熟角色的特权）
            android.util.Log.d("EmotionalExpressionController", "💭 检测到重复但允许继续（角色特性）")
            return ChunkProcessResult(
                shouldContinue = true,
                processedChunk = "",
                interventionMessage = null,
                state = ExpressionState.EMOTIONAL_OVERFLOW
            )
        }
        
        // 需要干预
        isInOverflowState = true
        overflowStartTime = System.currentTimeMillis()
        
        val interventionMessage = generateInterventionMessage(
            detectionResult,
            isEmergency = false
        )
        
        android.util.Log.w("EmotionalExpressionController", "⚠️ 触发干预: $interventionMessage")
        
        return ChunkProcessResult(
            shouldContinue = false,
            processedChunk = "",
            interventionMessage = interventionMessage,
            state = ExpressionState.SELF_AWARENESS_TRIGGERED
        )
    }
    
    /**
     * 判断是否应该干预
     */
    private fun shouldIntervene(severity: Double, affectionLevel: Int): Boolean {
        // 基于表现成熟度的基础阈值
        val baseThreshold = when (maturityProfile.getEffectiveMaturity()) {
            in 0..30 -> 0.90    // 孩童型 - 很高的容忍度
            in 31..60 -> 0.80   // 少年型 - 中等容忍度
            in 61..85 -> 0.70   // 成熟型 - 较低容忍度
            else -> 0.65        // 智者型 - 最低容忍度
        }
        
        // 好感度越高，越容易情感失控，需要更早干预
        val affectionAdjustment = when {
            affectionLevel > 80 -> -0.10  // 高好感度，降低阈值
            affectionLevel > 60 -> -0.05
            else -> 0.0
        }
        
        val finalThreshold = baseThreshold + affectionAdjustment
        
        return severity > finalThreshold
    }
    
    /**
     * 生成干预消息（模拟自我察觉）
     */
    private fun generateInterventionMessage(
        detectionResult: RepetitionResult,
        isEmergency: Boolean
    ): String {
        // ⭐ 记录自我察觉事件
        val responseQuality = if (isEmergency) 0.5 else 0.8
        maturityEvolutionSystem?.recordSelfAwarenessEvent(
            triggerType = detectionResult.repetitionType.name,
            responseQuality = responseQuality
        )
        
        if (isEmergency) {
            // 紧急情况 - 强制停止
            return when (maturityProfile.expressionStyle) {
                ExpressionStyle.VERBOSE -> "\n\n啊...我好像一直在重复同样的话...抱歉，让我冷静一下..."
                ExpressionStyle.RESTRAINED -> "\n\n抱歉，我有点失态了。"
                ExpressionStyle.POETIC -> "\n\n...让我整理一下思绪。"
            }
        }
        
        // 使用检测器生成的建议
        return "\n\n${detectionResult.suggestion}"
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        currentOutput.clear()
        isInOverflowState = false
        overflowStartTime = 0L
        totalChunksProcessed = 0
        interventionCount = 0
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): ControllerStatistics {
        return ControllerStatistics(
            totalChunksProcessed = totalChunksProcessed,
            interventionCount = interventionCount,
            currentOutputLength = currentOutput.length,
            isInOverflowState = isInOverflowState,
            overflowDuration = if (isInOverflowState) {
                System.currentTimeMillis() - overflowStartTime
            } else 0L
        )
    }
}

/**
 * 块处理结果
 */
data class ChunkProcessResult(
    val shouldContinue: Boolean,        // 是否应该继续生成
    val processedChunk: String,         // 处理后的块（可能被修改）
    val interventionMessage: String?,   // 干预消息（如果有）
    val state: ExpressionState          // 当前表达状态
)

/**
 * 表达状态
 */
enum class ExpressionState {
    NORMAL,                      // 正常状态
    EMOTIONAL_OVERFLOW,          // 情感失控（允许的）
    OVERFLOW_DETECTED,           // 检测到失控
    SELF_AWARENESS_TRIGGERED,    // 触发自我察觉
    RECOVERING                   // 恢复中
}

/**
 * 控制器统计信息
 */
data class ControllerStatistics(
    val totalChunksProcessed: Int,
    val interventionCount: Int,
    val currentOutputLength: Int,
    val isInOverflowState: Boolean,
    val overflowDuration: Long
)
