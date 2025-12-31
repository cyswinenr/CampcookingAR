package com.campcooking.ar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.ar.R
import com.campcooking.ar.data.Video
import com.campcooking.ar.databinding.ItemVideoBinding

/**
 * 视频列表适配器
 */
class VideoListAdapter(
    private val videos: List<Video>,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {
    
    private var selectedPosition = 0
    
    inner class VideoViewHolder(val binding: ItemVideoBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(video: Video, isSelected: Boolean) {
            binding.apply {
                videoTitle.text = video.title
                videoDuration.text = video.duration
                videoCategory.text = video.category
                
                // 设置选中状态
                root.isSelected = isSelected
                
                // 设置背景色
                if (isSelected) {
                    root.setBackgroundResource(R.color.video_item_selected)
                } else {
                    root.setBackgroundResource(android.R.color.white)
                }
                
                // 点击事件
                root.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = bindingAdapterPosition
                    
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    
                    onVideoClick(video)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        val isSelected = position == selectedPosition
        holder.bind(video, isSelected)
    }
    
    override fun getItemCount() = videos.size
    
    /**
     * 设置选中位置（供外部调用）
     */
    fun setSelectedPosition(position: Int) {
        if (position in videos.indices) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}

