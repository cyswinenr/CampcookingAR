package com.campcooking.ar

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campcooking.ar.adapter.VideoListAdapter
import com.campcooking.ar.config.VideoConfig
import com.campcooking.ar.data.Video
import com.campcooking.ar.databinding.ActivityVideoBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.RawResourceDataSource

/**
 * 微课视频页面
 * 左侧显示视频列表，右侧播放视频
 */
class VideoActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoBinding
    private var player: ExoPlayer? = null
    private lateinit var videoAdapter: VideoListAdapter
    private val videoList = mutableListOf<Video>()
    private var currentVideo: Video? = null
    private var isFullScreen = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializePlayer()
        setupVideoList()
        setupListeners()
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
            binding.videoDescription.text = "请在 res/raw/ 文件夹中添加视频文件，\n并在 VideoConfig.kt 中配置视频信息"
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
            // 获取本地视频资源ID
            val resourceId = resources.getIdentifier(
                video.resourceName,
                "raw",
                packageName
            )
            
            if (resourceId == 0) {
                // 资源未找到
                Toast.makeText(
                    this,
                    "视频文件未找到：${video.resourceName}.mp4\n请将视频文件放入 res/raw/ 文件夹",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressBar.visibility = View.GONE
                return
            }
            
            // 构建本地资源URI
            val videoUri = RawResourceDataSource.buildRawResourceUri(resourceId)
            
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
        
        // 全屏按钮
        binding.fullscreenButton?.setOnClickListener {
            if (isFullScreen) {
                exitFullScreen()
            } else {
                enterFullScreen()
            }
        }
    }
    
    /**
     * 进入全屏模式
     */
    private fun enterFullScreen() {
        isFullScreen = true
        
        // 隐藏系统UI
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // 隐藏其他UI元素
        binding.topBar?.visibility = View.GONE
        binding.videoListCard?.visibility = View.GONE
        binding.videoInfoSection?.visibility = View.GONE
        
        // 更新全屏按钮图标
        binding.fullscreenButton?.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        
        Toast.makeText(this, "全屏模式", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 退出全屏模式
     */
    private fun exitFullScreen() {
        isFullScreen = false
        
        // 恢复系统UI
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // 显示其他UI元素
        binding.topBar?.visibility = View.VISIBLE
        binding.videoListCard?.visibility = View.VISIBLE
        binding.videoInfoSection?.visibility = View.VISIBLE
        
        // 更新全屏按钮图标
        binding.fullscreenButton?.setImageResource(android.R.drawable.ic_menu_crop)
        
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

