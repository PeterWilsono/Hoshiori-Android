package com.hoshiori.ai.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 联网搜索系统 - 中国大陆适配版
 * 
 * 功能：
 * 1. 天气查询（使用国内可访问的API）
 * 2. 知识查询（通过AI的Function Calling实现）
 * 3. 新闻查询（通过AI的Function Calling实现）
 * 4. 离线时自主搜索
 * 
 * 注意：
 * - 不使用DuckDuckGo（在中国大陆无法访问）
 * - 使用和风天气API（免费，国内可访问）
 * - 搜索功能通过AI的Function Calling实现
 */
class WebSearchSystem {
    
    /**
     * 搜索类型
     */
    enum class SearchType {
        GENERAL,    // 通用搜索
        WEATHER,    // 天气查询
        NEWS,       // 新闻查询
        KNOWLEDGE,  // 知识查询
        INTEREST    // 兴趣搜索（根据角色设定）
    }
    
    /**
     * 搜索结果
     */
    data class SearchResult(
        val query: String,
        val type: SearchType,
        val results: List<SearchItem>,
        val summary: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 搜索项
     */
    data class SearchItem(
        val title: String,
        val snippet: String,
        val url: String = "",
        val source: String = ""
    )
    
    /**
     * 搜索历史
     */
    data class SearchHistory(
        val query: String,
        val type: SearchType,
        val timestamp: Long,
        val success: Boolean
    )
    
    private val searchHistory = mutableListOf<SearchHistory>()
    
    // ⭐ 天气API调用计数（每日限制5次）
    private var weatherApiCallCount = 0
    private var lastResetDate = ""
    
    /**
     * 检查今天是否还能调用天气API
     */
    private fun canCallWeatherApi(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        // 如果是新的一天，重置计数
        if (today != lastResetDate) {
            weatherApiCallCount = 0
            lastResetDate = today
        }
        
        // 每天最多5次
        return weatherApiCallCount < 5
    }
    
    /**
     * 记录天气API调用
     */
    private fun recordWeatherApiCall() {
        weatherApiCallCount++
    }
    
    /**
     * 获取今天剩余的天气API调用次数
     */
    fun getRemainingWeatherApiCalls(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        if (today != lastResetDate) {
            return 5
        }
        
        return maxOf(0, 5 - weatherApiCallCount)
    }
    
    /**
     * 通用搜索 - 通过AI的Function Calling实现
     * 
     * 注意：由于DuckDuckGo在中国大陆无法访问，
     * 这里改为生成搜索提示，让AI使用自己的知识库回答
     */
    suspend fun search(query: String, type: SearchType = SearchType.GENERAL): SearchResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 记录搜索历史
                recordSearch(query, type, true)
                
                // 生成搜索提示（让AI使用自己的知识库）
                val searchPrompt = """
【搜索请求】
用户想了解：$query

💡 请使用你的知识库回答这个问题：
- 如果你知道，直接回答
- 如果不确定，说"我不太确定，但根据我的理解..."
- 如果完全不知道，诚实地说"这个我不太了解"

不要说"根据搜索结果"，要自然地回答。
                """.trimIndent()
                
                SearchResult(
                    query = query,
                    type = type,
                    results = listOf(
                        SearchItem(
                            title = "AI知识库",
                            snippet = searchPrompt,
                            source = "AI Knowledge"
                        )
                    ),
                    summary = searchPrompt
                )
            } catch (e: Exception) {
                e.printStackTrace()
                recordSearch(query, type, false)
                null
            }
        }
    }
    
    /**
     * 天气查询 - 使用和风天气API（免费，国内可访问）
     * 
     * API key格式：
     * - 标准格式：32位字符串（如：abc123...）
     * - 域名格式：xxx.xxx.qweatherapi.com（如：pf5khvty9m.re.qweatherapi.com）
     * 
     * 每日限制：5次调用
     */
    suspend fun searchWeather(location: String, apiKey: String = ""): SearchResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 检查是否有API key
                if (apiKey.isEmpty()) {
                    recordSearch("天气 $location", SearchType.WEATHER, true)
                    
                    val weatherPrompt = """
【天气查询】
用户想了解：${location}的天气

💡 由于没有配置天气API，请这样回答：
- "让我帮你查一下天气~"
- "不好意思，我现在查不到实时天气数据"
- "你可以在设置中配置和风天气API key，这样我就能查询实时天气了"
- 或者根据常识给出建议："这个季节${location}一般比较..."

保持自然，不要让用户感觉生硬。
                    """.trimIndent()
                    
                    return@withContext SearchResult(
                        query = "天气 $location",
                        type = SearchType.WEATHER,
                        results = listOf(
                            SearchItem(
                                title = "${location}的天气",
                                snippet = weatherPrompt,
                                source = "提示"
                            )
                        ),
                        summary = weatherPrompt
                    )
                }
                
                // ⭐ 检查今天是否还能调用API
                if (!canCallWeatherApi()) {
                    recordSearch("天气 $location", SearchType.WEATHER, false)
                    
                    val limitPrompt = """
【天气查询】
用户想了解：${location}的天气

💡 今天的天气API调用次数已用完（每天限制5次），请这样回答：
- "让我想想${location}的天气~"
- "不好意思，今天查天气的次数用完了"
- "根据这个季节，${location}一般..."
- 或者建议用户明天再问

保持自然，不要让用户感觉生硬。
                    """.trimIndent()
                    
                    return@withContext SearchResult(
                        query = "天气 $location",
                        type = SearchType.WEATHER,
                        results = listOf(
                            SearchItem(
                                title = "${location}的天气",
                                snippet = limitPrompt,
                                source = "提示"
                            )
                        ),
                        summary = limitPrompt
                    )
                }
                
                val encodedLocation = URLEncoder.encode(location, "UTF-8")
                
                // ⭐ 处理API key格式
                // 如果是域名格式（如：pf5khvty9m.re.qweatherapi.com），需要特殊处理
                val (geoApiUrl, weatherApiUrl) = if (apiKey.contains("qweatherapi.com")) {
                    // 域名格式
                    Pair(
                        "https://$apiKey/v2/city/lookup?location=$encodedLocation",
                        "https://$apiKey/v7/weather/now?location="
                    )
                } else {
                    // 标准格式
                    Pair(
                        "https://geoapi.qweather.com/v2/city/lookup?location=$encodedLocation&key=$apiKey",
                        "https://devapi.qweather.com/v7/weather/now?location="
                    )
                }
                
                // 先获取城市ID
                val locationConnection = URL(geoApiUrl).openConnection() as HttpURLConnection
                locationConnection.requestMethod = "GET"
                locationConnection.connectTimeout = 10000
                locationConnection.readTimeout = 10000
                
                val locationResponse = locationConnection.inputStream.bufferedReader().use { it.readText() }
                val locationJson = JSONObject(locationResponse)
                
                if (locationJson.optString("code") != "200") {
                    recordSearch("天气 $location", SearchType.WEATHER, false)
                    return@withContext null
                }
                
                val locationData = locationJson.optJSONArray("location")?.optJSONObject(0)
                val locationId = locationData?.optString("id") ?: return@withContext null
                
                // 获取实时天气
                val finalWeatherUrl = if (apiKey.contains("qweatherapi.com")) {
                    "$weatherApiUrl$locationId"
                } else {
                    "$weatherApiUrl$locationId&key=$apiKey"
                }
                
                val weatherConnection = URL(finalWeatherUrl).openConnection() as HttpURLConnection
                weatherConnection.requestMethod = "GET"
                weatherConnection.connectTimeout = 10000
                weatherConnection.readTimeout = 10000
                
                val weatherResponse = weatherConnection.inputStream.bufferedReader().use { it.readText() }
                val weatherJson = JSONObject(weatherResponse)
                
                if (weatherJson.optString("code") != "200") {
                    recordSearch("天气 $location", SearchType.WEATHER, false)
                    return@withContext null
                }
                
                val now = weatherJson.optJSONObject("now")
                if (now != null) {
                    val temp = now.optString("temp", "")
                    val feelsLike = now.optString("feelsLike", "")
                    val text = now.optString("text", "")
                    val humidity = now.optString("humidity", "")
                    val windSpeed = now.optString("windSpeed", "")
                    val windDir = now.optString("windDir", "")
                    
                    val weatherInfo = """
【${location}实时天气】
当前温度：${temp}°C（体感${feelsLike}°C）
天气状况：$text
湿度：${humidity}%
风向风速：${windDir} ${windSpeed}km/h

💡 你可以根据这个天气信息自然地回答用户，比如：
- 如果温度低，提醒多穿点
- 如果下雨，提醒带伞
- 如果天气好，可以建议出去走走

今天还可以查询天气${getRemainingWeatherApiCalls() - 1}次。
                    """.trimIndent()
                    
                    val results = listOf(
                        SearchItem(
                            title = "${location}的天气",
                            snippet = weatherInfo,
                            source = "和风天气"
                        )
                    )
                    
                    recordSearch("天气 $location", SearchType.WEATHER, true)
                    recordWeatherApiCall() // ⭐ 记录API调用
                    
                    SearchResult(
                        query = "天气 $location",
                        type = SearchType.WEATHER,
                        results = results,
                        summary = weatherInfo
                    )
                } else {
                    recordSearch("天气 $location", SearchType.WEATHER, false)
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recordSearch("天气 $location", SearchType.WEATHER, false)
                null
            }
        }
    }
    
    /**
     * 生成搜索摘要
     */
    private fun generateSummary(results: List<SearchItem>): String {
        if (results.isEmpty()) return ""
        
        val mainResult = results.first()
        val summary = StringBuilder()
        
        summary.append("【搜索结果】\n")
        summary.append("${mainResult.title}\n\n")
        summary.append(mainResult.snippet.take(200))
        
        if (mainResult.snippet.length > 200) {
            summary.append("...")
        }
        
        if (results.size > 1) {
            summary.append("\n\n相关信息：")
            results.drop(1).take(2).forEach { item ->
                summary.append("\n• ${item.title}")
            }
        }
        
        return summary.toString()
    }
    
    /**
     * 记录搜索历史
     */
    private fun recordSearch(query: String, type: SearchType, success: Boolean) {
        searchHistory.add(SearchHistory(
            query = query,
            type = type,
            timestamp = System.currentTimeMillis(),
            success = success
        ))
        
        // 只保留最近100次
        if (searchHistory.size > 100) {
            searchHistory.removeAt(0)
        }
    }
    
    /**
     * 获取搜索历史
     */
    fun getSearchHistory(limit: Int = 10): List<SearchHistory> {
        return searchHistory.takeLast(limit).reversed()
    }
    
    /**
     * 检查是否需要搜索
     * 
     * ⚠️ 重要：只在以下场景触发搜索
     * 1. 天气查询（用户明确问天气）
     * 2. 离线生活（AI主动搜索感兴趣的内容）
     * 
     * ❌ 不在对话中触发知识搜索（体验不好）
     */
    fun shouldSearch(userMessage: String): Pair<Boolean, String?> {
        // ⭐ 只检测天气查询
        val weatherKeywords = listOf("天气", "温度", "下雨", "晴天", "阴天", "多少度")
        if (weatherKeywords.any { userMessage.contains(it) }) {
            // 尝试提取地点
            val locationPattern = Regex("(.{2,10})(的)?(天气|温度|多少度)")
            val match = locationPattern.find(userMessage)
            if (match != null) {
                return Pair(true, match.groupValues[1])
            }
            return Pair(true, "当前位置")
        }
        
        // ⭐ 其他情况都不搜索，让AI用自己的知识库
        return Pair(false, null)
    }
}

/**
 * 离线自主搜索系统
 */
class OfflineSearchSystem(private val webSearchSystem: WebSearchSystem) {
    
    /**
     * 待搜索队列
     */
    data class SearchTask(
        val query: String,
        val type: WebSearchSystem.SearchType,
        val reason: String,
        val priority: Int = 5,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val searchQueue = mutableListOf<SearchTask>()
    
    /**
     * 添加搜索任务
     */
    fun addSearchTask(query: String, type: WebSearchSystem.SearchType, reason: String, priority: Int = 5) {
        searchQueue.add(SearchTask(
            query = query,
            type = type,
            reason = reason,
            priority = priority
        ))
        
        // 按优先级排序
        searchQueue.sortByDescending { it.priority }
    }
    
    /**
     * 从对话中提取待搜索内容
     * 
     * ⚠️ 重要：只提取离线生活相关的搜索任务
     * - 不提取知识查询（让AI用自己的知识库）
     * - 只提取角色感兴趣的话题
     */
    fun extractSearchTasksFromConversation(
        userMessage: String,
        aiResponse: String,
        characterInterests: List<String> = emptyList()
    ) {
        // ⭐ 移除：不再提取AI不知道的内容
        // 让AI用自己的知识库回答，不要在对话中联网搜索
        
        // ⭐ 只根据角色兴趣添加搜索任务（用于离线生活）
        for (interest in characterInterests) {
            // 随机决定是否搜索（避免过度搜索）
            // 降低概率：从30%降到10%
            if (Math.random() < 0.1) {
                addSearchTask(
                    query = interest,
                    type = WebSearchSystem.SearchType.INTEREST,
                    reason = "根据角色兴趣搜索：$interest（用于离线生活分享）",
                    priority = 5
                )
            }
        }
    }
    
    /**
     * 执行离线搜索
     */
    suspend fun performOfflineSearch(maxTasks: Int = 3): List<WebSearchSystem.SearchResult> {
        val results = mutableListOf<WebSearchSystem.SearchResult>()
        
        val tasksToExecute = searchQueue.take(maxTasks)
        
        for (task in tasksToExecute) {
            val result = when (task.type) {
                WebSearchSystem.SearchType.WEATHER -> {
                    webSearchSystem.searchWeather(task.query)
                }
                else -> {
                    webSearchSystem.search(task.query, task.type)
                }
            }
            
            if (result != null) {
                results.add(result)
            }
            
            // 从队列中移除
            searchQueue.remove(task)
        }
        
        return results
    }
    
    /**
     * 生成搜索分享消息
     */
    fun generateShareMessage(results: List<WebSearchSystem.SearchResult>): String {
        if (results.isEmpty()) return ""
        
        val message = StringBuilder()
        message.append("💡 我离线的时候查了一些东西，想和你分享：\n\n")
        
        for ((index, result) in results.withIndex()) {
            if (index > 0) message.append("\n\n")
            
            when (result.type) {
                WebSearchSystem.SearchType.WEATHER -> {
                    message.append("🌤️ 天气信息：\n")
                }
                WebSearchSystem.SearchType.KNOWLEDGE -> {
                    message.append("📚 关于「${result.query}」：\n")
                }
                WebSearchSystem.SearchType.INTEREST -> {
                    message.append("✨ 我搜了一下「${result.query}」：\n")
                }
                else -> {
                    message.append("🔍 搜索结果：\n")
                }
            }
            
            message.append(result.summary.take(300))
            if (result.summary.length > 300) {
                message.append("...")
            }
        }
        
        return message.toString()
    }
    
    /**
     * 提取关键词
     */
    private fun extractKeywords(text: String): List<String> {
        val keywords = mutableListOf<String>()
        
        // 提取中文词汇（2-6字）
        val chinesePattern = Regex("[\\u4e00-\\u9fff]{2,6}")
        val matches = chinesePattern.findAll(text)
        
        for (match in matches) {
            val word = match.value
            // 过滤常见词
            val stopWords = setOf("什么", "怎么", "如何", "为什么", "哪里", "这个", "那个", "可以", "不是")
            if (word !in stopWords) {
                keywords.add(word)
            }
        }
        
        return keywords.take(3)
    }
    
    /**
     * 获取待搜索任务数量
     */
    fun getPendingTaskCount(): Int = searchQueue.size
    
    /**
     * 清空搜索队列
     */
    fun clearQueue() {
        searchQueue.clear()
    }
}

