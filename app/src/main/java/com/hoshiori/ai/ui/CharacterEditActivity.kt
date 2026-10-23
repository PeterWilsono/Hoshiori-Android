package com.hoshiori.ai.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.data.Character
import com.hoshiori.ai.databinding.ActivityCharacterEditBinding
import com.hoshiori.ai.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.File

class CharacterEditActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCharacterEditBinding
    private val dataManager by lazy { HoshioriApp.instance.dataManager }
    
    private var characterId: String? = null
    private var character: Character? = null
    private var isEditMode = false
    private var currentPhotoUri: Uri? = null
    private var pendingAvatarPath: String? = null  // 待保存的头像路径
    
    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleAvatarSelected(uri)
            }
        }
    }
    
    // 拍照
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                handleAvatarSelected(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        characterId = intent.getStringExtra("character_id")
        isEditMode = characterId != null
        
        if (isEditMode) {
            character = dataManager.getCharacter(characterId!!)
            loadCharacterData()
        }
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        binding.tvTitle.text = if (isEditMode) "编辑角色" else "创建角色"
        binding.btnDelete.visibility = if (isEditMode && character?.isDefault != true) 
            android.view.View.VISIBLE else android.view.View.GONE
    }
    
    private fun loadCharacterData() {
        character?.let { char ->
            binding.etName.setText(char.name)
            binding.etRolePrompt.setText(char.rolePrompt)
            
            // 加载头像
            char.avatar?.let { avatarPath ->
                val bitmap = when {
                    avatarPath == "default_hoshiori" -> {
                        // 从assets加载星织头像
                        try {
                            assets.open("avatars/xingzhi.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CharacterEditActivity", "加载星织头像失败", e)
                            null
                        }
                    }
                    avatarPath == "default_qiulan" -> {
                        // 从assets加载秋岚头像
                        try {
                            assets.open("avatars/qiulan.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CharacterEditActivity", "加载秋岚头像失败", e)
                            null
                        }
                    }
                    avatarPath == "default_zhaoLusi" -> {
                        // 从assets加载赵露思头像
                        try {
                            assets.open("avatars/zhaolusi.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CharacterEditActivity", "加载赵露思头像失败", e)
                            null
                        }
                    }
                    avatarPath == "default_huangJingyu" -> {
                        // 从assets加载黄景瑜头像
                        try {
                            assets.open("avatars/huangjingyu.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CharacterEditActivity", "加载黄景瑜头像失败", e)
                            null
                        }
                    }
                    avatarPath == "default_huangXiaoyun" -> {
                        // 从assets加载黄霄云头像
                        try {
                            assets.open("avatars/huangxiaoyun.jpg").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CharacterEditActivity", "加载黄霄云头像失败", e)
                            null
                        }
                    }
                    avatarPath.startsWith("default_") -> {
                        // 其他默认头像
                        null
                    }
                    else -> {
                        // 自定义头像
                        ImageUtils.loadAvatarBitmap(avatarPath)
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
        }
    }
    
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnSave.setOnClickListener { saveCharacter() }
        
        binding.btnSaveBottom.setOnClickListener { saveCharacter() }
        
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
        
        binding.btnResetPrompt.setOnClickListener {
            binding.etRolePrompt.setText(Character.DEFAULT_ROLE_PROMPT)
        }
        
        // 头像点击 - 显示选择对话框
        binding.ivAvatar.setOnClickListener {
            showAvatarSourceDialog()
        }
        
        // 修复EditText在ScrollView中的滚动问题
        binding.etRolePrompt.setOnTouchListener { v, event ->
            if (v.id == binding.etRolePrompt.id) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
        
        // 添加按钮动画
        setupButtonAnimation(binding.btnBack)
        setupButtonAnimation(binding.btnSave)
        setupButtonAnimation(binding.btnSaveBottom)
        setupButtonAnimation(binding.btnDelete)
        setupButtonAnimation(binding.btnResetPrompt)
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
    
    private fun saveCharacter() {
        val name = binding.etName.text.toString().trim()
        val rolePrompt = binding.etRolePrompt.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入角色名称", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (rolePrompt.isEmpty()) {
            Toast.makeText(this, "请输入角色设定", Toast.LENGTH_SHORT).show()
            return
        }
        
        val charToSave = character?.copy(
            name = name,
            rolePrompt = rolePrompt,
            avatar = pendingAvatarPath ?: character?.avatar  // 使用新头像或保持原头像
        ) ?: Character(
            name = name,
            rolePrompt = rolePrompt,
            avatar = pendingAvatarPath
        )
        
        dataManager.saveCharacter(charToSave)
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    // ==================== 头像上传功能 ====================
    
    /**
     * 显示头像来源选择对话框
     */
    private fun showAvatarSourceDialog() {
        val items = arrayOf(
            getString(com.hoshiori.ai.R.string.from_gallery),
            getString(com.hoshiori.ai.R.string.from_camera)
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.hoshiori.ai.R.string.select_avatar_source))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> openCamera()
                }
            }
            .show()
    }
    
    /**
     * 打开图片选择器
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    /**
     * 打开相机
     */
    private fun openCamera() {
        val photoFile = File(cacheDir, "avatar_temp_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        takePhotoLauncher.launch(intent)
    }
    
    /**
     * 处理选择的头像
     */
    private fun handleAvatarSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                // 获取或生成角色ID
                val charId = characterId ?: character?.id ?: java.util.UUID.randomUUID().toString()
                
                // 保存头像
                val avatarPath = ImageUtils.saveAvatarImage(this@CharacterEditActivity, uri, charId)
                
                if (avatarPath != null) {
                    // 更新UI
                    val bitmap = ImageUtils.loadAvatarBitmap(avatarPath)
                    if (bitmap != null) {
                        binding.ivAvatar.setImageBitmap(bitmap)
                        pendingAvatarPath = avatarPath
                        
                        Toast.makeText(
                            this@CharacterEditActivity,
                            getString(com.hoshiori.ai.R.string.avatar_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@CharacterEditActivity,
                        getString(com.hoshiori.ai.R.string.avatar_upload_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("CharacterEditActivity", "处理头像失败", e)
                Toast.makeText(
                    this@CharacterEditActivity,
                    getString(com.hoshiori.ai.R.string.avatar_upload_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun showDeleteConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除角色")
            .setMessage("确定要删除「${character?.name}」吗？所有聊天记录也会被删除。")
            .setPositiveButton("删除") { _, _ ->
                characterId?.let { id ->
                    if (dataManager.deleteCharacter(id)) {
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
