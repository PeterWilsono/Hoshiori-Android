package com.hoshiori.ai.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hoshiori.ai.brain.LongTermMemory
import com.hoshiori.ai.network.WebSearchSystem
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统一记忆管理器
 * 
 * 负责管理所有AI相关的数据：
 * 1. 对话记录（用户和AI的所有对话）
 * 2. 长期记忆（提取的重要信息）
 * 3. 搜索记录（联网搜索的历史和结果）
 * 4. 情绪历史（AI的情绪变化轨迹）
 * 5. 关系发展（用户和AI的关系变化）
 * 
 * 所有数据统一存储在 data/memories/ 目录下，按角色ID分类
 */
class UnifiedMemoryManager(private val context: Context) {
    
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "UnifiedMemoryManager"
        private const val PREFS_NAME = "unified_memory"
    }
    
    // ========== 对话记录管理 ==========
    
    /**
     * 记录对话
     * 
     * @param characterId 角色ID
     * @param content 对话内容
     * @param isUser 是否是用户消息
     * @param emotion AI的情绪（仅AI消息有效）
     * @param metadata 额外元数据（如：搜索结果、内心独白等）
     */
    fun recordConversation(
        characterId: String,
        content: String,
        isUser: Boolean,
        emotion: String = "neutral",
        metadata: Map<String, Any> = emptyMap()
    ): ConversationRecord {
        val record = ConversationRecord(
            characterId = characterId,
            content = content,
            isUser = isUser,
            emotion = emotion,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        
        // 保存到对话历史
        val conversations = getConversations(characterId).toMutableList()
        conversations.add(record)
        
        // 只保留最近1000条对话
        val trimmed = if (conversations.size > 1000) {
            conversations.takeLast(1000)
        } else {
            conversations
        }
        
        saveConversations(characterId, trimmed)
        
        Log.d(TAG, "📝 记录对话 [${if (isUser) "用户" else "AI"}]: ${content.take(50)}...")
        
        return record
    }
    
    /**
     * 获取对话历史
     * 
     * @param characterId 角色ID
     * @param limit 返回最近N条对话
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     */
    fun getConversations(
        characterId: String,
        limit: Int = 100,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ConversationRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("conversations", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<ConversationRecord>>() {}.type
            val conversations: List<ConversationRecord> = gson.fromJson(json, type)
            
            // 按时间过滤
            val filtered = conversations.filter { record ->
                (startTime == null || record.timestamp >= startTime) &&
                (endTime == null || record.timestamp <= endTime)
            }
            
            // 返回最近N条
            filtered.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取对话历史失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索对话
     * 
     * @param characterId 角色ID
     * @param keyword 关键词
     * @param limit 返回最多N条结果
     */
    @Suppress("unused")
    fun searchConversations(
        characterId: String,
        keyword: String,
        limit: Int = 20
    ): List<ConversationRecord> {
        val conversations = getConversations(characterId, limit = 1000)
        return conversations.filter { it.content.contains(keyword, ignoreCase = true) }
            .takeLast(limit)
    }
    
    private fun saveConversations(characterId: String, conversations: List<ConversationRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("conversations", gson.toJson(conversations)).apply()
    }
    
    // ========== 长期记忆管理 ==========
    
    /**
     * 添加长期记忆
     * 
     * @param characterId 角色ID
     * @param type 记忆类型（user_info, event, preference, knowledge等）
     * @param content 记忆内容
     * @param importance 重要性（1-10）
     * @param relatedConversationId 关联的对话ID（可选）
     */
    fun addLongTermMemory(
        characterId: String,
        type: MemoryType,
        content: String,
        importance: Int = 5,
        relatedConversationId: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): LongTermMemoryRecord {
        val record = LongTermMemoryRecord(
            characterId = characterId,
            type = type,
            content = content,
            importance = importance,
            relatedConversationId = relatedConversationId,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        
        val memories = getLongTermMemories(characterId).toMutableList()
        memories.add(record)
        
        // 按重要性排序，只保留最重要的500条
        val sorted = memories.sortedByDescending { it.importance }.take(500)
        saveLongTermMemories(characterId, sorted)
        
        Log.d(TAG, "🧠 添加长期记忆 [$type]: ${content.take(50)}... (重要性: $importance)")
        
        return record
    }
    
    /**
     * 获取长期记忆
     * 
     * @param characterId 角色ID
     * @param type 记忆类型（可选）
     * @param minImportance 最低重要性（可选）
     * @param limit 返回最多N条
     */
    fun getLongTermMemories(
        characterId: String,
        type: MemoryType? = null,
        minImportance: Int? = null,
        limit: Int = 100
    ): List<LongTermMemoryRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("long_term_memories", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<LongTermMemoryRecord>>() {}.type
            val memories: List<LongTermMemoryRecord> = gson.fromJson(json, typeToken)
            
            // 过滤
            val filtered = memories.filter { record ->
                (type == null || record.type == type) &&
                (minImportance == null || record.importance >= minImportance)
            }
            
            // 按重要性和时间排序
            filtered.sortedWith(
                compareByDescending<LongTermMemoryRecord> { it.importance }
                    .thenByDescending { it.timestamp }
            ).take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取长期记忆失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索长期记忆
     */
    @Suppress("unused")
    fun searchLongTermMemories(
        characterId: String,
        keyword: String,
        limit: Int = 20
    ): List<LongTermMemoryRecord> {
        val memories = getLongTermMemories(characterId, limit = 500)
        return memories.filter { it.content.contains(keyword, ignoreCase = true) }
            .take(limit)
    }
    
    private fun saveLongTermMemories(characterId: String, memories: List<LongTermMemoryRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("long_term_memories", gson.toJson(memories)).apply()
    }
    
    // ========== 搜索记录管理 ==========
    
    /**
     * 记录搜索
     * 
     * @param characterId 角色ID
     * @param query 搜索查询
     * @param type 搜索类型
     * @param result 搜索结果
     * @param success 是否成功
     */
    fun recordSearch(
        characterId: String,
        query: String,
        type: WebSearchSystem.SearchType,
        result: String? = null,
        success: Boolean = true
    ): SearchRecord {
        val record = SearchRecord(
            characterId = characterId,
            query = query,
            type = type,
            result = result,
            success = success,
            timestamp = System.currentTimeMillis()
        )
        
        val searches = getSearchRecords(characterId).toMutableList()
        searches.add(record)
        
        // 只保留最近200条搜索记录
        val trimmed = if (searches.size > 200) {
            searches.takeLast(200)
        } else {
            searches
        }
        
        saveSearchRecords(characterId, trimmed)
        
        Log.d(TAG, "🔍 记录搜索 [$type]: $query (${if (success) "成功" else "失败"})")
        
        return record
    }
    
    /**
     * 获取搜索记录
     */
    fun getSearchRecords(
        characterId: String,
        type: WebSearchSystem.SearchType? = null,
        limit: Int = 50
    ): List<SearchRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("search_records", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<SearchRecord>>() {}.type
            val records: List<SearchRecord> = gson.fromJson(json, typeToken)
            
            val filtered = if (type != null) {
                records.filter { it.type == type }
            } else {
                records
            }
            
            filtered.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取搜索记录失败", e)
            emptyList()
        }
    }
    
    private fun saveSearchRecords(characterId: String, records: List<SearchRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("search_records", gson.toJson(records)).apply()
    }
    
    // ========== 情绪历史管理 ==========
    
    /**
     * 记录情绪
     * 
     * @param characterId 角色ID
     * @param emotion 情绪标签
     * @param pad PAD值
     * @param trigger 触发原因
     */
    fun recordEmotion(
        characterId: String,
        emotion: String,
        pad: com.hoshiori.ai.brain.PADValue,
        trigger: String? = null
    ): EmotionRecord {
        val record = EmotionRecord(
            characterId = characterId,
            emotion = emotion,
            pleasure = pad.pleasure,
            arousal = pad.arousal,
            dominance = pad.dominance,
            trigger = trigger,
            timestamp = System.currentTimeMillis()
        )
        
        val emotions = getEmotionRecords(characterId).toMutableList()
        emotions.add(record)
        
        // 只保留最近500条情绪记录
        val trimmed = if (emotions.size > 500) {
            emotions.takeLast(500)
        } else {
            emotions
        }
        
        saveEmotionRecords(characterId, trimmed)
        
        Log.d(TAG, "😊 记录情绪: $emotion (P:${pad.pleasure}, A:${pad.arousal}, D:${pad.dominance})")
        
        return record
    }
    
    /**
     * 获取情绪历史
     */
    fun getEmotionRecords(
        characterId: String,
        limit: Int = 100
    ): List<EmotionRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("emotion_records", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<EmotionRecord>>() {}.type
            val records: List<EmotionRecord> = gson.fromJson(json, typeToken)
            records.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取情绪历史失败", e)
            emptyList()
        }
    }
    
    private fun saveEmotionRecords(characterId: String, records: List<EmotionRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("emotion_records", gson.toJson(records)).apply()
    }
    
    // ========== 关系发展管理 ==========
    
    /**
     * 记录关系变化
     * 
     * @param characterId 角色ID
     * @param relationship 当前关系状态
     * @param event 触发事件
     */
    fun recordRelationship(
        characterId: String,
        relationship: Relationship,
        event: String? = null
    ): RelationshipRecord {
        val record = RelationshipRecord(
            characterId = characterId,
            trust = relationship.trust,
            intimacy = relationship.intimacy,
            stress = relationship.stress,
            event = event,
            timestamp = System.currentTimeMillis()
        )
        
        val relationships = getRelationshipRecords(characterId).toMutableList()
        relationships.add(record)
        
        // 只保留最近300条关系记录
        val trimmed = if (relationships.size > 300) {
            relationships.takeLast(300)
        } else {
            relationships
        }
        
        saveRelationshipRecords(characterId, trimmed)
        
        Log.d(TAG, "💕 记录关系: 信任${relationship.trust} 亲密${relationship.intimacy} 压力${relationship.stress}")
        
        return record
    }
    
    /**
     * 获取关系历史
     */
    fun getRelationshipRecords(
        characterId: String,
        limit: Int = 100
    ): List<RelationshipRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("relationship_records", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<RelationshipRecord>>() {}.type
            val records: List<RelationshipRecord> = gson.fromJson(json, typeToken)
            records.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取关系历史失败", e)
            emptyList()
        }
    }
    
    private fun saveRelationshipRecords(characterId: String, records: List<RelationshipRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("relationship_records", gson.toJson(records)).apply()
    }
    
    // ========== 统计和分析 ==========
    
    /**
     * 获取记忆统计
     */
    fun getMemoryStatistics(characterId: String): MemoryStatistics {
        val conversations = getConversations(characterId, limit = 1000)
        val longTermMemories = getLongTermMemories(characterId, limit = 500)
        val searches = getSearchRecords(characterId, limit = 200)
        val emotions = getEmotionRecords(characterId, limit = 500)
        val relationships = getRelationshipRecords(characterId, limit = 300)
        
        return MemoryStatistics(
            totalConversations = conversations.size,
            userMessages = conversations.count { it.isUser },
            aiMessages = conversations.count { !it.isUser },
            totalLongTermMemories = longTermMemories.size,
            totalSearches = searches.size,
            successfulSearches = searches.count { it.success },
            totalEmotionRecords = emotions.size,
            totalRelationshipRecords = relationships.size,
            averageTrust = relationships.lastOrNull()?.trust ?: 50,
            averageIntimacy = relationships.lastOrNull()?.intimacy ?: 50,
            averageStress = relationships.lastOrNull()?.stress ?: 0
        )
    }
    
    /**
     * 生成记忆摘要（用于AI读取）
     */
    @Suppress("unused")
    fun generateMemorySummary(characterId: String): String {
        val stats = getMemoryStatistics(characterId)
        @Suppress("UNUSED_VARIABLE")
        val recentConversations = getConversations(characterId, limit = 10)
        val importantMemories = getLongTermMemories(characterId, minImportance = 7, limit = 10)
        val recentSearches = getSearchRecords(characterId, limit = 5)
        
        val summary = StringBuilder()
        
        summary.append("【记忆统计】\n")
        summary.append("- 总对话数: ${stats.totalConversations}条\n")
        summary.append("- 长期记忆: ${stats.totalLongTermMemories}条\n")
        summary.append("- 搜索记录: ${stats.totalSearches}次\n")
        summary.append("- 当前关系: 信任${stats.averageTrust} 亲密${stats.averageIntimacy} 压力${stats.averageStress}\n\n")
        
        if (importantMemories.isNotEmpty()) {
            summary.append("【重要记忆】\n")
            importantMemories.take(5).forEach { memory ->
                summary.append("- [${memory.type}] ${memory.content}\n")
            }
            summary.append("\n")
        }
        
        if (recentSearches.isNotEmpty()) {
            summary.append("【最近搜索】\n")
            recentSearches.forEach { search ->
                summary.append("- ${search.query} (${search.type})\n")
            }
            summary.append("\n")
        }
        
        return summary.toString()
    }
    
    /**
     * 清空角色的所有记忆
     */
    @Suppress("unused")
    fun clearAllMemories(characterId: String) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "🗑️ 清空角色 $characterId 的所有记忆")
    }
    
    /**
     * 导出记忆数据（用于备份）
     */
    @Suppress("unused")
    fun exportMemories(characterId: String): String {
        val data = mapOf(
            "conversations" to getConversations(characterId, limit = 1000),
            "longTermMemories" to getLongTermMemories(characterId, limit = 500),
            "searches" to getSearchRecords(characterId, limit = 200),
            "emotions" to getEmotionRecords(characterId, limit = 500),
            "relationships" to getRelationshipRecords(characterId, limit = 300),
            "statistics" to getMemoryStatistics(characterId),
            "exportTime" to dateFormat.format(Date())
        )
        
        return gson.toJson(data)
    }
    
    // ========== 经验记忆管理 ==========
    
    /**
     * 添加经验记忆
     * 
     * 经验记忆是对一段对话的"升华"，不是原始记录，而是提炼出的经验和洞察
     * 
     * @param characterId 角色ID
     * @param timeRange 时间段
     * @param emotionalTone 情感基调
     * @param keyInsight 关键洞察
     * @param relationshipChange 关系变化
     * @param emotionTrajectory 情绪轨迹
     * @param conversationCount 包含的对话轮数
     * @param importance 重要性（1-10）
     */
    fun addExperienceMemory(
        characterId: String,
        timeRange: String,
        emotionalTone: String,
        keyInsight: String,
        relationshipChange: String,
        emotionTrajectory: String,
        conversationCount: Int,
        importance: Int = 7,
        metadata: Map<String, Any> = emptyMap()
    ): ExperienceMemoryRecord {
        val record = ExperienceMemoryRecord(
            characterId = characterId,
            timeRange = timeRange,
            emotionalTone = emotionalTone,
            keyInsight = keyInsight,
            relationshipChange = relationshipChange,
            emotionTrajectory = emotionTrajectory,
            conversationCount = conversationCount,
            importance = importance,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        
        val experiences = getExperienceMemories(characterId).toMutableList()
        experiences.add(record)
        
        // 按重要性排序，只保留最重要的100条
        val sorted = experiences.sortedByDescending { it.importance }.take(100)
        saveExperienceMemories(characterId, sorted)
        
        Log.d(TAG, "✨ 添加经验记忆: $keyInsight (重要性: $importance)")
        
        return record
    }
    
    /**
     * 获取经验记忆
     */
    fun getExperienceMemories(
        characterId: String,
        limit: Int = 50
    ): List<ExperienceMemoryRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("experience_memories", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<ExperienceMemoryRecord>>() {}.type
            val records: List<ExperienceMemoryRecord> = gson.fromJson(json, typeToken)
            records.sortedByDescending { it.importance }.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取经验记忆失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取最近的经验记忆摘要（用于AI读取）
     */
    fun getRecentExperienceSummary(characterId: String, limit: Int = 3): String {
        val experiences = getExperienceMemories(characterId, limit = limit)
        
        if (experiences.isEmpty()) {
            return ""
        }
        
        val summary = StringBuilder()
        summary.append("【最近的经验记忆】\n\n")
        
        experiences.forEach { exp ->
            summary.append("时间: ${exp.timeRange}\n")
            summary.append("感觉: ${exp.emotionalTone}\n")
            summary.append("洞察: ${exp.keyInsight}\n")
            summary.append("关系: ${exp.relationshipChange}\n")
            summary.append("情绪: ${exp.emotionTrajectory}\n")
            summary.append("\n")
        }
        
        return summary.toString()
    }
    
    private fun saveExperienceMemories(characterId: String, memories: List<ExperienceMemoryRecord>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("experience_memories", gson.toJson(memories)).apply()
    }
    
    // ========== 情绪片段管理 ==========
    
    /**
     * 添加情绪片段
     * 
     * 情绪片段记录某个时刻的情绪状态和自然语言描述
     * 
     * @param characterId 角色ID
     * @param pad PAD值
     * @param description 自然语言描述
     * @param trigger 触发原因
     */
    fun addEmotionFragment(
        characterId: String,
        pad: com.hoshiori.ai.brain.PADValue,
        description: String,
        trigger: String
    ): EmotionFragment {
        val fragment = EmotionFragment(
            characterId = characterId,
            pad = pad,
            description = description,
            trigger = trigger,
            timestamp = System.currentTimeMillis()
        )
        
        val fragments = getEmotionFragments(characterId).toMutableList()
        fragments.add(fragment)
        
        // 只保留最近100条
        val trimmed = if (fragments.size > 100) {
            fragments.takeLast(100)
        } else {
            fragments
        }
        
        saveEmotionFragments(characterId, trimmed)
        
        Log.d(TAG, "💭 添加情绪片段: $description")
        
        return fragment
    }
    
    /**
     * 获取情绪片段
     */
    fun getEmotionFragments(
        characterId: String,
        limit: Int = 50
    ): List<EmotionFragment> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString("emotion_fragments", null) ?: return emptyList()
        
        return try {
            val typeToken = object : TypeToken<List<EmotionFragment>>() {}.type
            val fragments: List<EmotionFragment> = gson.fromJson(json, typeToken)
            fragments.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "获取情绪片段失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取最近的情绪摘要（用于AI读取）
     */
    fun getRecentEmotionSummary(characterId: String, limit: Int = 3): String {
        val fragments = getEmotionFragments(characterId, limit = limit)
        
        if (fragments.isEmpty()) {
            return ""
        }
        
        val summary = StringBuilder()
        summary.append("【最近的心情】\n\n")
        
        fragments.forEach { fragment ->
            val time = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(fragment.timestamp))
            summary.append("$time: ${fragment.description}\n")
            summary.append("原因: ${fragment.trigger}\n\n")
        }
        
        return summary.toString()
    }
    
    private fun saveEmotionFragments(characterId: String, fragments: List<EmotionFragment>) {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString("emotion_fragments", gson.toJson(fragments)).apply()
    }
    
    // ========== 每日日记管理 ==========
    
    /**
     * 添加每日日记
     * 
     * 每天23:00自动生成的日记，记录AI的成长和反思
     * 
     * @param characterId 角色ID
     * @param date 日期（yyyy-MM-dd）
     * @param conversationCount 当天对话数
     * @param emotionalHighlight 情绪亮点
     * @param keyInsights 关键洞察
     * @param growthReflection 成长反思
     * @param relationshipChange 关系变化
     * @param lookingForward 对未来的期待
     */
    fun addDailyDiary(
        characterId: String,
        date: String,
        conversationCount: Int,
        emotionalHighlight: String,
        keyInsights: List<String>,
        growthReflection: String,
        relationshipChange: String,
        lookingForward: String
    ): DailyDiaryRecord {
        val diary = DailyDiaryRecord(
            characterId = characterId,
            date = date,
            conversationCount = conversationCount,
            emotionalHighlight = emotionalHighlight,
            keyInsights = keyInsights,
            growthReflection = growthReflection,
            relationshipChange = relationshipChange,
            lookingForward = lookingForward,
            timestamp = System.currentTimeMillis()
        )
        
        // 使用日期作为key，确保每天只有一条日记
        val key = "daily_diary_${characterId}_$date"
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        prefs.edit().putString(key, gson.toJson(diary)).apply()
        
        Log.d(TAG, "📔 添加每日日记: $date (${diary.conversationCount}条对话)")
        
        return diary
    }
    
    /**
     * 获取最近的日记
     * 
     * @param characterId 角色ID
     * @param limit 返回最近N天的日记
     */
    fun getRecentDiaries(characterId: String, limit: Int = 3): List<DailyDiaryRecord> {
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val diaryKeys = allKeys.filter { it.startsWith("daily_diary_$characterId") }
        
        return diaryKeys
            .mapNotNull { key ->
                val json = prefs.getString(key, null)
                json?.let { gson.fromJson(it, DailyDiaryRecord::class.java) }
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 获取日记摘要（用于添加到上下文）
     * 
     * @param characterId 角色ID
     * @param limit 返回最近N天的日记
     */
    fun getRecentDiariesSummary(characterId: String, limit: Int = 3): String {
        val diaries = getRecentDiaries(characterId, limit)
        if (diaries.isEmpty()) return ""
        
        val summary = StringBuilder()
        summary.append("【最近的成长日记】\n\n")
        
        diaries.forEach { diary ->
            summary.append("📅 ${diary.date}\n")
            summary.append("💭 ${diary.growthReflection}\n")
            if (diary.keyInsights.isNotEmpty()) {
                summary.append("✨ ${diary.keyInsights.joinToString("；")}\n")
            }
            if (diary.lookingForward.isNotEmpty()) {
                summary.append("🌟 ${diary.lookingForward}\n")
            }
            summary.append("\n")
        }
        
        return summary.toString()
    }
    
    /**
     * 获取指定日期的日记
     * 
     * @param characterId 角色ID
     * @param date 日期（yyyy-MM-dd）
     */
    fun getDiaryByDate(characterId: String, date: String): DailyDiaryRecord? {
        val key = "daily_diary_${characterId}_$date"
        val prefs = context.getSharedPreferences("$PREFS_NAME.$characterId", Context.MODE_PRIVATE)
        val json = prefs.getString(key, null) ?: return null
        
        return try {
            gson.fromJson(json, DailyDiaryRecord::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "获取日记失败", e)
            null
        }
    }
    
    /**
     * 检查今天是否已经写过日记
     * 
     * @param characterId 角色ID
     * @param date 日期（yyyy-MM-dd）
     */
    fun hasDiaryForDate(characterId: String, date: String): Boolean {
        return getDiaryByDate(characterId, date) != null
    }
}

// ========== 数据模型 ==========

/**
 * 对话记录
 */
data class ConversationRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val content: String,
    val isUser: Boolean,
    val emotion: String = "neutral",
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long
)

/**
 * 长期记忆记录
 */
data class LongTermMemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val type: MemoryType,
    val content: String,
    val importance: Int,
    val relatedConversationId: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long
)

/**
 * 记忆类型
 */
@Suppress("unused")
enum class MemoryType {
    USER_INFO,      // 用户信息（姓名、年龄、职业等）
    USER_PREFERENCE,// 用户偏好（喜好、习惯等）
    EVENT,          // 重要事件
    KNOWLEDGE,      // 知识性内容
    EMOTION,        // 情感性内容
    RELATIONSHIP,   // 关系相关
    SEARCH_RESULT,  // 搜索结果
    OTHER           // 其他
}

/**
 * 搜索记录
 */
data class SearchRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val query: String,
    val type: WebSearchSystem.SearchType,
    val result: String? = null,
    val success: Boolean,
    val timestamp: Long
)

/**
 * 情绪记录
 */
data class EmotionRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val emotion: String,
    val pleasure: Double,
    val arousal: Double,
    val dominance: Double,
    val trigger: String? = null,
    val timestamp: Long
)

/**
 * 关系记录
 */
data class RelationshipRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val trust: Int,
    val intimacy: Int,
    val stress: Int,
    val event: String? = null,
    val timestamp: Long
)

/**
 * 记忆统计
 */
data class MemoryStatistics(
    val totalConversations: Int,
    val userMessages: Int,
    val aiMessages: Int,
    val totalLongTermMemories: Int,
    val totalSearches: Int,
    val successfulSearches: Int,
    val totalEmotionRecords: Int,
    val totalRelationshipRecords: Int,
    val averageTrust: Int,
    val averageIntimacy: Int,
    val averageStress: Int
)

/**
 * 经验记忆记录
 * 
 * 这是对一段对话的"升华"，不是原始对话记录，而是提炼出的经验和洞察
 * 类似于人类的"那次对话的感觉"
 */
data class ExperienceMemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val timeRange: String,                    // 时间段，如"2026-01-10 14:00-15:00"
    val emotionalTone: String,                // 情感基调，如"温暖、深入"
    val keyInsight: String,                   // 关键洞察，如"用户开始更信任我"
    val relationshipChange: String,           // 关系变化，如"信任度+0.1"
    val emotionTrajectory: String,            // 情绪轨迹，如"中性 → 共鸣 → 温暖"
    val conversationCount: Int,               // 包含的对话轮数
    val importance: Int,                      // 重要性（1-10）
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long
)

/**
 * 情绪片段
 * 
 * 记录某个时刻的情绪状态和触发原因
 */
data class EmotionFragment(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val pad: com.hoshiori.ai.brain.PADValue,
    val description: String,                  // 自然语言描述，如"比较开心，但不是那种兴奋的开心"
    val trigger: String,                      // 触发原因
    val timestamp: Long
)

/**
 * 每日日记记录
 * 
 * AI每天23:00自动生成的日记，记录当天的成长和反思
 */
data class DailyDiaryRecord(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val date: String,                         // 日期（yyyy-MM-dd）
    val conversationCount: Int,               // 当天对话数
    val emotionalHighlight: String,           // 情绪亮点
    val keyInsights: List<String>,            // 关键洞察
    val growthReflection: String,             // 成长反思
    val relationshipChange: String,           // 关系变化
    val lookingForward: String,               // 对未来的期待
    val timestamp: Long
)
