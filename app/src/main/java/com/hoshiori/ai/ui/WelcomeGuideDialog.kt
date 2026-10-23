package com.hoshiori.ai.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hoshiori.ai.R
import com.hoshiori.ai.databinding.DialogWelcomeGuideBinding

/**
 * 新手引导弹窗
 * 
 * 功能：
 * 1. 首次启动时自动显示
 * 2. 可翻页查看多个引导页
 * 3. 支持跳过和完成
 * 4. 设置中可重新查看
 */
class WelcomeGuideDialog(context: Context) : Dialog(context, R.style.Theme_HoshioriAI) {
    
    private lateinit var binding: DialogWelcomeGuideBinding
    private val pages = mutableListOf<GuidePage>()
    private var currentPage = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogWelcomeGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置全屏
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        
        setupPages()
        setupViewPager()
        setupButtons()
        setupIndicators()
    }
    
    private fun setupPages() {
        pages.clear()
        pages.addAll(listOf(
            GuidePage(
                icon = R.drawable.ic_chat_empty,
                title = "欢迎使用星织AI ✨",
                description = """
                    一个有温度的AI陪伴应用
                    
                    星织AI采用先进的心理学理论和情感计算技术，为您提供真实、自然的对话体验。
                    
                    让我们开始一段温暖的陪伴之旅吧！
                """.trimIndent()
            ),
            GuidePage(
                icon = R.drawable.ic_settings,
                title = "⚙️ 首次使用必读",
                description = """
                    在开始使用前，请先配置API Key：
                    
                    1️⃣ 打开设置 → API设置
                    2️⃣ 选择API提供商（推荐Mimo或DeepSeek）
                    3️⃣ 输入您的API Key
                    
                    💡 没有API Key？
                    • Mimo: https://api.xiaomimimo.com
                    • DeepSeek: https://platform.deepseek.com
                """.trimIndent()
            ),
            GuidePage(
                icon = R.drawable.ic_group,
                title = "🎯 核心功能",
                description = """
                    ✨ 智能对话 - AI会记住你们的点点滴滴
                    💭 主动聊天 - AI会主动找你聊天
                    🎨 个性定制 - 自定义背景、头像、主题
                    👥 多角色 - 创建不同性格的AI角色
                    📝 成长日记 - AI会记录成长历程
                    🌐 联网搜索 - 可以查询天气等信息
                """.trimIndent()
            ),
            GuidePage(
                icon = R.drawable.ic_sticker,
                title = "💡 使用技巧",
                description = """
                    📱 主动消息功能
                    • 退出软件后，AI会主动发消息找你
                    • 聊天界面停留2分钟，AI会主动发起话题
                    
                    🔋 省电省Token
                    • 关闭不需要的功能可以节省Token
                    • 调整主动消息间隔可以省电
                    
                    🎨 个性化
                    • 长按角色卡片可以编辑角色
                    • 聊天界面菜单可以设置主题
                """.trimIndent()
            ),
            GuidePage(
                icon = R.drawable.ic_theme,
                title = "⚠️ 注意事项",
                description = """
                    🔒 隐私安全
                    • 不要在对话中输入真实个人信息
                    • API Key请妥善保管，不要分享
                    
                    💰 Token消耗
                    • 某些功能会消耗Token（已标注）
                    • 建议根据需求开启功能
                    
                    📱 权限说明
                    • 相机：用于拍照上传头像
                    • 存储：用于选择图片
                    • 通知：用于主动消息提醒
                """.trimIndent()
            ),
            GuidePage(
                icon = R.drawable.ic_add,
                title = "🚀 开始使用",
                description = """
                    一切准备就绪！
                    
                    现在您可以：
                    
                    1️⃣ 配置API Key（必需）
                    2️⃣ 选择或创建角色
                    3️⃣ 开始愉快的对话
                    
                    💡 提示：
                    • 随时可以在设置→关于中查看本指南
                    • 遇到问题可以查看帮助文档
                    
                    祝您使用愉快！✨
                """.trimIndent()
            )
        ))
    }
    
    private fun setupViewPager() {
        val adapter = GuidePageAdapter(pages)
        binding.viewPager.adapter = adapter
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateButtons()
                updateIndicators()
            }
        })
    }
    
    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnSkip.setOnClickListener {
            dismiss()
        }
        
        binding.btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) {
                binding.viewPager.currentItem = currentPage + 1
            } else {
                dismiss()
            }
        }
        
        updateButtons()
    }
    
    private fun updateButtons() {
        if (currentPage == pages.size - 1) {
            binding.btnNext.text = "开始使用"
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnNext.text = "下一步"
            binding.btnSkip.visibility = View.VISIBLE
        }
    }
    
    private fun setupIndicators() {
        binding.indicatorLayout.removeAllViews()
        
        for (i in pages.indices) {
            val indicator = View(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    if (i == currentPage) 24 else 8,
                    8
                ).apply {
                    marginStart = 4
                    marginEnd = 4
                }
                setBackgroundResource(
                    if (i == currentPage) R.drawable.bg_indicator_active
                    else R.drawable.bg_indicator_inactive
                )
            }
            binding.indicatorLayout.addView(indicator)
        }
    }
    
    private fun updateIndicators() {
        for (i in 0 until binding.indicatorLayout.childCount) {
            val indicator = binding.indicatorLayout.getChildAt(i)
            val params = indicator.layoutParams as ViewGroup.MarginLayoutParams
            params.width = if (i == currentPage) 24 else 8
            indicator.layoutParams = params
            indicator.setBackgroundResource(
                if (i == currentPage) R.drawable.bg_indicator_active
                else R.drawable.bg_indicator_inactive
            )
        }
    }
    
    /**
     * 引导页数据类
     */
    data class GuidePage(
        val icon: Int,
        val title: String,
        val description: String
    )
    
    /**
     * 引导页适配器
     */
    private class GuidePageAdapter(
        private val pages: List<GuidePage>
    ) : RecyclerView.Adapter<GuidePageAdapter.PageViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_welcome_page, parent, false)
            return PageViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(pages[position])
        }
        
        override fun getItemCount() = pages.size
        
        class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            
            fun bind(page: GuidePage) {
                ivIcon.setImageResource(page.icon)
                tvTitle.text = page.title
                tvDescription.text = page.description
            }
        }
    }
    
    companion object {
        private const val PREF_NAME = "welcome_guide"
        private const val KEY_SHOWN = "guide_shown"
        
        /**
         * 检查是否已显示过引导
         */
        fun hasShown(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SHOWN, false)
        }
        
        /**
         * 标记引导已显示
         */
        fun markAsShown(context: Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SHOWN, true).apply()
        }
        
        /**
         * 重置引导状态（用于测试）
         */
        fun reset(context: Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SHOWN, false).apply()
        }
        
        /**
         * 显示引导弹窗
         */
        fun show(context: Context, markAsShown: Boolean = true) {
            val dialog = WelcomeGuideDialog(context)
            dialog.show()
            
            if (markAsShown) {
                markAsShown(context)
            }
        }
    }
}
