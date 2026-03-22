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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.campcooking.ar.adapter.SummaryPhotoAdapter
import com.campcooking.ar.data.MediaType
import com.campcooking.ar.databinding.ActivitySummaryBinding
import com.campcooking.ar.utils.ProcessRecordManager
import com.campcooking.ar.utils.SummaryManager
import com.campcooking.ar.utils.TeamInfoManager
import com.campcooking.ar.utils.ServerConfigManager
import com.campcooking.ar.utils.DataSubmitManager
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * 课后总结Activity
 * 让学生总结野炊过程中的收获和感悟
 * 支持图文并茂的总结
 */
class SummaryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySummaryBinding
    private lateinit var summaryManager: SummaryManager
    private lateinit var teamInfoManager: TeamInfoManager
    private lateinit var serverConfigManager: ServerConfigManager
    private lateinit var dataSubmitManager: DataSubmitManager
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 图片列表
    private val photos1 = mutableListOf<String>()
    private val photos2 = mutableListOf<String>()
    private val photos3 = mutableListOf<String>()
    
    // 适配器
    private lateinit var adapter1: SummaryPhotoAdapter
    private lateinit var adapter2: SummaryPhotoAdapter
    private lateinit var adapter3: SummaryPhotoAdapter
    
    // 拍照相关
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var currentQuestionNumber: Int = 1 // 当前正在为哪个问题添加图片
    
    companion object {
        private const val TAG = "SummaryActivity"
        private const val REQUEST_CAMERA_PERMISSION = 200
        private const val REQUEST_TAKE_PHOTO_1 = 201
        private const val REQUEST_TAKE_PHOTO_2 = 202
        private const val REQUEST_TAKE_PHOTO_3 = 203
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // 保持全屏模式
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            
            binding = ActivitySummaryBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // 初始化数据管理器
            summaryManager = SummaryManager(this)
            teamInfoManager = TeamInfoManager(this)
            serverConfigManager = ServerConfigManager(this)
            dataSubmitManager = DataSubmitManager(this)
            
            // 获取团队信息
            val teamName = intent.getStringExtra("teamName") ?: "野炊小组"
            binding.teamNameText.text = teamName
            
            setupPhotoRecyclerViews()
            setupUI()
            loadSavedData()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "页面加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    /**
     * 设置图片RecyclerView
     */
    private fun setupPhotoRecyclerViews() {
        // 问题1的图片列表
        adapter1 = SummaryPhotoAdapter(
            photos1,
            onPhotoClick = { photoPath -> viewPhoto(photoPath) },
            onDeleteClick = { position -> deletePhoto(1, position) }
        )
        binding.photosRecyclerView1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photosRecyclerView1.adapter = adapter1
        
        // 问题2的图片列表
        adapter2 = SummaryPhotoAdapter(
            photos2,
            onPhotoClick = { photoPath -> viewPhoto(photoPath) },
            onDeleteClick = { position -> deletePhoto(2, position) }
        )
        binding.photosRecyclerView2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photosRecyclerView2.adapter = adapter2
        
        // 问题3的图片列表
        adapter3 = SummaryPhotoAdapter(
            photos3,
            onPhotoClick = { photoPath -> viewPhoto(photoPath) },
            onDeleteClick = { position -> deletePhoto(3, position) }
        )
        binding.photosRecyclerView3.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photosRecyclerView3.adapter = adapter3
    }
    
    /**
     * 设置UI交互
     */
    private fun setupUI() {
        try {
            // 返回按钮
            binding.backButton.setOnClickListener {
                finish()
            }
            
            // 保存按钮
            binding.saveButton.setOnClickListener {
                saveAndUploadSummary()
            }
            
            // 添加图片按钮
            binding.addPhotoButton1.setOnClickListener {
                showPhotoSourceDialog(1)
            }
            binding.addPhotoButton2.setOnClickListener {
                showPhotoSourceDialog(2)
            }
            binding.addPhotoButton3.setOnClickListener {
                showPhotoSourceDialog(3)
            }
            
            // 自动保存（输入时实时保存）
            binding.answer1Input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    autoSave()
                }
            })
            
            binding.answer2Input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    autoSave()
                }
            })
            
            binding.answer3Input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    autoSave()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "UI设置失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 显示图片来源选择对话框
     */
    private fun showPhotoSourceDialog(questionNumber: Int) {
        currentQuestionNumber = questionNumber
        val options = arrayOf("📷 拍照", "🖼️ 从过程记录选择", "取消")
        
        AlertDialog.Builder(this)
            .setTitle("添加图片")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> selectPhotoFromRecord()
                }
            }
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
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
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_1 + currentQuestionNumber - 1)
        }
    }
    
    /**
     * 从过程记录中选择图片
     */
    private fun selectPhotoFromRecord() {
        val processRecord = ProcessRecordManager(this).loadProcessRecord()
        val allPhotos = processRecord?.stages?.values?.flatMap { stageRecord ->
            stageRecord.mediaItems
        }?.filter { mediaItem ->
            mediaItem.type == MediaType.PHOTO
        }?.map { mediaItem ->
            mediaItem.path
        } ?: emptyList()
        
        if (allPhotos.isEmpty()) {
            Toast.makeText(this, "过程记录中没有照片", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 显示照片选择对话框
        val photoNames: Array<CharSequence> = allPhotos.mapIndexed { index, _ ->
            "照片 ${index + 1}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("选择图片")
            .setItems(photoNames) { _, which ->
                if (which < allPhotos.size) {
                    addPhotoToQuestion(currentQuestionNumber, allPhotos[which])
                }
            }
            .show()
    }
    
    /**
     * 添加图片到指定问题
     */
    private fun addPhotoToQuestion(questionNumber: Int, photoPath: String) {
        when (questionNumber) {
            1 -> {
                photos1.add(photoPath)
                adapter1.notifyItemInserted(photos1.size - 1)
            }
            2 -> {
                photos2.add(photoPath)
                adapter2.notifyItemInserted(photos2.size - 1)
            }
            3 -> {
                photos3.add(photoPath)
                adapter3.notifyItemInserted(photos3.size - 1)
            }
        }
        autoSave()
    }
    
    /**
     * 删除图片
     */
    private fun deletePhoto(questionNumber: Int, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除图片")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                when (questionNumber) {
                    1 -> {
                        if (position < photos1.size) {
                            photos1.removeAt(position)
                            adapter1.notifyItemRemoved(position)
                            adapter1.notifyItemRangeChanged(position, photos1.size - position)
                        }
                    }
                    2 -> {
                        if (position < photos2.size) {
                            photos2.removeAt(position)
                            adapter2.notifyItemRemoved(position)
                            adapter2.notifyItemRangeChanged(position, photos2.size - position)
                        }
                    }
                    3 -> {
                        if (position < photos3.size) {
                            photos3.removeAt(position)
                            adapter3.notifyItemRemoved(position)
                            adapter3.notifyItemRangeChanged(position, photos3.size - position)
                        }
                    }
                }
                autoSave()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 查看图片
     */
    private fun viewPhoto(photoPath: String) {
        val file = File(photoPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 创建图片文件
     */
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "SUMMARY_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "创建照片文件失败", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 处理Activity结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_TAKE_PHOTO_1, REQUEST_TAKE_PHOTO_2, REQUEST_TAKE_PHOTO_3 -> {
                    currentPhotoPath?.let { path ->
                        val questionNumber = requestCode - REQUEST_TAKE_PHOTO_1 + 1
                        addPhotoToQuestion(questionNumber, path)
                    }
                }
            }
        }
    }
    
    /**
     * 加载已保存的数据
     */
    private fun loadSavedData() {
        try {
            val summary = summaryManager.loadSummary()
            if (summary != null) {
                binding.answer1Input.setText(summary.answer1)
                binding.answer2Input.setText(summary.answer2)
                binding.answer3Input.setText(summary.answer3)
                
                // 加载图片
                photos1.clear()
                photos1.addAll(summary.photos1)
                adapter1.notifyDataSetChanged()
                
                photos2.clear()
                photos2.addAll(summary.photos2)
                adapter2.notifyDataSetChanged()
                
                photos3.clear()
                photos3.addAll(summary.photos3)
                adapter3.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 自动保存（延迟保存，避免频繁写入）
     */
    private var autoSaveHandler: android.os.Handler? = null
    private var autoSaveRunnable: Runnable? = null
    
    private fun autoSave() {
        // 取消之前的保存任务
        autoSaveRunnable?.let { autoSaveHandler?.removeCallbacks(it) }
        
        // 延迟1秒后保存
        autoSaveRunnable = Runnable {
            saveSummary(silent = true)
        }
        autoSaveHandler = android.os.Handler(android.os.Looper.getMainLooper())
        autoSaveHandler?.postDelayed(autoSaveRunnable!!, 1000)
    }
    
    /**
     * 保存总结
     */
    private fun saveSummary(silent: Boolean = false) {
        try {
            val answer1 = binding.answer1Input.text?.toString()?.trim() ?: ""
            val answer2 = binding.answer2Input.text?.toString()?.trim() ?: ""
            // 问题三已屏蔽，使用空字符串
            val answer3 = ""

            val success = summaryManager.saveSummary(
                answer1 = answer1,
                answer2 = answer2,
                answer3 = answer3,
                photos1 = photos1,
                photos2 = photos2,
                photos3 = emptyList()
            )
            
            if (success && !silent) {
                Toast.makeText(this, "✅ 总结已保存", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (!silent) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 保存并上传总结到服务器
     */
    private fun saveAndUploadSummary() {
        try {
            // 先保存到本地
            val answer1 = binding.answer1Input.text?.toString()?.trim() ?: ""
            val answer2 = binding.answer2Input.text?.toString()?.trim() ?: ""
            // 问题三已屏蔽，使用空字符串
            val answer3 = ""

            val saveSuccess = summaryManager.saveSummary(
                answer1 = answer1,
                answer2 = answer2,
                answer3 = answer3,
                photos1 = photos1,
                photos2 = photos2,
                photos3 = emptyList()
            )
            
            if (!saveSuccess) {
                Toast.makeText(this, "保存失败，无法上传", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 获取团队信息
            val teamInfo = teamInfoManager.loadTeamInfo()
            if (teamInfo == null) {
                Toast.makeText(this, "团队信息不存在，无法上传", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 显示上传提示
            Toast.makeText(this, "正在上传到服务器...", Toast.LENGTH_SHORT).show()
            
            // 在后台线程上传
            Thread {
                try {
                    uploadSummaryToServer(teamInfo, answer1, answer2, answer3)
                } catch (e: Exception) {
                    Log.e(TAG, "上传失败: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 上传总结数据到服务器
     */
    private fun uploadSummaryToServer(
        teamInfo: com.campcooking.ar.data.TeamInfo,
        answer1: String,
        answer2: String,
        answer3: String
    ) {
        try {
            val studentId = "${teamInfo.school}_${teamInfo.grade}_${teamInfo.className}_${teamInfo.stoveNumber}"
            val serverUrl = serverConfigManager.getServerUrl()
            
            // 1. 先上传所有总结图片
            val allPhotos = photos1 + photos2 + photos3
            if (allPhotos.isNotEmpty()) {
                Log.d(TAG, "开始上传 ${allPhotos.size} 张总结图片")
                var uploadSuccessCount = 0
                var uploadFailCount = 0
                
                for (photoPath in allPhotos) {
                    try {
                        val file = File(photoPath)
                        if (file.exists()) {
                            val success = uploadSummaryPhoto(serverUrl, studentId, file)
                            if (success) {
                                uploadSuccessCount++
                                Log.d(TAG, "✅ 上传成功: ${file.name}")
                            } else {
                                uploadFailCount++
                                Log.w(TAG, "⚠️ 上传失败: ${file.name}")
                            }
                        } else {
                            uploadFailCount++
                            Log.w(TAG, "⚠️ 文件不存在: $photoPath")
                        }
                    } catch (e: Exception) {
                        uploadFailCount++
                        Log.e(TAG, "上传图片异常: $photoPath, ${e.message}", e)
                    }
                }
                
                Log.d(TAG, "总结图片上传完成: 成功 $uploadSuccessCount, 失败 $uploadFailCount")
            }
            
            // 2. 构建总结数据包
            val summaryData = mapOf(
                "answer1" to answer1,
                "answer2" to answer2,
                "answer3" to answer3,
                "photos1" to photos1,
                "photos2" to photos2,
                "photos3" to photos3
            )
            
            // 3. 构建完整数据包（只包含团队信息和总结数据）
            val dataPackage = mapOf(
                "teamInfo" to mapOf(
                    "school" to teamInfo.school,
                    "grade" to teamInfo.grade,
                    "className" to teamInfo.className,
                    "stoveNumber" to teamInfo.stoveNumber,
                    "memberCount" to teamInfo.memberCount,
                    "memberNames" to teamInfo.memberNames
                ),
                "processRecord" to null,
                "summaryData" to summaryData,
                "exportTime" to System.currentTimeMillis()
            )
            
            // 4. 提交到服务器
            val json = gson.toJson(dataPackage)
            val requestBody = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$serverUrl/api/submit")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "提交总结数据到: $serverUrl/api/submit")
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "上传成功: $responseBody")
                runOnUiThread {
                    Toast.makeText(this, "✅ 总结已保存并上传", Toast.LENGTH_SHORT).show()
                }
            } else {
                val errorMsg = "服务器错误: ${response.code}"
                Log.e(TAG, errorMsg)
                runOnUiThread {
                    Toast.makeText(this, "上传失败: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "上传总结数据异常: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 上传总结图片到服务器
     */
    private fun uploadSummaryPhoto(serverUrl: String, studentId: String, file: File): Boolean {
        return try {
            // 根据文件类型确定MIME类型
            val mimeType = when {
                file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                else -> "image/jpeg"
            }
            
            // 构建请求体（multipart/form-data）
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaType()))
                .addFormDataPart("original_path", file.absolutePath)
                .addFormDataPart("type", "PHOTO")
                .addFormDataPart("timestamp", System.currentTimeMillis().toString())
                .build()
            
            val encodedStudentId = java.net.URLEncoder.encode(studentId, "UTF-8")
            val request = Request.Builder()
                .url("$serverUrl/api/student/$encodedStudentId/media/upload")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "上传总结图片: ${file.name} (${file.length()} 字节)")
            
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                val responseBody = response.body?.string()
                Log.d(TAG, "上传成功: ${file.name}, 响应: $responseBody")
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "上传图片失败: ${file.name}, 响应码: ${response.code}, 错误: $errorBody")
            }
            
            response.close()
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "上传图片异常: ${file.name}, ${e.message}", e)
            false
        }
    }
    
    override fun onBackPressed() {
        // 返回前自动保存
        saveSummary(silent = true)
        super.onBackPressed()
    }
}

