package com.campcooking.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.campcooking.ar.adapter.PhotoListAdapter
import com.campcooking.ar.adapter.StageListAdapter
import com.campcooking.ar.config.RecordConfig
import com.campcooking.ar.data.CookingStage
import com.campcooking.ar.data.ProcessRecord
import com.campcooking.ar.data.TeamInfo
import com.campcooking.ar.databinding.ActivityRecordBinding
import com.campcooking.ar.utils.ProcessRecordManager
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
class RecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordBinding
    private lateinit var processRecord: ProcessRecord
    private lateinit var processRecordManager: ProcessRecordManager
    
    // 适配器
    private lateinit var stageListAdapter: StageListAdapter
    private lateinit var photoListAdapter: PhotoListAdapter
    
    // 拍照相关
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // 录像相关
    private var currentVideoUri: Uri? = null
    private var currentVideoPath: String? = null

    // 定时器（用于更新用时显示）
    private var timer: Timer? = null

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

        // 获取团队信息
        val teamName = intent.getStringExtra("teamName") ?: "野炊小组"
        binding.teamNameText.text = teamName

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
                // 切换到选中的阶段
                if (processRecord.currentStage != stage) {
                    showSwitchStageDialog(stage)
                }
            }
        )
        
        binding.stagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecordActivity)
            adapter = stageListAdapter
        }
    }
    
    /**
     * 设置照片列表
     */
    private fun setupPhotosList() {
        photoListAdapter = PhotoListAdapter(
            mediaItems = getCurrentStageRecord().mediaItems,
            onDeleteClick = { position ->
                showDeleteMediaDialog(position)
            }
        )

        binding.photosRecyclerView.apply {
            layoutManager = GridLayoutManager(this@RecordActivity, 4)
            adapter = photoListAdapter
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
        
        // 完成本环节按钮
        binding.completeStageButton.setOnClickListener {
            completeCurrentStage()
        }
        
        // 下一步按钮
        binding.nextStageButton.setOnClickListener {
            moveToNextStage()
        }
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

        // 更新标签
        setupTags()

        // 更新照片列表
        photoListAdapter.updatePhotos(stageRecord.photos)

        // 更新节点列表
        stageListAdapter.notifyDataSetChanged()

        // 更新按钮状态
        updateButtonStates()

        // 更新进度UI
        updateProgressUI()
    }

    /**
     * 更新进度UI
     */
    private fun updateProgressUI() {
        val stageRecord = getCurrentStageRecord()

        // 统计照片和视频数量
        val photoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.PHOTO }
        val videoCount = stageRecord.mediaItems.count { it.type == com.campcooking.ar.data.MediaType.VIDEO }

        // 更新进度文本
        binding.photoProgressText.text = "${photoCount}/${RecordConfig.MIN_PHOTOS_REQUIRED}"
        binding.videoProgressText.text = "${videoCount}/${RecordConfig.MIN_VIDEOS_REQUIRED}"

        // 更新进度提示
        binding.progressHintText.text = RecordConfig.getProgressHint(photoCount, videoCount)

        // 计算总进度（照片占75%，视频占25%）
        val photoProgress = (photoCount.toFloat() / RecordConfig.MIN_PHOTOS_REQUIRED).coerceAtMost(1f)
        val videoProgress = (videoCount.toFloat() / RecordConfig.MIN_VIDEOS_REQUIRED).coerceAtMost(1f)
        val totalProgress = ((photoProgress * 0.75 + videoProgress * 0.25) * 100).toInt()

        binding.progressBar.progress = totalProgress

        // 更新进度文本颜色（达标时变绿）
        binding.photoProgressText.setTextColor(
            getColor(
                if (photoCount >= RecordConfig.MIN_PHOTOS_REQUIRED) R.color.nature_green
                else R.color.water_lake
            )
        )

        binding.videoProgressText.setTextColor(
            getColor(
                if (videoCount >= RecordConfig.MIN_VIDEOS_REQUIRED) R.color.nature_green
                else R.color.fire_coral
            )
        )
    }
    
    /**
     * 设置标签
     */
    private fun setupTags() {
        val currentStage = processRecord.currentStage
        val stageRecord = getCurrentStageRecord()
        val tagGroup = RecordConfig.stageTagsMap[currentStage]
        
        // 清空现有标签
        binding.positiveTagsChipGroup.removeAllViews()
        binding.problemTagsChipGroup.removeAllViews()
        
        // 添加正面标签
        tagGroup?.positive?.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = stageRecord.selectedTags.contains(tag)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        stageRecord.selectedTags.add(tag)
                    } else {
                        stageRecord.selectedTags.remove(tag)
                    }
                    saveRecord()
                }
            }
            binding.positiveTagsChipGroup.addView(chip)
        }
        
        // 添加问题标签
        tagGroup?.problems?.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = stageRecord.selectedTags.contains(tag)
                setChipBackgroundColorResource(R.color.fire_red)
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        stageRecord.selectedTags.add(tag)
                    } else {
                        stageRecord.selectedTags.remove(tag)
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
     */
    private fun showDeleteMediaDialog(position: Int) {
        val stageRecord = getCurrentStageRecord()
        val mediaItem = stageRecord.mediaItems[position]
        val mediaTypeText = if (mediaItem.type == com.campcooking.ar.data.MediaType.PHOTO) "照片" else "视频"
        val mediaEmoji = if (mediaItem.type == com.campcooking.ar.data.MediaType.PHOTO) "📷" else "🎥"

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
                    if (mediaItem.type == com.campcooking.ar.data.MediaType.PHOTO) {
                        stageRecord.photos.remove(mediaItem.path)
                    }

                    // 更新UI
                    photoListAdapter.updateMediaItems(stageRecord.mediaItems)
                    saveRecord()
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
        val meetsRequirements = photoCount >= RecordConfig.MIN_PHOTOS_REQUIRED && videoCount >= RecordConfig.MIN_VIDEOS_REQUIRED

        // 完成按钮：如果已完成或未满足要求，则禁用
        binding.completeStageButton.isEnabled = !stageRecord.isCompleted && meetsRequirements
        if (stageRecord.isCompleted) {
            binding.completeStageButton.text = "✓ 已完成"
        } else if (meetsRequirements) {
            binding.completeStageButton.text = "✓ 完成本环节"
        } else {
            binding.completeStageButton.text = "完成本环节（未达标）"
        }

        // 下一步按钮
        binding.nextStageButton.visibility = if (isLastStage) View.GONE else View.VISIBLE
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
                photoListAdapter.updateMediaItems(stageRecord.mediaItems)
                saveRecord()

                // 更新进度UI
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
                photoListAdapter.updateMediaItems(stageRecord.mediaItems)
                saveRecord()

                // 更新进度UI
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
    
    override fun onBackPressed() {
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

