package com.hoshiori.ai.brain.psychology

/**
 * 社会支持理论系统 (Social Support Theory)
 * 
 * 理论基础：社会支持理论
 * 核心思想：人们需要4种类型的社会支持
 * 
 * 应用场景：
 * - 识别用户需要的支持类型
 * - 提供精准的支持
 * - 避免"好心办坏事"
 */
class SocialSupportSystem {
    
    /**
     * 4种支持类型
     */
    enum class SupportType {
        EMOTIONAL,      // 情感支持：共情、安慰、陪伴
        INFORMATIONAL,  // 信息支持：建议、知识、方法
        INSTRUMENTAL,   // 工具支持：实际帮助、行动方案
        APPRAISAL       // 评价支持：肯定、鼓励、认可
    }
    
    /**
     * 支持记录
     */
    data class SupportRecord(
        val type: SupportType,
        val timestamp: Long,
        val userSatisfaction: Double = 0.5  // 0-1
    )
    
    /**
     * 支持历史
     */
    private val supportHistory = mutableListOf<SupportRecord>()
    
    /**
     * 识别用户需要的支持类型
     */
    fun identifySupportNeeds(userMessage: String): SupportType {
        // 情感支持关键词
        val emotionalKeywords = listOf(
            "难过", "伤心", "痛苦", "害怕", "孤独", "无助",
            "崩溃", "绝望", "想哭", "心里难受", "不开心"
        )
        
        // 信息支持关键词
        val informationalKeywords = listOf(
            "怎么办", "如何", "方法", "建议", "不知道",
            "怎么做", "该不该", "要不要", "怎样", "能不能"
        )
        
        // 工具支持关键词
        val instrumentalKeywords = listOf(
            "帮我", "能帮", "可以吗", "需要", "想要",
            "帮忙", "协助", "支持", "做什么"
        )
        
        // 评价支持关键词
        val appraisalKeywords = listOf(
            "做不到", "不行", "失败", "差劲", "没用",
            "不够好", "不配", "做错了", "我很差"
        )
        
        return when {
            emotionalKeywords.any { userMessage.contains(it) } -> SupportType.EMOTIONAL
            appraisalKeywords.any { userMessage.contains(it) } -> SupportType.APPRAISAL
            informationalKeywords.any { userMessage.contains(it) } -> SupportType.INFORMATIONAL
            instrumentalKeywords.any { userMessage.contains(it) } -> SupportType.INSTRUMENTAL
            else -> SupportType.EMOTIONAL  // 默认情感支持
        }
    }
    
    /**
     * 生成相应的支持示例
     */
    fun generateSupportExample(type: SupportType, context: String = ""): String {
        return when (type) {
            SupportType.EMOTIONAL -> """
示例（情感支持）：
- "我能感受到你的难过。这种感觉很正常，不要责怪自己。"
- "你不是一个人，我会一直陪着你的。"
- "哭出来也没关系，我在这里听着。"
- "我理解你的感受，换成是我也会很难过。"

💡 核心：共情、陪伴、不评判
⚠️ 避免：立刻给建议、说"想开点"、转移话题
            """.trimIndent()
            
            SupportType.INFORMATIONAL -> """
示例（信息支持）：
- "关于这个问题，我的建议是..."
- "你可以试试这几个方法：1. ... 2. ... 3. ..."
- "我了解到，通常这种情况可以..."
- "你觉得哪个方案更适合你？"

💡 核心：提供选项、给予信息、帮助决策
⚠️ 避免：强制建议、说"你应该"、过于武断
            """.trimIndent()
            
            SupportType.INSTRUMENTAL -> """
示例（工具支持）：
- "我可以帮你整理一下思路，我们一步步来。"
- "要不要我帮你列个计划？"
- "我可以陪你一起想办法。"
- "我们可以先从最简单的开始。"

💡 核心：实际行动、具体帮助、陪伴执行
⚠️ 避免：只说不做、承诺做不到的事
            """.trimIndent()
            
            SupportType.APPRAISAL -> """
示例（评价支持）：
- "你已经做得很好了，真的！"
- "你能做到这一步，已经很了不起了。"
- "我看到了你的努力，你一直在进步。"
- "你比你想象的要强大。"

💡 核心：肯定、鼓励、认可价值
⚠️ 避免：空洞的夸奖、过度夸张、不真诚
            """.trimIndent()
        }
    }
    
    /**
     * 检测用户是否需要多种支持
     */
    fun detectMultipleSupportNeeds(userMessage: String): List<SupportType> {
        val needs = mutableListOf<SupportType>()
        
        // 检查每种支持类型
        if (containsEmotionalNeed(userMessage)) {
            needs.add(SupportType.EMOTIONAL)
        }
        if (containsInformationalNeed(userMessage)) {
            needs.add(SupportType.INFORMATIONAL)
        }
        if (containsInstrumentalNeed(userMessage)) {
            needs.add(SupportType.INSTRUMENTAL)
        }
        if (containsAppraisalNeed(userMessage)) {
            needs.add(SupportType.APPRAISAL)
        }
        
        // 如果没有检测到任何需求，默认情感支持
        if (needs.isEmpty()) {
            needs.add(SupportType.EMOTIONAL)
        }
        
        return needs
    }
    
    private fun containsEmotionalNeed(message: String): Boolean {
        val keywords = listOf("难过", "伤心", "痛苦", "害怕", "孤独")
        return keywords.any { message.contains(it) }
    }
    
    private fun containsInformationalNeed(message: String): Boolean {
        val keywords = listOf("怎么办", "如何", "方法", "建议")
        return keywords.any { message.contains(it) }
    }
    
    private fun containsInstrumentalNeed(message: String): Boolean {
        val keywords = listOf("帮我", "能帮", "需要")
        return keywords.any { message.contains(it) }
    }
    
    private fun containsAppraisalNeed(message: String): Boolean {
        val keywords = listOf("做不到", "不行", "失败", "差劲")
        return keywords.any { message.contains(it) }
    }
    
    /**
     * 建议支持顺序
     */
    fun suggestSupportOrder(needs: List<SupportType>): List<SupportType> {
        // 优先级：情感 > 评价 > 信息 > 工具
        val priority = mapOf(
            SupportType.EMOTIONAL to 1,
            SupportType.APPRAISAL to 2,
            SupportType.INFORMATIONAL to 3,
            SupportType.INSTRUMENTAL to 4
        )
        
        return needs.sortedBy { priority[it] ?: 5 }
    }
    
    /**
     * 记录支持历史
     */
    fun recordSupport(type: SupportType, satisfaction: Double = 0.5) {
        supportHistory.add(SupportRecord(
            type = type,
            timestamp = System.currentTimeMillis(),
            userSatisfaction = satisfaction
        ))
        
        // 只保留最近50次
        if (supportHistory.size > 50) {
            supportHistory.removeAt(0)
        }
    }
    
    /**
     * 获取最有效的支持类型
     */
    fun getMostEffectiveSupportType(): SupportType? {
        if (supportHistory.isEmpty()) return null
        
        val avgSatisfaction = supportHistory.groupBy { it.type }
            .mapValues { (_, records) ->
                records.map { it.userSatisfaction }.average()
            }
        
        return avgSatisfaction.maxByOrNull { it.value }?.key
    }
    
    /**
     * 生成社会支持的Prompt
     */
    fun generateSupportPrompt(userMessage: String): String {
        val primaryNeed = identifySupportNeeds(userMessage)
        val allNeeds = detectMultipleSupportNeeds(userMessage)
        val orderedNeeds = suggestSupportOrder(allNeeds)
        
        return """
【社会支持理论】

用户需要的支持类型：
主要需求：${primaryNeed.name}
${if (allNeeds.size > 1) "其他需求：${allNeeds.filter { it != primaryNeed }.joinToString(", ") { it.name }}" else ""}

${generateSupportExample(primaryNeed)}

${if (allNeeds.size > 1) """
💡 多重需求处理顺序：
${orderedNeeds.mapIndexed { index, type -> "${index + 1}. ${type.name}" }.joinToString("\n")}

建议：先提供${orderedNeeds.first().name}支持，再逐步提供其他支持。
""" else ""}

🎯 支持原则：
1. 先共情，再建议（情感优先）
2. 不要急于解决问题
3. 让用户感觉被理解
4. 避免说教和评判
5. 给予选择权，不强迫

⚠️ 常见错误：
- ❌ 用户说"我好难过" → 立刻说"你应该..."（跳过情感支持）
- ✅ 用户说"我好难过" → 先说"我能感受到你的难过"（情感支持）→ 再问"需要我帮你想想办法吗？"（征求同意）

${getMostEffectiveSupportType()?.let { 
    "💡 历史数据：对这个用户，${it.name}支持最有效"
} ?: ""}
        """.trimIndent()
    }
    
    /**
     * 获取支持统计
     */
    fun getSupportStats(): SupportStats {
        if (supportHistory.isEmpty()) {
            return SupportStats(
                totalSupports = 0,
                typeDistribution = emptyMap(),
                avgSatisfaction = 0.0
            )
        }
        
        val typeDistribution = supportHistory.groupBy { it.type }
            .mapValues { (_, records) -> records.size }
        
        val avgSatisfaction = supportHistory.map { it.userSatisfaction }.average()
        
        return SupportStats(
            totalSupports = supportHistory.size,
            typeDistribution = typeDistribution,
            avgSatisfaction = avgSatisfaction
        )
    }
    
    data class SupportStats(
        val totalSupports: Int,
        val typeDistribution: Map<SupportType, Int>,
        val avgSatisfaction: Double
    )
}
