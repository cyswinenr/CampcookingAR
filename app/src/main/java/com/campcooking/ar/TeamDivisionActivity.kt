package com.campcooking.ar

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.campcooking.ar.databinding.ActivityTeamDivisionBinding
import com.campcooking.ar.utils.DataSubmitManager
import com.campcooking.ar.utils.TeamInfoManager
import com.google.android.material.button.MaterialButton

/**
 * 团队分工页面
 * 支持拖拽分配人员到不同的小组（A、B、C、D组）
 */
class TeamDivisionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTeamDivisionBinding
    private lateinit var teamInfoManager: TeamInfoManager
    private lateinit var dataSubmitManager: DataSubmitManager
    
    // 存储各小组的人员
    private val groupLeader = mutableListOf<String>()  // 项目组长（1人）
    private val groupCooking = mutableListOf<String>() // 烹饪组（3人）
    private val groupSoupRice = mutableListOf<String>() // 汤饭组（2人）
    private val groupFire = mutableListOf<String>()     // 生火组（2人）
    private val groupHealth = mutableListOf<String>()   // 卫生组（2人）
    
    // 所有成员列表
    private val allMembers = mutableListOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 保持全屏模式
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        binding = ActivityTeamDivisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        teamInfoManager = TeamInfoManager(this)
        dataSubmitManager = DataSubmitManager(this)
        
        // 获取团队信息
        val teamName = intent.getStringExtra("teamName") ?: "野炊小组"
        binding.teamNameText.text = teamName
        
        setupMembers()
        setupDragAndDrop()
        setupListeners()
        loadSavedDivision()
        setupDutyTextStyles()
        setupServerSettingsButton()
    }
    
    /**
     * 设置职责文字样式：将"领取炊具"、"领取食材"、"领取野炊用柴"标红并加大
     */
    private fun setupDutyTextStyles() {
        // 烹饪组：领取炊具
        binding.groupCookingDutyText?.let { textView ->
            val fullText = textView.text.toString()
            val highlightText = "领取炊具"
            val spannable = SpannableString(fullText)
            val startIndex = fullText.indexOf(highlightText)
            if (startIndex >= 0) {
                val endIndex = startIndex + highlightText.length
                // 设置红色
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.error)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置字体大小（增大50%）
                spannable.setSpan(
                    RelativeSizeSpan(1.5f),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置加粗
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.text = spannable
            }
        }
        
        // 汤饭组：领取食材
        binding.groupSoupRiceDutyText?.let { textView ->
            val fullText = textView.text.toString()
            val highlightText = "领取食材"
            val spannable = SpannableString(fullText)
            val startIndex = fullText.indexOf(highlightText)
            if (startIndex >= 0) {
                val endIndex = startIndex + highlightText.length
                // 设置红色
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.error)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置字体大小（增大50%）
                spannable.setSpan(
                    RelativeSizeSpan(1.5f),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置加粗
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.text = spannable
            }
        }
        
        // 生火组：领取野炊用柴
        binding.groupFireDutyText?.let { textView ->
            val fullText = textView.text.toString()
            val highlightText = "领取野炊用柴"
            val spannable = SpannableString(fullText)
            val startIndex = fullText.indexOf(highlightText)
            if (startIndex >= 0) {
                val endIndex = startIndex + highlightText.length
                // 设置红色
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.error)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置字体大小（增大50%）
                spannable.setSpan(
                    RelativeSizeSpan(1.5f),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置加粗
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.text = spannable
            }
        }
    }
    
    /**
     * 设置成员列表
     */
    private fun setupMembers() {
        val savedInfo = teamInfoManager.loadTeamInfo()
        if (savedInfo != null && savedInfo.memberNames.isNotBlank()) {
            // 解析成员姓名（使用"、"分隔）
            allMembers.clear()
            allMembers.addAll(savedInfo.memberNames.split("、").filter { it.isNotBlank() })
        }
        
        // 显示成员列表
        displayMembers()
    }
    
    /**
     * 显示成员列表（左侧）
     */
    private fun displayMembers() {
        binding.membersContainer.removeAllViews()
        
        // 获取已分配的人员
        val assignedMembers = groupLeader + groupCooking + groupSoupRice + groupFire + groupHealth
        
        // 显示未分配的人员
        val unassignedMembers = allMembers.filter { it !in assignedMembers }
        
        if (unassignedMembers.isEmpty() && allMembers.isEmpty()) {
            val hintText = TextView(this).apply {
                text = "暂无成员信息\n请返回填写团队信息"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@TeamDivisionActivity, R.color.subtitle_color))
                setPadding(32, 32, 32, 32)
                gravity = android.view.Gravity.CENTER
            }
            binding.membersContainer.addView(hintText)
            return
        }
        
        unassignedMembers.forEach { memberName ->
            val memberCard = createMemberCard(memberName)
            binding.membersContainer.addView(memberCard)
        }
        
        // 更新各小组显示
        updateGroupDisplay()
    }
    
    /**
     * 创建成员卡片（可拖拽 + 点击选择）
     */
    private fun createMemberCard(memberName: String): View {
        // 使用LinearLayout包含成员名和选择按钮
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            background = ContextCompat.getDrawable(this@TeamDivisionActivity, R.drawable.member_card_background)
        }
        
        // 成员名（可拖拽）
        val nameView = TextView(this).apply {
            text = memberName
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@TeamDivisionActivity, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            
            // 设置可拖拽
            setOnLongClickListener { view ->
                val dragShadow = View.DragShadowBuilder(view)
                val data = android.content.ClipData.newPlainText("member", memberName)
                view.startDragAndDrop(data, dragShadow, view, 0)
                view.alpha = 0.5f
                true
            }
        }
        
        // 快速选择按钮
        val selectButton = MaterialButton(this).apply {
            text = "选择"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            minimumWidth = 0
            minimumHeight = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
            setBackgroundColor(ContextCompat.getColor(this@TeamDivisionActivity, R.color.fire_orange))
            setTextColor(ContextCompat.getColor(this@TeamDivisionActivity, android.R.color.white))
            
            setOnClickListener {
                showGroupSelectionDialog(memberName)
            }
        }
        
        container.addView(nameView)
        container.addView(selectButton)
        
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_normal)
        }
        container.layoutParams = layoutParams
        
        return container
    }
    
    /**
     * 显示组选择对话框
     */
    private fun showGroupSelectionDialog(memberName: String) {
        val groups = listOf(
            "项目组长" to ::groupLeader,
            "烹饪组" to ::groupCooking,
            "汤饭组" to ::groupSoupRice,
            "生火组" to ::groupFire,
            "卫生组" to ::groupHealth
        )
        
        val groupNames = groups.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("将 $memberName 分配到")
            .setItems(groupNames) { _, which ->
                val (_, groupGetter) = groups[which]
                val targetGroup = groupGetter()
                
                // 从其他组中移除
                groupLeader.remove(memberName)
                groupCooking.remove(memberName)
                groupSoupRice.remove(memberName)
                groupFire.remove(memberName)
                groupHealth.remove(memberName)
                
                // 添加到目标组
                if (memberName !in targetGroup) {
                    targetGroup.add(memberName)
                }
                
                // 更新显示
                displayMembers()
                
                // 检查分配是否合理
                checkDivisionValidity()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 设置拖拽功能
     */
    private fun setupDragAndDrop() {
        val dragListeners = listOf(
            binding.groupLeaderContainer to ::groupLeader,
            binding.groupCookingContainer to ::groupCooking,
            binding.groupSoupRiceContainer to ::groupSoupRice,
            binding.groupFireContainer to ::groupFire,
            binding.groupHealthContainer to ::groupHealth
        )
        
        dragListeners.forEach { (container, groupGetter) ->
            container.setOnDragListener { view, event ->
                handleDragEvent(view, event, groupGetter())
            }
        }
    }
    
    /**
     * 处理拖拽事件
     */
    private fun handleDragEvent(view: View, event: DragEvent, targetGroup: MutableList<String>): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                view.alpha = 0.7f
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.alpha = 1.0f
                view.background = ContextCompat.getDrawable(this, R.drawable.group_drop_highlight)
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                view.alpha = 0.7f
                view.background = ContextCompat.getDrawable(this, R.drawable.group_background)
                return true
            }
            DragEvent.ACTION_DROP -> {
                val memberName = event.clipData.getItemAt(0).text.toString()
                
                // 从其他组中移除
                groupLeader.remove(memberName)
                groupCooking.remove(memberName)
                groupSoupRice.remove(memberName)
                groupFire.remove(memberName)
                groupHealth.remove(memberName)
                
                // 添加到目标组
                if (memberName !in targetGroup) {
                    targetGroup.add(memberName)
                }
                
                // 恢复拖拽源视图
                event.localState?.let { sourceView ->
                    if (sourceView is View) {
                        sourceView.alpha = 1.0f
                        sourceView.visibility = View.VISIBLE
                    }
                }
                
                // 更新显示
                displayMembers()
                
                // 检查分配是否合理
                checkDivisionValidity()
                
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.alpha = 1.0f
                view.background = ContextCompat.getDrawable(this, R.drawable.group_background)
                // 无论拖拽是否成功，都要恢复源视图的可见性
                event.localState?.let { sourceView ->
                    if (sourceView is View) {
                        sourceView.visibility = View.VISIBLE
                        // 如果拖拽失败，重新显示成员列表
                        if (!event.result) {
                            displayMembers()
                        }
                    }
                }
                return true
            }
        }
        return false
    }
    
    /**
     * 更新各小组显示
     */
    private fun updateGroupDisplay() {
        updateGroupContainer(binding.groupLeaderMembersContainer, groupLeader, 1, R.id.groupLeaderCount, R.id.groupLeaderProgress)
        updateGroupContainer(binding.groupCookingMembersContainer, groupCooking, 3, R.id.groupCookingCount, R.id.groupCookingProgress)
        updateGroupContainer(binding.groupSoupRiceMembersContainer, groupSoupRice, 2, R.id.groupSoupRiceCount, R.id.groupSoupRiceProgress)
        updateGroupContainer(binding.groupFireMembersContainer, groupFire, 2, R.id.groupFireCount, R.id.groupFireProgress)
        updateGroupContainer(binding.groupHealthMembersContainer, groupHealth, 2, R.id.groupHealthCount, R.id.groupHealthProgress)
    }
    
    /**
     * 更新单个小组容器
     */
    private fun updateGroupContainer(
        membersContainer: ViewGroup,
        members: List<String>,
        requiredCount: Int,
        countTextViewId: Int,
        progressViewId: Int
    ) {
        // 更新人数显示（格式：已分配/总需求）
        val countTextView = findViewById<TextView>(countTextViewId)
        countTextView?.text = "${members.size}/$requiredCount"
        
        // 更新进度条（使用post确保布局完成后再计算）
        val progressView = findViewById<View>(progressViewId)
        progressView?.let { progress ->
            progress.post {
                val progressBar = progress.parent as? ViewGroup
                progressBar?.let { bar ->
                    val maxWidth = bar.width
                    if (maxWidth > 0 && requiredCount > 0) {
                        val progressWidth = (maxWidth * members.size / requiredCount.toFloat()).toInt()
                        val layoutParams = progress.layoutParams
                        layoutParams.width = progressWidth.coerceAtMost(maxWidth).coerceAtLeast(0)
                        progress.layoutParams = layoutParams
                    }
                }
            }
        }
        
        // 清除现有成员卡片
        membersContainer.removeAllViews()
        
        // 如果容器是横向LinearLayout，需要处理换行
        if (membersContainer is LinearLayout && membersContainer.orientation == LinearLayout.HORIZONTAL) {
            // 创建换行容器（垂直布局，包含多个横向行）
            val rowsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            membersContainer.addView(rowsContainer)
            
            var currentRow: LinearLayout? = null
            
            members.forEach { memberName ->
                val memberCard = createAssignedMemberCard(memberName) { member ->
                    // 点击移除
                    groupLeader.remove(member)
                    groupCooking.remove(member)
                    groupSoupRice.remove(member)
                    groupFire.remove(member)
                    groupHealth.remove(member)
                    displayMembers()
                }
                
                // 创建新行或添加到当前行
                if (currentRow == null) {
                    currentRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 8
                        }
                    }
                    rowsContainer.addView(currentRow)
                }
                
                currentRow!!.addView(memberCard)
            }
        } else {
            // 垂直布局，直接添加
            members.forEach { memberName ->
                val memberCard = createAssignedMemberCard(memberName) { member ->
                    // 点击移除
                    groupLeader.remove(member)
                    groupCooking.remove(member)
                    groupSoupRice.remove(member)
                    groupFire.remove(member)
                    groupHealth.remove(member)
                    displayMembers()
                }
                membersContainer.addView(memberCard)
            }
        }
    }
    
    /**
     * 创建已分配的成员卡片（可点击移除）
     */
    private fun createAssignedMemberCard(memberName: String, onRemove: (String) -> Unit): View {
        val card = TextView(this).apply {
            text = memberName
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@TeamDivisionActivity, R.color.white))
            setPadding(16, 12, 16, 12)
            background = ContextCompat.getDrawable(this@TeamDivisionActivity, R.drawable.assigned_member_background)
            gravity = android.view.Gravity.CENTER
            
            setOnClickListener {
                onRemove(memberName)
            }
        }
        
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 8
            bottomMargin = 8
        }
        card.layoutParams = layoutParams
        
        return card
    }
    
    /**
     * 检查分配是否合理
     */
    private fun checkDivisionValidity() {
        val totalAssigned = groupLeader.size + groupCooking.size + groupSoupRice.size + groupFire.size + groupHealth.size
        
        // 检查是否所有人都已分配
        if (totalAssigned == allMembers.size) {
            // 检查各小组人数是否符合要求
            val warnings = mutableListOf<String>()
            
            if (groupLeader.size != 1) {
                warnings.add("项目组长应为1人，当前${groupLeader.size}人")
            }
            if (groupCooking.size != 3) {
                warnings.add("烹饪组应为3人，当前${groupCooking.size}人")
            }
            if (groupSoupRice.size != 2) {
                warnings.add("汤饭组应为2人，当前${groupSoupRice.size}人")
            }
            if (groupFire.size != 2) {
                warnings.add("生火组应为2人，当前${groupFire.size}人")
            }
            if (groupHealth.size != 2) {
                warnings.add("卫生组应为2人，当前${groupHealth.size}人")
            }
            
            if (warnings.isEmpty()) {
                binding.validityText.text = "✓ 分配合理，可以开始野炊！"
                binding.validityText.setTextColor(ContextCompat.getColor(this, R.color.nature_green))
            } else {
                binding.validityText.text = warnings.joinToString("\n")
                binding.validityText.setTextColor(ContextCompat.getColor(this, R.color.fire_red))
            }
        } else {
            binding.validityText.text = "还有 ${allMembers.size - totalAssigned} 人未分配"
            binding.validityText.setTextColor(ContextCompat.getColor(this, R.color.subtitle_color))
        }
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 重置按钮
        binding.resetButton.setOnClickListener {
            groupLeader.clear()
            groupCooking.clear()
            groupSoupRice.clear()
            groupFire.clear()
            groupHealth.clear()
            displayMembers()
            binding.validityText.text = ""
        }
        
        // 保存按钮
        binding.saveButton.setOnClickListener {
            saveDivision()
            Toast.makeText(this, "分工信息已保存", Toast.LENGTH_SHORT).show()
            // 延迟一下确保数据已保存，然后发送数据到服务器
            binding.root.postDelayed({
                submitDataToServer()
            }, 200)
        }
        
        // 开始野炊按钮
        binding.nextButton.setOnClickListener {
            val totalAssigned = groupLeader.size + groupCooking.size + groupSoupRice.size + groupFire.size + groupHealth.size
            if (totalAssigned != allMembers.size) {
                Toast.makeText(this, "请先完成所有人员分配", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 保存分工信息
            saveDivision()
            
            // 延迟一下确保数据已保存，然后发送数据到服务器
            binding.root.postDelayed({
                submitDataToServer()
            }, 200)
            
            // 跳转到导航页面（微视频，过程评价）
            val teamName = intent.getStringExtra("teamName") ?: binding.teamNameText.text.toString()
            val intent = Intent(this, NavigationActivity::class.java)
            intent.putExtra("teamName", teamName)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
    
    /**
     * 保存分工信息
     */
    private fun saveDivision() {
        val division = mapOf(
            "groupLeader" to groupLeader.joinToString("、"),
            "groupCooking" to groupCooking.joinToString("、"),
            "groupSoupRice" to groupSoupRice.joinToString("、"),
            "groupFire" to groupFire.joinToString("、"),
            "groupHealth" to groupHealth.joinToString("、")
        )
        
        android.util.Log.d("TeamDivision", "保存分工信息: $division")
        
        val prefs = getSharedPreferences("team_division_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            division.forEach { (key, value) ->
                putString(key, value)
                android.util.Log.d("TeamDivision", "保存 $key = $value")
            }
            commit() // 使用 commit() 确保同步保存
        }
        
        // 验证保存是否成功
        val savedLeader = prefs.getString("groupLeader", "")
        android.util.Log.d("TeamDivision", "验证保存结果 - groupLeader: $savedLeader")
    }
    
    /**
     * 加载保存的分工信息
     */
    private fun loadSavedDivision() {
        val prefs = getSharedPreferences("team_division_prefs", MODE_PRIVATE)
        
        groupLeader.clear()
        groupCooking.clear()
        groupSoupRice.clear()
        groupFire.clear()
        groupHealth.clear()
        
        prefs.getString("groupLeader", "")?.takeIf { it.isNotBlank() }?.let {
            groupLeader.addAll(it.split("、").filter { name -> name.isNotBlank() })
        }
        prefs.getString("groupCooking", "")?.takeIf { it.isNotBlank() }?.let {
            groupCooking.addAll(it.split("、").filter { name -> name.isNotBlank() })
        }
        prefs.getString("groupSoupRice", "")?.takeIf { it.isNotBlank() }?.let {
            groupSoupRice.addAll(it.split("、").filter { name -> name.isNotBlank() })
        }
        prefs.getString("groupFire", "")?.takeIf { it.isNotBlank() }?.let {
            groupFire.addAll(it.split("、").filter { name -> name.isNotBlank() })
        }
        prefs.getString("groupHealth", "")?.takeIf { it.isNotBlank() }?.let {
            groupHealth.addAll(it.split("、").filter { name -> name.isNotBlank() })
        }
        
        displayMembers()
    }
    
    /**
     * 提交数据到服务器
     */
    private fun submitDataToServer() {
        val teamInfo = teamInfoManager.loadTeamInfo()
        if (teamInfo == null || !teamInfo.isValid()) {
            return
        }
        
        // 提交完整数据（包含团队信息和当前已有的过程记录、总结等）
        dataSubmitManager.submitAllData(
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "✅ 数据已发送到服务器", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "⚠️ 发送失败: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
    
    /**
     * 设置服务器设置按钮
     */
    private fun setupServerSettingsButton() {
        binding.serverSettingsButton.setOnClickListener {
            showServerSettingsDialog()
        }
    }
    
    /**
     * 显示服务器设置对话框
     */
    private fun showServerSettingsDialog() {
        val serverConfig = com.campcooking.ar.utils.ServerConfigManager(this)
        val currentIp = serverConfig.getServerIp()
        val currentPort = serverConfig.getServerPort()
        
        // 创建对话框视图
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_settings, null)
        val ipInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.serverIpInput)
        val portInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.serverPortInput)
        
        // 设置当前值
        ipInput?.setText(currentIp)
        portInput?.setText(currentPort.toString())
        
        AlertDialog.Builder(this)
            .setTitle("服务器设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newIp = ipInput?.text?.toString()?.trim() ?: ""
                val newPortStr = portInput?.text?.toString()?.trim() ?: ""
                
                // 验证IP地址
                if (!serverConfig.isValidIp(newIp)) {
                    Toast.makeText(this, "IP地址格式不正确", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 验证端口
                val newPort = try {
                    newPortStr.toInt()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "端口号必须是数字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (!serverConfig.isValidPort(newPort)) {
                    Toast.makeText(this, "端口号必须在1-65535之间", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 保存配置
                serverConfig.saveServerConfig(newIp, newPort)
                Toast.makeText(this, "✅ 服务器设置已保存\n地址: http://$newIp:$newPort", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onBackPressed() {
        finish()
    }
}

