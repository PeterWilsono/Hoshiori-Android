package com.hoshiori.ai.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoshiori.ai.R
import com.hoshiori.ai.databinding.DialogStickerPickerBinding
import java.io.IOException

/**
 * 表情包选择器对话框
 */
class StickerPickerDialog(
    context: Context,
    private val onStickerSelected: (String) -> Unit,
    private val onImageSelected: () -> Unit,
    private val onTakePhoto: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogStickerPickerBinding
    private val stickerList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogStickerPickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // 设置对话框样式
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()
        loadStickers()
    }

    private fun setupViews() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            android.util.Log.d("StickerPickerDialog", "关闭按钮被点击")
            dismiss()
        }

        // 选择图片按钮
        binding.btnSelectImage.setOnClickListener {
            android.util.Log.d("StickerPickerDialog", "选择图片按钮被点击")
            dismiss()
            onImageSelected()
        }

        // 拍照按钮
        binding.btnTakePhoto.setOnClickListener {
            android.util.Log.d("StickerPickerDialog", "拍照按钮被点击")
            dismiss()
            onTakePhoto()
        }

        // 设置表情包网格
        binding.rvStickers.layoutManager = GridLayoutManager(context, 4)
        binding.rvStickers.adapter = StickerAdapter(stickerList) { stickerPath ->
            android.util.Log.d("StickerPickerDialog", "表情包被选中: $stickerPath")
            dismiss()
            onStickerSelected(stickerPath)
        }
    }

    private fun loadStickers() {
        try {
            // 从assets加载所有表情包
            val assetManager = context.assets
            val stickers = assetManager.list("stickers") ?: emptyArray()
            
            stickerList.clear()
            stickerList.addAll(stickers.filter { it.endsWith(".png") || it.endsWith(".jpg") })
            
            binding.rvStickers.adapter?.notifyDataSetChanged()
            
            android.util.Log.d("StickerPickerDialog", "加载了 ${stickerList.size} 个表情包")
        } catch (e: IOException) {
            android.util.Log.e("StickerPickerDialog", "加载表情包失败", e)
        }
    }

    /**
     * 表情包适配器
     */
    private class StickerAdapter(
        private val stickers: List<String>,
        private val onStickerClick: (String) -> Unit
    ) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            holder.bind(stickers[position], onStickerClick)
        }

        override fun getItemCount() = stickers.size

        class StickerViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val ivSticker: ImageView = itemView.findViewById(R.id.ivSticker)

            fun bind(stickerName: String, onStickerClick: (String) -> Unit) {
                android.util.Log.d("StickerViewHolder", "绑定表情包: $stickerName")
                
                try {
                    val inputStream = itemView.context.assets.open("stickers/$stickerName")
                    val drawable = android.graphics.drawable.Drawable.createFromStream(inputStream, null)
                    ivSticker.setImageDrawable(drawable)
                    inputStream.close()
                    android.util.Log.d("StickerViewHolder", "表情包加载成功: $stickerName")
                } catch (e: Exception) {
                    android.util.Log.e("StickerViewHolder", "加载表情包失败: $stickerName", e)
                }

                // 设置点击事件
                itemView.setOnClickListener {
                    val fullPath = "stickers/$stickerName"
                    android.util.Log.d("StickerViewHolder", "表情包被点击: $fullPath")
                    onStickerClick(fullPath)
                }
                
                // 同时在ImageView上也设置点击事件（双保险）
                ivSticker.setOnClickListener {
                    val fullPath = "stickers/$stickerName"
                    android.util.Log.d("StickerViewHolder", "表情包ImageView被点击: $fullPath")
                    onStickerClick(fullPath)
                }
            }
        }
    }
}
