package com.campcooking.teacher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.teacher.R
import com.campcooking.teacher.data.TeamInfo
import com.campcooking.teacher.databinding.ItemTeamBinding

/**
 * 团队列表适配器
 */
class TeamListAdapter(
    private var teams: List<TeamInfo> = emptyList(),
    private val onTeamClick: (TeamInfo) -> Unit
) : RecyclerView.Adapter<TeamListAdapter.TeamViewHolder>() {

    private var selectedPosition = -1

    inner class TeamViewHolder(private val binding: ItemTeamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(team: TeamInfo, position: Int) {
            // 设置团队信息
            binding.teamNameText.text = team.getDisplayName()
            binding.groupLeaderText.text = if (team.groupLeader.isNotEmpty()) {
                "组长：${team.groupLeader}"
            } else {
                "组长：未设置"
            }
            
            // 设置分工信息
            val divisionText = team.getDivisionText()
            binding.divisionText.text = if (divisionText == "暂无分工信息") {
                "分工：未分配"
            } else {
                "分工：已分配"
            }

            // 设置选中状态
            binding.root.isSelected = position == selectedPosition
            if (position == selectedPosition) {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.surface_variant)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.surface)
                )
            }

            // 设置点击事件
            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                
                // 更新选中状态
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                
                onTeamClick(team)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemTeamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teams[position], position)
    }

    override fun getItemCount(): Int = teams.size

    /**
     * 更新团队列表
     */
    fun updateTeams(newTeams: List<TeamInfo>) {
        teams = newTeams
        selectedPosition = -1
        notifyDataSetChanged()
    }

    /**
     * 获取选中的团队
     */
    fun getSelectedTeam(): TeamInfo? {
        return if (selectedPosition >= 0 && selectedPosition < teams.size) {
            teams[selectedPosition]
        } else {
            null
        }
    }
}

