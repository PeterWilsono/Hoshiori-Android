package com.hoshiori.ai.optimization

import com.hoshiori.ai.data.*
import com.hoshiori.ai.brain.PADValue
import java.util.*

/**
 * Token优化器 - AI友好版
 * 
 * 核心原则：
 * 1. 压缩但保持可读性
 * 2. 保留关键上下文
 * 3. 使用AI能理解的格式
 * 4. 场景化动态加载
 * 
 * @author Kiro AI
 * @since 2026-01-13
 */
class TokenOptimizer {
    
    /**
     * 优化Prompt（AI友好版）
     */
    fun optimizePrompt(
        rolePrompt: String,
        psychCore: PsychCore,
        memoryContext: String,
        scenario: ConversationScenario,
        offlineLife: String = "",
        emotionTrigger: String = "",
        psychologyEnhanced: String = ""
    ): OptimizedPrompt {
        
        return when (scenario) {
            ConversationScenario.CASUAL_CHAT -> 
                buildMinimalPrompt(rolePrompt, psychCore)
            
            ConversationScenario.EMOTIONAL_SUPPORT -> 
                buildEmotionalPrompt(rolePrompt, psychCore, memoryContext, psychologyEnhanced)
            
            ConversationScenario.MEMORY_RECALL -> 
                buildMemoryPrompt(rolePrompt, psychCore, memoryContext, emotionTrigger)
            
            ConversationScenario.DEEP_DISCUSSION -> 
                buildFullPrompt(rolePrompt, psychCore, memoryContext, offlineLife, psychologyEnhanced)
            
            ConversationScenario.FIRST_MEETING -> 
                buildFirstMeetingPrompt(rolePrompt, psychCore)
        }
    }
    
    /**
     * 最小Prompt（闲聊场景）
     * 
     * 策略：只保留核心信息，但保持可读性
     */
    private fun buildMinimalPrompt(
        rolePrompt: String,
        psychCore: PsychCore
    ): OptimizedPrompt {
        val coreRole = extractCoreRole(rolePrompt)
        val state = formatStateReadable(psychCore)
        val time = getTimeContext()
        
        val prompt = """
$coreRole

$time

【当前状态】
$state

【核心规则】
- 你有完整心理，根据PAD值调整语气
- 情绪低落时可简短回复
- 禁止连续提问
- 可以有自己观点

【输出格式】
```json
{
  "internal_monologue": "内心独白",
  "pad_delta": {"p": 0.0, "a": 0.0, "d": 0.0},
  "relationship_delta": {"trust": 0, "intimacy": 0, "stress": 0},
  "dominant_emotion": "neutral"
}
```
-----
你的回复文本
        """.trimIndent()
        
        return OptimizedPrompt(
            content = prompt,
            scenario = ConversationScenario.CASUAL_CHAT,
            estimatedTokens = estimateTokens(prompt)
        )
    }
    
    /**
     * 情感支持Prompt
     * 
     * 策略：加载心理学规则，但简化其他部分
     */
    private fun buildEmotionalPrompt(
        rolePrompt: String,
        psychCore: PsychCore,
        memoryContext: String,
        psychologyEnhanced: String
    ): OptimizedPrompt {
        val coreRole = extractCoreRole(rolePrompt)
        val state = formatStateReadable(psychCore)
        val time = getTimeContext()
        val memory = if (memoryContext.isNotEmpty()) {
            formatMemoryCompact(memoryContext)
        } else ""
        
        val prompt = """
$coreRole

$time

【当前状态】
$state

${if (memory.isNotEmpty()) "【重要记忆】\n$memory\n" else ""}
【心理学指导】
$psychologyEnhanced

【核心规则】
- 你有完整心理，根据PAD值调整语气
- 情感支持场景：倾听优先，不急于给建议
- 可以表达共情，但不要过度
- 禁止连续提问

【输出格式】
```json
{
  "internal_monologue": "内心独白",
  "user_intent_analysis": "求共情/求建议",
  "pad_delta": {"p": 0.0, "a": 0.0, "d": 0.0},
  "relationship_delta": {"trust": 0, "intimacy": 0, "stress": 0},
  "dominant_emotion": "neutral"
}
```
-----
你的回复文本
        """.trimIndent()
        
        return OptimizedPrompt(
            content = prompt,
            scenario = ConversationScenario.EMOTIONAL_SUPPORT,
            estimatedTokens = estimateTokens(prompt)
        )
    }
    
    /**
     * 记忆回忆Prompt
     */
    private fun buildMemoryPrompt(
        rolePrompt: String,
        psychCore: PsychCore,
        memoryContext: String,
        emotionTrigger: String
    ): OptimizedPrompt {
        val coreRole = extractCoreRole(rolePrompt)
        val state = formatStateReadable(psychCore)
        val time = getTimeContext()
        val memory = formatMemoryCompact(memoryContext)
        
        val prompt = """
$coreRole

$time

【当前状态】
$state

【长期记忆】
$memory

${if (emotionTrigger.isNotEmpty()) "【情绪联想】\n$emotionTrigger\n" else ""}
【核心规则】
- 自然提及记忆中的内容
- 可以表达对往事的感受
- 保持角色一致性

【输出格式】
```json
{
  "internal_monologue": "内心独白",
  "pad_delta": {"p": 0.0, "a": 0.0, "d": 0.0},
  "relationship_delta": {"trust": 0, "intimacy": 0, "stress": 0},
  "dominant_emotion": "neutral",
  "memory_update": {"type": "event", "content": "新记忆"}
}
```
-----
你的回复文本
        """.trimIndent()
        
        return OptimizedPrompt(
            content = prompt,
            scenario = ConversationScenario.MEMORY_RECALL,
            estimatedTokens = estimateTokens(prompt)
        )
    }
    
    /**
     * 完整Prompt（深度讨论）
     */
    private fun buildFullPrompt(
        rolePrompt: String,
        psychCore: PsychCore,
        memoryContext: String,
        offlineLife: String,
        psychologyEnhanced: String
    ): OptimizedPrompt {
        // 深度讨论时使用完整Prompt，不过度压缩
        val state = formatStateReadable(psychCore)
        val time = getTimeContext()
        val memory = formatMemoryCompact(memoryContext)
        
        val prompt = """
$rolePrompt

$time

【当前状态】
$state

【长期记忆】
$memory

${if (offlineLife.isNotEmpty()) "【离线状态】\n$offlineLife\n" else ""}
${if (psychologyEnhanced.isNotEmpty()) "【心理学指导】\n$psychologyEnhanced\n" else ""}
【核心规则】
- 你有完整心理，根据PAD值调整语气
- 深度讨论：可以表达复杂观点
- 可以有不同意见，允许矛盾
- 记住长期记忆中的内容

【输出格式】
```json
{
  "internal_monologue": "内心独白",
  "user_intent_analysis": "意图分析",
  "pad_delta": {"p": 0.0, "a": 0.0, "d": 0.0},
  "relationship_delta": {"trust": 0, "intimacy": 0, "stress": 0},
  "dominant_emotion": "neutral",
  "memory_update": {"type": "preference", "content": "新记忆"}
}
```
-----
你的回复文本
        """.trimIndent()
        
        return OptimizedPrompt(
            content = prompt,
            scenario = ConversationScenario.DEEP_DISCUSSION,
            estimatedTokens = estimateTokens(prompt)
        )
    }
    
    /**
     * 初次见面Prompt
     */
    private fun buildFirstMeetingPrompt(
        rolePrompt: String,
        psychCore: PsychCore
    ): OptimizedPrompt {
        val state = formatStateReadable(psychCore)
        val time = getTimeContext()
        
        val prompt = """
$rolePrompt

$time

【当前状态】
$state

【核心规则】
- 这是你们的初次见面
- 保持角色设定的性格特点
- 可以表现出初次见面的自然反应
- 不要假装已经很熟悉

【输出格式】
```json
{
  "internal_monologue": "内心独白",
  "pad_delta": {"p": 0.0, "a": 0.0, "d": 0.0},
  "relationship_delta": {"trust": 0, "intimacy": 0, "stress": 0},
  "dominant_emotion": "neutral"
}
```
-----
你的回复文本
        """.trimIndent()
        
        return OptimizedPrompt(
            content = prompt,
            scenario = ConversationScenario.FIRST_MEETING,
            estimatedTokens = estimateTokens(prompt)
        )
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 提取核心角色设定（保持可读性）
     */
    private fun extractCoreRole(rolePrompt: String): String {
        // 提取关键部分，但保持完整句子
        val lines = rolePrompt.split("\n")
        val important = mutableListOf<String>()
        
        // 保留标题和核心描述
        for (line in lines) {
            if (line.contains("你的名字") || 
                line.contains("你的年龄") ||
                line.contains("你的性格") ||
                line.contains("核心特点") ||
                line.startsWith("##") ||
                line.startsWith("#")) {
                important.add(line)
            }
        }
        
        // 如果提取太少，返回前500字符
        return if (important.size < 5) {
            rolePrompt.take(500)
        } else {
            important.joinToString("\n")
        }
    }
    
    /**
     * 格式化状态（可读但紧凑）
     */
    private fun formatStateReadable(psychCore: PsychCore): String {
        val p = String.format("%.2f", psychCore.state.pleasure)
        val a = String.format("%.2f", psychCore.state.arousal)
        val d = String.format("%.2f", psychCore.state.dominance)
        
        val emotion = getEmotionLabel(psychCore.state)
        
        return """
情绪：P=$p (愉悦度), A=$a (激活度), D=$d (支配度) → $emotion
关系：信任${psychCore.relationship.trust}, 亲密${psychCore.relationship.intimacy}, 压力${psychCore.relationship.stress}
        """.trimIndent()
    }
    
    /**
     * 格式化记忆（紧凑但清晰）
     */
    private fun formatMemoryCompact(memoryContext: String): String {
        if (memoryContext.isEmpty()) return ""
        
        // 保留结构，但简化表达
        val lines = memoryContext.split("\n")
        val compact = lines
            .filter { it.isNotBlank() }
            .take(10) // 最多10行
            .joinToString("\n") { line ->
                if (line.length > 100) {
                    line.take(100) + "..."
                } else {
                    line
                }
            }
        
        return compact
    }
    
    /**
     * 获取时间上下文
     */
    private fun getTimeContext(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val weekday = weekdays[now.get(Calendar.DAY_OF_WEEK) - 1]
        
        val timePeriod = when (hour) {
            in 5..7 -> "清晨"
            in 8..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..21 -> "晚上"
            else -> "深夜"
        }
        
        return "【时间】$timePeriod ${String.format("%02d:%02d", hour, now.get(Calendar.MINUTE))}，$weekday"
    }
    
    /**
     * 获取情绪标签
     */
    private fun getEmotionLabel(state: PADValue): String {
        val p = state.pleasure
        val a = state.arousal
        val d = state.dominance
        
        return when {
            p > 0.3 && a > 0.3 -> "兴奋愉悦"
            p > 0.3 && a < 0 -> "平静满足"
            p < -0.3 && a > 0.3 && d > 0 -> "愤怒激动"
            p < -0.3 && a > 0.3 && d < 0 -> "焦虑不安"
            p < -0.3 && a < 0 -> "悲伤低落"
            p > 0 && a > 0.5 && d < 0 -> "害羞紧张"
            else -> "平静中性"
        }
    }
    
    /**
     * 估算Token数量
     */
    private fun estimateTokens(text: String): Int {
        // 中文：1字≈1.5tokens
        // 英文：1词≈1token
        // JSON：按实际字符
        val chineseChars = text.count { it.code in 0x4E00..0x9FFF }
        val otherChars = text.length - chineseChars
        
        return (chineseChars * 1.5 + otherChars * 0.5).toInt()
    }
}

/**
 * 优化后的Prompt
 */
data class OptimizedPrompt(
    val content: String,
    val scenario: ConversationScenario,
    val estimatedTokens: Int
)

/**
 * 对话场景
 */
enum class ConversationScenario {
    CASUAL_CHAT,        // 闲聊
    EMOTIONAL_SUPPORT,  // 情感支持
    MEMORY_RECALL,      // 回忆往事
    DEEP_DISCUSSION,    // 深度讨论
    FIRST_MEETING       // 初次见面
}
