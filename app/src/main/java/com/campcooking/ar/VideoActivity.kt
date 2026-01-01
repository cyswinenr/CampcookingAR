package com.campcooking.ar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.campcooking.ar.adapter.VideoListAdapter
import com.campcooking.ar.config.VideoConfig
import com.campcooking.ar.data.Video
import com.campcooking.ar.databinding.ActivityVideoBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.io.File

/**
 * 微课视频页面
 * 左侧显示视频列表，右侧播放视频
 * 
 * 视频文件存放位置：
 * 内部存储/Documents/CampcookingAR/Videos/
 */
class VideoActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoBinding
    private var player: ExoPlayer? = null
    private lateinit var videoAdapter: VideoListAdapter
    private val videoList = mutableListOf<Video>()
    private var currentVideo: Video? = null
    private var isFullScreen = false
    
    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，初始化视频列表
            setupVideoList()
        } else {
            // 权限被拒绝
            Toast.makeText(
                this,
                "需要存储权限才能播放视频\n请在设置中授予权限",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializePlayer()
        setupListeners()
        
        // 检查并请求权限
        checkAndRequestPermission()
    }
    
    /**
     * 检查并请求存储权限
     */
    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            // Android 12 及以下
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，直接设置视频列表
                setupVideoList()
            }
            else -> {
                // 请求权限
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.videoPlayer.player = player
        
        // 监听播放状态
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        Toast.makeText(
                            this@VideoActivity,
                            "视频播放完成",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 自动播放下一个视频
                        playNextVideo()
                    }
                    Player.STATE_IDLE -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
            
            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                Toast.makeText(
                    this@VideoActivity,
                    "视频加载失败：${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }
    
    /**
     * 设置视频列表
     */
    private fun setupVideoList() {
        // 检查并创建视频文件夹
        checkVideoFolder()
        
        // 从配置文件加载视频列表
        videoList.addAll(VideoConfig.getVideoList())
        
        // 设置RecyclerView适配器
        videoAdapter = VideoListAdapter(videoList) { video ->
            playVideo(video)
        }
        
        binding.videoRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@VideoActivity)
            adapter = videoAdapter
        }
        
        // 自动播放第一个视频
        if (videoList.isNotEmpty()) {
            playVideo(videoList[0])
        } else {
            // 如果没有视频，显示提示信息
            binding.videoTitle.text = "暂无视频"
            binding.videoDescription.text = "请将视频文件复制到：\nDocuments/CampcookingAR/Videos/\n并在 VideoConfig.kt 中配置视频信息"
        }
    }
    
    /**
     * 检查视频文件夹是否存在，不存在则创建
     */
    private fun checkVideoFolder() {
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val videoFolder = File(documentsDir, VideoConfig.VIDEO_FOLDER)
            
            if (!videoFolder.exists()) {
                if (videoFolder.mkdirs()) {
                    Toast.makeText(
                        this,
                        "已创建视频文件夹：\n${videoFolder.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "创建视频文件夹失败：${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * 播放视频
     */
    private fun playVideo(video: Video) {
        currentVideo = video
        
        // 更新视频信息显示
        binding.videoTitle.text = video.title
        binding.videoDuration.text = "时长：${video.duration}"
        binding.videoDescription.text = video.description
        binding.videoCategory.text = "分类：${video.category}"
        
        try {
            // 获取外部存储视频文件路径
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val videoFile = File(documentsDir, "${VideoConfig.VIDEO_FOLDER}/${video.fileName}")
            
            if (!videoFile.exists()) {
                // 视频文件不存在
                Toast.makeText(
                    this,
                    "视频文件未找到：\n${videoFile.absolutePath}\n\n请将视频文件复制到此位置",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressBar.visibility = View.GONE
                return
            }
            
            // 构建视频文件URI
            val videoUri = Uri.fromFile(videoFile)
            
            // 创建媒体项
            val mediaItem = MediaItem.fromUri(videoUri)
            
            // 设置视频源并播放
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            
            Toast.makeText(this, "正在播放：${video.title}", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "播放出错：${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
    
    /**
     * 播放下一个视频
     */
    private fun playNextVideo() {
        currentVideo?.let { current ->
            val currentIndex = videoList.indexOf(current)
            if (currentIndex >= 0 && currentIndex < videoList.size - 1) {
                val nextVideo = videoList[currentIndex + 1]
                playVideo(nextVideo)
                videoAdapter.setSelectedPosition(currentIndex + 1)
            } else {
                Toast.makeText(this, "已是最后一个视频", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            if (isFullScreen) {
                exitFullScreen()
            } else {
                finish()
            }
        }
        
        // 全屏按钮（正常模式）
        binding.fullscreenButton?.setOnClickListener {
            enterFullScreen()
        }
        
        // 全屏退出按钮
        val fullscreenExitButton = binding.root.findViewById<android.widget.ImageButton>(R.id.fullscreenExitButton)
        fullscreenExitButton?.setOnClickListener {
            exitFullScreen()
        }
    }
    
    /**
     * 进入全屏模式
     */
    private fun enterFullScreen() {
        isFullScreen = true
        
        // 设置真正的沉浸式全屏
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        // 隐藏主容器
        val mainContainer = binding.root.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainContainer)
        mainContainer?.visibility = View.GONE
        
        // 显示全屏容器
        val fullscreenContainer = binding.root.findViewById<android.widget.FrameLayout>(R.id.fullscreenContainer)
        fullscreenContainer?.visibility = View.VISIBLE
        
        // 将视频播放器移动到全屏容器
        (binding.videoPlayer.parent as? android.view.ViewGroup)?.removeView(binding.videoPlayer)
        fullscreenContainer?.addView(binding.videoPlayer, 0, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        Toast.makeText(this, "全屏模式 - 点击右下角按钮或按返回键退出", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 退出全屏模式
     */
    private fun exitFullScreen() {
        isFullScreen = false
        
        // 恢复系统UI
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        // 隐藏全屏容器
        val fullscreenContainer = binding.root.findViewById<android.widget.FrameLayout>(R.id.fullscreenContainer)
        fullscreenContainer?.let { container ->
            // 将视频播放器移回原位置
            container.removeView(binding.videoPlayer)
        }
        
        // 恢复到原始容器
        val videoPlayerContainer = binding.root.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.videoPlayerContainer)
        videoPlayerContainer?.let { parent ->
            val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = binding.videoInfoSection?.id ?: androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                matchConstraintPercentHeight = 0.65f
            }
            parent.addView(binding.videoPlayer, 0, layoutParams)
        }
        
        fullscreenContainer?.visibility = View.GONE
        
        // 显示主容器
        val mainContainer = binding.root.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainContainer)
        mainContainer?.visibility = View.VISIBLE
        
        Toast.makeText(this, "退出全屏", Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        if (isFullScreen) {
            exitFullScreen()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onStop() {
        super.onStop()
        player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

