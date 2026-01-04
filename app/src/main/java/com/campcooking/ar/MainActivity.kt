package com.campcooking.ar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.ar.data.TeamInfo
import com.campcooking.ar.databinding.ActivityMainBinding
import com.campcooking.ar.utils.DataSubmitManager
import com.campcooking.ar.utils.TeamInfoManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

    /**
     * 主Activity - 团队信息采集页面（重新设计版本）
     * - 年级：下拉框（高一、高二）
     * - 班级：下拉框（1-25班）
     * - 炉号：纯数字
     * - 小组人数：1-12人
     * - 人员姓名：根据人数动态生成输入框
     * - 支持数据持久化保存
     */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val teamInfo = TeamInfo()
    private lateinit var teamInfoManager: TeamInfoManager
    private lateinit var dataSubmitManager: DataSubmitManager
    
    // 存储动态生成的姓名输入框
    private val memberNameInputs = mutableListOf<TextInputEditText>()
    
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
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化数据管理器
        teamInfoManager = TeamInfoManager(this)
        dataSubmitManager = DataSubmitManager(this)
        
        setupSpinners()
        setupListeners()
        setupServerSettingsButton()
        
        // 加载保存的数据
        loadSavedData()
    }
    
    /**
     * 设置下拉框
     */
    private fun setupSpinners() {
        // 设置年级下拉框
        val gradeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.grades,
            android.R.layout.simple_spinner_item
        )
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.gradeSpinner.adapter = gradeAdapter
        
        // 设置班级下拉框
        val classAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.classes,
            android.R.layout.simple_spinner_item
        )
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.classSpinner.adapter = classAdapter
        
        // 设置炉号下拉框
        val stoveAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.stoves,
            android.R.layout.simple_spinner_item
        )
        stoveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.stoveSpinner.adapter = stoveAdapter
        
        // 设置小组人数下拉框
        val memberCountAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.member_counts,
            android.R.layout.simple_spinner_item
        )
        memberCountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.memberCountSpinner.adapter = memberCountAdapter
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 小组人数下拉框监听 - 动态生成姓名输入框
        binding.memberCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // position 0 是"请选择人数"
                    val count = position // position 1 = 1人, position 2 = 2人, ...
                    generateMemberNameInputs(count)
                } else {
                    clearMemberNameInputs()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                clearMemberNameInputs()
            }
        }
        
        // 保存按钮点击事件 - 保存当前已填写的信息，不强制验证
        binding.saveButton.setOnClickListener {
            collectCurrentInfo()
            saveTeamInfo()
            // 发送团队信息到服务器
            submitTeamInfoToServer()
        }
        
        // 开始按钮点击事件
        binding.startButton.setOnClickListener {
            if (validateAndSaveInfo()) {
                saveTeamInfo()
                // 发送团队信息到服务器
                submitTeamInfoToServer()
                showConfirmDialog()
            }
        }
        
        // 重置按钮点击事件
        binding.resetButton.setOnClickListener {
            showResetDialog()
        }

        // 输入框焦点监听：adjustPan 模式下系统会自动处理滚动
        binding.schoolInput.setOnFocusChangeListener { _, hasFocus ->
            android.util.Log.d("MainActivity", "schoolInput focus changed: $hasFocus")
        }
    }
    
    /**
     * 获取选中的小组人数（从Spinner）
     */
    private fun getSelectedMemberCount(): Int {
        val position = binding.memberCountSpinner.selectedItemPosition
        return if (position > 0) position else 0 // position 1 = 1人, position 2 = 2人, ...
    }
    
    /**
     * 动态生成姓名输入框
     */
    private fun generateMemberNameInputs(count: Int) {
        // 清空现有输入框
        clearMemberNameInputs()
        
        // 隐藏提示文本
        binding.memberNamesHint.visibility = View.GONE
        
        // 生成指定数量的输入框
        for (i in 1..count) {
            // 创建TextInputLayout
            val textInputLayout = TextInputLayout(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                }
                hint = getString(R.string.member_label, i)
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(12f, 12f, 12f, 12f)
                setStartIconDrawable(android.R.drawable.ic_menu_edit)
                setStartIconTintList(getColorStateList(R.color.fire_red))
            }
            
            // 创建TextInputEditText
            val editText = TextInputEditText(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 18f
                setPadding(16, 16, 16, 16)
                maxLines = 1
                filters = arrayOf(android.text.InputFilter.LengthFilter(5))
            }
            
            // 将EditText添加到Layout中
            textInputLayout.addView(editText)
            
            // 将Layout添加到容器中
            binding.memberNamesContainer.addView(textInputLayout)
            
            // 保存EditText引用
            memberNameInputs.add(editText)
        }
    }
    
    /**
     * 清空姓名输入框
     */
    private fun clearMemberNameInputs() {
        binding.memberNamesContainer.removeAllViews()
        memberNameInputs.clear()
        
        // 重新添加提示文本
        binding.memberNamesHint.visibility = View.VISIBLE
        binding.memberNamesContainer.addView(binding.memberNamesHint)
    }

    /**
     * 滚动到指定视图，确保其在可见区域内
     * 使用更可靠的滚动算法
     */
    private fun scrollToViewSmoothly(view: View) {
        try {
            // 找到父ScrollView
            var parent = view.parent
            while (parent != null && parent !is ScrollView) {
                parent = parent.parent
            }

            if (parent is ScrollView) {
                // 获取输入框在屏幕上的位置
                val viewLocation = IntArray(2)
                view.getLocationOnScreen(viewLocation)

                // 获取ScrollView在屏幕上的位置
                val scrollViewLocation = IntArray(2)
                parent.getLocationOnScreen(scrollViewLocation)

                // 计算相对位置
                val viewTopRelativeToScrollView = viewLocation[1] - scrollViewLocation[1] + parent.scrollY
                val viewHeight = view.height
                val scrollViewHeight = parent.height

                // 计算目标滚动位置：将输入框滚动到可见区域的上半部分
                val targetScrollY = (viewTopRelativeToScrollView - scrollViewHeight / 3).coerceAtLeast(0)

                // 平滑滚动到目标位置
                parent.smoothScrollTo(0, targetScrollY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 滚动到指定视图，确保其在可见区域内
     */
    private fun scrollToView(view: View) {
        scrollToViewSmoothly(view)
    }

    /**
     * 收集当前已填写的信息（不验证，用于保存）
     */
    private fun collectCurrentInfo() {
        // 收集学校
        teamInfo.school = binding.schoolInput.text.toString().trim()
        
        // 收集年级
        val gradePosition = binding.gradeSpinner.selectedItemPosition
        teamInfo.grade = if (gradePosition > 0) {
            binding.gradeSpinner.selectedItem.toString()
        } else {
            ""
        }
        
        // 收集班级
        val classPosition = binding.classSpinner.selectedItemPosition
        teamInfo.className = if (classPosition > 0) {
            binding.classSpinner.selectedItem.toString()
        } else {
            ""
        }
        
        // 收集炉号
        val stovePosition = binding.stoveSpinner.selectedItemPosition
        teamInfo.stoveNumber = if (stovePosition > 0) {
            binding.stoveSpinner.selectedItem.toString()
        } else {
            ""
        }
        
        // 收集小组人数
        val position = binding.memberCountSpinner.selectedItemPosition
        teamInfo.memberCount = if (position > 0) position else 0
        
        // 收集人员姓名（收集所有已填写的，包括不完整的）
        val names = mutableListOf<String>()
        for (input in memberNameInputs) {
            val name = input.text.toString().trim()
            if (name.isNotBlank()) {
                names.add(name)
            }
        }
        teamInfo.memberNames = names.joinToString("、")
    }
    
    /**
     * 验证并保存信息
     */
    private fun validateAndSaveInfo(): Boolean {
        val missingFields = mutableListOf<String>()
        
        // 验证学校
        teamInfo.school = binding.schoolInput.text.toString().trim()
        if (teamInfo.school.isBlank()) {
            missingFields.add("学校")
        }
        
        // 验证年级
        val gradePosition = binding.gradeSpinner.selectedItemPosition
        if (gradePosition == 0) {
            missingFields.add("年级")
            teamInfo.grade = ""
        } else {
            teamInfo.grade = binding.gradeSpinner.selectedItem.toString()
        }
        
        // 验证班级
        val classPosition = binding.classSpinner.selectedItemPosition
        if (classPosition == 0) {
            missingFields.add("班级")
            teamInfo.className = ""
        } else {
            teamInfo.className = binding.classSpinner.selectedItem.toString()
        }
        
        // 验证炉号
        val stovePosition = binding.stoveSpinner.selectedItemPosition
        if (stovePosition == 0) {
            missingFields.add("炉号")
            teamInfo.stoveNumber = ""
        } else {
            teamInfo.stoveNumber = binding.stoveSpinner.selectedItem.toString()
        }
        
        // 验证小组人数
        val position = binding.memberCountSpinner.selectedItemPosition
        teamInfo.memberCount = if (position > 0) position else 0
        if (teamInfo.memberCount == 0) {
            missingFields.add("小组人数（1-12人）")
        }
        
        // 验证人员姓名
        val names = mutableListOf<String>()
        for ((index, input) in memberNameInputs.withIndex()) {
            val name = input.text.toString().trim()
            if (name.isBlank()) {
                missingFields.add("成员${index + 1}姓名")
            } else {
                names.add(name)
            }
        }
        teamInfo.memberNames = names.joinToString("、")
        
        // 如果有缺失字段，显示提示
        if (missingFields.isNotEmpty()) {
            val message = "请填写：${missingFields.joinToString("、")}"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return false
        }
        
        return true
    }
    
    /**
     * 显示确认对话框
     */
    private fun showConfirmDialog() {
        val message = """
            学校：${teamInfo.school}
            年级：${teamInfo.grade}
            班级：${teamInfo.className}
            炉号：${teamInfo.stoveNumber}
            人数：${teamInfo.memberCount}人
            成员：${teamInfo.memberNames}
            
            确认信息无误？
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("确认团队信息")
            .setMessage(message)
            .setPositiveButton("确认") { _, _ ->
                startCooking()
            }
            .setNegativeButton("修改", null)
            .show()
    }
    
    /**
     * 显示重置确认对话框
     */
    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("重置信息")
            .setMessage("确定要清空所有输入的信息吗？")
            .setPositiveButton("确定") { _, _ ->
                resetForm()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 重置表单
     */
    private fun resetForm() {
        // 清空所有输入
        binding.schoolInput.text?.clear()
        binding.gradeSpinner.setSelection(0)
        binding.classSpinner.setSelection(0)
        binding.stoveSpinner.setSelection(0)
        binding.memberCountSpinner.setSelection(0)
        
        // 清空姓名输入框
        clearMemberNameInputs()
        
        // 清空数据
        teamInfo.school = ""
        teamInfo.grade = ""
        teamInfo.className = ""
        teamInfo.stoveNumber = ""
        teamInfo.memberCount = 0
        teamInfo.memberNames = ""
        
        // 清除持久化数据
        teamInfoManager.clearTeamInfo()
        
        Toast.makeText(this, R.string.toast_reset_confirm, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 保存团队信息
     */
    private fun saveTeamInfo() {
        teamInfoManager.saveTeamInfo(teamInfo)
        Toast.makeText(this, "信息已保存", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 加载保存的数据
     */
    private fun loadSavedData() {
        val savedInfo = teamInfoManager.loadTeamInfo()
        if (savedInfo != null) {
            // 恢复学校
            binding.schoolInput.setText(savedInfo.school)
            
            // 恢复年级
            val gradeArray = resources.getStringArray(R.array.grades)
            val gradeIndex = gradeArray.indexOf(savedInfo.grade)
            if (gradeIndex >= 0) {
                binding.gradeSpinner.setSelection(gradeIndex)
            }
            
            // 恢复班级
            val classArray = resources.getStringArray(R.array.classes)
            val classIndex = classArray.indexOf(savedInfo.className)
            if (classIndex >= 0) {
                binding.classSpinner.setSelection(classIndex)
            }
            
            // 恢复炉号
            val stoveArray = resources.getStringArray(R.array.stoves)
            val stoveIndex = stoveArray.indexOf(savedInfo.stoveNumber)
            if (stoveIndex >= 0) {
                binding.stoveSpinner.setSelection(stoveIndex)
            }
            
            // 恢复小组人数和成员姓名
            if (savedInfo.memberCount > 0 && savedInfo.memberCount <= 12) {
                // Spinner的position从0开始，0是"请选择人数"，1是"1人"，所以memberCount直接对应position
                binding.memberCountSpinner.setSelection(savedInfo.memberCount, false) // false表示不触发监听器
                
                // 获取保存的成员姓名列表
                val namesList = teamInfoManager.getMemberNamesList()
                
                // 手动触发输入框生成（因为setSelection的第二个参数是false，不会触发监听器）
                if (savedInfo.memberCount > 0) {
                    generateMemberNameInputs(savedInfo.memberCount)
                    
                    // 等待输入框生成后填充数据
                    binding.memberCountSpinner.post {
                        // 再次等待确保输入框已完全生成
                        binding.memberCountSpinner.postDelayed({
                            if (namesList.isNotEmpty() && namesList.size <= memberNameInputs.size) {
                                namesList.forEachIndexed { index, name ->
                                    if (index < memberNameInputs.size) {
                                        memberNameInputs[index].setText(name)
                                    }
                                }
                            }
                        }, 150) // 延迟150ms确保输入框已生成
                    }
                }
            }
            
            Toast.makeText(this, "已恢复上次保存的信息", Toast.LENGTH_SHORT).show()
        } else {
            // 如果没有保存的数据，设置默认值
            setDefaultValues()
        }
    }
    
    /**
     * 设置默认值
     */
    private fun setDefaultValues() {
        // 设置默认学校名称
        binding.schoolInput.setText("黄埔区开元学校")
        
        // 设置默认年级为"高二"（索引2：0=请选择，1=高一，2=高二）
        binding.gradeSpinner.setSelection(2)
    }
    
    /**
     * 提交团队信息到服务器
     */
    private fun submitTeamInfoToServer() {
        // 确保团队信息有效
        if (!teamInfo.isValid()) {
            return
        }
        
        dataSubmitManager.submitTeamInfo(
            teamInfo = teamInfo,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "✅ 团队信息已发送到服务器", Toast.LENGTH_SHORT).show()
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
     * 进行团队分工
     */
    private fun startCooking() {
        Toast.makeText(
            this,
            "欢迎 ${teamInfo.getTeamName()}！开始进行团队分工...",
            Toast.LENGTH_SHORT
        ).show()
        
        // 跳转到团队分工页面
        val intent = Intent(this, TeamDivisionActivity::class.java)
        intent.putExtra("teamName", teamInfo.getTeamName())
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // 不调用 finish()，保留 MainActivity 在返回栈中
    }
    
    /**
     * 设置服务器设置按钮
     */
    private fun setupServerSettingsButton() {
        binding.serverSettingsButton?.setOnClickListener {
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
        AlertDialog.Builder(this)
            .setTitle("退出")
            .setMessage("确定要退出应用吗？")
            .setPositiveButton("确定") { _, _ ->
                super.onBackPressed()
                finishAffinity()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
