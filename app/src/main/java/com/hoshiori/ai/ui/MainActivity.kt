package com.hoshiori.ai.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.R
import com.hoshiori.ai.data.Character
import com.hoshiori.ai.data.GroupChat
import com.hoshiori.ai.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    internal lateinit var adapter: CharacterAdapter
    private lateinit var groupAdapter: GroupChatAdapter
    
    private var currentTab = 0  // 0=角色，1=群聊
    
    internal val dataManager by lazy { HoshioriApp.instance.dataManager }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== MainActivity onCreate started ===")
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            Log.d(TAG, "Binding inflated successfully")
            
            setContentView(binding.root)
            Log.d(TAG, "Content view set successfully")
            
            setupRecyclerView()
            Log.d(TAG, "RecyclerView setup completed")
            
            setupGroupRecyclerView()
            Log.d(TAG, "Group RecyclerView setup completed")
            
            setupTabLayout()
            Log.d(TAG, "TabLayout setup completed")
            
            setupClickListeners()
            Log.d(TAG, "Click listeners setup completed")
            
            // ⭐ 显示新手引导（仅首次）
            showWelcomeGuideIfNeeded()
            
            Log.d(TAG, "=== MainActivity onCreate completed successfully ===")
        } catch (e: Exception) {
            Log.e(TAG, "!!! FATAL ERROR in MainActivity onCreate !!!", e)
            e.printStackTrace()
            // 显示错误信息
            android.widget.Toast.makeText(this, "启动失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        try {
            if (currentTab == 0) {
                loadCharacters()
            } else {
                loadGroups()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
    
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView...")
        adapter = CharacterAdapter(
            onItemClick = { character ->
                Log.d(TAG, "Character clicked: ${character.name}")
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("character_id", character.id)
                })
            },
            onEditClick = { character ->
                Log.d(TAG, "Edit clicked: ${character.name}")
                startActivity(Intent(this, CharacterEditActivity::class.java).apply {
                    putExtra("character_id", character.id)
                })
            },
            onDeleteClick = { character ->
                Log.d(TAG, "Delete clicked: ${character.name}")
                showDeleteConfirmDialog(character)
            }
        )
        
        binding.rvCharacters.layoutManager = LinearLayoutManager(this)
        binding.rvCharacters.adapter = adapter
        Log.d(TAG, "RecyclerView setup completed")
    }
    
    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners...")
        binding.btnSettings.setOnClickListener {
            Log.d(TAG, "Settings button clicked")
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.fabAdd.setOnClickListener {
            Log.d(TAG, "Add button clicked")
            startActivity(Intent(this, CharacterEditActivity::class.java))
        }
        
        // 创建群聊按钮
        binding.fabCreateGroup.setOnClickListener {
            Log.d(TAG, "Create group button clicked")
            showCreateGroupDialog()
        }
        
        // 添加按钮动画
        setupButtonAnimation(binding.btnSettings)
        setupButtonAnimation(binding.fabAdd)
        setupButtonAnimation(binding.fabCreateGroup)
        
        Log.d(TAG, "Click listeners setup completed")
    }
    
    /**
     * 为按钮添加按压动画
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupButtonAnimation(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val anim = AnimationUtils.loadAnimation(this, com.hoshiori.ai.R.anim.button_press)
                    v.startAnimation(anim)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val anim = AnimationUtils.loadAnimation(this, com.hoshiori.ai.R.anim.button_release)
                    v.startAnimation(anim)
                }
            }
            false // 返回false让点击事件继续传递
        }
    }
    
    internal fun loadCharacters() {
        Log.d(TAG, "Loading characters...")
        try {
            val characters = dataManager.getAllCharacters()
                .sortedByDescending { it.lastMessageAt }
            
            Log.d(TAG, "Loaded ${characters.size} characters")
            
            // ⭐ 创建新列表实例，确保DiffUtil检测到变化
            adapter.submitList(characters.toList())
            
            binding.emptyState.visibility = if (characters.isEmpty()) View.VISIBLE else View.GONE
            Log.d(TAG, "Characters loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading characters", e)
            binding.emptyState.visibility = View.VISIBLE
        }
    }
    
    private fun setupGroupRecyclerView() {
        Log.d(TAG, "Setting up Group RecyclerView...")
        groupAdapter = GroupChatAdapter(
            onItemClick = { groupChat ->
                Log.d(TAG, "Group clicked: ${groupChat.name}")
                startActivity(Intent(this, GroupChatActivity::class.java).apply {
                    putExtra("group_id", groupChat.id)
                })
            },
            onDeleteClick = { groupChat ->
                Log.d(TAG, "Delete group clicked: ${groupChat.name}")
                showDeleteGroupDialog(groupChat)
            }
        )
        
        binding.rvGroups.layoutManager = LinearLayoutManager(this)
        binding.rvGroups.adapter = groupAdapter
        Log.d(TAG, "Group RecyclerView setup completed")
    }
    
    private fun setupTabLayout() {
        Log.d(TAG, "Setting up TabLayout...")
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // 显示角色列表
                        currentTab = 0
                        binding.rvCharacters.visibility = View.VISIBLE
                        binding.rvGroups.visibility = View.GONE
                        binding.emptyStateGroup.visibility = View.GONE
                        binding.fabAdd.show()
                        binding.fabCreateGroup.hide()
                        loadCharacters()
                    }
                    1 -> {
                        // 显示群聊列表
                        currentTab = 1
                        binding.rvCharacters.visibility = View.GONE
                        binding.rvGroups.visibility = View.VISIBLE
                        binding.fabAdd.hide()
                        binding.fabCreateGroup.show()
                        loadGroups()
                    }
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        Log.d(TAG, "TabLayout setup completed")
    }
    
    private fun loadGroups() {
        Log.d(TAG, "Loading groups...")
        try {
            val groups = dataManager.getAllGroupChats()
                .sortedByDescending { it.lastMessageAt }
            
            Log.d(TAG, "Loaded ${groups.size} groups")
            groupAdapter.submitList(groups)
            
            binding.emptyStateGroup.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
            Log.d(TAG, "Groups loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading groups", e)
            binding.emptyStateGroup.visibility = View.VISIBLE
        }
    }
    
    private fun showDeleteGroupDialog(groupChat: GroupChat) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("解散群聊")
            .setMessage("确定要解散「${groupChat.name}」吗？")
            .setPositiveButton("解散") { _, _ ->
                dataManager.deleteGroupChat(groupChat.id)
                loadGroups()
                android.widget.Toast.makeText(this, "群聊已解散", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(character: Character) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除角色")
            .setMessage("确定要删除「${character.name}」吗？所有聊天记录也会被删除。")
            .setPositiveButton("删除") { _, _ ->
                if (dataManager.deleteCharacter(character.id)) {
                    loadCharacters()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示创建群聊对话框
     */
    private fun showCreateGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val etGroupName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etGroupName)
        val rvCharacters = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCharacters)
        val tvSelectedCount = dialogView.findViewById<android.widget.TextView>(R.id.tvSelectedCount)
        
        // ⭐ 只获取在线角色
        val onlineStatusManager = HoshioriApp.instance.onlineStatusManager
        val allCharacters = onlineStatusManager.getOnlineCharacters()
        
        if (allCharacters.isEmpty()) {
            Toast.makeText(this, "没有在线角色，请先将角色设为在线", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedCharacters = mutableListOf<Character>()
        
        // 设置角色选择列表
        val selectAdapter = CharacterSelectAdapter(allCharacters) { character, isSelected ->
            if (isSelected) {
                if (selectedCharacters.size < 4) {
                    selectedCharacters.add(character)
                    tvSelectedCount.text = getString(R.string.selected_count, selectedCharacters.size, 4)
                    true
                } else {
                    Toast.makeText(this@MainActivity, R.string.group_full, Toast.LENGTH_SHORT).show()
                    false
                }
            } else {
                selectedCharacters.remove(character)
                tvSelectedCount.text = getString(R.string.selected_count, selectedCharacters.size, 4)
                true
            }
        }
        
        rvCharacters.layoutManager = LinearLayoutManager(this)
        rvCharacters.adapter = selectAdapter
        
        // 显示对话框
        AlertDialog.Builder(this)
            .setTitle(R.string.create_group)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val groupName = etGroupName.text.toString().trim()
                if (groupName.isEmpty()) {
                    Toast.makeText(this@MainActivity, "请输入群聊名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selectedCharacters.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.group_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 创建群聊
                val groupChat = GroupChat(
                    name = groupName,
                    memberIds = selectedCharacters.map { it.id }.toMutableList()
                )
                dataManager.saveGroupChat(groupChat)
                
                // 跳转到群聊界面
                startActivity(Intent(this@MainActivity, GroupChatActivity::class.java).apply {
                    putExtra("group_id", groupChat.id)
                })
                
                Toast.makeText(this@MainActivity, R.string.group_created, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    /**
     * ⭐ 显示新手引导（仅首次）
     */
    private fun showWelcomeGuideIfNeeded() {
        if (!WelcomeGuideDialog.hasShown(this)) {
            // 延迟500ms显示，让界面先加载完成
            binding.root.postDelayed({
                WelcomeGuideDialog.show(this, markAsShown = true)
            }, 500)
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}

class CharacterAdapter(
    private val onItemClick: (Character) -> Unit,
    private val onEditClick: (Character) -> Unit,
    private val onDeleteClick: (Character) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Character, CharacterAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Character>() {
        override fun areItemsTheSame(oldItem: Character, newItem: Character) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Character, newItem: Character) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = com.hoshiori.ai.databinding.ItemCharacterBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: com.hoshiori.ai.databinding.ItemCharacterBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(character: Character) {
            binding.tvName.text = character.name
            binding.tvLastMessage.text = character.lastMessage.ifEmpty { "点击开始聊天~" }
            binding.tvTime.text = formatTime(character.lastMessageAt)
            
            // ⭐ 显示在线状态和视觉效果
            if (character.isOnline) {
                binding.onlineIndicator.setBackgroundResource(com.hoshiori.ai.R.drawable.bg_online_indicator)
                binding.onlineIndicator.visibility = View.VISIBLE
                binding.root.alpha = 1.0f  // 在线角色完全不透明
            } else {
                binding.onlineIndicator.setBackgroundResource(com.hoshiori.ai.R.drawable.bg_offline_indicator)
                binding.onlineIndicator.visibility = View.VISIBLE
                binding.root.alpha = 0.6f  // 离线角色半透明
            }
            
            // 加载头像
            character.avatar?.let { avatarPath ->
                val bitmap = when {
                    avatarPath == "default_hoshiori" -> {
                        // 从assets加载星织头像
                        try {
                            binding.root.context.assets.open("avatars/xingzhi.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_qiulan" -> {
                        // 从assets加载秋岚头像
                        try {
                            binding.root.context.assets.open("avatars/qiulan.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_zhaoLusi" -> {
                        // 从assets加载赵露思头像
                        try {
                            binding.root.context.assets.open("avatars/zhaolusi.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_huangJingyu" -> {
                        // 从assets加载黄景瑜头像
                        try {
                            binding.root.context.assets.open("avatars/huangjingyu.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_huangXiaoyun" -> {
                        // 从assets加载黄霄云头像
                        try {
                            binding.root.context.assets.open("avatars/huangxiaoyun.jpg").use { inputStream ->
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
                    binding.ivAvatar.setImageBitmap(bitmap)
                } else {
                    binding.ivAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
                }
            } ?: run {
                binding.ivAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
            }
            
            // 未读消息红点
            if (character.unreadCount > 0) {
                binding.tvUnread.visibility = View.VISIBLE
                binding.tvUnread.text = if (character.unreadCount > 99) "99+" else character.unreadCount.toString()
            } else {
                binding.tvUnread.visibility = View.GONE
            }
            
            binding.root.setOnClickListener { 
                // 点击后清除未读数
                if (character.unreadCount > 0) {
                    character.unreadCount = 0
                    binding.root.context.let { ctx ->
                        if (ctx is MainActivity) {
                            ctx.dataManager.saveCharacter(character)
                        }
                    }
                }
                onItemClick(character) 
            }
            
            binding.btnMore.setOnClickListener { view ->
                showPopupMenu(view, character)
            }
        }
        
        private fun showPopupMenu(view: View, character: Character) {
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(com.hoshiori.ai.R.menu.menu_character, popup.menu)
            
            // ⭐ 动态更新在线状态菜单项文本
            val toggleItem = popup.menu.findItem(com.hoshiori.ai.R.id.action_toggle_online)
            toggleItem?.title = if (character.isOnline) "设为离线" else "设为在线"
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    com.hoshiori.ai.R.id.action_toggle_online -> {
                        // ⭐ 切换在线状态
                        view.context.let { ctx ->
                            if (ctx is MainActivity) {
                                val onlineStatusManager = HoshioriApp.instance.onlineStatusManager
                                val newStatus = onlineStatusManager.toggleCharacterOnline(character.id)
                                
                                val statusText = if (newStatus) "在线" else "离线"
                                android.widget.Toast.makeText(
                                    ctx,
                                    "${character.name} 已设为${statusText}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                
                                // ⭐ 强制刷新：先提交null，再提交新列表
                                ctx.runOnUiThread {
                                    ctx.adapter.submitList(null)
                                    ctx.adapter.notifyDataSetChanged()
                                    ctx.loadCharacters()
                                }
                            }
                        }
                        true
                    }
                    com.hoshiori.ai.R.id.action_edit -> {
                        onEditClick(character)
                        true
                    }
                    com.hoshiori.ai.R.id.action_delete -> {
                        if (!character.isDefault) {
                            onDeleteClick(character)
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                diff < 7 * 24 * 60 * 60 * 1000 -> SimpleDateFormat("E", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}

/**
 * 角色选择适配器（用于创建群聊）
 */
class CharacterSelectAdapter(
    private val characters: List<Character>,
    private val onSelectionChanged: (Character, Boolean) -> Boolean  // 返回是否允许选择
) : androidx.recyclerview.widget.RecyclerView.Adapter<CharacterSelectAdapter.ViewHolder>() {
    
    private val selectedIds = mutableSetOf<String>()
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(com.hoshiori.ai.R.layout.item_character_select, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(characters[position])
    }
    
    override fun getItemCount(): Int = characters.size
    
    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val checkbox: android.widget.CheckBox = itemView.findViewById(com.hoshiori.ai.R.id.checkbox)
        private val ivAvatar: android.widget.ImageView = itemView.findViewById(com.hoshiori.ai.R.id.ivAvatar)
        private val tvName: android.widget.TextView = itemView.findViewById(com.hoshiori.ai.R.id.tvName)
        
        fun bind(character: Character) {
            tvName.text = character.name
            checkbox.isChecked = selectedIds.contains(character.id)
            
            // 加载头像
            character.avatar?.let { avatarPath ->
                val bitmap = when {
                    avatarPath == "default_hoshiori" -> {
                        try {
                            itemView.context.assets.open("avatars/xingzhi.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_qiulan" -> {
                        try {
                            itemView.context.assets.open("avatars/qiulan.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_zhaoLusi" -> {
                        try {
                            itemView.context.assets.open("avatars/zhaolusi.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_huangJingyu" -> {
                        try {
                            itemView.context.assets.open("avatars/huangjingyu.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath == "default_huangXiaoyun" -> {
                        try {
                            itemView.context.assets.open("avatars/huangxiaoyun.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    avatarPath.startsWith("default_") -> null
                    else -> com.hoshiori.ai.utils.ImageUtils.loadAvatarBitmap(avatarPath)
                }
                
                if (bitmap != null) {
                    ivAvatar.setImageBitmap(bitmap)
                } else {
                    ivAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
                }
            } ?: run {
                ivAvatar.setImageResource(com.hoshiori.ai.R.drawable.ic_default_avatar)
            }
            
            // 点击事件
            itemView.setOnClickListener {
                val isCurrentlySelected = selectedIds.contains(character.id)
                val newState = !isCurrentlySelected
                
                if (onSelectionChanged(character, newState)) {
                    if (newState) {
                        selectedIds.add(character.id)
                    } else {
                        selectedIds.remove(character.id)
                    }
                    checkbox.isChecked = newState
                }
            }
        }
    }
}


/**
 * 群聊列表适配器
 */
class GroupChatAdapter(
    private val onItemClick: (GroupChat) -> Unit,
    private val onDeleteClick: (GroupChat) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<GroupChatAdapter.ViewHolder>() {
    
    private val groups = mutableListOf<GroupChat>()
    
    fun submitList(newGroups: List<GroupChat>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = com.hoshiori.ai.databinding.ItemGroupChatBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position])
    }
    
    override fun getItemCount(): Int = groups.size
    
    inner class ViewHolder(
        private val binding: com.hoshiori.ai.databinding.ItemGroupChatBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(groupChat: GroupChat) {
            binding.tvGroupName.text = groupChat.name
            binding.tvMemberCount.text = "${groupChat.getMemberCount() + 1}人"
            binding.tvLastMessage.text = groupChat.lastMessage.ifEmpty { "暂无消息" }
            
            // 显示时间
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(groupChat.lastMessageAt))
            binding.tvTime.text = time
            
            // 点击进入群聊
            binding.root.setOnClickListener {
                onItemClick(groupChat)
            }
            
            // 长按删除
            binding.root.setOnLongClickListener {
                onDeleteClick(groupChat)
                true
            }
        }
    }
}
