package com.hoshiori.ai.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.R
import com.hoshiori.ai.api.ApiHandler
import com.hoshiori.ai.api.ApiResult
import com.hoshiori.ai.brain.AiInteractionTrigger
import com.hoshiori.ai.brain.GroupChatManager
import com.hoshiori.ai.brain.GroupChatMemoryExtractor
import com.hoshiori.ai.data.*
import com.hoshiori.ai.databinding.ActivityGroupChatBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 群聊界面
 */
class GroupChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var adapter: GroupMessageAdapter
    private val dataManager by lazy { HoshioriApp.instance.dataManager }
    private val groupChatManager by lazy { GroupChatManager() }
    private val aiInteractionTrigger by lazy { AiInteractionTrigger() }  // ⭐ AI互动触发器
    private val memoryExtractor by lazy { GroupChatMemoryExtractor() }
    
    private var groupId: String = ""
    private var groupChat: GroupChat? = null
    private val characters = mutableListOf<Character>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        groupId = intent.getStringExtra("group_id") ?: ""
        if (groupId.isEmpty()) {
            Toast.makeText(this, "群聊不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        loadGroupData()
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }
    
    private fun loadGroupData() {
        groupChat = dataManager.getGroupChat(groupId)
        if (groupChat == null) {
            Toast.makeText(this, "群聊不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 加载所有成员角色
        groupChat?.memberIds?.forEach { memberId ->
            dataManager.getCharacter(memberId)?.let { character ->
                characters.add(character)
            }
        }
    }
    
    private fun setupUI() {
        groupChat?.let { group ->
            binding.tvGroupName.text = group.name
            binding.tvMemberCount.text = getString(R.string.member_count, group.getMemberCount() + 1)  // +1 包含用户
            
            // 加载群头像（如果有）
            group.avatar?.let { _ ->
                // TODO: 加载群头像
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = GroupMessageAdapter(characters)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
        
        // 设置长按菜单回调
        adapter.onMessageLongClick = { message ->
            showMessageOptionsDialog(message)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnSend.setOnClickListener { sendMessage() }
        
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
        
        binding.btnMore.setOnClickListener { view ->
            showGroupMenu(view)
        }
        
        binding.btnSticker.setOnClickListener {
            // TODO: 表情包选择器
            Toast.makeText(this, "表情包功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadMessages() {
        val messages = dataManager.getGroupMessages(groupId)
        adapter.submitList(messages)
        
        // 滚动到底部
        if (messages.isNotEmpty()) {
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }
    
    /**
     * 发送消息
     */
    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty()) return
        
        // 清空输入框
        binding.etMessage.text?.clear()
        
        // 创建用户消息
        val userMessage = GroupChatMessage(
            groupId = groupId,
            senderId = "user",
            senderName = "我",
            type = GroupMessageType.USER,
            content = content
        )
        
        // 保存并显示用户消息
        dataManager.saveGroupMessage(userMessage)
        loadMessages()
        
        // 触发AI回复
        lifecycleScope.launch {
            handleAiResponses(content)
        }
    }
    
    /**
     * 处理AI回复
     * 智能选择1-2个AI角色回复
     */
    private suspend fun handleAiResponses(userMessage: String) {
        try {
            // 获取最近的消息历史（用于上下文分析）
            val recentMessages = dataManager.getGroupMessages(groupId)
                .takeLast(10)
                .map { Pair(it.senderId, it.content) }
            
            // 智能选择发言者
            val speakers = groupChatManager.selectSpeakers(
                userMessage = userMessage,
                characters = characters,
                recentMessages = recentMessages
            )
            
            android.util.Log.d("GroupChat", "Selected ${speakers.size} speakers: ${speakers.joinToString { it.name }}")
            
            // 让选中的AI角色依次回复
            speakers.forEachIndexed { index, character ->
                // 添加延迟，模拟打字时间
                if (index > 0) {
                    delay(2000)  // 第二个角色延迟2秒回复
                }
                
                generateAiResponse(character, userMessage)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("GroupChat", "Error handling AI responses", e)
            Toast.makeText(this, "AI回复失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 生成单个AI角色的回复
     */
    private suspend fun generateAiResponse(character: Character, userMessage: String) {
        try {
            // 显示打字指示器
            showTypingIndicator(character.name)
            
            // 构建群聊上下文
            val groupPrompt = groupChatManager.generateGroupChatPrompt(
                groupName = groupChat?.name ?: "群聊",
                allMembers = characters,
                currentCharacter = character
            )
            
            // 使用智能记忆提取器获取角色记忆
            val characterChatHistory = dataManager.getMessages(character.id, limit = 20)
            val characterMemories = memoryExtractor.extractSmartMemory(
                longTermMemory = character.memory,
                recentMessages = characterChatHistory,
                characterName = character.name,
                maxTokens = 800  // 约400-500字的记忆
            )
            
            // 获取最近的群聊对话历史
            val recentMessages = dataManager.getGroupMessages(groupId)
                .takeLast(10)
                .joinToString("\n") { "${it.senderName}: ${it.content}" }
            
            // 构建完整的Prompt（包含智能记忆）
            val fullPrompt = """
$groupPrompt

【你与用户的私聊记忆】
$characterMemories

【最近的群聊对话】
$recentMessages

【用户刚刚说】
$userMessage

【你的回复】
请以 ${character.name} 的身份，根据群聊氛围、你的性格和你与用户的私聊记忆，给出简洁自然的回复（1-2句话即可）。

💡 记忆使用提示：
- 如果用户提到了你们私聊中的内容（如姓名、喜好、约定等），可以自然地回应
- 如果记忆中有相关的情感事件或重要回忆，可以适当提及
- 不要生硬地罗列记忆，要自然融入对话
- 如果记忆与当前话题无关，就正常回应即可
""".trimIndent()
            
            // 调用API
            val settings = dataManager.getSettings()
            val apiHandler = ApiHandler(settings.apiConfig)
            
            // 构建消息列表
            val messages = listOf(
                mapOf("role" to "user", "content" to userMessage)
            )
            
            val result = apiHandler.callApi(
                messages = messages,
                systemPrompt = fullPrompt,
                psychCore = character.psychCore
            )
            
            // 隐藏打字指示器
            hideTypingIndicator()
            
            when (result) {
                is ApiResult.Success -> {
                    // ⭐ 过滤JSON和内心独白
                    var cleanedContent = result.parsed.textContent
                    cleanedContent = filterInternalMonologue(cleanedContent)
                    
                    android.util.Log.d("GroupChat", "🤖 ${character.name}回复: $cleanedContent")
                    
                    // 如果过滤后为空，使用默认回复
                    if (cleanedContent.isEmpty()) {
                        cleanedContent = "嗯... (｡･ω･｡)"
                    }
                    
                    // 创建AI消息
                    val aiMessage = GroupChatMessage(
                        groupId = groupId,
                        senderId = character.id,
                        senderName = character.name,
                        senderAvatar = character.avatar,
                        type = GroupMessageType.AI,
                        content = cleanedContent
                    )
                    
                    // 保存并显示AI消息
                    dataManager.saveGroupMessage(aiMessage)
                    loadMessages()
                    
                    // ⭐ 检查是否触发AI之间的互动
                    checkAndTriggerAiInteraction(aiMessage)
                }
                is ApiResult.Error -> {
                    android.util.Log.e("GroupChat", "AI response error: ${result.message}")
                }
            }
            
        } catch (e: Exception) {
            hideTypingIndicator()
            android.util.Log.e("GroupChat", "Error generating AI response", e)
        }
    }
    
    /**
     * 显示打字指示器
     */
    private fun showTypingIndicator(characterName: String) {
        binding.tvTypingIndicator.text = "$characterName 正在输入..."
        binding.tvTypingIndicator.visibility = View.VISIBLE
        android.util.Log.d("GroupChat", "$characterName 正在输入...")
    }
    
    /**
     * 隐藏打字指示器
     */
    private fun hideTypingIndicator() {
        binding.tvTypingIndicator.visibility = View.GONE
    }
    
    /**
     * ⭐ 检查并触发AI之间的互动
     */
    private fun checkAndTriggerAiInteraction(lastMessage: GroupChatMessage) {
        lifecycleScope.launch {
            try {
                // 获取最近的消息历史
                val recentMessages = dataManager.getGroupMessages(groupId).takeLast(10)
                
                // 检查是否应该触发AI互动
                val triggeredCharacters = aiInteractionTrigger.shouldTriggerInteraction(
                    lastMessage = lastMessage,
                    allCharacters = characters,
                    recentMessages = recentMessages
                )
                
                if (triggeredCharacters.isNotEmpty()) {
                    android.util.Log.d("GroupChat", "🔥 触发AI互动: ${triggeredCharacters.joinToString { it.name }}")
                    
                    // 延迟一下，让用户看到上一条消息
                    delay(2000)
                    
                    // 让触发的AI回复
                    for (character in triggeredCharacters) {
                        generateAiInteractionResponse(character, lastMessage, recentMessages)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GroupChat", "Error checking AI interaction", e)
            }
        }
    }
    
    /**
     * ⭐ 生成AI互动回复（简化版，减少token消耗）
     */
    private suspend fun generateAiInteractionResponse(
        character: Character,
        triggeringMessage: GroupChatMessage,
        recentMessages: List<GroupChatMessage>
    ) {
        try {
            // 显示打字指示器
            showTypingIndicator(character.name)
            
            // 使用简化的Prompt（减少token消耗）
            val simplifiedPrompt = aiInteractionTrigger.generateAiInteractionPrompt(
                currentCharacter = character,
                triggeringMessage = triggeringMessage,
                allMembers = characters,
                recentMessages = recentMessages
            )
            
            // 调用API（使用简化的上下文）
            val settings = dataManager.getSettings()
            val apiHandler = ApiHandler(settings.apiConfig)
            
            // 只传递触发消息，不传递完整历史
            val messages = listOf(
                mapOf("role" to "user", "content" to triggeringMessage.content)
            )
            
            val result = apiHandler.callApi(
                messages = messages,
                systemPrompt = simplifiedPrompt,
                psychCore = character.psychCore
            )
            
            // 隐藏打字指示器
            hideTypingIndicator()
            
            when (result) {
                is ApiResult.Success -> {
                    var cleanedContent = result.parsed.textContent
                    cleanedContent = filterInternalMonologue(cleanedContent)
                    
                    android.util.Log.d("GroupChat", "💬 ${character.name}互动回复: $cleanedContent")
                    
                    if (cleanedContent.isEmpty()) {
                        cleanedContent = "嗯嗯~"
                    }
                    
                    // 创建AI互动消息
                    val aiMessage = GroupChatMessage(
                        groupId = groupId,
                        senderId = character.id,
                        senderName = character.name,
                        senderAvatar = character.avatar,
                        type = GroupMessageType.AI,
                        content = cleanedContent
                    )
                    
                    // 保存并显示
                    dataManager.saveGroupMessage(aiMessage)
                    loadMessages()
                    
                    // ⭐ 注意：这里不再递归触发，避免无限循环
                    // AI互动回复不会再触发其他AI
                }
                is ApiResult.Error -> {
                    android.util.Log.e("GroupChat", "AI interaction error: ${result.message}")
                }
            }
            
        } catch (e: Exception) {
            hideTypingIndicator()
            android.util.Log.e("GroupChat", "Error generating AI interaction response", e)
        }
    }
    
    /**
     * 显示群聊菜单
     */
    private fun showGroupMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        popup.menu.add(0, 1, 0, getString(R.string.manage_members))
        popup.menu.add(0, 2, 0, getString(R.string.clear_chat_history))
        popup.menu.add(0, 3, 0, getString(R.string.delete_group))
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showManageMembersDialog()
                    true
                }
                2 -> {
                    showClearChatDialog()
                    true
                }
                3 -> {
                    showDeleteGroupDialog()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    /**
     * 显示管理成员对话框
     */
    private fun showManageMembersDialog() {
        // TODO: 实现成员管理对话框
        Toast.makeText(this, "成员管理功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示消息选项对话框（复制、删除）
     */
    private fun showMessageOptionsDialog(message: GroupChatMessage) {
        val options = arrayOf(
            getString(R.string.copy_message),
            getString(R.string.delete_message)
        )
        
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyMessage(message)
                    1 -> showDeleteMessageDialog(message)
                }
            }
            .show()
    }
    
    /**
     * 复制消息到剪贴板
     */
    private fun copyMessage(message: GroupChatMessage) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示删除消息对话框
     */
    private fun showDeleteMessageDialog(message: GroupChatMessage) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_message))
            .setMessage(getString(R.string.delete_message_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 删除消息
     */
    private fun deleteMessage(message: GroupChatMessage) {
        dataManager.deleteGroupMessage(groupId, message.id)
        loadMessages()
        Toast.makeText(this, getString(R.string.message_deleted), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示清空聊天记录对话框
     */
    private fun showClearChatDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_chat_history))
            .setMessage(getString(R.string.clear_chat_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                clearChatHistory()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 清空聊天记录
     */
    private fun clearChatHistory() {
        dataManager.clearGroupMessages(groupId)
        loadMessages()
        Toast.makeText(this, getString(R.string.chat_cleared), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示解散群聊对话框
     */
    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_group))
            .setMessage(getString(R.string.delete_group_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteGroup()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 解散群聊
     */
    private fun deleteGroup() {
        dataManager.deleteGroupChat(groupId)
        Toast.makeText(this, getString(R.string.group_deleted), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * 过滤内心独白和JSON泄露
     * 
     * 与ChatActivity中的过滤逻辑保持一致
     */
    /**
     * ⭐ 智能过滤逻辑 - 从源头解决问题
     * 
     * 新策略：通过改进Prompt让AI输出结构化内容（JSON块 + ----- + 对话文本）
     * 这样就不需要复杂的过滤规则了
     * 
     * 这个方法现在只作为最后的保险措施，处理AI偶尔不遵守格式的情况
     */
    private fun filterInternalMonologue(text: String): String {
        android.util.Log.d("GroupChat", "🔍 过滤前: $text")
        var filtered = text
        
        // ⭐ 第一优先级：移除结构化格式的残留
        // 1. 移除```json```包裹的JSON块（如果有残留）
        filtered = filtered.replace(Regex("```json[\\s\\S]*?```"), "").trim()
        
        // 2. 移除-----分隔符（如果有残留）
        filtered = filtered.replace(Regex("^-----\\s*", RegexOption.MULTILINE), "").trim()
        
        // 3. 移除裸JSON（如果AI没有用```json```包裹）
        filtered = filtered.replace(Regex("\\{[^{}]*\"internal_monologue\"[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}"), "").trim()
        
        // ⭐ 早期检查：如果过滤后太短，可能是过度过滤，返回原文本
        if (filtered.length < 3 && text.length > 10) {
            android.util.Log.d("GroupChat", "⚠️ 过滤后内容太短，返回原文本")
            return text
        }
        
        // ⭐ 第二优先级：清理可能的JSON残留片段
        filtered = filtered.replace(Regex("\"relationship_delta\"[\\s\\S]*?\\}\\s*\\}"), "").trim()
        filtered = filtered.replace(Regex("\"dominant_emotion\"[\\s\\S]*?\\}"), "").trim()
        filtered = filtered.replace(Regex("\"memory_update\"[\\s\\S]*?\\}\\s*\\}"), "").trim()
        filtered = filtered.replace(Regex(",\\s*\"[^\"]+\"\\s*:[\\s\\S]*"), "").trim()
        
        // ⭐ 第三优先级：清理明确的元数据标记
        filtered = filtered.replace(Regex("内心：[^\\n]*"), "")
        filtered = filtered.replace(Regex("用户意图分析：[^\\n]*"), "")
        filtered = filtered.replace(Regex("【内心[^】]*】"), "")
        
        // ⭐ 第四优先级：清理格式标记
        filtered = filtered.replace(Regex("\\s+"), " ").trim()
        filtered = filtered.replace(Regex("^[-=]+\\s*"), "")
        
        // ⭐ 最终检查：如果过滤后为空或异常短，返回原文本
        if (filtered.isEmpty() || (filtered.length < 5 && text.length > 20)) {
            android.util.Log.d("GroupChat", "⚠️ 过滤结果异常，返回原文本")
            return text
        }
        
        android.util.Log.d("GroupChat", "🔍 过滤后: $filtered")
        return filtered
    }
}

/**
 * 群聊消息适配器
 */
class GroupMessageAdapter(
    private val characters: List<Character>
) : androidx.recyclerview.widget.RecyclerView.Adapter<GroupMessageAdapter.ViewHolder>() {
    
    private val messages = mutableListOf<GroupChatMessage>()
    var onMessageLongClick: ((GroupChatMessage) -> Unit)? = null
    
    fun submitList(newMessages: List<GroupChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(com.hoshiori.ai.R.layout.item_group_message, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val layoutAiMessage: android.view.ViewGroup = itemView.findViewById(com.hoshiori.ai.R.id.layoutAiMessage)
        private val layoutUserMessage: android.view.ViewGroup = itemView.findViewById(com.hoshiori.ai.R.id.layoutUserMessage)
        private val tvSystemMessage: android.widget.TextView = itemView.findViewById(com.hoshiori.ai.R.id.tvSystemMessage)
        
        private val ivAiAvatar: android.widget.ImageView = itemView.findViewById(com.hoshiori.ai.R.id.ivAiAvatar)
        private val tvAiName: android.widget.TextView = itemView.findViewById(com.hoshiori.ai.R.id.tvAiName)
        private val tvAiMessage: android.widget.TextView = itemView.findViewById(com.hoshiori.ai.R.id.tvAiMessage)
        private val tvUserMessage: android.widget.TextView = itemView.findViewById(com.hoshiori.ai.R.id.tvUserMessage)
        
        fun bind(message: GroupChatMessage) {
            // 隐藏所有布局
            layoutAiMessage.visibility = View.GONE
            layoutUserMessage.visibility = View.GONE
            tvSystemMessage.visibility = View.GONE
            
            when (message.type) {
                GroupMessageType.USER -> {
                    layoutUserMessage.visibility = View.VISIBLE
                    tvUserMessage.text = message.content
                    
                    // 长按删除
                    itemView.setOnLongClickListener {
                        onMessageLongClick?.invoke(message)
                        true
                    }
                }
                GroupMessageType.AI -> {
                    layoutAiMessage.visibility = View.VISIBLE
                    tvAiName.text = message.senderName
                    tvAiMessage.text = message.content
                    
                    // 加载头像
                    message.senderAvatar?.let { avatarPath ->
                        val bitmap = when {
                            avatarPath == "default_hoshiori" -> {
                                // 从assets加载星织头像
                                try {
                                    itemView.context.assets.open("avatars/xingzhi.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            avatarPath == "default_qiulan" -> {
                                // 从assets加载秋岚头像
                                try {
                                    itemView.context.assets.open("avatars/qiulan.jpg").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            avatarPath == "default_zhaoLusi" -> {
                                // 从assets加载赵露思头像
                                try {
                                    itemView.context.assets.open("avatars/zhaolusi.jpg").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            avatarPath == "default_huangJingyu" -> {
                                // 从assets加载黄景瑜头像
                                try {
                                    itemView.context.assets.open("avatars/huangjingyu.jpg").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            avatarPath == "default_huangXiaoyun" -> {
                                // 从assets加载黄霄云头像
                                try {
                                    itemView.context.assets.open("avatars/huangxiaoyun.jpg").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            avatarPath.startsWith("default_") -> {
                                // 其他默认头像
                                null
                            }
                            else -> {
                                // 自定义头像
                                com.hoshiori.ai.utils.ImageUtils.loadAvatarBitmap(avatarPath)
                            }
                        }
                        
                        if (bitmap != null) {
                            ivAiAvatar.setImageBitmap(bitmap)
                        } else {
                            ivAiAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
                        }
                    } ?: run {
                        ivAiAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
                    }
                    
                    // 长按删除
                    itemView.setOnLongClickListener {
                        onMessageLongClick?.invoke(message)
                        true
                    }
                }
                GroupMessageType.SYSTEM -> {
                    tvSystemMessage.visibility = View.VISIBLE
                    tvSystemMessage.text = message.content
                }
                else -> {}
            }
        }
    }
}
