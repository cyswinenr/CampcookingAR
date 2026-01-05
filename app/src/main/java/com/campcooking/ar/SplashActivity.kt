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
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.ar.databinding.ActivitySplashBinding
import com.campcooking.ar.utils.DataCleaner

/**
 * 野炊教学应用封面页
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
        
        // 点击垃圾桶图标清理数据
        binding.clearDataButton.setOnClickListener {
            showClearDataDialog()
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
    
    override fun onBackPressed() {
        // 在封面页按返回键直接退出应用
        super.onBackPressed()
        finishAffinity()
    }
}

