package com.hoshiori.ai.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.data.ApiConfig
import com.hoshiori.ai.data.ApiProvider
import com.hoshiori.ai.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private val dataManager by lazy { HoshioriApp.instance.dataManager }
    private var currentProvider: ApiProvider = ApiProvider.MIMO
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadSettings()
        setupClickListeners()
        setupBackHandler()
        setupProviderToggle()
    }
    
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveSettings()
                finish()
            }
        })
    }
    
    private fun setupProviderToggle() {
        // 设置提供商切换监听
        binding.toggleGroupProvider.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentProvider = when (checkedId) {
                    binding.btnProviderMimo.id -> ApiProvider.MIMO
                    binding.btnProviderDeepSeek.id -> ApiProvider.DEEPSEEK
                    binding.btnProviderDeepSeekReasoner.id -> ApiProvider.DEEPSEEK_REASONER
                    binding.btnProviderSpikey.id -> ApiProvider.SPIKEY
                    else -> ApiProvider.MIMO
                }
                updateUIForProvider(currentProvider)
            }
        }
    }
    
    private fun updateUIForProvider(provider: ApiProvider) {
        when (provider) {
            ApiProvider.SPIKEY -> {
                // 显示自定义配置
                binding.layoutCustomConfig.visibility = View.VISIBLE
                binding.tvProviderHint.text = "自定义: 可以使用任何兼容OpenAI格式的API"
            }
            ApiProvider.MIMO -> {
                binding.layoutCustomConfig.visibility = View.GONE
                binding.tvProviderHint.text = "Mimo: 免费额度，适合测试\n默认模型: mimo-v2-flash"
            }
            ApiProvider.DEEPSEEK -> {
                binding.layoutCustomConfig.visibility = View.GONE
                binding.tvProviderHint.text = "DeepSeek: 高性价比，强大的推理能力\n默认模型: deepseek-chat (非思考模式)"
            }
            ApiProvider.DEEPSEEK_REASONER -> {
                binding.layoutCustomConfig.visibility = View.GONE
                binding.tvProviderHint.text = "DeepSeek (思考模式): 深度推理，适合复杂问题\n默认模型: deepseek-reasoner (会显示思考过程)"
            }
        }
    }
    
    private fun loadSettings() {
        val settings = dataManager.getSettings()
        
        // API设置
        currentProvider = settings.apiConfig.provider
        binding.etApiKey.setText(settings.apiConfig.apiKey)
        binding.etApiEndpoint.setText(settings.apiConfig.apiEndpoint)
        binding.etModel.setText(settings.apiConfig.model)
        
        // 设置提供商选择
        when (currentProvider) {
            ApiProvider.MIMO -> binding.toggleGroupProvider.check(binding.btnProviderMimo.id)
            ApiProvider.DEEPSEEK -> binding.toggleGroupProvider.check(binding.btnProviderDeepSeek.id)
            ApiProvider.DEEPSEEK_REASONER -> binding.toggleGroupProvider.check(binding.btnProviderDeepSeekReasoner.id)
            ApiProvider.SPIKEY -> binding.toggleGroupProvider.check(binding.btnProviderSpikey.id)
        }
        
        updateUIForProvider(currentProvider)
        
        // 聊天设置
        binding.sliderStickerFrequency.value = (settings.stickerFrequency * 100).coerceIn(0f, 50f)
        binding.tvStickerFrequency.text = getString(com.hoshiori.ai.R.string.percentage_format, binding.sliderStickerFrequency.value.toInt())
        
        // ⭐ 退出软件后主动消息设置
        binding.switchProactiveMessage.isChecked = settings.proactiveMessageEnabled
        binding.sliderProactiveInterval.value = settings.proactiveMessageInterval.toFloat().coerceIn(15f, 360f)
        updateProactiveIntervalText(settings.proactiveMessageInterval)
        
        // ⭐ 聊天界面主动对话设置
        binding.switchProactiveChat.isChecked = settings.proactiveChatEnabled
        
        binding.switchMemoryCompression.isChecked = settings.memoryCompressionEnabled
        binding.switchWebSearch.isChecked = settings.webSearchEnabled
        binding.switchOfflineSearch.isChecked = settings.offlineSearchEnabled
        binding.etWeatherApiKey.setText(settings.weatherApiKey)
        binding.switchDualStageThinking.isChecked = settings.dualStageThinkingEnabled
        binding.switchNotification.isChecked = settings.notificationEnabled
        
        // 根据主动消息开关状态显示/隐藏间隔设置
        updateProactiveIntervalVisibility(settings.proactiveMessageEnabled)
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            saveSettings()
            finish() 
        }
        
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        binding.sliderStickerFrequency.addOnChangeListener { _, value, _ ->
            binding.tvStickerFrequency.text = getString(com.hoshiori.ai.R.string.percentage_format, value.toInt())
        }
        
        binding.sliderProactiveInterval.addOnChangeListener { _, value, _ ->
            updateProactiveIntervalText(value.toInt())
        }
        
        binding.switchProactiveMessage.setOnCheckedChangeListener { _, isChecked ->
            updateProactiveIntervalVisibility(isChecked)
        }
        
        // ⭐ 关于卡片点击 - 显示新手引导
        binding.cardAbout.setOnClickListener {
            WelcomeGuideDialog.show(this, markAsShown = false)
        }
        
        // 添加按钮动画
        setupButtonAnimation(binding.btnBack)
        setupButtonAnimation(binding.btnSave)
    }
    
    private fun updateProactiveIntervalText(minutes: Int) {
        val text = when {
            minutes < 60 -> "${minutes}分钟"
            minutes % 60 == 0 -> "${minutes / 60}小时"
            else -> "${minutes / 60}小时${minutes % 60}分钟"
        }
        binding.tvProactiveInterval.text = text
    }
    
    private fun updateProactiveIntervalVisibility(enabled: Boolean) {
        binding.layoutProactiveInterval.visibility = if (enabled) View.VISIBLE else View.GONE
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
            false
        }
    }
    
    private fun saveSettings() {
        val apiConfig = ApiConfig(
            provider = currentProvider,
            apiKey = binding.etApiKey.text.toString().trim(),
            apiEndpoint = if (currentProvider == ApiProvider.SPIKEY) {
                binding.etApiEndpoint.text.toString().trim()
            } else {
                currentProvider.defaultEndpoint
            },
            model = if (currentProvider == ApiProvider.SPIKEY) {
                binding.etModel.text.toString().trim()
            } else {
                currentProvider.defaultModel
            }
        )
        
        val settings = dataManager.getSettings().copy(
            apiConfig = apiConfig,
            stickerFrequency = binding.sliderStickerFrequency.value / 100f,
            proactiveMessageEnabled = binding.switchProactiveMessage.isChecked,
            proactiveMessageInterval = binding.sliderProactiveInterval.value.toInt(),
            proactiveChatEnabled = binding.switchProactiveChat.isChecked,
            memoryCompressionEnabled = binding.switchMemoryCompression.isChecked,
            webSearchEnabled = binding.switchWebSearch.isChecked,
            offlineSearchEnabled = binding.switchOfflineSearch.isChecked,
            weatherApiKey = binding.etWeatherApiKey.text.toString().trim(),
            dualStageThinkingEnabled = binding.switchDualStageThinking.isChecked,
            notificationEnabled = binding.switchNotification.isChecked
        )
        
        dataManager.saveSettings(settings)
        
        // 根据设置启动或停止主动消息服务
        if (settings.proactiveMessageEnabled) {
            // 传入用户设置的间隔时间，让WorkManager使用相同的检查频率
            com.hoshiori.ai.service.ProactiveMessageWorker.schedule(this, settings.proactiveMessageInterval)
        } else {
            com.hoshiori.ai.service.ProactiveMessageWorker.cancel(this)
        }
        
        Toast.makeText(this, getString(com.hoshiori.ai.R.string.settings_saved), Toast.LENGTH_SHORT).show()
    }
}
