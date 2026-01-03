package com.campcooking.teacher

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campcooking.teacher.databinding.ActivityMainBinding

/**
 * 教师端主Activity
 * 使用WebView显示服务器管理界面
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWebView()
        setupListeners()
        loadServerUrl()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        binding.evaluationButton.setOnClickListener {
            val intent = android.content.Intent(this, EvaluationActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * 配置WebView
     */
    private fun setupWebView() {
        binding.webView.settings.apply {
            // 启用JavaScript
            javaScriptEnabled = true
            domStorageEnabled = true
            
            // 页面显示设置
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // 缩放设置
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(true)
            
            // 缓存设置
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            
            // 允许文件访问（用于上传）
            allowFileAccess = true
            allowContentAccess = true
        }
        
        // 设置WebViewClient，处理页面加载
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                binding.errorText.visibility = View.GONE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                binding.progressBar.visibility = View.GONE
                
                // 显示错误信息
                if (request?.isForMainFrame == true) {
                    binding.errorText.visibility = View.VISIBLE
                    binding.webView.visibility = View.GONE
                    
                    val errorMsg = error?.description?.toString() ?: "未知错误"
                    Toast.makeText(this@MainActivity, "加载失败: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // 设置WebChromeClient，处理进度和文件上传
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * 加载服务器URL
     */
    private fun loadServerUrl() {
        val serverUrl = getServerUrl()
        
        if (serverUrl.isNotEmpty()) {
            binding.webView.loadUrl(serverUrl)
        } else {
            // 如果没有配置服务器地址，显示提示
            binding.errorText.text = "请先配置服务器地址\n\n默认地址: http://192.168.1.100:5000"
            binding.errorText.visibility = View.VISIBLE
            binding.webView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        }
    }
    
    /**
     * 获取服务器地址
     * 可以从SharedPreferences读取，或使用默认值
     */
    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("teacher_settings", MODE_PRIVATE)
        val savedUrl = prefs.getString("server_url", "")
        
        // 如果已保存地址，使用保存的地址
        if (savedUrl?.isNotEmpty() == true) {
            return savedUrl
        }
        
        // 否则使用默认地址（可以根据实际情况修改）
        return "http://192.168.1.100:5000"
    }
    
    /**
     * 处理返回键
     */
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    /**
     * 释放WebView资源
     */
    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
