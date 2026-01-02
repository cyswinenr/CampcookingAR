package com.campcooking.ar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.ar.databinding.ActivityNavigationBinding

/**
 * 导航页Activity
 * 显示四个功能入口：团队信息登记、团队分工、微课视频、过程记录
 * 四个图标横向排列在一行，每个图标都有高级的渐变背景和阴影效果
 */
class NavigationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNavigationBinding
    
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
        
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取传递的团队信息
        val teamName = intent.getStringExtra("teamName") ?: "野炊小组"
        binding.teamNameText.text = teamName
        
        setupListeners()
        setupServerSettingsButton()
    }
    
    /**
     * 设置点击事件
     */
    private fun setupListeners() {
        // 返回按钮点击
        binding.backButton.setOnClickListener {
            finish()  // 返回到团队信息输入页面
        }
        
        // 团队信息登记按钮点击
        binding.teamInfoButton?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        // 团队分工按钮点击
        binding.teamDivisionButton?.setOnClickListener {
            val intent = Intent(this, TeamDivisionActivity::class.java)
            intent.putExtra("teamName", binding.teamNameText.text.toString())
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        // 微课视频区域点击
        binding.videoSection.setOnClickListener {
            val intent = Intent(this, VideoActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        // 过程记录区域点击
        binding.recordSection.setOnClickListener {
            val intent = Intent(this, RecordActivity::class.java)
            intent.putExtra("teamName", binding.teamNameText.text.toString())
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        // 课后总结区域点击
        binding.summarySection.setOnClickListener {
            val intent = Intent(this, SummaryActivity::class.java)
            intent.putExtra("teamName", binding.teamNameText.text.toString())
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
    
    /**
     * 设置服务器设置按钮
     */
    private fun setupServerSettingsButton() {
        // 先尝试使用 ViewBinding
        var button = binding.serverSettingsButton
        
        // 如果 ViewBinding 找不到，尝试通过 findViewById 查找
        if (button == null) {
            button = findViewById(R.id.serverSettingsButton)
            if (button != null) {
                android.util.Log.d("NavigationActivity", "通过 findViewById 找到服务器设置按钮")
            }
        }
        
        // 如果还是找不到，尝试通过 topBar 查找
        if (button == null) {
            val topBar = binding.topBar
            if (topBar != null) {
                button = topBar.findViewById(R.id.serverSettingsButton)
                if (button != null) {
                    android.util.Log.d("NavigationActivity", "通过 topBar 找到服务器设置按钮")
                }
            }
        }
        
        if (button != null) {
            // 确保按钮可见
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                showServerSettingsDialog()
            }
            android.util.Log.d("NavigationActivity", "服务器设置按钮已成功设置")
        } else {
            android.util.Log.e("NavigationActivity", "服务器设置按钮未找到！请检查布局文件。")
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
        
        androidx.appcompat.app.AlertDialog.Builder(this)
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
                Toast.makeText(this, "✅ 服务器设置已保存\n地址: http://$newIp:$newPort\n所有页面将使用新地址", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onBackPressed() {
        // 返回到团队信息输入页面
        finish()
    }
}

