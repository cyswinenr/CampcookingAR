package com.campcooking.ar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.ar.databinding.ActivityNavigationBinding

/**
 * 导航页Activity
 * 横向分为两部分：微课视频 和 过程记录
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
    }
    
    override fun onBackPressed() {
        // 返回到团队信息输入页面
        finish()
    }
}

