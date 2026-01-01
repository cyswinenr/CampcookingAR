package com.campcooking.ar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.ar.R
import com.campcooking.ar.data.CookingStage
import com.campcooking.ar.data.ProcessRecord
import com.campcooking.ar.databinding.ItemStageBinding

/**
 * 流程节点列表适配器
 */
class StageListAdapter(
    private val stages: List<CookingStage>,
    private val processRecord: ProcessRecord,
    private val onStageClick: (CookingStage) -> Unit
) : RecyclerView.Adapter<StageListAdapter.StageViewHolder>() {

    inner class StageViewHolder(private val binding: ItemStageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stage: CookingStage) {
            val stageRecord = processRecord.stages[stage]
            val isCurrent = processRecord.currentStage == stage
            val isCompleted = stageRecord?.isCompleted == true

            // 设置基本信息
            binding.stageEmojiText.text = stage.emoji
            binding.stageNameText.text = stage.displayName

            // 设置状态图标和颜色
            when {
                isCompleted -> {
                    binding.stageStatusIcon.text = "✓"
                    binding.stageStatusIcon.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.nature_green)
                    )
                    binding.stageNameText.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.nature_green)
                    )
                }
                isCurrent -> {
                    binding.stageStatusIcon.text = "▶"
                    binding.stageStatusIcon.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.fire_orange)
                    )
                    binding.stageNameText.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.fire_orange)
                    )
                }
                else -> {
                    binding.stageStatusIcon.text = "○"
                    binding.stageStatusIcon.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.subtitle_color)
                    )
                    binding.stageNameText.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.subtitle_color)
                    )
                }
            }

            // 设置用时
            binding.stageDurationText.text = stageRecord?.getDurationText() ?: "未开始"

            // 设置评分（如果已完成）
            if (isCompleted && stageRecord != null && stageRecord.selfRating > 0) {
                val stars = "⭐".repeat(stageRecord.selfRating)
                binding.stageRatingText.text = stars
                binding.stageRatingText.visibility = android.view.View.VISIBLE
            } else {
                binding.stageRatingText.visibility = android.view.View.GONE
            }

            // 高亮当前阶段
            if (isCurrent) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.fire_yellow)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )
            }

            // 点击事件
            binding.root.setOnClickListener {
                onStageClick(stage)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val binding = ItemStageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        holder.bind(stages[position])
    }

    override fun getItemCount() = stages.size
}

