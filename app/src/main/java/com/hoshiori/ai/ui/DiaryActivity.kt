package com.hoshiori.ai.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoshiori.ai.HoshioriApp
import com.hoshiori.ai.data.DailyDiaryRecord
import com.hoshiori.ai.databinding.ActivityDiaryBinding

/**
 * 日记本Activity
 * 显示AI的成长日记
 */
class DiaryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDiaryBinding
    private val memoryManager by lazy { HoshioriApp.instance.unifiedMemoryManager }
    private lateinit var adapter: DiaryAdapter
    private var characterId: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        characterId = intent.getStringExtra("character_id") ?: return finish()
        
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        loadDiaries()
    }
    
    private fun setupUI() {
        // 设置标题
        val character = HoshioriApp.instance.dataManager.getCharacter(characterId)
        binding.tvTitle.text = "📔 ${character?.name ?: "TA"}的成长日记"
    }
    
    private fun setupRecyclerView() {
        adapter = DiaryAdapter()
        binding.rvDiaries.layoutManager = LinearLayoutManager(this)
        binding.rvDiaries.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        // 添加按钮动画
        setupButtonAnimation(binding.btnBack)
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
    
    private fun loadDiaries() {
        // 获取所有日记（最近30天）
        val diaries = memoryManager.getRecentDiaries(characterId, limit = 30)
        
        if (diaries.isEmpty()) {
            // 显示空状态
            binding.rvDiaries.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            // 显示日记列表
            binding.rvDiaries.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            adapter.submitList(diaries)
        }
    }
}

/**
 * 日记适配器
 */
class DiaryAdapter : androidx.recyclerview.widget.ListAdapter<DailyDiaryRecord, DiaryAdapter.DiaryViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<DailyDiaryRecord>() {
        override fun areItemsTheSame(oldItem: DailyDiaryRecord, newItem: DailyDiaryRecord): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: DailyDiaryRecord, newItem: DailyDiaryRecord): Boolean {
            return oldItem == newItem
        }
    }
) {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = com.hoshiori.ai.databinding.ItemDiaryBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiaryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DiaryViewHolder(
        private val binding: com.hoshiori.ai.databinding.ItemDiaryBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(diary: DailyDiaryRecord) {
            binding.tvDate.text = diary.date
            binding.tvConversationCount.text = "💬 ${diary.conversationCount}条对话"
            binding.tvEmotionalHighlight.text = diary.emotionalHighlight
            
            // 关键洞察
            if (diary.keyInsights.isNotEmpty()) {
                binding.tvInsights.text = diary.keyInsights.joinToString("\n") { "• $it" }
            } else {
                binding.layoutInsights.visibility = View.GONE
            }
            
            binding.tvGrowthReflection.text = diary.growthReflection
            binding.tvRelationshipChange.text = diary.relationshipChange
            binding.tvLookingForward.text = diary.lookingForward
        }
    }
}
