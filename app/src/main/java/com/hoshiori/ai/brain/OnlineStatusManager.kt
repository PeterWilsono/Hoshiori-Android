package com.hoshiori.ai.brain

import android.util.Log
import com.hoshiori.ai.data.Character
import com.hoshiori.ai.data.DataManager
import java.util.concurrent.TimeUnit

/**
 * 在线状态管理器
 * 
 * 功能：
 * 1. 智能管理角色在线/离线状态
 * 2. 根据活跃度自动调整在线状态
 * 3. 离线角色不占用token，不发送主动消息
 * 4. 增加真实感和沉浸体验
 */
class OnlineStatusManager(private val dataManager: DataManager) {
    
    companion object {
        private const val TAG = "OnlineStatusManager"
        
        // 自动离线阈值（默认角色）
        private val AUTO_OFFLINE_THRESHOLD = TimeUnit.HOURS.toMillis(24)  // 24小时无互动自动离线
        
        // 自动上线阈值（用户主动打开聊天）
        private val AUTO_ONLINE_ON_CHAT = true
        
        // 最大在线角色数（优化token使用）
        private const val MAX_ONLINE_CHARACTERS = 3
    }
    
    /**
     * 检查并更新所有角色的在线状态
     * 建议在应用启动时调用
     */
    fun updateAllOnlineStatus() {
        Log.d(TAG, "=== 开始更新所有角色在线状态 ===")
        
        val characters = dataManager.getAllCharacters()
        val now = System.currentTimeMillis()
        
        var updatedCount = 0
        
        characters.forEach { character ->
            if (character.autoOfflineEnabled && character.isOnline) {
                val inactiveDuration = now - character.lastMessageAt
                
                if (inactiveDuration > AUTO_OFFLINE_THRESHOLD) {
                    // 超过阈值，自动离线
                    character.isOnline = false
                    character.lastOnlineAt = now
                    dataManager.saveCharacter(character)
                    updatedCount++
                    
                    Log.d(TAG, "角色 ${character.name} 因长时间无互动自动离线")
                }
            }
        }
        
        // 智能在线数量控制
        balanceOnlineCharacters()
        
        Log.d(TAG, "=== 在线状态更新完成，共更新 $updatedCount 个角色 ===")
    }
    
    /**
     * 平衡在线角色数量
     * 如果在线角色过多，将最不活跃的角色设为离线
     */
    private fun balanceOnlineCharacters() {
        val characters = dataManager.getAllCharacters()
        val onlineCharacters = characters.filter { it.isOnline && it.autoOfflineEnabled }
        
        if (onlineCharacters.size > MAX_ONLINE_CHARACTERS) {
            Log.d(TAG, "在线角色过多(${onlineCharacters.size})，开始平衡...")
            
            // 按最后消息时间排序，最不活跃的排前面
            val sortedByActivity = onlineCharacters.sortedBy { it.lastMessageAt }
            
            // 将超出数量的角色设为离线
            val toOffline = sortedByActivity.take(onlineCharacters.size - MAX_ONLINE_CHARACTERS)
            toOffline.forEach { character ->
                character.isOnline = false
                character.lastOnlineAt = System.currentTimeMillis()
                dataManager.saveCharacter(character)
                
                Log.d(TAG, "角色 ${character.name} 因在线数量过多被设为离线")
            }
        }
    }
    
    /**
     * 手动设置角色在线状态
     */
    fun setCharacterOnline(characterId: String, isOnline: Boolean) {
        val character = dataManager.getCharacter(characterId) ?: return
        
        if (character.isOnline == isOnline) {
            return  // 状态未变化
        }
        
        character.isOnline = isOnline
        character.lastOnlineAt = System.currentTimeMillis()
        
        if (isOnline) {
            Log.d(TAG, "角色 ${character.name} 上线了")
        } else {
            Log.d(TAG, "角色 ${character.name} 离线了")
        }
        
        dataManager.saveCharacter(character)
        
        // 如果上线，检查是否需要让其他角色离线
        if (isOnline) {
            balanceOnlineCharacters()
        }
    }
    
    /**
     * 切换角色在线状态
     */
    fun toggleCharacterOnline(characterId: String): Boolean {
        val character = dataManager.getCharacter(characterId) ?: return false
        val newStatus = !character.isOnline
        setCharacterOnline(characterId, newStatus)
        return newStatus
    }
    
    /**
     * 当用户打开聊天时，自动将角色设为在线
     */
    fun onChatOpened(characterId: String) {
        val character = dataManager.getCharacter(characterId) ?: return
        
        if (!character.isOnline && AUTO_ONLINE_ON_CHAT) {
            Log.d(TAG, "用户打开聊天，角色 ${character.name} 自动上线")
            setCharacterOnline(characterId, true)
        }
    }
    
    /**
     * 获取所有在线角色
     */
    fun getOnlineCharacters(): List<Character> {
        return dataManager.getAllCharacters().filter { it.isOnline }
    }
    
    /**
     * 获取所有离线角色
     */
    fun getOfflineCharacters(): List<Character> {
        return dataManager.getAllCharacters().filter { !it.isOnline }
    }
    
    /**
     * 获取在线角色数量
     */
    fun getOnlineCount(): Int {
        return getOnlineCharacters().size
    }
    
    /**
     * 检查角色是否在线
     */
    fun isCharacterOnline(characterId: String): Boolean {
        return dataManager.getCharacter(characterId)?.isOnline ?: false
    }
    
    /**
     * 获取角色的在线状态文本
     */
    fun getOnlineStatusText(character: Character): String {
        return if (character.isOnline) {
            "在线"
        } else {
            val offlineDuration = System.currentTimeMillis() - character.lastOnlineAt
            when {
                offlineDuration < TimeUnit.HOURS.toMillis(1) -> "刚刚离线"
                offlineDuration < TimeUnit.HOURS.toMillis(24) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(offlineDuration)
                    "${hours}小时前在线"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(offlineDuration)
                    "${days}天前在线"
                }
            }
        }
    }
    
    /**
     * 启用/禁用角色的自动离线功能
     */
    fun setAutoOfflineEnabled(characterId: String, enabled: Boolean) {
        val character = dataManager.getCharacter(characterId) ?: return
        character.autoOfflineEnabled = enabled
        dataManager.saveCharacter(character)
        
        Log.d(TAG, "角色 ${character.name} 自动离线功能: ${if (enabled) "启用" else "禁用"}")
    }
}
