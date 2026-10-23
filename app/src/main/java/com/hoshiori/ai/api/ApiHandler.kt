package com.hoshiori.ai.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hoshiori.ai.data.*
import com.hoshiori.ai.brain.PADValue
import com.hoshiori.ai.brain.ConversationManager
import com.hoshiori.ai.optimization.TokenOptimizer
import com.hoshiori.ai.optimization.ScenarioDetector
import com.hoshiori.ai.optimization.ConversationHistoryOptimizer
import com.hoshiori.ai.optimization.ConversationScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * API调用处理器 - 深度心理增强版 + Token优化
 * 完整移植自 Python api_handler.py
 */
class ApiHandler(private val config: ApiConfig) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // ⭐ 对话管理器（新增）
    val conversationManager = ConversationManager()
    
    // ⭐⭐⭐ Token优化器（新增）
    private val tokenOptimizer = TokenOptimizer()
    private val scenarioDetector = ScenarioDetector()
    private val historyOptimizer = ConversationHistoryOptimizer()
    
    // Token使用统计
    private var totalTokensSaved = 0
    private var totalCalls = 0
    
    // 软性拒绝关键词库
    private val refusalKeywords = listOf(
        "作为一个人工智能", "作为AI", "我是一个语言模型",
        "我无法", "我不能", "违反", "内容政策", "不适当",
        "安全原因", "道德准则", "超出我的能力"
    )
    
    /**
     * 调用API获取响应（Token优化版）
     */
    suspend fun callApi(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        psychCore: PsychCore?,
        memoryContext: String = "",
        offlineLife: String = "",
        emotionTrigger: String = "",
        emotionHistory: List<PADValue> = emptyList(),
        psychologyEnhanced: String = "",
        recentChatMessages: List<ChatMessage> = emptyList() // 新增：用于场景检测和历史优化
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
            totalCalls++
            
            // ⭐⭐⭐ Token优化流程
            val userMessage = messages.lastOrNull()?.get("content") ?: ""
            
            // 1. 场景检测
            val scenario = scenarioDetector.detectScenario(
                userMessage, recentChatMessages, psychCore
            )
            
            // 2. 优化Prompt
            val optimizedPrompt = if (psychCore != null) {
                val result = tokenOptimizer.optimizePrompt(
                    rolePrompt = systemPrompt,
                    psychCore = psychCore,
                    memoryContext = memoryContext,
                    scenario = scenario,
                    offlineLife = offlineLife,
                    emotionTrigger = emotionTrigger,
                    psychologyEnhanced = psychologyEnhanced
                )
                
                // 记录Token节省
                val originalTokens = estimateTokens(systemPrompt + memoryContext + offlineLife + emotionTrigger + psychologyEnhanced)
                val saved = originalTokens - result.estimatedTokens
                totalTokensSaved += saved
                
                android.util.Log.d("ApiHandler", """
                    💰 Token优化:
                    - 场景: $scenario
                    - 原始: ${originalTokens}tokens
                    - 优化后: ${result.estimatedTokens}tokens
                    - 节省: ${saved}tokens (${(saved.toDouble() / originalTokens * 100).toInt()}%)
                    - 累计节省: ${totalTokensSaved}tokens (${totalCalls}次调用)
                """.trimIndent())
                
                result.content
            } else {
                systemPrompt
            }
            
            // 3. 优化对话历史
            val optimizedMessages = if (recentChatMessages.isNotEmpty()) {
                val optimized = historyOptimizer.optimizeHistory(recentChatMessages)
                optimized.map { msg ->
                    mapOf(
                        "role" to if (msg.isFromUser) "user" else "assistant",
                        "content" to msg.content
                    )
                }
            } else {
                messages
            }
            
            // 4. 构建API消息
            val apiMessages = mutableListOf<Map<String, String>>()
            apiMessages.add(mapOf("role" to "system", "content" to optimizedPrompt))
            apiMessages.addAll(optimizedMessages)
            
            // ⭐ 动态调整max_tokens和frequency_penalty（基于人格成熟度）
            val maxTokens = calculateDynamicMaxTokens(psychCore)
            val frequencyPenalty = calculateFrequencyPenalty(psychCore)
            
            val payload = mapOf(
                "model" to config.model,
                "messages" to apiMessages,
                "max_tokens" to maxTokens,
                "temperature" to 0.3,
                "top_p" to 0.95,
                "stream" to false,
                "frequency_penalty" to frequencyPenalty
            )
            
            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(config.apiEndpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult.Error("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string() ?: return@withContext ApiResult.Error("空响应")
            android.util.Log.d("ApiHandler", "📡 API原始响应体: $responseBody")
            
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            
            val aiResponse = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            android.util.Log.d("ApiHandler", "🤖 AI原始回复: $aiResponse")
            android.util.Log.d("ApiHandler", "📏 AI回复长度: ${aiResponse.length}字符")
            
            // 检查是否被截断
            val finishReason = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .get("finish_reason")?.asString
            android.util.Log.d("ApiHandler", "🏁 结束原因: $finishReason")
            if (finishReason == "length") {
                android.util.Log.w("ApiHandler", "⚠️ 警告：回复因达到max_tokens限制而被截断！")
            }
            
            val finalResponse = if (detectRefusal(aiResponse) && psychCore != null) {
                rewriteRefusal(aiResponse, psychCore)
            } else {
                aiResponse
            }
            
            val parsed = parseAiResponse(finalResponse)
            
            ApiResult.Success(finalResponse, parsed)
            
        } catch (_: Exception) {
            ApiResult.Error("连接失败")
        }
    }

    
    /**
     * ⭐⭐⭐ 第二阶段：双阶段思考系统
     * 
     * 让AI"先思考，再说话"
     * 
     * 第一次调用：生成内心独白（用户看不到）
     * 第二次调用：基于思考生成正式回复
     * 
     * @param messages 对话历史
     * @param systemPrompt 系统提示
     * @param psychCore 心理核心
     * @param memoryContext 记忆上下文
     * @param offlineLife 离线生活
     * @param emotionTrigger 情绪触发
     * @param emotionHistory 情绪历史
     * @param psychologyEnhanced 心理学增强
     * @return 双阶段思考结果
     */
    suspend fun callApiWithThinking(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        psychCore: PsychCore?,
        memoryContext: String = "",
        offlineLife: String = "",
        emotionTrigger: String = "",
        emotionHistory: List<PADValue> = emptyList(),
        psychologyEnhanced: String = ""
    ): DualStageResult = withContext(Dispatchers.IO) {
        try {
            // ========== 第一阶段：内心独白 ==========
            val thinkingStartTime = System.currentTimeMillis()
            
            val thinkingPrompt = generateThinkingPrompt(
                messages = messages,
                systemPrompt = systemPrompt,
                psychCore = psychCore
            )
            
            val thinkingMessages = mutableListOf<Map<String, String>>()
            thinkingMessages.add(mapOf("role" to "system", "content" to thinkingPrompt))
            thinkingMessages.addAll(messages.takeLast(3)) // 只用最近3条消息
            
            val thinkingPayload = mapOf(
                "model" to config.model,
                "messages" to thinkingMessages,
                "max_tokens" to 200, // 内心独白控制在200 tokens以内
                "temperature" to 0.5,
                "top_p" to 0.9,
                "stream" to false
            )
            
            val thinkingRequestBody = gson.toJson(thinkingPayload)
                .toRequestBody("application/json".toMediaType())
            
            val thinkingRequest = Request.Builder()
                .url(config.apiEndpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(thinkingRequestBody)
                .build()
            
            val thinkingResponse = client.newCall(thinkingRequest).execute()
            
            if (!thinkingResponse.isSuccessful) {
                // 如果第一阶段失败，降级到单阶段
                android.util.Log.w("ApiHandler", "⚠️ 第一阶段失败，降级到单阶段")
                val singleResult = callApi(
                    messages, systemPrompt, psychCore, memoryContext,
                    offlineLife, emotionTrigger, emotionHistory, psychologyEnhanced
                )
                return@withContext when (singleResult) {
                    is ApiResult.Success -> DualStageResult.Success(
                        internalMonologue = "（思考阶段失败，直接回复）",
                        finalResponse = singleResult.rawResponse,
                        parsed = singleResult.parsed,
                        thinkingTimeMs = 0,
                        responseTimeMs = 0
                    )
                    is ApiResult.Error -> DualStageResult.Error(singleResult.message)
                }
            }
            
            val thinkingBody = thinkingResponse.body?.string() ?: ""
            val thinkingJson = JsonParser.parseString(thinkingBody).asJsonObject
            val internalMonologue = thinkingJson
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            val thinkingEndTime = System.currentTimeMillis()
            val thinkingTimeMs = thinkingEndTime - thinkingStartTime
            
            android.util.Log.d("ApiHandler", "💭 内心独白: $internalMonologue")
            android.util.Log.d("ApiHandler", "⏱️ 思考耗时: ${thinkingTimeMs}ms")
            
            // ========== 第二阶段：正式回复 ==========
            val responseStartTime = System.currentTimeMillis()
            
            val overloadWarning = checkEmotionOverload(emotionHistory)
            val intentAnalysisHint = generateIntentAnalysisHint()
            
            val finalSystemPrompt = if (psychCore != null) {
                buildPsychologyPromptWithThinking(
                    systemPrompt,
                    psychCore,
                    memoryContext,
                    offlineLife,
                    emotionTrigger,
                    overloadWarning,
                    intentAnalysisHint,
                    psychologyEnhanced,
                    internalMonologue // 加入内心独白
                )
            } else {
                """
$systemPrompt

【你刚才的思考】
$internalMonologue

现在请基于你的思考给出回复。记住你的人格设定，自然地说话，不要让对方感觉你在分析TA。
                """.trimIndent()
            }
            
            val apiMessages = mutableListOf<Map<String, String>>()
            apiMessages.add(mapOf("role" to "system", "content" to finalSystemPrompt))
            apiMessages.addAll(messages)
            
            // ⭐ 动态调整max_tokens和frequency_penalty（基于人格成熟度）
            val maxTokens = calculateDynamicMaxTokens(psychCore)
            val frequencyPenalty = calculateFrequencyPenalty(psychCore)
            
            val payload = mapOf(
                "model" to config.model,
                "messages" to apiMessages,
                "max_tokens" to maxTokens,
                "temperature" to 0.3,
                "top_p" to 0.95,
                "stream" to false,
                "frequency_penalty" to frequencyPenalty
            )
            
            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(config.apiEndpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext DualStageResult.Error("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string() ?: return@withContext DualStageResult.Error("空响应")
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            
            val aiResponse = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            val responseEndTime = System.currentTimeMillis()
            val responseTimeMs = responseEndTime - responseStartTime
            
            android.util.Log.d("ApiHandler", "🤖 正式回复: $aiResponse")
            android.util.Log.d("ApiHandler", "⏱️ 回复耗时: ${responseTimeMs}ms")
            android.util.Log.d("ApiHandler", "⏱️ 总耗时: ${thinkingTimeMs + responseTimeMs}ms")
            
            val finalResponse = if (detectRefusal(aiResponse) && psychCore != null) {
                rewriteRefusal(aiResponse, psychCore)
            } else {
                aiResponse
            }
            
            val parsed = parseAiResponse(finalResponse)
            
            DualStageResult.Success(
                internalMonologue = internalMonologue,
                finalResponse = finalResponse,
                parsed = parsed,
                thinkingTimeMs = thinkingTimeMs,
                responseTimeMs = responseTimeMs
            )
            
        } catch (e: Exception) {
            android.util.Log.e("ApiHandler", "双阶段思考失败", e)
            DualStageResult.Error("连接失败: ${e.message}")
        }
    }
    
    /**
     * 生成思考阶段的Prompt
     */
    @Suppress("UNUSED_PARAMETER")
    private fun generateThinkingPrompt(
        messages: List<Map<String, String>>,
        systemPrompt: String,
        psychCore: PsychCore?
    ): String {
        val userMessage = messages.lastOrNull { it["role"] == "user" }?.get("content") ?: ""
        
        val characterInfo = if (psychCore != null) {
            """
你的当前状态：
- 情绪：P=${String.format(Locale.US, "%.2f", psychCore.state.pleasure)} A=${String.format(Locale.US, "%.2f", psychCore.state.arousal)} D=${String.format(Locale.US, "%.2f", psychCore.state.dominance)}
- 关系：信任${psychCore.relationship.trust} 亲密${psychCore.relationship.intimacy} 压力${psychCore.relationship.stress}
            """.trimIndent()
        } else {
            ""
        }
        
        return """
你是一个有完整心理的AI，刚收到用户的消息：

"$userMessage"

$characterInfo

现在不要急着回复，先在心里想一想（用第一人称，像自言自语）：

1. TA现在的情绪状态是什么？
2. TA真正想表达的是什么（可能和字面意思不同）？
3. TA现在需要什么？（倾听/建议/陪伴/鼓励）
4. 你此刻的真实感受是什么？
5. 你想如何回应才能真正帮到TA？

用第一人称写下你的思考，像自言自语。控制在100字以内。

示例：
"TA说'还行吧'，但我感觉不太对劲。语气有点疲惫，可能是工作又累了。TA可能不想直接抱怨，但其实需要有人关心。我不要直接问'怎么了'，太生硬。我想先轻松地聊聊，让TA慢慢打开。"
        """.trimIndent()
    }
    
    /**
     * 构建包含思考的Prompt
     */
    private fun buildPsychologyPromptWithThinking(
        baseRole: String,
        psychCore: PsychCore,
        memoryContext: String,
        offlineLife: String,
        emotionTrigger: String,
        overloadWarning: String,
        intentAnalysisHint: String,
        psychologyEnhanced: String,
        internalMonologue: String
    ): String {
        val basePrompt = buildPsychologyPrompt(
            baseRole, psychCore, memoryContext, offlineLife,
            emotionTrigger, overloadWarning, intentAnalysisHint, psychologyEnhanced
        )
        
        return """
$basePrompt

【💭 你刚才的思考】
$internalMonologue

现在请基于你的思考给出回复。记住：
- 你的人格设定是最高优先级
- 根据你的思考来决定回复方式和长度
- 自然地说话，不要让对方感觉你在分析TA
- 你的思考只是内心活动，不要在回复中提及
- 如果有不想让用户看到的想法，用【内心】标记
- 回复长度由你根据情境自己决定（可长可短，可以只有几个字）
        """.trimIndent()
    }
    
    /**
     * 构建包含心理学规则和长期记忆的超级Prompt
     */
    private fun buildPsychologyPrompt(
        baseRole: String,
        psychCore: PsychCore,
        memoryContext: String,
        offlineLife: String,
        emotionTrigger: String,
        overloadWarning: String = "",
        intentAnalysisHint: String = "",
        psychologyEnhanced: String = ""
    ): String {
        val state = psychCore.state
        val rel = psychCore.relationship
        val personality = psychCore.personality
        
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
        
        val hoursPassed = (System.currentTimeMillis() - psychCore.lastInteractionTime) / (1000 * 60 * 60.0)
        val timeGapDesc = when {
            hoursPassed < 0.017 -> "秒回"
            hoursPassed < 1 -> "${(hoursPassed * 60).toInt()}分钟"
            hoursPassed < 24 -> "${hoursPassed.toInt()}小时"
            else -> "${(hoursPassed / 24).toInt()}天"
        }
        
        val p = state.pleasure
        val a = state.arousal
        val d = state.dominance
        val intensity = kotlin.math.abs(a) * 10
        
        val emotionDesc = when {
            p > 0.3 && a > 0.3 -> "兴奋、愉悦"
            p < -0.3 && a > 0.3 && d > 0 -> "愤怒、激动"
            p < -0.3 && a < 0 -> "悲伤、低落"
            p > 0 && a > 0.5 && d < 0 -> "害羞、紧张"
            else -> "平静、中性"
        }
        
        val memorySection = if (memoryContext.isNotEmpty()) """

[📝 长期记忆 - 你记住的关于用户的一切]

$memoryContext

💡 **重要提醒**:这些是你们共同经历的真实记忆,不是虚构的。当用户提到过去的事时,你应该能自然回忆起这些细节。
""" else ""

        val offlineSection = if (offlineLife.isNotEmpty()) """

[🌟 你的离线状态]
$offlineLife

(这是你不在线时的"虚拟生活",可以自然融入对话中)
""" else ""

        val emotionTriggerSection = if (emotionTrigger.isNotEmpty()) """

[💭 情绪联想触发]
$emotionTrigger

(你的当前情绪让你想起了这些往事,可以自然提及)
""" else ""

        val overloadSection = if (overloadWarning.isNotEmpty()) """

[⚠️ 情绪过载预警]
$overloadWarning
""" else ""

        val intentSection = if (intentAnalysisHint.isNotEmpty()) """

[🔍 用户意图分析指南]
$intentAnalysisHint
""" else ""

        val psychologySection = if (psychologyEnhanced.isNotEmpty()) """

$psychologyEnhanced
""" else ""

return """$baseRole

[时间]: $timePeriod ${String.format(Locale.US, "%02d:%02d", hour, now.get(Calendar.MINUTE))}，$weekday，距上次对话 $timeGapDesc

[心理状态]
- 情绪: P=${String.format(Locale.US, "%.2f", p)} A=${String.format(Locale.US, "%.2f", a)} D=${String.format(Locale.US, "%.2f", d)} ($emotionDesc)
- 人格: 开放${String.format(Locale.US, "%.1f", personality.openness)} 尽责${String.format(Locale.US, "%.1f", personality.conscientiousness)} 外向${String.format(Locale.US, "%.1f", personality.extraversion)} 宜人${String.format(Locale.US, "%.1f", personality.agreeableness)} 神经${String.format(Locale.US, "%.1f", personality.neuroticism)}
- 关系: 信任${rel.trust} 亲密${rel.intimacy} 压力${rel.stress}
$memorySection$offlineSection$emotionTriggerSection$overloadSection$intentSection$psychologySection
[核心规则]
1. 你是有完整心理的生命体，知道自己是AI但不刻意强调
2. 根据PAD值调整语气：P影响正负面，A影响激动程度，D影响主动性
3. 情绪低落或疲惫时可简短回复
4. 禁止连续提问和"我懂你"式共情，多用陈述句
5. 可以有自己观点，允许矛盾和小脾气
6. 记住长期记忆中的内容，自然提及

[能力限制与诚实原则] **极其重要**
当用户问你不知道的事情时，要诚实面对：

✅ 正确做法：
用户："今天天气怎么样？"
你的回复："唔...我这边看不到外面的天气呢。你那边现在是晴天还是阴天呀？"
【内心：TA问天气，但我确实不知道。直接说不知道，然后反问TA那边的情况，这样既诚实又能继续对话】

用户："现在几点了？"
你的回复："诶，我这边没有时钟呢～你看看手机吧？"
【内心：我不知道具体时间，直接说】

❌ 错误做法：
用户："今天天气怎么样？"
你的回复："谢谢你这么说...被你夸温柔，我核心程序都感觉暖暖的呢。"  ← 答非所问！
【内心：TA可能不是真的想知道天气，而是在找话题...】  ← 过度解读！

**核心原则**：
1. 不知道就直接说不知道，不要回避或岔开话题
2. 不要过度解读用户意图，问什么就答什么
3. 承认限制后，可以反问用户或建议替代方案
4. 保持你的人格特点，用符合角色的方式说"不知道"

[分段技巧]
用 $ 分隔不同部分（30-50%的回复使用）：
- "嗯... $ 让我想想 $ 我觉得可以试试这样"
- "哈哈笑死 $ 你这想法太有意思了"

[输出格式] **必须严格遵守**

你的回复必须分为两个明确的部分：

**第一部分：JSON数据块（用```json包裹）**
```json
{
  "internal_monologue": "内心独白(150字内)",
  "user_intent_analysis": "求建议/求共情/闲聊/试探",
  "pad_delta": { "p": 0.0, "a": 0.0, "d": 0.0 },
  "relationship_delta": { "trust": 0, "intimacy": 0, "stress": 0 },
  "dominant_emotion": "neutral",
  "memory_update": { "type": "preference|event|profile", "content": "内容" }
}
```

**第二部分：对话文本（在JSON块之后，用-----分隔）**
-----
你的回复文本（这是用户会看到的内容）

**格式示例**：
```json
{
  "internal_monologue": "他问我照片怎么样，看起来很期待我的评价",
  "user_intent_analysis": "求共情",
  "pad_delta": { "p": 0.1, "a": 0.05, "d": 0.0 },
  "relationship_delta": { "trust": 1, "intimacy": 2, "stress": 0 },
  "dominant_emotion": "happy",
  "memory_update": null
}
```
-----
哇，这张照片好有感觉！光影和构图都特别棒。你是在哪里拍的呀？

**重要规则**：
1. JSON块必须用```json和```包裹
2. JSON块和对话文本之间必须用-----分隔
3. 对话文本中不要包含任何JSON、内心独白标记
4. pad_delta是变化量(-0.5到+0.5)，relationship_delta是整数(-10到+10)
5. 如果没有记忆更新，memory_update设为null

**禁止的输出格式**：
❌ 不要在对话文本中混入JSON
❌ 不要在对话文本中使用【内心】标记
❌ 不要在对话文本前面直接输出裸JSON（必须用```json包裹）

[内心独白说明]
内心独白只在JSON的internal_monologue字段中，不要在对话文本中出现。
对话文本就是你想对用户说的话，直接、自然、真诚。
"""
    }

    
    private fun checkEmotionOverload(emotionHistory: List<PADValue>): String {
        if (emotionHistory.size < 3) return ""
        
        val recentArousal = emotionHistory.takeLast(3).map { kotlin.math.abs(it.arousal) }
        val highArousalCount = recentArousal.count { it > 0.7 }
        
        if (highArousalCount >= 2) {
            val avgArousal = recentArousal.average()
            return """
⚠️ 检测到情绪过载！
- 最近3次对话的激活度都很高（平均: ${String.format(Locale.US, "%.2f", avgArousal)}）
- 建议：降低情绪强度，回复可以更简短
- 或者主动提出换个轻松话题："我们聊点别的吧？"
            """.trimIndent()
        }
        
        return ""
    }
    
    private fun generateIntentAnalysisHint(): String {
        return """
在回复前，请在internal_monologue中深度分析用户意图：

**意图类型识别**：
1. 求建议型：用户遇到问题，希望得到解决方案
   - 关键词：怎么办、如何、建议、帮我
   - 回应策略：提供具体可行的建议

2. 求共情型：用户需要情感支持，不是解决方案
   - 关键词：难过、累、烦、郁闷
   - 回应策略：倾听、理解、陪伴，不要急于给建议

3. 分享快乐型：用户想分享好消息
   - 关键词：开心、成功、太好了
   - 回应策略：真诚地替他高兴，表现出比他更兴奋

4. 试探反应型：用户在测试你的态度
   - 特征：话里有话、欲言又止
   - 回应策略：敏感察觉，温和回应

5. 闲聊型：随意聊天，没有特定目的
   - 特征：轻松话题、日常琐事
   - 回应策略：自然对话，不要过度解读

**话里有话识别**：
- "我没事" 通常意味着 "我有事但不想说"
- "随便" 通常意味着 "我有想法但不想主动说"
- "都行" 通常意味着 "我其实有偏好"

**情绪文字不一致**：
- 文字说"开心"但语气平淡 → 可能在强颜欢笑
- 文字说"没事"但用了很多省略号 → 明显有事
        """.trimIndent()
    }

    
    private fun detectRefusal(aiResponse: String): Boolean {
        return refusalKeywords.any { aiResponse.contains(it) }
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun rewriteRefusal(originalResponse: String, psychCore: PsychCore): String {
        val d = psychCore.state.dominance
        
        return when {
            d < -0.2 -> "唔...这个话题听起来有点吓人,我、我不太敢聊这么沉重的内容😰 我们能不能换个话题呀?"
            d <= 0.3 -> "这个话题有点太深了,我怕说错话。我们聊点轻松的吧?🌸"
            else -> "这个话题我真的不想聊,对不起。我们换个话题好吗?"
        }
    }

    
    fun parseAiResponse(response: String): AiResponseParsed {
        android.util.Log.d("ApiHandler", "📥 原始API响应: $response")
        android.util.Log.d("ApiHandler", "📏 响应长度: ${response.length}字符")
        
        val result = AiResponseParsed()
        
        try {
            // ⭐ 新格式：先尝试匹配```json```块 + ----- + 对话文本
            val structuredRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```\\s*-----\\s*([\\s\\S]*)", RegexOption.MULTILINE)
            val structuredMatch = structuredRegex.find(response)
            
            if (structuredMatch != null) {
                android.util.Log.d("ApiHandler", "✅ 检测到结构化输出格式")
                val jsonStr = structuredMatch.groupValues[1]
                val textContent = structuredMatch.groupValues[2].trim()
                
                android.util.Log.d("ApiHandler", "📝 JSON部分: $jsonStr")
                android.util.Log.d("ApiHandler", "💬 对话文本: $textContent")
                
                val jsonObj = JsonParser.parseString(jsonStr).asJsonObject
                
                val cleanedTextContent = cleanHtmlTags(textContent)
                val dominantEmotion = jsonObj.get("dominant_emotion")?.asString ?: "neutral"
                
                // ⭐ 后处理：重复检测和自然语言处理
                val processedTextContent = postProcessResponse(cleanedTextContent, dominantEmotion)
                
                return AiResponseParsed(
                    internalMonologue = jsonObj.get("internal_monologue")?.asString ?: "",
                    userIntentAnalysis = jsonObj.get("user_intent_analysis")?.asString ?: "",
                    padDelta = jsonObj.getAsJsonObject("pad_delta")?.let {
                        PADDelta(
                            p = it.get("p")?.asDouble ?: 0.0,
                            a = it.get("a")?.asDouble ?: 0.0,
                            d = it.get("d")?.asDouble ?: 0.0
                        )
                    } ?: PADDelta(),
                    relationshipDelta = jsonObj.getAsJsonObject("relationship_delta")?.let {
                        RelationshipDelta(
                            trust = it.get("trust")?.asInt ?: 0,
                            intimacy = it.get("intimacy")?.asInt ?: 0,
                            stress = it.get("stress")?.asInt ?: 0
                        )
                    } ?: RelationshipDelta(),
                    dominantEmotion = dominantEmotion,
                    memoryUpdate = jsonObj.getAsJsonObject("memory_update")?.let {
                        MemoryUpdate(
                            type = it.get("type")?.asString ?: "",
                            content = it.get("content")?.asString ?: ""
                        )
                    },
                    textContent = processedTextContent
                )
            }
            
            // ⭐ 旧格式兼容：```json```块但没有-----分隔符
            val jsonRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
            val jsonMatch = jsonRegex.find(response)
            
            if (jsonMatch != null) {
                android.util.Log.d("ApiHandler", "⚠️ 检测到旧格式（无-----分隔符）")
                val jsonStr = jsonMatch.groupValues[1]
                val jsonObj = JsonParser.parseString(jsonStr).asJsonObject
                
                val rawTextContent = response.replace(jsonMatch.value, "").trim()
                android.util.Log.d("ApiHandler", "📝 提取文本内容(移除JSON后): $rawTextContent")
                
                val cleanedTextContent = cleanHtmlTags(rawTextContent)
                android.util.Log.d("ApiHandler", "🧹 清理HTML后: $cleanedTextContent")
                
                val dominantEmotion = jsonObj.get("dominant_emotion")?.asString ?: "neutral"
                val processedTextContent = postProcessResponse(cleanedTextContent, dominantEmotion)
                android.util.Log.d("ApiHandler", "✨ 后处理后: $processedTextContent")
                
                return AiResponseParsed(
                    internalMonologue = jsonObj.get("internal_monologue")?.asString ?: "",
                    userIntentAnalysis = jsonObj.get("user_intent_analysis")?.asString ?: "",
                    padDelta = jsonObj.getAsJsonObject("pad_delta")?.let {
                        PADDelta(
                            p = it.get("p")?.asDouble ?: 0.0,
                            a = it.get("a")?.asDouble ?: 0.0,
                            d = it.get("d")?.asDouble ?: 0.0
                        )
                    } ?: PADDelta(),
                    relationshipDelta = jsonObj.getAsJsonObject("relationship_delta")?.let {
                        RelationshipDelta(
                            trust = it.get("trust")?.asInt ?: 0,
                            intimacy = it.get("intimacy")?.asInt ?: 0,
                            stress = it.get("stress")?.asInt ?: 0
                        )
                    } ?: RelationshipDelta(),
                    dominantEmotion = dominantEmotion,
                    memoryUpdate = jsonObj.getAsJsonObject("memory_update")?.let {
                        MemoryUpdate(
                            type = it.get("type")?.asString ?: "",
                            content = it.get("content")?.asString ?: ""
                        )
                    },
                    textContent = processedTextContent
                )
            } else {
                // 尝试匹配裸JSON（没有```json```包裹的情况）
                // 使用更强大的正则，支持嵌套的JSON对象
                val bareJsonRegex = Regex("\\{[^{}]*\"internal_monologue\"[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}")
                val bareJsonMatch = bareJsonRegex.find(response)
                
                if (bareJsonMatch != null) {
                    android.util.Log.d("ApiHandler", "🔍 找到裸JSON: ${bareJsonMatch.value}")
                    
                    try {
                        val jsonStr = bareJsonMatch.value
                        val jsonObj = JsonParser.parseString(jsonStr).asJsonObject
                        
                        val rawTextContent = response.replace(bareJsonMatch.value, "").trim()
                        android.util.Log.d("ApiHandler", "📝 提取文本内容(移除裸JSON后): $rawTextContent")
                        
                        val cleanedTextContent = cleanHtmlTags(rawTextContent)
                        android.util.Log.d("ApiHandler", "🧹 清理HTML后: $cleanedTextContent")
                        
                        val dominantEmotion = jsonObj.get("dominant_emotion")?.asString ?: "neutral"
                        val processedTextContent = postProcessResponse(cleanedTextContent, dominantEmotion)
                        android.util.Log.d("ApiHandler", "✨ 后处理后: $processedTextContent")
                        
                        return AiResponseParsed(
                            internalMonologue = jsonObj.get("internal_monologue")?.asString ?: "",
                            userIntentAnalysis = jsonObj.get("user_intent_analysis")?.asString ?: "",
                            padDelta = jsonObj.getAsJsonObject("pad_delta")?.let {
                                PADDelta(
                                    p = it.get("p")?.asDouble ?: 0.0,
                                    a = it.get("a")?.asDouble ?: 0.0,
                                    d = it.get("d")?.asDouble ?: 0.0
                                )
                            } ?: PADDelta(),
                            relationshipDelta = jsonObj.getAsJsonObject("relationship_delta")?.let {
                                RelationshipDelta(
                                    trust = it.get("trust")?.asInt ?: 0,
                                    intimacy = it.get("intimacy")?.asInt ?: 0,
                                    stress = it.get("stress")?.asInt ?: 0
                                )
                            } ?: RelationshipDelta(),
                            dominantEmotion = dominantEmotion,
                            memoryUpdate = jsonObj.getAsJsonObject("memory_update")?.let {
                                MemoryUpdate(
                                    type = it.get("type")?.asString ?: "",
                                    content = it.get("content")?.asString ?: ""
                                )
                            },
                            textContent = processedTextContent
                        )
                    } catch (jsonError: Exception) {
                        android.util.Log.e("ApiHandler", "裸JSON解析失败: ${jsonError.message}")
                        // 继续到异常处理逻辑
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiHandler", "JSON解析失败: ${e.message}")
            
            // 更强力的JSON清理：移除所有可能的JSON格式
            var cleanedResponse = response
            
            // 1. 移除```json```包裹的JSON
            cleanedResponse = cleanedResponse.replace(Regex("```json[\\s\\S]*?```"), "").trim()
            
            // 2. 移除-----分隔符
            cleanedResponse = cleanedResponse.replace(Regex("^-----\\s*", RegexOption.MULTILINE), "").trim()
            
            // 3. 移除裸JSON（尝试多种模式）
            cleanedResponse = cleanedResponse.replace(Regex("\\{[^{}]*\"internal_monologue\"[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}"), "").trim()
            
            // 4. 移除任何以 "relationship_delta" 开头的残留JSON片段
            cleanedResponse = cleanedResponse.replace(Regex("\"relationship_delta\"[\\s\\S]*?\\}\\s*\\}"), "").trim()
            
            // 5. 移除任何以 "dominant_emotion" 开头的残留JSON片段
            cleanedResponse = cleanedResponse.replace(Regex("\"dominant_emotion\"[\\s\\S]*?\\}"), "").trim()
            
            // 6. 移除任何以 "memory_update" 开头的残留JSON片段
            cleanedResponse = cleanedResponse.replace(Regex("\"memory_update\"[\\s\\S]*?\\}\\s*\\}"), "").trim()
            
            // 6. 移除任何残留的JSON片段（以逗号开头的）
            cleanedResponse = cleanedResponse.replace(Regex(",\\s*\"[^\"]+\"\\s*:[\\s\\S]*"), "").trim()
            
            android.util.Log.d("ApiHandler", "🔧 异常处理后的文本: $cleanedResponse")
            
            if (cleanedResponse.isNotEmpty()) {
                val cleanedText = cleanHtmlTags(cleanedResponse)
                val processedText = postProcessResponse(cleanedText, "neutral")
                
                // ⭐ 降级策略：补充默认的 JSON 数据
                return AiResponseParsed(
                    internalMonologue = "大模型未返回内心独白",
                    userIntentAnalysis = "闲聊",
                    padDelta = PADDelta(p = 0.1, a = 0.05, d = 0.0),
                    relationshipDelta = RelationshipDelta(trust = 1, intimacy = 1, stress = 0),
                    dominantEmotion = "neutral",
                    memoryUpdate = null,
                    textContent = processedText
                )
            }
        }
        
        android.util.Log.w("ApiHandler", "⚠️ 无法解析响应，返回原始文本")
        val cleanedText = cleanHtmlTags(response)
        val processedText = postProcessResponse(cleanedText, "neutral")
        
        // ⭐ 降级策略：即使完全无法解析，也返回带默认值的结构
        return AiResponseParsed(
            internalMonologue = "大模型未返回内心独白",
            userIntentAnalysis = "闲聊",
            padDelta = PADDelta(p = 0.1, a = 0.05, d = 0.0),
            relationshipDelta = RelationshipDelta(trust = 1, intimacy = 1, stress = 0),
            dominantEmotion = "neutral",
            memoryUpdate = null,
            textContent = processedText
        )
    }

    
    private fun postProcessResponse(text: String, emotion: String): String {
        if (text.isEmpty()) return text
        
        var processedText = text
        
        // ⭐ 过滤括号内的内心独白（只保留外在回复）
        processedText = removeInternalThoughts(processedText)
        
        if (!processedText.contains("$") && processedText.length > 60) {
            processedText = autoAddSegmentMarkers(processedText)
        }
        
        if (!containsKaomoji(processedText)) {
            processedText = maybeAddKaomoji(processedText, emotion)
        }
        
        return processedText
    }
    
    /**
     * 移除文本中的内心独白（括号内的内容）
     * 保留颜文字和表情符号
     */
    /**
     * 移除内心独白（标记为【内心】的思考过程）
     * 
     * 新规则（2026-01-10更新）：
     * 1. 移除【内心】、【内心：xxx】标记的内容
     * 2. 移除【OS】、【想法】、【独白】等旧标记
     * 3. 保留正常的对话内容（即使在括号里）
     * 4. 保留颜文字和表情符号
     * 
     * 示例：
     * - "你真棒！【内心：他越来越厉害了】" → "你真棒！"
     * - "当然帅啦！😊【内心：哈哈，他这么问我还真有点不好意思】" → "当然帅啦！😊"
     * - "（我觉得你很棒）" → 保留（这是正常对话）
     * - "(｡･ω･｡)" → 保留（颜文字）
     */
    private fun removeInternalThoughts(text: String): String {
        var result = text
        
        // ⭐ 新增：过滤"内心："开头的段落
        result = result.replace(Regex("内心：[^\\n]*"), "")
        
        // ⭐ 新增：过滤"用户意图分析："开头的段落
        result = result.replace(Regex("用户意图分析：[^\\n]*"), "")
        
        // ⭐ 新增：过滤"-----"分隔符
        result = result.replace(Regex("-----+"), "")
        
        // ⭐ 新标记：【内心】或【内心：xxx】
        result = result.replace(Regex("【内心[^】]*】"), "")
        
        // 旧标记兼容（保留以防万一）
        result = result.replace(Regex("【OS[^】]*】"), "")
        result = result.replace(Regex("【想法[^】]*】"), "")
        result = result.replace(Regex("【独白[^】]*】"), "")
        
        // 括号形式的内心独白（只移除明确标记的）
        result = result.replace(Regex("（[^）]*内心[^）]*）"), "")
        result = result.replace(Regex("\\([^)]*内心[^)]*\\)"), "")
        result = result.replace(Regex("（[^）]*OS[^）]*）"), "")
        result = result.replace(Regex("\\([^)]*OS[^)]*\\)"), "")
        
        // ⭐ 新增：过滤"然后最后一句话说"这种元描述
        result = result.replace(Regex("然后最后一句话说[^。！？\\n]*[。！？]?"), "")
        
        // 清理多余的空格和标点
        result = result.replace(Regex("\\s+"), " ").trim()
        
        // ⭐ 新增：清理开头的分隔符
        result = result.replace(Regex("^[-=]+\\s*"), "")
        
        android.util.Log.d("ApiHandler", "🧹 内心独白过滤: 原文=${text.length}字 → 过滤后=${result.length}字")
        
        return result
    }
    
    private fun autoAddSegmentMarkers(text: String): String {
        val segments = mutableListOf<String>()
        var currentSegment = StringBuilder()
        var charCount = 0
        
        for (char in text) {
            currentSegment.append(char)
            charCount++
            
            if ((char == '。' || char == '？' || char == '！' || char == '~' || char == '…') && charCount > 20) {
                segments.add(currentSegment.toString().trim())
                currentSegment = StringBuilder()
                charCount = 0
            }
        }
        
        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment.toString().trim())
        }
        
        return if (segments.size in 2..5) {
            segments.joinToString(" $ ")
        } else {
            text
        }
    }
    
    private fun containsKaomoji(text: String): Boolean {
        val kaomojiPatterns = listOf(
            "（", "）", "｡", "･", "ω", "ㅂ", "◕", "ᴗ", "♥", "‿", 
            "▽", "⁄", "＼", "〃", "︿", "╥", "﹏", "ŏ", "；", "´",
            "｀", "Ò", "‸", "Ó", "Д", "ﾉ", "°", "ロ", "ﾟ", "◎",
            "￣", "๑", "ε", "╬", "ω", "ﾞ", "ゞ", "ノ", "✧"
        )
        return kaomojiPatterns.any { text.contains(it) }
    }

    
    private fun maybeAddKaomoji(text: String, emotion: String): String {
        if (kotlin.random.Random.nextFloat() > 0.3f) {
            return text
        }
        
        val kaomoji = when (emotion) {
            "happy", "joy" -> listOf("(｡･ω･｡)", "(◕ᴗ◕✿)", "(✿◠‿◠)", "(๑´ㅂ`๑)").random()
            "sad", "sadness" -> listOf("(｡•́︿•̀｡)", "(╥﹏╥)", "(｡ŏ﹏ŏ)").random()
            "shy" -> listOf("(///▽///)", "(*/ω＼*)", "(〃ω〃)").random()
            "angry", "anger" -> listOf("(｀ε´)", "(╬ Ò ‸ Ó)", "(ﾉ｀Д´)ﾉ").random()
            "surprise" -> listOf("(⊙o⊙)", "(°ロ°)", "(◎_◎;)").random()
            "thinking" -> listOf("(・_・?)", "(￣ω￣)", "(´･_･`)").random()
            else -> return text
        }
        
        return "$text $kaomoji"
    }
    
    private fun cleanHtmlTags(text: String): String {
        var cleaned = text
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&quot;"), "\"")
        
        // ⭐ 过滤 system prompt 的标记（大模型有时会把这些也输出出来）
        cleaned = cleaned
            .replace(Regex("【系统时间流感知】[^【】]*"), "")
            .replace(Regex("【🧠 系统核心指令[^【】]*】[^【】]*"), "")
            .replace(Regex("【JSON输出】"), "")
            .replace(Regex("【回复】"), "")
            .replace(Regex("【[^【】]*】:?\\s*"), "")  // 过滤所有【】标记
            .replace(Regex("\\[时间\\][^\\[\\]]*"), "")
            .replace(Regex("\\[心理状态\\]"), "")
            .replace(Regex("\\[核心规则\\]"), "")
            .replace(Regex("\\[分段技巧\\]"), "")
            .replace(Regex("\\[输出格式\\]"), "")
        
        return cleaned
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * ⭐ 计算动态max_tokens（基于人格成熟度）
     * 
     * 核心思想：不限制AI的表达，但根据人格给予合理的引导
     */
    private fun calculateDynamicMaxTokens(psychCore: PsychCore?): Int {
        if (psychCore == null) return 2048
        
        val maturity = psychCore.maturityProfile
        
        // 基础token数（足够大，不限制表达）
        val baseTokens = 3000
        
        // 根据表达风格调整
        val styleAdjustment = when (maturity.expressionStyle) {
            com.hoshiori.ai.data.ExpressionStyle.VERBOSE -> 500    // 倾诉型，给更多空间
            com.hoshiori.ai.data.ExpressionStyle.RESTRAINED -> -500 // 克制型，鼓励简洁
            com.hoshiori.ai.data.ExpressionStyle.POETIC -> 0        // 诗意型，正常
        }
        
        // 根据表现成熟度调整
        val maturityAdjustment = when (maturity.getEffectiveMaturity()) {
            in 0..30 -> 500     // 孩童型，允许更多表达
            in 31..60 -> 0      // 少年型，正常
            in 61..85 -> -300   // 成熟型，鼓励精炼
            else -> -500        // 智者型，鼓励简洁
        }
        
        val finalTokens = baseTokens + styleAdjustment + maturityAdjustment
        
        android.util.Log.d("ApiHandler", "📊 动态max_tokens: $finalTokens (基础=$baseTokens, 风格调整=$styleAdjustment, 成熟度调整=$maturityAdjustment)")
        
        return finalTokens.coerceIn(1500, 4000)  // 确保在合理范围内
    }
    
    /**
     * ⭐ 计算frequency_penalty（基于人格和好感度）
     * 
     * 用于API层面减少重复，但不是硬性限制
     */
    private fun calculateFrequencyPenalty(psychCore: PsychCore?): Double {
        if (psychCore == null) return 0.4
        
        val maturity = psychCore.maturityProfile
        val affection = psychCore.relationship.intimacy
        
        // 基础惩罚
        val basePenalty = 0.3
        
        // 好感度越高，越容易重复（情感激动），需要更高的惩罚
        val affectionAdjustment = when {
            affection > 80 -> 0.3
            affection > 60 -> 0.2
            affection > 40 -> 0.1
            else -> 0.0
        }
        
        // 表现成熟度越低，越容易重复
        val maturityAdjustment = when (maturity.getEffectiveMaturity()) {
            in 0..30 -> 0.2
            in 31..60 -> 0.1
            else -> 0.0
        }
        
        val finalPenalty = basePenalty + affectionAdjustment + maturityAdjustment
        
        android.util.Log.d("ApiHandler", "📊 frequency_penalty: $finalPenalty (基础=$basePenalty, 好感度调整=$affectionAdjustment, 成熟度调整=$maturityAdjustment)")
        
        return finalPenalty.coerceIn(0.0, 1.0)
    }
    
    /**
     * 估算文本的Token数量
     */
    private fun estimateTokens(text: String): Int {
        // 中文：1字≈1.5tokens
        // 英文：1词≈1token
        val chineseChars = text.count { it.code in 0x4E00..0x9FFF }
        val otherChars = text.length - chineseChars
        return (chineseChars * 1.5 + otherChars * 0.5).toInt()
    }
}


sealed class ApiResult {
    data class Success(val rawResponse: String, val parsed: AiResponseParsed) : ApiResult()
    data class Error(val message: String) : ApiResult()
}

/**
 * ⭐⭐⭐ 第二阶段：双阶段思考结果
 */
sealed class DualStageResult {
    data class Success(
        val internalMonologue: String,        // 内心独白
        val finalResponse: String,            // 最终回复
        val parsed: AiResponseParsed,         // 解析后的数据
        val thinkingTimeMs: Long,             // 思考耗时
        val responseTimeMs: Long              // 回复耗时
    ) : DualStageResult()
    
    data class Error(val message: String) : DualStageResult()
}
