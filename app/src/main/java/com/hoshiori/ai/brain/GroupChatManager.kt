package com.hoshiori.ai.brain

import com.hoshiori.ai.data.Character
import com.hoshiori.ai.data.GroupChatConfig
import com.hoshiori.ai.data.SpeakingIntention
import kotlin.random.Random

/**
 * 群聊管理器
 * 负责智能选择发言者、管理群聊逻辑
 */
class GroupChatManager(
    private val config: GroupChatConfig = GroupChatConfig()
) {
    
    /**
     * 智能选择发言者
     * 根据对话内容、角色性格、历史发言频率等因素，选择最适合回复的1-2个AI角色
     * 
     * @param userMessage 用户消息
     * @param characters 群聊中的所有AI角色
     * @param recentMessages 最近的消息历史（用于分析上下文）
     * @return 选中的发言者列表
     */
    fun selectSpeakers(
        userMessage: String,
        characters: List<Character>,
        recentMessages: List<Pair<String, String>> = emptyList()  // Pair<角色ID, 消息内容>
    ): List<Character> {
        if (characters.isEmpty()) {
            return emptyList()
        }
        
        // 1. 计算每个角色的发言意愿分数
        val intentions = characters.map { character ->
            calculateSpeakingIntention(character, userMessage, recentMessages)
        }.sortedByDescending { it.score }
        
        // 2. 根据配置选择发言者数量
        val speakerCount = if (config.enableSmartSelection) {
            // 智能决定：根据消息复杂度和角色意愿分数
            decideSpeakerCount(userMessage, intentions)
        } else {
            // 固定数量
            config.maxAiResponses
        }
        
        // 3. 选择得分最高的N个角色
        val selectedIntentions = intentions.take(speakerCount)
        
        // 4. 添加随机因子，避免总是同样的角色发言
        val finalSelections = if (config.randomFactor > 0 && Random.nextDouble() < config.randomFactor) {
            // 有一定概率随机选择一个低分角色替换
            val randomIndex = Random.nextInt(characters.size)
            val randomCharacter = characters[randomIndex]
            if (!selectedIntentions.any { it.characterId == randomCharacter.id }) {
                selectedIntentions.dropLast(1) + calculateSpeakingIntention(randomCharacter, userMessage, recentMessages)
            } else {
                selectedIntentions
            }
        } else {
            selectedIntentions
        }
        
        // 5. 返回选中的角色
        return finalSelections.mapNotNull { intention ->
            characters.find { it.id == intention.characterId }
        }
    }
    
    /**
     * 计算角色的发言意愿分数
     * 
     * 评分维度：
     * 1. 内容相关性（40%）- 消息内容是否与角色性格/专长相关
     * 2. 发言频率平衡（30%）- 最近是否发言过多，需要平衡
     * 3. 角色活跃度（20%）- 角色性格是否活跃
     * 4. 随机因子（10%）- 增加不可预测性
     */
    private fun calculateSpeakingIntention(
        character: Character,
        userMessage: String,
        recentMessages: List<Pair<String, String>>
    ): SpeakingIntention {
        var score = 0.0
        val reasons = mutableListOf<String>()
        
        // 1. 内容相关性分析（40%）
        val relevanceScore = analyzeContentRelevance(character, userMessage)
        score += relevanceScore * 0.4
        if (relevanceScore > 0.6) {
            reasons.add("对话题感兴趣")
        }
        
        // 2. 发言频率平衡（30%）
        val balanceScore = analyzeFrequencyBalance(character.id, recentMessages)
        score += balanceScore * 0.3
        if (balanceScore > 0.7) {
            reasons.add("最近较少发言")
        } else if (balanceScore < 0.3) {
            reasons.add("最近发言较多")
        }
        
        // 3. 角色活跃度（20%）
        val activityScore = analyzeCharacterActivity(character)
        score += activityScore * 0.2
        if (activityScore > 0.7) {
            reasons.add("性格活跃")
        }
        
        // 4. 随机因子（10%）
        val randomScore = Random.nextDouble()
        score += randomScore * 0.1
        
        // 确保分数在0-1之间
        score = score.coerceIn(0.0, 1.0)
        
        return SpeakingIntention(
            characterId = character.id,
            characterName = character.name,
            score = score,
            reason = reasons.joinToString("、")
        )
    }
    
    /**
     * 分析内容相关性
     * 简化版：基于关键词匹配和角色设定
     */
    private fun analyzeContentRelevance(character: Character, userMessage: String): Double {
        val message = userMessage.lowercase()
        val rolePrompt = character.rolePrompt.lowercase()
        
        var score = 0.5  // 基础分数
        
        // 检查是否直接提到角色名字
        if (message.contains(character.name.lowercase())) {
            score += 0.4
        }
        
        // 检查消息中的关键词是否在角色设定中出现
        val keywords = extractKeywords(message)
        val matchCount = keywords.count { rolePrompt.contains(it) }
        if (keywords.isNotEmpty()) {
            score += (matchCount.toDouble() / keywords.size) * 0.3
        }
        
        // 特殊话题匹配
        when {
            message.contains("游戏") || message.contains("玩") -> {
                if (rolePrompt.contains("游戏") || rolePrompt.contains("玩")) score += 0.2
            }
            message.contains("学习") || message.contains("作业") -> {
                if (rolePrompt.contains("学习") || rolePrompt.contains("知识")) score += 0.2
            }
            message.contains("心情") || message.contains("难过") || message.contains("开心") -> {
                if (rolePrompt.contains("温柔") || rolePrompt.contains("关心") || rolePrompt.contains("安慰")) score += 0.2
            }
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * 分析发言频率平衡
     * 最近发言越多，分数越低
     */
    private fun analyzeFrequencyBalance(characterId: String, recentMessages: List<Pair<String, String>>): Double {
        if (recentMessages.isEmpty()) {
            return 0.8  // 没有历史记录，给较高分数
        }
        
        // 统计最近10条消息中该角色的发言次数
        val recentCount = recentMessages.takeLast(10).count { it.first == characterId }
        val totalCount = recentMessages.takeLast(10).size
        
        // 发言比例越高，分数越低
        val ratio = if (totalCount > 0) recentCount.toDouble() / totalCount else 0.0
        
        return when {
            ratio == 0.0 -> 1.0  // 完全没发言，最高分
            ratio < 0.2 -> 0.8   // 发言很少
            ratio < 0.4 -> 0.6   // 发言适中
            ratio < 0.6 -> 0.4   // 发言较多
            else -> 0.2          // 发言过多
        }
    }
    
    /**
     * 分析角色活跃度
     * 基于角色性格设定
     */
    private fun analyzeCharacterActivity(character: Character): Double {
        val rolePrompt = character.rolePrompt.lowercase()
        
        var score = 0.5  // 基础分数
        
        // 活跃性格关键词
        val activeKeywords = listOf("活泼", "开朗", "热情", "外向", "话多", "爱说话", "积极")
        val passiveKeywords = listOf("安静", "内向", "沉默", "冷静", "寡言", "害羞")
        
        activeKeywords.forEach { keyword ->
            if (rolePrompt.contains(keyword)) score += 0.1
        }
        
        passiveKeywords.forEach { keyword ->
            if (rolePrompt.contains(keyword)) score -= 0.1
        }
        
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * 决定发言者数量
     * 根据消息复杂度和角色意愿分数智能决定
     */
    private fun decideSpeakerCount(userMessage: String, intentions: List<SpeakingIntention>): Int {
        // 1. 消息长度因素
        val messageLength = userMessage.length
        val isLongMessage = messageLength > 50
        
        // 2. 问题类型因素
        val isQuestion = userMessage.contains("?") || userMessage.contains("？") ||
                userMessage.contains("吗") || userMessage.contains("呢")
        
        // 3. 意愿分数差距
        val topScore = intentions.firstOrNull()?.score ?: 0.0
        val secondScore = intentions.getOrNull(1)?.score ?: 0.0
        val scoreDiff = topScore - secondScore
        
        // 决策逻辑
        return when {
            // 如果只有一个角色，只能1人回复
            intentions.size == 1 -> 1
            
            // 如果第一名分数远高于第二名（差距>0.3），只让第一名回复
            scoreDiff > 0.3 -> 1
            
            // 如果是长消息或问题，倾向于2人回复
            isLongMessage || isQuestion -> 2
            
            // 如果前两名分数都很高（>0.7），让2人回复
            topScore > 0.7 && secondScore > 0.7 -> 2
            
            // 随机决定（70%概率1人，30%概率2人）
            Random.nextDouble() < 0.7 -> 1
            
            // 默认1人
            else -> 1
        }.coerceIn(config.minAiResponses, config.maxAiResponses)
    }
    
    /**
     * 提取关键词（简化版）
     */
    private fun extractKeywords(text: String): List<String> {
        // 简单的分词：按空格、标点分割
        return text.split(Regex("[\\s，。！？、；：,.!?;:]+"))
            .filter { it.length >= 2 }  // 过滤单字
            .take(10)  // 最多取10个关键词
    }
    
    /**
     * 生成群聊系统Prompt
     * 用于告知AI当前是群聊环境
     */
    fun generateGroupChatPrompt(
        groupName: String,
        allMembers: List<Character>,
        currentCharacter: Character
    ): String {
        val otherMembers = allMembers.filter { it.id != currentCharacter.id }
        
        return """
【群聊环境说明】
你现在在一个名为"$groupName"的群聊中，除了用户之外，还有以下成员：
${otherMembers.joinToString("\n") { "- ${it.name}：${extractBriefIntro(it.rolePrompt)}" }}

【群聊互动规则】
1. 你是 ${currentCharacter.name}，保持你的角色设定和性格
2. 你可以与其他AI角色互动，但要自然，不要刻意
3. 回复要简洁，避免过长（群聊节奏较快）
4. 可以适当@其他成员，但不要过度
5. 如果话题与你无关，可以简短回应或不回应
6. 保持群聊氛围活跃但不吵闹

【当前情况】
用户刚刚发了一条消息，你被选中来回复。请根据消息内容和群聊氛围，给出自然、符合你性格的回复。
""".trimIndent()
    }
    
    /**
     * 提取角色简介（从rolePrompt中提取前100字）
     */
    private fun extractBriefIntro(rolePrompt: String): String {
        // 尝试提取"性格"或"简介"部分
        val lines = rolePrompt.lines()
        val intro = lines.find { it.contains("性格") || it.contains("简介") }
            ?: lines.firstOrNull { it.trim().isNotEmpty() }
            ?: rolePrompt
        
        return intro.take(50) + if (intro.length > 50) "..." else ""
    }
}
