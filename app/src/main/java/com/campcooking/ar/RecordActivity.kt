package com.campcooking.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.content.res.ColorStateList
import com.campcooking.ar.adapter.StageListAdapter
import com.campcooking.ar.config.RecordConfig
import com.campcooking.ar.data.CookingStage
import com.campcooking.ar.data.ProcessRecord
import com.campcooking.ar.data.TeamInfo
import com.campcooking.ar.databinding.ActivityRecordBinding
import com.campcooking.ar.utils.ProcessRecordManager
import com.campcooking.ar.utils.DataSubmitManager
import com.campcooking.ar.utils.TeamInfoManager
import com.google.android.material.chip.Chip
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 过程记录Activity - 学生端
 * 
 * 功能：
 * 1. 流程节点管理（准备、生火、煮饭、炒菜、完成）
 * 2. 拍照记录
 * 3. 自我评价（星级+标签）
 * 4. 自动计时
 * 5. 数据持久化
 */
class RecordActivity : TouchCursorBaseActivity() {

    private lateinit var binding: ActivityRecordBinding
    private lateinit var processRecord: ProcessRecord
    private lateinit var processRecordManager: ProcessRecordManager
    private lateinit var dataSubmitManager: DataSubmitManager
    private lateinit var teamInfoManager: TeamInfoManager
    
    // 适配器
    private lateinit var stageListAdapter: StageListAdapter
    
    // 拍照相关
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // 录像相关
    private var currentVideoUri: Uri? = null
    private var currentVideoPath: String? = null

    // 定时器（用于更新用时显示）
    private var timer: Timer? = null
    
    // 上传相关
    private var uploadProgressDialog: android.app.ProgressDialog? = null
    private var isUploading = false

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 100      // 一次性请求所有权限
        private const val REQUEST_TAKE_PHOTO = 101
        private const val REQUEST_TAKE_VIDEO = 102
    }

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

        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化数据管理器
        processRecordManager = ProcessRecordManager(this)
        dataSubmitManager = DataSubmitManager(this)
        teamInfoManager = TeamInfoManager(this)

        // 获取团队信息，只显示炉号
        val teamInfo = teamInfoManager.loadTeamInfo()
        val stoveNumber = teamInfo?.stoveNumber ?: ""
        binding.teamNameText.text = if (stoveNumber.isNotEmpty()) stoveNumber else "未设置炉号"

        // 初始化或加载记录
        initializeProcessRecord()

        // 设置UI
        setupStagesList()
        setupPhotosList()
        setupListeners()
        updateCurrentStageUI()

        // 启动定时器
        startTimer()

        // 请求所有必需的权限
        requestAllRequiredPermissions()
    }

    /**
     * 请求所有必需的权限（在页面启动时一次性请求）
     */
    private fun requestAllRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // 检查存储权限（Android 10及以下需要）
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // 如果有需要请求的权限，一次性请求
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_ALL_PERMISSIONS
            )
        }
    }
    
    /**
     * 初始化过程记录
     */
    private fun initializeProcessRecord() {
        // 尝试加载已保存的记录
        val savedRecord = processRecordManager.loadProcessRecord()
        
        processRecord = if (savedRecord != null) {
            Toast.makeText(this, "已恢复记录，继续上次的进度", Toast.LENGTH_SHORT).show()
            savedRecord
        } else {
            // 创建新记录
            val teamInfo = TeamInfo().apply {
                // 这里可以从Intent获取团队信息
                memberNames = intent.getStringExtra("teamName") ?: "野炊小组"
            }
            ProcessRecord(teamInfo = teamInfo).apply {
                // 自动开始第一个阶段
                startStage(CookingStage.PREPARATION)
            }
        }
    }
    
    /**
     * 设置流程节点列表
     */
    private fun setupStagesList() {
        stageListAdapter = StageListAdapter(
            stages = CookingStage.getAllStages(),
            processRecord = processRecord,
            onStageClick = { stage ->
                // 直接切换到选中的阶段，不弹出确认框
                if (processRecord.currentStage != stage) {
                    processRecord.currentStage = stage
                    if (!processRecord.stages.containsKey(stage)) {
                        processRecord.startStage(stage)
                    }
                    saveRecord()
                    updateCurrentStageUI()
                }
            }
        )
        
        binding.stagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecordActivity)
            adapter = stageListAdapter
        }
    }
    
    /**
     * 设置照片/视频列表（使用LinearLayout直接显示，无需滚动）
     */
    private fun setupPhotosList() {
        // 初始化时会通过updateCurrentStageUI()来刷新列表
    }

    /**
     * 添加照片item到容器
     */
    private fun addMediaItemToContainer(
        mediaItem: com.campcooking.ar.data.MediaItem,
        container: android.widget.LinearLayout,
        index: Int
    ) {
        // 动态创建item view
        val itemView = layoutInflater.inflate(R.layout.item_photo, container, false)

        // 获取各个view
        val photoImageView = itemView.findViewById<android.widget.ImageView>(R.id.photoImageView)
        val videoIconView = itemView.findViewById<android.widget.ImageView>(R.id.videoIconView)
        val videoDurationView = itemView.findViewById<android.widget.TextView>(R.id.videoDurationView)
        val mediaTypeView = itemView.findViewById<android.widget.TextView>(R.id.mediaTypeView)
        val mediaInfoView = itemView.findViewById<android.widget.TextView>(R.id.mediaInfoView)
        val mediaTimeView = itemView.findViewById<android.widget.TextView>(R.id.mediaTimeView)
        val viewButton = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.viewButton)
        val deleteButton = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.deleteButton)

        val file = java.io.File(mediaItem.path)
        if (!file.exists()) {
            return
        }

        when (mediaItem.type) {
            com.campcooking.ar.data.MediaType.PHOTO -> {
                // 加载照片缩略图
                loadPhotoThumbnail(file, photoImageView)
                mediaTypeView.text = "📷 照片"
                mediaTypeView.setTextColor(getColor(R.color.water_lake))
                videoIconView.visibility = android.view.View.GONE
                videoDurationView.visibility = android.view.View.GONE
                viewButton.text = "查看"
            }
            com.campcooking.ar.data.MediaType.VIDEO -> {
                // 加载视频缩略图
                loadVideoThumbnail(file, photoImageView, videoIconView, videoDurationView)
                mediaTypeView.text = "🎥 视频"
                mediaTypeView.setTextColor(getColor(R.color.fire_coral))
                videoIconView.visibility = android.view.View.VISIBLE
                videoDurationView.visibility = android.view.View.VISIBLE
                viewButton.text = "播放"
            }
        }

        // 设置文件信息
        val fileSizeKB = file.length() / 1024
        mediaInfoView.text = when {
            fileSizeKB < 1024 -> "$fileSizeKB KB"
            else -> "${fileSizeKB / 1024}.${(fileSizeKB % 1024) / 100} MB"
        }

        // 设置时间
        val lastModified = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(file.lastModified()))
        mediaTimeView.text = lastModified

        // 动态设置按钮图标颜色
        // 查看按钮：绿色
        val greenColor = ContextCompat.getColor(this, R.color.material_green_500)
        viewButton.iconTint = ColorStateList.valueOf(greenColor)
        
        // 删除按钮：红色
        val redColor = ContextCompat.getColor(this, R.color.error)
        deleteButton.iconTint = ColorStateList.valueOf(redColor)

        // 查看按钮
        viewButton.setOnClickListener {
            viewMedia(mediaItem)
        }

        // 删除按钮
        deleteButton.setOnClickListener {
            showDeleteMediaDialog(index, mediaItem.type == com.campcooking.ar.data.MediaType.PHOTO)
        }

        // 添加到容器
        container.addView(itemView)
    }

    /**
     * 加载照片缩略图（优化版本）
     */
    private fun loadPhotoThumbnail(file: java.io.File, imageView: android.widget.ImageView) {
        try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)

            // 计算采样率
            var inSampleSize = 1
            val reqWidth = 200
            val reqHeight = 200

            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }

            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    /**
     * 加载视频缩略图
     */
    private fun loadVideoThumbnail(
        file: java.io.File,
        imageView: android.widget.ImageView,
        videoIconView: android.widget.ImageView,
        durationView: android.widget.TextView
    ) {
        // 方法1: 使用ThumbnailUtils
        var thumbnail = android.media.ThumbnailUtils.createVideoThumbnail(
            file.absolutePath,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND
        )

        if (thumbnail == null) {
            // 方法2: 使用MediaMetadataRetriever
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val bitmap = retriever.frameAtTime
                if (bitmap != null) {
                    thumbnail = android.media.ThumbnailUtils.extractThumbnail(bitmap, 200, 200)
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (thumbnail != null) {
            imageView.setImageBitmap(thumbnail)
        } else {
            // 使用默认灰色渐变背景
            imageView.setImageDrawable(null)
            imageView.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }

        // 提取视频时长
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()

            val durationMs = time?.toLongOrNull() ?: 0L
            val seconds = (durationMs / 1000).toInt()
            val minutes = seconds / 60
            val secs = seconds % 60
            durationView.text = String.format("%02d:%02d", minutes, secs)
        } catch (e: Exception) {
            durationView.text = "00:00"
        }
    }

    /**
     * 查看/播放媒体
     */
    private fun viewMedia(mediaItem: com.campcooking.ar.data.MediaItem) {
        when (mediaItem.type) {
            com.campcooking.ar.data.MediaType.PHOTO -> {
                // 查看照片大图
                viewPhoto(mediaItem.path)
            }
            com.campcooking.ar.data.MediaType.VIDEO -> {
                // 播放视频
                playVideo(mediaItem.path)
            }
        }
    }

    /**
     * 查看照片大图
     */
    private fun viewPhoto(photoPath: String) {
        try {
            val file = File(photoPath)
            if (!file.exists()) {
                Toast.makeText(this, "❌ 照片文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            // 使用FileProvider获取URI
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "❌ 未找到图片查看应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ 查看照片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 播放视频
     */
    private fun playVideo(videoPath: String) {
        try {
            val file = File(videoPath)
            if (!file.exists()) {
                Toast.makeText(this, "❌ 视频文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            // 检查文件大小
            val fileSizeKB = file.length() / 1024
            if (fileSizeKB < 10) {
                Toast.makeText(this, "⚠️ 视频文件过小(${fileSizeKB}KB)，可能录制失败", Toast.LENGTH_SHORT).show()
                return
            }

            // 使用FileProvider获取URI
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "❌ 未找到视频播放应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ 播放视频失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        
        // 保存发送按钮 - 使用多种方法确保能找到按钮
        var saveAndSendButton = binding.saveAndSendButton
        if (saveAndSendButton == null) {
            // 如果ViewBinding找不到，使用findViewById
            saveAndSendButton = findViewById(R.id.saveAndSendButton)
        }

        saveAndSendButton?.setOnClickListener {
            saveAndSendData()
        }
        
        // 生成报告按钮
        binding.generateReportButton.setOnClickListener {
            showGenerateReportDialog()
        }
        
        // 拍照按钮
        binding.takePhotoButton.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }

        // 录像按钮
        binding.takeVideoButton.setOnClickListener {
            checkCameraPermissionAndTakeVideo()
        }

        // 评分监听
        binding.selfRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            onRatingChanged(rating.toInt())
        }

        // 其它备注输入监听（做得好的地方的其它）
        binding.otherCommentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val stageRecord = getCurrentStageRecord()
                stageRecord.notes = s?.toString() ?: ""
                saveRecord()
            }
        })

        // 需要改进的其它输入监听
        binding.problemOtherInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val stageRecord = getCurrentStageRecord()
                stageRecord.problemNotes = s?.toString() ?: ""
                saveRecord()
            }
        })

        // 输入框焦点监听：adjustPan 模式下系统会自动处理滚动
        binding.otherCommentInput.setOnFocusChangeListener { _, hasFocus ->
            android.util.Log.d("RecordActivity", "otherCommentInput focus changed: $hasFocus")
        }

        binding.problemOtherInput.setOnFocusChangeListener { _, hasFocus ->
            android.util.Log.d("RecordActivity", "problemOtherInput focus changed: $hasFocus")
        }

        // 完成本环节按钮 - 已删除
        // binding.completeStageButton.setOnClickListener {
        //     completeCurrentStage()
        // }
        
        // 下一步按钮 - 已删除
        // binding.nextStageButton.setOnClickListener {
        //     moveToNextStage()
        // }
    }
    
    /**
     * 更新当前阶段UI
     */
    private fun updateCurrentStageUI() {
        val currentStage = processRecord.currentStage
        val stageRecord = getCurrentStageRecord()

        // 更新阶段信息
        binding.stageEmojiText.text = currentStage.emoji
        binding.stageNameText.text = currentStage.displayName
        binding.stageDescText.text = currentStage.description
        binding.stageDurationText.text = stageRecord.getDurationText()

        // 更新提示语
        val hint = RecordConfig.stageHints[currentStage] ?: ""
        binding.stageHintText.text = "💡 $hint"

        // 更新评分
        binding.selfRatingBar.rating = stageRecord.selfRating.toFloat()
        updateRatingDescription(stageRecord.selfRating)

        // 更新其它备注输入框（做得好的地方的其它）
        binding.otherCommentInput.setText(stageRecord.notes)

        // 更新需要改进的其它输入框
        binding.problemOtherInput.setText(stageRecord.problemNotes)

        // 更新标签
        setupTags()

        // 更新照片和视频列表（直接显示，无需滚动）
        refreshMediaLists()

        // 更新节点列表
        stageListAdapter.notifyDataSetChanged()

        // 更新按钮状态
        updateButtonStates()

        // 更新进度UI
        updateProgressUI()
    }

    /**
     * 刷新照片和视频列表（清空容器后重新添加）
     */
    private fun refreshMediaLists() {
        val stageRecord = getCurrentStageRecord()

        // 清空容器
        binding.photosContainer.removeAllViews()
        binding.videosContainer.removeAllViews()

        // 分别添加照片和视频
        stageRecord.mediaItems.forEachIndexed { index, mediaItem ->
            when (mediaItem.type) {
                com.campcooking.ar.data.MediaType.PHOTO -> {
                    addMediaItemToContainer(mediaItem, binding.photosContainer, index)
                }
                com.campcooking.ar.data.MediaType.VIDEO -> {
                    addMediaItemToContainer(mediaItem, binding.videosContainer, index)
                }
            }
        }

        // 如果容器为空，显示提示
        if (binding.photosContainer.childCount == 0) {
            val emptyView = layoutInflater.inflate(android.R.layout.simple_list_item_1, binding.photosContainer, false)
            (emptyView as android.widget.TextView).apply {
                text = "暂无照片，点击上方\"拍照\"按钮添加"
                textSize = 13f
                setTextColor(getColor(R.color.subtitle_color))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            binding.photosContainer.addView(emptyView)
        }

        if (binding.videosContainer.childCount == 0) {
            val emptyView = layoutInflater.inflate(android.R.layout.simple_list_item_1, binding.videosContainer, false)
            (emptyView as android.widget.TextView).apply {
                text = "暂无视频，点击上方\"录像\"按钮添加"
                textSize = 13f
                setTextColor(getColor(R.color.subtitle_color))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            binding.videosContainer.addView(emptyView)
        }
    }

    /**
     * 滚动到指定视图，确保其在可见区域内
     * 使用更可靠的滚动算法
     */
    private fun scrollToViewSmoothly(view: View) {
        try {
            android.util.Log.d("RecordActivity", "scrollToViewSmoothly called for view: ${view.id}")

            // 找到父ScrollView
            var parent = view.parent
            var depth = 0
            while (parent != null && parent !is ScrollView && depth < 10) {
                parent = parent.parent
                depth++
            }

            if (parent is ScrollView) {
                android.util.Log.d("RecordActivity", "Found ScrollView at depth: $depth")

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

                android.util.Log.d("RecordActivity", "viewTopRelativeToScrollView: $viewTopRelativeToScrollView, scrollViewHeight: $scrollViewHeight")

                // 计算目标滚动位置：将输入框滚动到可见区域的上半部分
                val targetScrollY = (viewTopRelativeToScrollView - scrollViewHeight / 4).coerceAtLeast(0)

                android.util.Log.d("RecordActivity", "Scrolling to: $targetScrollY, current scrollY: ${parent.scrollY}")

                // 平滑滚动到目标位置
                parent.smoothScrollTo(0, targetScrollY)
            } else {
                android.util.Log.e("RecordActivity", "ScrollView not found! Parent at depth 10: $parent")
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordActivity", "Error in scrollToViewSmoothly", e)
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
     * 更新进度UI（大数字徽章版）
     */
    private fun updateProgressUI() {
        val stageRecord = getCurrentStageRecord()

        // 统计照片和视频数量
        val photoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.PHOTO }
        val videoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.VIDEO }

        // 成果展示阶段使用特殊要求
        val isShowcase = processRecord.currentStage == com.campcooking.ar.data.CookingStage.SHOWCASE
        val photoTarget = if (isShowcase) RecordConfig.SHOWCASE_GROUP_PHOTO_REQUIRED + RecordConfig.SHOWCASE_DISH_PHOTO_REQUIRED else RecordConfig.MIN_PHOTOS_REQUIRED
        val videoTarget = if (isShowcase) RecordConfig.SHOWCASE_SPEECH_VIDEO_REQUIRED else RecordConfig.MIN_VIDEOS_REQUIRED

        // 更新大数字显示
        binding.photoProgressText.text = "${photoCount}/${photoTarget}"
        binding.videoProgressText.text = "${videoCount}/${videoTarget}"

        // 更新状态指示
        val photoMeets = photoCount >= photoTarget
        val videoMeets = videoCount >= videoTarget

        // 照片状态
        when {
            photoMeets -> {
                binding.photoProgressStatusText.text = "✅ 已达标"
                binding.photoProgressStatusText.setTextColor(getColor(R.color.nature_green))
                binding.photoProgressText.setTextColor(getColor(R.color.nature_green))
            }
            photoCount == 0 -> {
                binding.photoProgressStatusText.text = "📌 未开始"
                binding.photoProgressStatusText.setTextColor(getColor(R.color.subtitle_color))
                binding.photoProgressText.setTextColor(getColor(R.color.water_lake))
            }
            else -> {
                val remaining = photoTarget - photoCount
                binding.photoProgressStatusText.text = "📌 还需${remaining}张"
                binding.photoProgressStatusText.setTextColor(getColor(R.color.fire_orange))
                binding.photoProgressText.setTextColor(getColor(R.color.water_lake))
            }
        }

        // 视频状态
        when {
            videoMeets -> {
                binding.videoProgressStatusText.text = "✅ 已达标"
                binding.videoProgressStatusText.setTextColor(getColor(R.color.nature_green))
                binding.videoProgressText.setTextColor(getColor(R.color.nature_green))
            }
            videoCount == 0 -> {
                binding.videoProgressStatusText.text = "📌 未开始"
                binding.videoProgressStatusText.setTextColor(getColor(R.color.subtitle_color))
                binding.videoProgressText.setTextColor(getColor(R.color.fire_coral))
            }
            else -> {
                val remaining = videoTarget - videoCount
                binding.videoProgressStatusText.text = "📌 还需${remaining}段"
                binding.videoProgressStatusText.setTextColor(getColor(R.color.fire_orange))
                binding.videoProgressText.setTextColor(getColor(R.color.fire_coral))
            }
        }

        // 更新进度提示（各模块内的提示文字）
        // 成果展示阶段使用特殊提示
        if (processRecord.currentStage == com.campcooking.ar.data.CookingStage.SHOWCASE) {
            // 成果展示阶段：需要小组合照、菜品合照和语言表述视频
            binding.photoProgressHintText.text = when {
                photoCount >= 2 -> "✅ 小组合照和菜品合照已完成！"
                photoCount == 1 -> "📸 已拍1张（小组合照或菜品合照），还需要1张"
                else -> "📸 请拍摄：1张小组合照 + 1张菜品合照"
            }
            
            binding.videoProgressHintText.text = when {
                videoMeets -> "✅ 语言表述视频已录制！"
                else -> "🎤 请录制1段语言表述视频，介绍你们的成果"
            }
        } else {
            // 其他阶段使用常规提示
            binding.photoProgressHintText.text = when {
                photoMeets -> "🎉 太棒了！照片数量已达标"
                photoCount == 0 -> "还没有拍照，点击上方按钮拍照记录"
                else -> "继续加油！还需要${RecordConfig.MIN_PHOTOS_REQUIRED - photoCount}张照片"
            }

            binding.videoProgressHintText.text = when {
                videoMeets -> "🎉 太棒了！视频数量已达标"
                videoCount == 0 -> "还没有录像，点击上方按钮录像记录"
                else -> "继续加油！还需要${RecordConfig.MIN_VIDEOS_REQUIRED - videoCount}段视频"
            }
        }
    }
    
    /**
     * 设置标签（Material Design 3 标准方案 - 大字体）
     */
    private fun setupTags() {
        val currentStage = processRecord.currentStage
        val stageRecord = getCurrentStageRecord()
        val tagGroup = RecordConfig.stageTagsMap[currentStage]

        // 清空现有标签
        binding.positiveTagsChipGroup.removeAllViews()
        binding.problemTagsChipGroup.removeAllViews()

        // 添加正面标签（Material Design 3 绿色系）
        tagGroup?.positive?.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = stageRecord.selectedTags.contains(tag)
                // 增大Chip字体
                textSize = 16f
                chipMinHeight = 52f

                // 未选中状态：浅绿背景，深绿文字，绿色边框
                if (!isChecked) {
                    chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.nature_green_alpha)
                    setTextColor(ContextCompat.getColor(context, R.color.nature_green_dark))
                    chipStrokeColor = ContextCompat.getColorStateList(context, R.color.material_green_500)
                    chipStrokeWidth = 2f
                } else {
                    // 选中状态：绿色填充，白色文字
                    chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.material_green_500)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    chipStrokeWidth = 0f
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        stageRecord.selectedTags.add(tag)
                        // 选中后更新样式
                        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.material_green_500)
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        chipStrokeWidth = 0f
                    } else {
                        stageRecord.selectedTags.remove(tag)
                        // 取消选中后更新样式
                        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.nature_green_alpha)
                        setTextColor(ContextCompat.getColor(context, R.color.nature_green_dark))
                        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.material_green_500)
                        chipStrokeWidth = 2f
                    }
                    saveRecord()
                }
            }
            binding.positiveTagsChipGroup.addView(chip)
        }

        // 添加问题标签（Material Design 3 橙色系）
        tagGroup?.problems?.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = stageRecord.selectedTags.contains(tag)
                // 增大Chip字体
                textSize = 16f
                chipMinHeight = 52f

                // 未选中状态：浅橙背景，深橙文字，橙色边框
                if (!isChecked) {
                    chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.fire_orange_alpha)
                    setTextColor(ContextCompat.getColor(context, R.color.fire_orange_dark))
                    chipStrokeColor = ContextCompat.getColorStateList(context, R.color.material_orange_500)
                    chipStrokeWidth = 2f
                } else {
                    // 选中状态：橙色填充，白色文字
                    chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.material_orange_500)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    chipStrokeWidth = 0f
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        stageRecord.selectedTags.add(tag)
                        // 选中后更新样式
                        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.material_orange_500)
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        chipStrokeWidth = 0f
                    } else {
                        stageRecord.selectedTags.remove(tag)
                        // 取消选中后更新样式
                        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.fire_orange_alpha)
                        setTextColor(ContextCompat.getColor(context, R.color.fire_orange_dark))
                        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.material_orange_500)
                        chipStrokeWidth = 2f
                    }
                    saveRecord()
                }
            }
            binding.problemTagsChipGroup.addView(chip)
        }
    }
    
    /**
     * 评分变化处理
     */
    private fun onRatingChanged(rating: Int) {
        val stageRecord = getCurrentStageRecord()
        stageRecord.selfRating = rating
        updateRatingDescription(rating)
        saveRecord()
    }
    
    /**
     * 更新评分描述
     */
    private fun updateRatingDescription(rating: Int) {
        val ratingLevel = RecordConfig.ratingDescriptions[rating]
        binding.ratingDescText.text = ratingLevel?.title ?: "未评价"
    }
    
    /**
     * 获取当前阶段记录
     */
    private fun getCurrentStageRecord() = processRecord.getOrCreateStageRecord(processRecord.currentStage)
    
    /**
     * 完成当前阶段
     */
    private fun completeCurrentStage() {
        val stageRecord = getCurrentStageRecord()
        
        // 检查是否已评分
        if (stageRecord.selfRating == 0) {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("请先对本环节进行评分哦！")
                .setPositiveButton("确定", null)
                .show()
            return
        }
        
        // 检查是否有照片或视频
        if (stageRecord.mediaItems.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("建议拍照或录像记录本环节，是否继续完成？")
                .setPositiveButton("继续") { _, _ ->
                    doCompleteStage()
                }
                .setNegativeButton("去记录", null)
                .show()
        } else {
            doCompleteStage()
        }
    }
    
    /**
     * 执行完成阶段
     */
    private fun doCompleteStage() {
        processRecord.completeCurrentStage()
        saveRecord()
        updateCurrentStageUI()
        
        Toast.makeText(
            this,
            "✓ ${processRecord.currentStage.displayName}已完成！",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * 进入下一阶段
     */
    private fun moveToNextStage() {
        val stageRecord = getCurrentStageRecord()
        
        // 如果当前阶段未完成，先完成
        if (!stageRecord.isCompleted) {
            if (stageRecord.selfRating == 0) {
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("请先对本环节进行评分！")
                    .setPositiveButton("确定", null)
                    .show()
                return
            }
        }
        
        val hasNext = processRecord.moveToNextStage()
        
        if (hasNext) {
            saveRecord()
            updateCurrentStageUI()
            Toast.makeText(
                this,
                "进入 ${processRecord.currentStage.displayName}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // 已经是最后一个阶段
            showAllCompletedDialog()
        }
    }
    
    /**
     * 显示切换阶段对话框
     */
    private fun showSwitchStageDialog(targetStage: CookingStage) {
        AlertDialog.Builder(this)
            .setTitle("切换阶段")
            .setMessage("要切换到 ${targetStage.emoji} ${targetStage.displayName} 吗？")
            .setPositiveButton("确定") { _, _ ->
                processRecord.currentStage = targetStage
                if (!processRecord.stages.containsKey(targetStage)) {
                    processRecord.startStage(targetStage)
                }
                saveRecord()
                updateCurrentStageUI()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示全部完成对话框
     */
    private fun showAllCompletedDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎉 恭喜完成！")
            .setMessage("野炊的所有环节都完成啦！现在可以生成报告查看哦～")
            .setPositiveButton("生成报告") { _, _ ->
                // TODO: 生成报告
                Toast.makeText(this, "报告功能开发中...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("稍后", null)
            .show()
    }
    
    /**
     * 检查相机权限并拍照
     */
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            showPermissionDeniedDialog("相机")
        }
    }

    /**
     * 拍照
     */
    private fun takePhoto() {
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoPath = photoFile.absolutePath
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
        }
    }

    /**
     * 检查相机和录音权限并录像
     */
    private fun checkCameraPermissionAndTakeVideo() {
        val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasCamera && hasAudio) {
            takeVideo()
        } else {
            val missingPermissions = mutableListOf<String>()
            if (!hasCamera) missingPermissions.add("相机")
            if (!hasAudio) missingPermissions.add("录音")
            showPermissionDeniedDialog(missingPermissions.joinToString("和"))
        }
    }

    /**
     * 录像
     */
    private fun takeVideo() {
        val videoFile = createVideoFile()
        if (videoFile != null) {
            currentVideoPath = videoFile.absolutePath
            currentVideoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                videoFile
            )

            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentVideoUri)
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // 1 = 高质量
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, RecordConfig.MAX_VIDEO_DURATION_SECONDS) // 限制录制时长为30秒
            startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO)
        }
    }

    /**
     * 显示权限被拒绝的对话框，引导用户到设置页面
     */
    private fun showPermissionDeniedDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("📷 需要权限")
            .setMessage("拍照录像需要${permissionName}权限\n\n请在设置中手动开启权限")
            .setPositiveButton("去设置") { _, _ ->
                // 打开应用设置页面
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 创建图片文件
     */
    private fun createImageFile(): File? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(
                "COOKING_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "创建照片文件失败", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * 创建视频文件
     */
    private fun createVideoFile(): File? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            return File.createTempFile(
                "COOKING_${timeStamp}_",
                ".mp4",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "创建视频文件失败", Toast.LENGTH_SHORT).show()
            return null
        }
    }
    
    /**
     * 显示删除媒体对话框
     * @param position 在mediaItems列表中的索引
     * @param isPhoto true表示照片，false表示视频
     */
    private fun showDeleteMediaDialog(position: Int, isPhoto: Boolean) {
        val stageRecord = getCurrentStageRecord()
        val mediaItem = stageRecord.mediaItems[position]
        val mediaTypeText = if (isPhoto) "照片" else "视频"
        val mediaEmoji = if (isPhoto) "📷" else "🎥"

        AlertDialog.Builder(this)
            .setTitle("${mediaEmoji} 删除${mediaTypeText}")
            .setMessage("确定要删除这个${mediaTypeText}吗？\n\n删除后无法恢复，请确认。")
            .setPositiveButton("确定删除") { _, _ ->
                try {
                    // 删除物理文件
                    val file = File(mediaItem.path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (!deleted) {
                            Toast.makeText(this, "⚠️ 文件删除失败，但已从记录中移除", Toast.LENGTH_LONG).show()
                        }
                    }

                    // 从列表中移除
                    stageRecord.mediaItems.removeAt(position)
                    // 同时从photos列表中删除（保持兼容）
                    if (isPhoto) {
                        stageRecord.photos.remove(mediaItem.path)
                    }

                    // 刷新UI
                    saveRecord()
                    refreshMediaLists()  // 刷新列表显示
                    updateProgressUI()
                    updateButtonStates()

                    Toast.makeText(this, "✓ ${mediaTypeText}已删除", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "❌ 删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示生成报告对话框
     */
    private fun showGenerateReportDialog() {
        val completedCount = processRecord.getCompletedStagesCount()
        val totalCount = CookingStage.getAllStages().size
        
        AlertDialog.Builder(this)
            .setTitle("生成报告")
            .setMessage("已完成 $completedCount/$totalCount 个环节\n\n确定要生成报告吗？")
            .setPositiveButton("生成") { _, _ ->
                // TODO: 实现报告生成功能
                Toast.makeText(this, "报告功能开发中...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 更新按钮状态
     */
    private fun updateButtonStates() {
        val stageRecord = getCurrentStageRecord()
        val allStages = CookingStage.getAllStages()
        val isLastStage = processRecord.currentStage == allStages.last()

        // 检查是否满足最低要求
        val photoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.PHOTO }
        val videoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.VIDEO }
        
        // 成果展示阶段使用特殊要求：2张照片（小组合照+菜品合照）+ 1段视频（语言表述）
        val isShowcase = processRecord.currentStage == com.campcooking.ar.data.CookingStage.SHOWCASE
        val photoTarget = if (isShowcase) RecordConfig.SHOWCASE_GROUP_PHOTO_REQUIRED + RecordConfig.SHOWCASE_DISH_PHOTO_REQUIRED else RecordConfig.MIN_PHOTOS_REQUIRED
        val videoTarget = if (isShowcase) RecordConfig.SHOWCASE_SPEECH_VIDEO_REQUIRED else RecordConfig.MIN_VIDEOS_REQUIRED
        val meetsRequirements = photoCount >= photoTarget && videoCount >= videoTarget

        // 完成按钮 - 已删除
        // binding.completeStageButton.isEnabled = !stageRecord.isCompleted && meetsRequirements
        // if (stageRecord.isCompleted) {
        //     binding.completeStageButton.text = "✓ 已完成"
        // } else if (meetsRequirements) {
        //     binding.completeStageButton.text = "✓ 完成本环节"
        // } else {
        //     binding.completeStageButton.text = "完成本环节（未达标）"
        // }

        // 下一步按钮 - 已删除
        // binding.nextStageButton.visibility = if (isLastStage) View.GONE else View.VISIBLE
    }
    
    /**
     * 启动定时器（更新用时显示）
     */
    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateTimeDisplay()
                }
            }
        }, 0, 1000)  // 每秒更新一次
    }
    
    /**
     * 更新时间显示
     */
    private fun updateTimeDisplay() {
        // 更新总用时
        binding.totalTimeText.text = "总用时: ${processRecord.getTotalDurationMinutes()}分钟"
        
        // 更新当前阶段用时
        val stageRecord = getCurrentStageRecord()
        binding.stageDurationText.text = stageRecord.getDurationText()
    }
    
    /**
     * 保存记录
     */
    private fun saveRecord() {
        processRecordManager.saveProcessRecord(processRecord)
    }
    
    /**
     * 保存并发送数据到服务器
     */
    private fun saveAndSendData() {
        // 先保存到本地
        saveRecord()
        
        // 检查是否正在上传
        if (isUploading) {
            Toast.makeText(this, "正在上传中，请稍候...", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 显示上传进度对话框
        showUploadProgressDialog()
        
        // 发送数据到服务器
        dataSubmitManager.submitAllData(
            onSuccess = {
                runOnUiThread {
                    dismissUploadProgressDialog()
                    Toast.makeText(this, "✅ 数据已保存并发送到服务器", Toast.LENGTH_LONG).show()
                    isUploading = false
                }
            },
            onError = { errorMsg ->
                runOnUiThread {
                    dismissUploadProgressDialog()
                    Toast.makeText(this, "⚠️ 保存成功，但发送失败: $errorMsg", Toast.LENGTH_LONG).show()
                    isUploading = false
                }
            },
            onProgress = { current, total, fileName, fileProgress ->
                runOnUiThread {
                    updateUploadProgress(current, total, fileName, fileProgress)
                }
            }
        )
        
        isUploading = true
    }
    
    /**
     * 显示上传进度对话框
     */
    private fun showUploadProgressDialog() {
        uploadProgressDialog = android.app.ProgressDialog(this).apply {
            setTitle("正在上传数据")
            setMessage("准备上传...")
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            setMax(100)
            setProgress(0)
            setCancelable(false)  // 不可取消
            setCanceledOnTouchOutside(false)  // 点击外部不可取消
            show()
        }
    }
    
    /**
     * 更新上传进度
     */
    private fun updateUploadProgress(current: Int, total: Int, fileName: String, fileProgress: Int) {
        uploadProgressDialog?.apply {
            if (total > 0) {
                val overallProgress = ((current - 1) * 100 + fileProgress) / total
                setProgress(overallProgress)
                setMessage("正在上传文件 $current/$total\n$fileName\n${fileProgress}%")
            } else {
                // 如果没有文件需要上传，显示完成
                setProgress(100)
                setMessage("正在提交数据...")
            }
        }
    }
    
    /**
     * 关闭上传进度对话框
     */
    private fun dismissUploadProgressDialog() {
        uploadProgressDialog?.dismiss()
        uploadProgressDialog = null
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            currentPhotoPath?.let { path ->
                // 添加照片到当前阶段
                val stageRecord = getCurrentStageRecord()
                val photoItem = com.campcooking.ar.data.MediaItem(
                    path = path,
                    type = com.campcooking.ar.data.MediaType.PHOTO
                )
                stageRecord.mediaItems.add(photoItem)
                stageRecord.photos.add(path) // 保持向后兼容

                saveRecord()
                refreshMediaLists()  // 刷新列表显示
                updateProgressUI()

                // 显示鼓励消息
                val photoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.PHOTO }
                val videoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.VIDEO }

                val encouragement = RecordConfig.getEncouragementMessage(photoCount, videoCount)
                if (encouragement != null) {
                    Toast.makeText(this, encouragement, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (requestCode == REQUEST_TAKE_VIDEO && resultCode == RESULT_OK) {
            currentVideoPath?.let { path ->
                // 添加视频到当前阶段
                val stageRecord = getCurrentStageRecord()
                val videoItem = com.campcooking.ar.data.MediaItem(
                    path = path,
                    type = com.campcooking.ar.data.MediaType.VIDEO
                )
                stageRecord.mediaItems.add(videoItem)

                saveRecord()
                refreshMediaLists()  // 刷新列表显示
                updateProgressUI()

                // 显示鼓励消息
                val photoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.PHOTO }
                val videoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.VIDEO }

                val encouragement = RecordConfig.getEncouragementMessage(photoCount, videoCount)
                if (encouragement != null) {
                    Toast.makeText(this, encouragement, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "视频已保存", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            // 检查是否所有权限都已授予
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                // 所有权限已授予，可以正常使用拍照录像功能
                Toast.makeText(this, "✅ 权限已授予，可以正常使用拍照录像功能", Toast.LENGTH_SHORT).show()
            } else {
                // 部分权限被拒绝，提示用户
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }

                val permissionNames = deniedPermissions.map { permission ->
                    when (permission) {
                        Manifest.permission.CAMERA -> "相机"
                        Manifest.permission.RECORD_AUDIO -> "录音"
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> "存储"
                        else -> "相关"
                    }
                }.joinToString("、")

                AlertDialog.Builder(this)
                    .setTitle("⚠️ 权限未授予")
                    .setMessage("您拒绝了${permissionNames}权限\n\n虽然可以继续使用，但无法拍照录像\n\n如需使用这些功能，请到设置中手动开启权限")
                    .setPositiveButton("知道了", null)
                    .setNeutralButton("去设置") { _, _ ->
                        // 打开应用设置页面
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = android.net.Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        saveRecord()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
    
    /**
     * 重写返回键，上传期间禁用返回
     */
    override fun onBackPressed() {
        if (isUploading) {
            Toast.makeText(this, "正在上传中，请稍候...", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("返回")
            .setMessage("记录会自动保存，确定返回吗？")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

