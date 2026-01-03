package com.campcooking.teacher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.teacher.databinding.ActivitySplashBinding

/**
 * 野炊教学应用封面页 - 教师端
 * 专为10-11寸平板横向使用设计
 */
class SplashActivity : AppCompatActivity() {
    
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
        
        // 点击"设置"按钮打开服务器地址设置
        binding.settingsButton.setOnClickListener {
            showServerSettingsDialog()
        }
    }
    
    /**
     * 显示服务器设置对话框
     */
    private fun showServerSettingsDialog() {
        val currentUrl = getServerUrl()
        val (currentHost, currentPort) = parseServerUrl(currentUrl)
        
        // 创建容器布局
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        
        // 网址标签
        val hostLabel = TextView(this).apply {
            text = "服务器网址（IP地址或域名）"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        
        // 网址输入框
        val hostInput = EditText(this).apply {
            setText(currentHost)
            hint = "例如: 192.168.1.100"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(16, 16, 16, 16)
        }
        
        // 端口标签
        val portLabel = TextView(this).apply {
            text = "端口号"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        
        // 端口输入框
        val portInput = EditText(this).apply {
            setText(currentPort)
            hint = "例如: 5000"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(16, 16, 16, 16)
        }
        
        // 添加到容器
        container.addView(hostLabel)
        container.addView(hostInput)
        container.addView(portLabel)
        container.addView(portInput)
        
        // 创建对话框
        AlertDialog.Builder(this)
            .setTitle("设置服务器地址")
            .setMessage("当前地址: $currentUrl")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val host = hostInput.text.toString().trim()
                val port = portInput.text.toString().trim()
                
                if (host.isEmpty()) {
                    Toast.makeText(this, "请输入服务器网址", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (port.isEmpty()) {
                    Toast.makeText(this, "请输入端口号", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 验证端口号
                val portNum = port.toIntOrNull()
                if (portNum == null || portNum < 1 || portNum > 65535) {
                    Toast.makeText(this, "请输入有效的端口号（1-65535）", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 组合URL
                val newUrl = "http://$host:$port"
                saveServerUrl(newUrl)
                Toast.makeText(this, "服务器地址已更新: $newUrl", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("使用默认") { _, _ ->
                val defaultUrl = "http://192.168.1.100:5000"
                saveServerUrl(defaultUrl)
                Toast.makeText(this, "已恢复默认地址: $defaultUrl", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    /**
     * 解析服务器URL，返回（网址, 端口）
     */
    private fun parseServerUrl(url: String): Pair<String, String> {
        return try {
            // 移除 http:// 或 https://
            var cleanUrl = url.replace(Regex("^https?://"), "")
            
            // 分离网址和端口
            if (cleanUrl.contains(":")) {
                val parts = cleanUrl.split(":")
                val host = parts[0]
                val port = parts[1].split("/")[0] // 移除路径部分
                Pair(host, port)
            } else {
                // 如果没有端口，使用默认端口
                val host = cleanUrl.split("/")[0]
                Pair(host, "5000")
            }
        } catch (e: Exception) {
            Pair("192.168.1.100", "5000")
        }
    }
    
    /**
     * 保存服务器地址
     */
    private fun saveServerUrl(url: String) {
        val prefs = getSharedPreferences("teacher_settings", MODE_PRIVATE)
        prefs.edit().putString("server_url", url).apply()
    }
    
    /**
     * 获取服务器地址
     */
    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("teacher_settings", MODE_PRIVATE)
        val savedUrl = prefs.getString("server_url", "")
        
        // 如果已保存地址，使用保存的地址
        if (savedUrl?.isNotEmpty() == true) {
            return savedUrl
        }
        
        // 否则使用默认地址
        return "http://192.168.1.100:5000"
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
        
        // 教师端标识延迟淡入
        Handler(Looper.getMainLooper()).postDelayed({
            binding.teacherBadge.apply {
                visibility = View.VISIBLE
                startAnimation(fadeIn)
            }
        }, 800)
        
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
    
    override fun onBackPressed() {
        // 在封面页按返回键直接退出应用
        super.onBackPressed()
        finishAffinity()
    }
}

