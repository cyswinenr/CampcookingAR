package com.campcooking.ar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.campcooking.ar.databinding.ActivitySplashBinding
import com.campcooking.ar.utils.DataCleaner
import com.campcooking.ar.utils.DataSubmitManager
import com.campcooking.ar.utils.ServerConfigManager
import com.campcooking.ar.utils.StoveNumberManager

/**
 * 野炊教学应用封面页
 * 专为10-11寸平板横向使用设计
 */
class SplashActivity : TouchCursorBaseActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏系统状态栏和导航栏，实现全屏效果
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupStoveNumberDisplay()
        startAnimations()
    }
    
    /**
     * 设置UI交互
     */
    private fun setupUI() {
        // 点击"进入应用"按钮进入主页
        binding.enterButton.setOnClickListener {
            navigateToMain()
        }
        
        // 点击垃圾桶图标清理数据
        binding.clearDataButton.setOnClickListener {
            showClearDataDialog()
        }
        
        // 网络设置按钮
        binding.networkSettingsButton.setOnClickListener {
            showServerSettingsDialog()
        }
        
        // 网络测试按钮
        binding.networkTestButton.setOnClickListener {
            testNetworkConnection()
        }
        
        // 炉号设置按钮
        binding.stoveSettingsButton.setOnClickListener {
            showStoveSettingsDialog()
        }
    }
    
    /**
     * 显示清理数据确认对话框
     */
    private fun showClearDataDialog() {
        val cleaner = DataCleaner(this)
        
        // 先显示清理模式选择对话框
        val options = arrayOf(
            "仅清理应用内数据（文件保留在平板）",
            "完全清理（删除用户拍摄的照片和视频）"
        )
        
        AlertDialog.Builder(this)
            .setTitle("🗑️ 选择清理方式")
            .setItems(options) { _, which ->
                val mode = when (which) {
                    0 -> DataCleaner.ClearMode.APP_ONLY
                    1 -> DataCleaner.ClearMode.FULL_DELETE
                    else -> DataCleaner.ClearMode.APP_ONLY
                }
                showConfirmDialog(cleaner, mode)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示确认清理对话框
     */
    private fun showConfirmDialog(cleaner: DataCleaner, mode: DataCleaner.ClearMode) {
        val dataSummary = cleaner.getDataSummary(mode)
        val modeText = when (mode) {
            DataCleaner.ClearMode.APP_ONLY -> "仅清理应用内数据"
            DataCleaner.ClearMode.FULL_DELETE -> "完全清理（删除用户文件）"
        }
        
        AlertDialog.Builder(this)
            .setTitle("🗑️ $modeText")
            .setMessage(dataSummary)
            .setPositiveButton("确定清理") { _, _ ->
                clearAllData(mode)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 执行清理所有数据
     */
    private fun clearAllData(mode: DataCleaner.ClearMode) {
        val cleaner = DataCleaner(this)
        val success = cleaner.clearAllData(mode)
        
        if (success) {
            val message = when (mode) {
                DataCleaner.ClearMode.APP_ONLY -> 
                    "✅ 应用数据已清理完成\n\n文件仍保留在平板中\n教学视频不会被删除"
                DataCleaner.ClearMode.FULL_DELETE -> 
                    "✅ 所有数据已清理完成，应用已复原\n\n用户拍摄的照片和视频已删除\n教学视频（Documents/CampcookingAR/Videos/）已保留"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "❌ 清理数据时出错，请重试", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 启动动画效果
     */
    private fun startAnimations() {
        // 淡入动画
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1500
        }
        
        // 主图片淡入
        binding.coverImage.startAnimation(fadeIn)
        
        // 标题缩放动画
        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_title_scale)
        binding.appTitle.startAnimation(scaleAnim)
        
        // 副标题延迟淡入
        Handler(Looper.getMainLooper()).postDelayed({
            binding.appSubtitle.apply {
                visibility = View.VISIBLE
                startAnimation(fadeIn)
            }
        }, 500)
        
        // 进入按钮延迟出现，带缩放和淡入效果
        Handler(Looper.getMainLooper()).postDelayed({
            binding.enterButton.apply {
                visibility = View.VISIBLE
                // 缩放+淡入组合动画
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .withEndAction {
                        // 动画结束后添加轻微的呼吸效果
                        animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(800)
                            .withEndAction {
                                animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(800)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
        }, 1500)
    }
    
    /**
     * 跳转到主页
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    /**
     * 显示服务器设置对话框
     */
    private fun showServerSettingsDialog() {
        val serverConfig = ServerConfigManager(this)
        val currentIp = serverConfig.getServerIp()
        val currentPort = serverConfig.getServerPort()
        
        // 创建对话框视图（使用首页专用布局）
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_settings_splash, null)
        val ipGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.serverIpGroup)
        val ipOption1 = dialogView.findViewById<android.widget.RadioButton>(R.id.serverIpOption1)
        val ipOption2 = dialogView.findViewById<android.widget.RadioButton>(R.id.serverIpOption2)
        val portInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.serverPortInput)
        
        // 设置当前选中的IP地址
        when (currentIp) {
            "192.168.3.17" -> ipOption1?.isChecked = true
            "172.16.70.101" -> ipOption2?.isChecked = true
            else -> {
                // 如果当前IP不在选项中，默认选择第一个
                ipOption1?.isChecked = true
            }
        }
        
        // 设置当前端口
        portInput?.setText(currentPort.toString())
        
        AlertDialog.Builder(this)
            .setTitle("网络设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 获取选中的IP地址
                val selectedIp = when (ipGroup?.checkedRadioButtonId) {
                    R.id.serverIpOption1 -> "192.168.3.17"
                    R.id.serverIpOption2 -> "172.16.70.101"
                    else -> {
                        Toast.makeText(this, "请选择服务器IP地址", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                
                val newPortStr = portInput?.text?.toString()?.trim() ?: ""
                
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
                serverConfig.saveServerConfig(selectedIp, newPort)
                Toast.makeText(this, "✅ 网络设置已保存\n地址: http://$selectedIp:$newPort", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 测试网络连接
     */
    private fun testNetworkConnection() {
        val serverConfig = ServerConfigManager(this)
        val dataSubmitManager = DataSubmitManager(this)
        
        val serverIp = serverConfig.getServerIp()
        val serverPort = serverConfig.getServerPort()
        val serverUrl = "http://$serverIp:$serverPort"
        
        // 显示测试中提示
        Toast.makeText(this, "正在测试连接...", Toast.LENGTH_SHORT).show()
        
        // 测试连接
        dataSubmitManager.testConnection(
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "✅ 连接成功！\n服务器地址: $serverUrl",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "❌ 连接失败\n$error\n\n请检查：\n1. 服务器是否启动\n2. IP地址和端口是否正确\n3. 设备是否连接到同一网络",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
    
    /**
     * 设置炉号显示
     * 如果已设置炉号，在右下角显示
     */
    private fun setupStoveNumberDisplay() {
        val stoveManager = StoveNumberManager(this)
        val stoveNumber = stoveManager.getStoveNumber()
        
        if (stoveNumber != null && stoveNumber.isNotBlank()) {
            binding.stoveNumberDisplay.text = stoveNumber
            binding.stoveNumberDisplay.visibility = View.VISIBLE
        } else {
            binding.stoveNumberDisplay.visibility = View.GONE
        }
    }
    
    /**
     * 显示炉号设置对话框
     */
    private fun showStoveSettingsDialog() {
        val stoveManager = StoveNumberManager(this)
        val currentStove = stoveManager.getStoveNumber()
        val isLocked = stoveManager.isStoveNumberLocked()
        
        // 创建对话框视图
        val dialogView = layoutInflater.inflate(R.layout.dialog_stove_settings, null)
        val stoveSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.stoveSpinner)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordInput)
        val passwordLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.passwordLayout)
        
        // 设置炉号下拉框
        val stoveAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.stoves,
            android.R.layout.simple_spinner_item
        )
        stoveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stoveSpinner.adapter = stoveAdapter
        
        // 如果已设置炉号，显示当前值并需要密码
        if (isLocked && currentStove != null) {
            val stoveArray = resources.getStringArray(R.array.stoves)
            val currentIndex = stoveArray.indexOf(currentStove)
            if (currentIndex >= 0) {
                stoveSpinner.setSelection(currentIndex)
            }
            passwordLayout.visibility = View.VISIBLE
            passwordLayout.hint = "请输入密码以修改炉号"
        } else {
            passwordLayout.visibility = View.GONE
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (isLocked) "修改炉号设置" else "设置炉号")
            .setMessage(if (isLocked) "炉号已锁定，需要密码才能修改" else "设置后炉号将被锁定，需要密码才能修改")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val selectedPosition = stoveSpinner.selectedItemPosition
                if (selectedPosition == 0) {
                    Toast.makeText(this, "请选择炉号", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val selectedStove = stoveSpinner.selectedItem.toString()
                val password = passwordInput?.text?.toString()?.trim() ?: ""
                
                if (isLocked) {
                    // 需要密码验证
                    if (password.isEmpty()) {
                        Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    if (stoveManager.updateStoveNumber(selectedStove, password)) {
                        Toast.makeText(this, "✅ 炉号已更新为: $selectedStove", Toast.LENGTH_LONG).show()
                        // 更新显示
                        setupStoveNumberDisplay()
                    } else {
                        Toast.makeText(this, "❌ 密码错误，修改失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 首次设置，不需要密码
                    stoveManager.setStoveNumber(selectedStove)
                    Toast.makeText(this, "✅ 炉号已设置为: $selectedStove\n\n已锁定，需要密码才能修改", Toast.LENGTH_LONG).show()
                    // 更新显示
                    setupStoveNumberDisplay()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton(if (isLocked) "清除设置" else null) { _, _ ->
                if (isLocked) {
                    showClearStoveDialog(stoveManager)
                }
            }
            .show()
    }
    
    /**
     * 显示清除炉号设置对话框
     */
    private fun showClearStoveDialog(stoveManager: StoveNumberManager) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordInput)
        
        AlertDialog.Builder(this)
            .setTitle("清除炉号设置")
            .setMessage("确定要清除炉号设置吗？清除后可以重新设置。")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val password = passwordInput?.text?.toString()?.trim() ?: ""
                if (password.isEmpty()) {
                    Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (stoveManager.clearStoveNumber(password)) {
                    Toast.makeText(this, "✅ 炉号设置已清除", Toast.LENGTH_SHORT).show()
                    // 更新显示
                    setupStoveNumberDisplay()
                } else {
                    Toast.makeText(this, "❌ 密码错误，清除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onBackPressed() {
        // 在封面页按返回键直接退出应用
        super.onBackPressed()
        finishAffinity()
    }
}

