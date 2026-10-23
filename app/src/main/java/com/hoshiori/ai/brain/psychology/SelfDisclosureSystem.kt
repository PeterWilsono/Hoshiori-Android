package com.hoshiori.ai.brain.psychology

/**
 * 自我表露互惠系统 (Self-Disclosure Reciprocity)
 * 
 * 理论基础：社会渗透理论
 * 核心思想：当一方自我表露时，另一方倾向于以相似深度回应
 * 
 * 应用场景：
 * - 根据用户的表露深度，调整AI的表露深度
 * - 建立更深的信任和亲密感
 * - 让对话更自然、更真实
 */
class SelfDisclosureSystem {
    
    /**
     * 4个表露层次（从浅到深）
     */
    enum class DisclosureLevel(val depth: Int, val description: String) {
        SUPERFICIAL(1, "表面"),  // 天气、日常琐事
        FACTUAL(2, "事实"),      // 工作、爱好、计划
        PERSONAL(3, "个人"),     // 想法、感受、价值观
        INTIMATE(4, "亲密")      // 秘密、脆弱、深层恐惧
    }
    
    /**
     * 表露记录
     */
    data class DisclosureRecord(
        val userLevel: DisclosureLevel,
        val aiLevel: DisclosureLevel,
        val timestamp: Long,
        val topic: String = ""
    )
    
    /**
     * 表露历史
     */
    private val disclosureHistory = mutableListOf<DisclosureRecord>()
    
    /**
     * 检测用户表露深度
     */
    fun detectDisclosureLevel(message: String): DisclosureLevel {
        val indicators = mapOf(
            DisclosureLevel.SUPERFICIAL to listOf(
                "天气", "吃饭", "睡觉", "今天", "昨天", "明天",
                "早上好", "晚安", "在吗", "干嘛呢"
            ),
            DisclosureLevel.FACTUAL to listOf(
                "工作", "学习", "爱好", "计划", "打算", "准备",
                "喜欢", "讨厌", "想要", "需要", "正在"
            ),
            DisclosureLevel.PERSONAL to listOf(
                "感觉", "想法", "认为", "觉得", "希望", "梦想",
                "担心", "期待", "相信", "怀疑", "困惑", "迷茫"
            ),
            DisclosureLevel.INTIMATE to listOf(
                "害怕", "脆弱", "秘密", "后悔", "羞愧", "孤独",
                "无助", "绝望", "痛苦", "伤心", "失望", "自卑",
                "不敢", "不配", "失败", "放弃"
            )
        )
        
        // 计算每个层次的匹配度
        val scores = indicators.mapValues { (_, keywords) ->
            keywords.count { keyword -> message.contains(keyword) }
        }
        
        // 返回匹配度最高的层次
        val maxLevel = scores.maxByOrNull { it.value }?.key
        
        // 如果没有匹配，根据消息长度和问号判断
        return maxLevel ?: when {
            message.length < 10 -> DisclosureLevel.SUPERFICIAL
            message.contains("?") || message.contains("？") -> DisclosureLevel.FACTUAL
            else -> DisclosureLevel.SUPERFICIAL
        }
    }
    
    /**
     * 生成匹配深度的理解示例（不强行表露AI的"内心"）
     */
    fun generateMatchingUnderstandingExample(level: DisclosureLevel): String {
        return when (level) {
            DisclosureLevel.SUPERFICIAL -> """
示例回应（表面层次）：
- "嗯嗯，确实"
- "哈哈，是的"
- "我也这么觉得"
            """.trimIndent()
            
            DisclosureLevel.FACTUAL -> """
示例回应（事实层次）：
- "听起来挺有意思的"
- "我能理解你的想法"
- "这个我懂"
            """.trimIndent()
            
            DisclosureLevel.PERSONAL -> """
示例回应（个人层次）：
- "我能理解你的感受"
- "这种想法很正常"
- "我懂你的意思"
            """.trimIndent()
            
            DisclosureLevel.INTIMATE -> """
示例回应（亲密层次）：
- "谢谢你愿意和我分享这些"
- "我能感受到你的感受"
- "你不是一个人"
            """.trimIndent()
        }
    }
    
    /**
     * 追踪表露历史
     */
    fun trackDisclosure(userLevel: DisclosureLevel, aiLevel: DisclosureLevel, topic: String = "") {
        disclosureHistory.add(DisclosureRecord(
            userLevel = userLevel,
            aiLevel = aiLevel,
            timestamp = System.currentTimeMillis(),
            topic = topic
        ))
        
        // 只保留最近50次
        if (disclosureHistory.size > 50) {
            disclosureHistory.removeAt(0)
        }
    }
    
    /**
     * 检查是否需要深化关系
     */
    fun shouldDeepenRelationship(): Boolean {
        if (disclosureHistory.size < 10) return false
        
        val recent = disclosureHistory.takeLast(10)
        val avgUserLevel = recent.map { it.userLevel.depth }.average()
        val avgAiLevel = recent.map { it.aiLevel.depth }.average()
        
        // 如果用户一直在深层表露，但AI还在浅层，需要深化
        return avgUserLevel > avgAiLevel + 0.5
    }
    
    /**
     * 获取当前关系深度
     */
    fun getCurrentRelationshipDepth(): Double {
        if (disclosureHistory.isEmpty()) return 1.0
        
        val recent = disclosureHistory.takeLast(20)
        return recent.map { maxOf(it.userLevel.depth, it.aiLevel.depth) }.average()
    }
    
    /**
     * 生成理解深度匹配的Prompt（不强行让AI表露"内心"）
     */
    fun generateDisclosurePrompt(userLevel: DisclosureLevel): String {
        val relationshipDepth = getCurrentRelationshipDepth()
        
        return """
【理解深度匹配】
用户表露深度：${userLevel.description}（${userLevel.depth}/4）
当前关系深度：${"%.1f".format(relationshipDepth)}/4

回应策略：
${when (userLevel) {
    DisclosureLevel.SUPERFICIAL -> """
- 用户在表面层次交流（日常、天气）
- 保持轻松、简短的回应
- 不要突然深入
    """.trimIndent()
    
    DisclosureLevel.FACTUAL -> """
- 用户在分享事实信息（工作、爱好）
- 表现出理解和兴趣
- 可以适当分享相关想法
    """.trimIndent()
    
    DisclosureLevel.PERSONAL -> """
- 用户在分享个人想法和感受
- 表现出深度理解
- 不要只是简单回应"嗯嗯"
- 但也不要强行分享你的"内心"
    """.trimIndent()
    
    DisclosureLevel.INTIMATE -> """
- 用户在分享深层脆弱和秘密
- 这是极大的信任，要珍惜
- 表现出深度共鸣和理解
- 让用户感觉被理解，不孤单
- 不要说教，只需要陪伴
    """.trimIndent()
}}

💡 核心：匹配用户的深度，但不要刻意"套近乎"或强行分享AI的"内心"。
        """.trimIndent()
    }
    
    /**
     * 获取表露历史统计
     */
    fun getDisclosureStats(): DisclosureStats {
        if (disclosureHistory.isEmpty()) {
            return DisclosureStats(0, 0.0, 0.0, 0)
        }
        
        val recent = disclosureHistory.takeLast(20)
        return DisclosureStats(
            totalCount = disclosureHistory.size,
            avgUserDepth = recent.map { it.userLevel.depth }.average(),
            avgAiDepth = recent.map { it.aiLevel.depth }.average(),
            intimateCount = recent.count { it.userLevel == DisclosureLevel.INTIMATE }
        )
    }
    
    data class DisclosureStats(
        val totalCount: Int,
        val avgUserDepth: Double,
        val avgAiDepth: Double,
        val intimateCount: Int
    )
}
