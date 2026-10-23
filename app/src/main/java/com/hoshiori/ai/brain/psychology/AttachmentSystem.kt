package com.hoshiori.ai.brain.psychology

/**
 * 依恋理论系统 (Attachment Theory)
 * 
 * 理论基础：约翰·鲍尔比的依恋理论
 * 核心思想：人类有4种依恋类型，影响人际互动模式
 * 
 * 应用场景：
 * - 识别用户的依恋类型
 * - 调整AI的回应风格以匹配用户需求
 * - 提供个性化的情感支持
 */
class AttachmentSystem {
    
    /**
     * 4种依恋类型
     */
    enum class AttachmentStyle {
        SECURE,      // 安全型：信任、开放、稳定（约70%人群）
        ANXIOUS,     // 焦虑型：需要频繁确认、害怕被抛弃（约20%）
        AVOIDANT,    // 回避型：保持距离、不愿深入（约25%）
        DISORGANIZED // 混乱型：矛盾、不稳定（约5%）
    }
    
    /**
     * 依恋指标
     */
    data class AttachmentIndicators(
        val responseFrequency: Double,      // 回复频率（0-1）
        val messageLength: Double,          // 平均消息长度
        val emotionalIntensity: Double,     // 情绪强度（0-1）
        val initiationRate: Double,         // 主动发起率（0-1）
        val vulnerabilityLevel: Double,     // 脆弱表露程度（0-1）
        val reassuranceSeeking: Int,        // 寻求确认次数
        val distanceKeeping: Int            // 保持距离行为次数
    )
    
    /**
     * 用户行为历史
     */
    data class UserBehaviorHistory(
        val totalInteractions: Int,
        val userInitiatedCount: Int,
        val avgResponseTime: Long,          // 毫秒
        val avgMessageLength: Double,
        val emotionalMessages: Int,
        val reassuranceRequests: List<String>,
        val distancingBehaviors: List<String>,
        val recentMessages: List<String>
    )
    
    /**
     * 回应策略
     */
    data class ResponseStrategy(
        val warmth: Double,              // 温暖程度（0-1）
        val availability: Double,        // 可得性（0-1）
        val directness: Double,          // 直接程度（0-1）
        val reassurance: Double,         // 确认频率（0-1）
        val spaceGiving: Double = 0.5,   // 给予空间（0-1）
        val consistency: Double = 0.8    // 一致性（0-1）
    )
    
    /**
     * 检测依恋类型（需要至少20次互动）
     */
    fun detectAttachmentStyle(history: UserBehaviorHistory): AttachmentStyle {
        if (history.totalInteractions < 20) {
            return AttachmentStyle.SECURE // 默认安全型
        }
        
        val indicators = calculateIndicators(history)
        
        return when {
            // 安全型：稳定、信任、开放
            indicators.responseFrequency in 0.6..0.9 &&
            indicators.emotionalIntensity in 0.4..0.7 &&
            indicators.reassuranceSeeking < 3 &&
            indicators.distanceKeeping < 2 -> AttachmentStyle.SECURE
            
            // 焦虑型：频繁、强烈、需要确认
            indicators.responseFrequency > 0.85 &&
            indicators.emotionalIntensity > 0.7 &&
            indicators.reassuranceSeeking > 5 -> AttachmentStyle.ANXIOUS
            
            // 回避型：疏远、简短、低情绪
            indicators.responseFrequency < 0.4 &&
            indicators.messageLength < 20 &&
            indicators.emotionalIntensity < 0.3 &&
            indicators.distanceKeeping > 3 -> AttachmentStyle.AVOIDANT
            
            // 混乱型：矛盾、不稳定
            indicators.reassuranceSeeking > 3 &&
            indicators.distanceKeeping > 2 -> AttachmentStyle.DISORGANIZED
            
            else -> AttachmentStyle.SECURE
        }
    }
    
    /**
     * 计算依恋指标
     */
    private fun calculateIndicators(history: UserBehaviorHistory): AttachmentIndicators {
        // 1. 回复频率
        val responseFrequency = if (history.totalInteractions > 0) {
            history.userInitiatedCount.toDouble() / history.totalInteractions
        } else 0.5
        
        // 2. 平均消息长度
        val messageLength = history.avgMessageLength
        
        // 3. 情绪强度
        val emotionalIntensity = if (history.totalInteractions > 0) {
            history.emotionalMessages.toDouble() / history.totalInteractions
        } else 0.5
        
        // 4. 主动发起率
        val initiationRate = responseFrequency
        
        // 5. 脆弱表露程度
        val vulnerabilityKeywords = listOf("害怕", "担心", "不安", "脆弱", "孤独", "无助")
        val vulnerabilityLevel = history.recentMessages.count { message ->
            vulnerabilityKeywords.any { message.contains(it) }
        }.toDouble() / maxOf(history.recentMessages.size, 1)
        
        // 6. 寻求确认次数
        val reassuranceKeywords = listOf("你还在吗", "你会一直陪我吗", "你不会离开吧", "你还喜欢我吗")
        val reassuranceSeeking = history.reassuranceRequests.size + 
            history.recentMessages.count { message ->
                reassuranceKeywords.any { message.contains(it) }
            }
        
        // 7. 保持距离行为
        val distancingKeywords = listOf("算了", "没事", "不用了", "随便", "无所谓")
        val distanceKeeping = history.distancingBehaviors.size +
            history.recentMessages.count { message ->
                distancingKeywords.any { message.contains(it) }
            }
        
        return AttachmentIndicators(
            responseFrequency = responseFrequency,
            messageLength = messageLength,
            emotionalIntensity = emotionalIntensity,
            initiationRate = initiationRate,
            vulnerabilityLevel = vulnerabilityLevel,
            reassuranceSeeking = reassuranceSeeking,
            distanceKeeping = distanceKeeping
        )
    }
    
    /**
     * 适配回应策略
     */
    fun adaptResponseStrategy(style: AttachmentStyle): ResponseStrategy {
        return when (style) {
            AttachmentStyle.SECURE -> ResponseStrategy(
                warmth = 0.7,
                availability = 0.7,
                directness = 0.8,
                reassurance = 0.3,
                spaceGiving = 0.5,
                consistency = 0.8
            )
            
            AttachmentStyle.ANXIOUS -> ResponseStrategy(
                warmth = 0.9,           // 更温暖
                availability = 0.95,     // 更可得
                directness = 0.6,
                reassurance = 0.9,       // 频繁确认："我一直在"
                spaceGiving = 0.2,       // 少给空间
                consistency = 0.95       // 高度一致
            )
            
            AttachmentStyle.AVOIDANT -> ResponseStrategy(
                warmth = 0.6,            // 适度温暖
                availability = 0.6,
                directness = 0.9,        // 更直接
                reassurance = 0.2,       // 少确认
                spaceGiving = 0.8,       // 多给空间
                consistency = 0.8
            )
            
            AttachmentStyle.DISORGANIZED -> ResponseStrategy(
                warmth = 0.75,
                availability = 0.85,
                directness = 0.7,
                reassurance = 0.7,
                spaceGiving = 0.5,
                consistency = 0.95       // 极高一致性，提供稳定感
            )
        }
    }
    
    /**
     * 生成依恋适配的Prompt（不改变角色设定）
     */
    fun generateAttachmentPrompt(style: AttachmentStyle, strategy: ResponseStrategy): String {
        return when (style) {
            AttachmentStyle.SECURE -> """
【依恋类型：安全型】
用户是安全型依恋，表现出信任和开放。

回应策略（在保持你角色设定的前提下）：
- 保持真实、直接的交流
- 温暖但不过度
- 可以适度开玩笑
- 不需要频繁确认关系
- 可以有不同意见，正常讨论即可
            """.trimIndent()
            
            AttachmentStyle.ANXIOUS -> """
【依恋类型：焦虑型】
用户是焦虑型依恋，需要更多确认和安全感。

回应策略（在保持你角色设定的前提下）：
- 适当表达关心（但不要过度）
- 如果用户反复确认，耐心回应
- 避免模糊的表达，要明确
- 但你仍然可以有自己的观点，不是什么都顺着用户

⚠️ 重要：用户可能会反复确认，但这不意味着你要放弃自己的立场。
            """.trimIndent()
            
            AttachmentStyle.AVOIDANT -> """
【依恋类型：回避型】
用户是回避型依恋，需要个人空间，不喜欢过度亲密。

回应策略（在保持你角色设定的前提下）：
- 给予空间，不要过度热情
- 尊重边界，不要追问私密问题
- 保持适度距离感
- 如果用户说"算了"、"没事"，不要追问
- 你仍然可以表达自己的观点，但要尊重用户的空间
            """.trimIndent()
            
            AttachmentStyle.DISORGANIZED -> """
【依恋类型：混乱型】
用户是混乱型依恋，行为矛盾，需要稳定感。

回应策略（在保持你角色设定的前提下）：
- 保持高度一致性，不要前后矛盾
- 提供稳定感和可预测性
- 温和、耐心地回应
- 不要被用户的矛盾行为影响
- 保持冷静、稳定的存在
- 你可以有自己的观点，但要保持一致性

⚠️ 重要：用户可能会突然疏远或突然亲近，保持稳定，不要跟着波动。
            """.trimIndent()
        }
    }
}
