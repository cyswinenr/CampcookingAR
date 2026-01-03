package com.campcooking.teacher

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campcooking.teacher.adapter.TeamListAdapter
import com.campcooking.teacher.config.EvaluationConfig
import com.campcooking.teacher.data.EvaluationData
import com.campcooking.teacher.data.StageEvaluation
import com.campcooking.teacher.data.TeamInfo
import com.campcooking.teacher.databinding.ActivityEvaluationBinding
import com.campcooking.teacher.utils.EvaluationStorageManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 教师评价Activity
 * 左侧显示团队列表，右侧显示评价界面
 */
class EvaluationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvaluationBinding
    private lateinit var teamAdapter: TeamListAdapter
    private val teams = mutableListOf<TeamInfo>()
    private var currentTeam: TeamInfo? = null
    private val evaluationData = mutableMapOf<String, StageEvaluation>()
    private val gson = Gson()
    private lateinit var storageManager: EvaluationStorageManager
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

        binding = ActivityEvaluationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化本地存储管理器
        storageManager = EvaluationStorageManager(this)

        setupTeamList()
        setupListeners()
        loadTeams()
    }

    /**
     * 设置团队列表
     */
    private fun setupTeamList() {
        teamAdapter = TeamListAdapter(teams) { team ->
            selectTeam(team)
        }
        binding.teamListRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.teamListRecyclerView.adapter = teamAdapter
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            saveEvaluation()
        }
    }

    /**
     * 加载团队列表（使用新的API接口）
     */
    private fun loadTeams() {
        val serverUrl = getServerUrl()
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "请先配置服务器地址", Toast.LENGTH_LONG).show()
            return
        }

        val url = "$serverUrl/api/evaluation/teams"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EvaluationActivity, "加载团队列表失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@EvaluationActivity, "加载团队列表失败: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val responseBody = response.body?.string() ?: return
                    val json = gson.fromJson(responseBody, Map::class.java) as Map<*, *>
                    val teamsData = json["teams"] as? List<Map<*, *>> ?: emptyList()

                    android.util.Log.d("EvaluationActivity", "加载团队列表（新API） - 收到 ${teamsData.size} 个团队")
                    
                    val teamList = teamsData.map { data ->
                        val teamData = data as Map<String, Any?>
                        val teamId = teamData["id"] as? String ?: teamData["teamId"] as? String ?: ""
                        val teamName = teamData["teamName"] as? String ?: teamId
                        
                        android.util.Log.d("EvaluationActivity", "团队ID: '$teamId', 名称: '$teamName'")
                        
                        // 创建简化的TeamInfo对象
                        TeamInfo(
                            id = teamId,
                            teamName = teamName,
                            school = "",
                            grade = "",
                            className = "",
                            stoveNumber = "",
                            memberCount = 0,
                            memberNames = "",
                            groupLeader = ""
                        )
                    }

                    runOnUiThread {
                        teams.clear()
                        teams.addAll(teamList)
                        teamAdapter.updateTeams(teams)
                        
                        android.util.Log.d("EvaluationActivity", "成功加载 ${teams.size} 个团队")
                        
                        if (teams.isEmpty()) {
                            Toast.makeText(this@EvaluationActivity, "暂无可评价的团队", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EvaluationActivity", "解析团队数据失败: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this@EvaluationActivity, "解析团队数据失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    /**
     * 选择团队
     */
    private fun selectTeam(team: TeamInfo) {
        currentTeam = team
        binding.selectedTeamNameText.text = team.getDisplayName()
        binding.selectedTeamDivisionText.text = team.getDivisionText()
        
        // 清空之前的评价数据
        evaluationData.clear()
        
        // 生成评价界面
        generateEvaluationUI()
        
        // 加载已保存的评价数据（如果有）
        loadSavedEvaluation(team.id)
    }

    /**
     * 生成评价界面（7个环节）
     */
    private fun generateEvaluationUI() {
        binding.evaluationContainer.removeAllViews()

        val allStages = EvaluationConfig.getAllStages()
        
        allStages.forEach { stage ->
            val stageCard = createStageEvaluationCard(stage)
            binding.evaluationContainer.addView(stageCard)
        }
    }

    /**
     * 创建单个环节的评价卡片
     */
    private fun createStageEvaluationCard(stage: String): View {
        val cardView = android.widget.LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16.dpToPx())
            }
        }

        // 环节标题
        val titleView = android.widget.TextView(this).apply {
            text = "${EvaluationConfig.getStageEmoji(stage)} ${EvaluationConfig.getStageDisplayName(stage)}"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12.dpToPx())
            }
        }
        cardView.addView(titleView)

        // 获取评价标签
        val tagGroup = EvaluationConfig.getEvaluationTags(stage) ?: return cardView

        // 获取已保存的评价数据（如果有）
        val savedEval = evaluationData[stage]
        val selectedPositiveTags = savedEval?.positiveTags ?: emptyList()
        val selectedImprovementTags = savedEval?.improvementTags ?: emptyList()

        // 做得好的地方
        val positiveSection = createTagSection(
            "👍 做得好的地方（可多选）",
            tagGroup.positive,
            stage,
            true,
            selectedPositiveTags
        )
        cardView.addView(positiveSection)

        // 需要改进的地方
        val improvementSection = createTagSection(
            "💪 需要改进的地方（可多选）",
            tagGroup.improvements,
            stage,
            false,
            selectedImprovementTags
        )
        cardView.addView(improvementSection)

        // 其它评价输入框
        val otherComment = savedEval?.otherComment ?: ""
        val otherCommentSection = createOtherCommentSection(stage, otherComment)
        cardView.addView(otherCommentSection)

        return cardView
    }

    /**
     * 创建标签选择区域
     */
    private fun createTagSection(
        title: String,
        tags: List<String>,
        stage: String,
        isPositive: Boolean,
        selectedTags: List<String> = emptyList()
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12.dpToPx())
            }
        }

        // 标题
        val titleView = android.widget.TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8.dpToPx())
            }
        }
        container.addView(titleView)

        // 标签组
        val chipGroup = ChipGroup(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isSingleSelection = false
            // 通过设置padding来模拟间距
            setPadding(0, 0, 0, 0)
        }

        tags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                isChecked = isSelected
                
                // 根据初始选中状态和标签类型设置颜色
                updateChipColor(this, isSelected, isPositive)
                
                setOnCheckedChangeListener { _, isChecked ->
                    updateEvaluationData(stage, tag, isChecked, isPositive)
                    // 更新颜色
                    updateChipColor(this, isChecked, isPositive)
                }
                
                // 设置margin来实现间距
                val margin = 4.dpToPx()
                layoutParams = ChipGroup.LayoutParams(
                    ChipGroup.LayoutParams.WRAP_CONTENT,
                    ChipGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(margin, margin, margin, margin)
                }
            }
            chipGroup.addView(chip)
        }

        container.addView(chipGroup)
        return container
    }

    /**
     * 创建其它评价输入框
     */
    private fun createOtherCommentSection(stage: String, savedText: String = ""): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16.dpToPx())
            }
        }

        // 标题
        val titleView = android.widget.TextView(this).apply {
            text = "📝 其它评价"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8.dpToPx())
            }
        }
        container.addView(titleView)

        // 输入框
        val textInputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            hint = "请输入其它评价或建议..."
            setHintTextColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.fire_orange)
            ))
        }

        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minLines = 3
            maxLines = 5
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            // 设置已保存的文字
            if (savedText.isNotEmpty()) {
                setText(savedText)
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    updateEvaluationOtherComment(stage, s?.toString() ?: "")
                }
            })
        }

        textInputLayout.addView(editText)
        container.addView(textInputLayout)

        return container
    }

    /**
     * 更新评价数据
     */
    private fun updateEvaluationData(stage: String, tag: String, isChecked: Boolean, isPositive: Boolean) {
        val stageEval = evaluationData.getOrPut(stage) {
            StageEvaluation(stage)
        }

        if (isPositive) {
            if (isChecked) {
                if (!stageEval.positiveTags.contains(tag)) {
                    evaluationData[stage] = stageEval.copy(
                        positiveTags = stageEval.positiveTags + tag
                    )
                }
            } else {
                evaluationData[stage] = stageEval.copy(
                    positiveTags = stageEval.positiveTags.filter { it != tag }
                )
            }
        } else {
            if (isChecked) {
                if (!stageEval.improvementTags.contains(tag)) {
                    evaluationData[stage] = stageEval.copy(
                        improvementTags = stageEval.improvementTags + tag
                    )
                }
            } else {
                evaluationData[stage] = stageEval.copy(
                    improvementTags = stageEval.improvementTags.filter { it != tag }
                )
            }
        }
    }

    /**
     * 更新Chip的颜色
     */
    private fun updateChipColor(chip: Chip, isChecked: Boolean, isPositive: Boolean) {
        if (isChecked) {
            if (isPositive) {
                // 做得好的地方：选中时变成深绿色
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.nature_green)
                )
                chip.setTextColor(getColor(R.color.white))
            } else {
                // 需要改进的地方：选中时变成粉红色
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.fire_coral)
                )
                chip.setTextColor(getColor(R.color.white))
            }
        } else {
            // 未选中时恢复默认颜色
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                getColor(R.color.surface_variant)
            )
            chip.setTextColor(getColor(R.color.text_primary))
        }
    }

    /**
     * 更新其它评价文字
     */
    private fun updateEvaluationOtherComment(stage: String, comment: String) {
        val stageEval = evaluationData.getOrPut(stage) {
            StageEvaluation(stage)
        }
        evaluationData[stage] = stageEval.copy(otherComment = comment)
    }

    /**
     * 保存评价
     * 先保存到本地，再尝试同步到服务器
     */
    private fun saveEvaluation() {
        val team = currentTeam ?: run {
            Toast.makeText(this, "请先选择团队", Toast.LENGTH_SHORT).show()
            return
        }

        val evaluation = EvaluationData(
            teamId = team.id,
            teamName = team.getDisplayName(),
            evaluations = evaluationData.toMap(),
            timestamp = System.currentTimeMillis()
        )

        // 1. 先保存到本地（确保数据不丢失）
        val savedLocally = storageManager.saveEvaluation(evaluation)
        if (!savedLocally) {
            Toast.makeText(this, "保存到本地失败", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 显示本地保存成功
        Toast.makeText(this, "✅ 评价已保存到本地", Toast.LENGTH_SHORT).show()
        android.util.Log.d("EvaluationActivity", "✅ 评价已保存到本地: ${evaluation.teamId}")

        // 2. 尝试同步到服务器（后台进行，不阻塞UI）
        syncToServer(evaluation)
    }
    
    /**
     * 同步评价到服务器
     */
    private fun syncToServer(evaluation: EvaluationData) {
        val serverUrl = getServerUrl()
        if (serverUrl.isEmpty()) {
            android.util.Log.w("EvaluationActivity", "服务器地址未配置，跳过同步")
            return
        }

        val url = "$serverUrl/api/evaluation"
        val json = gson.toJson(evaluation)
        
        // 添加详细日志输出
        android.util.Log.d("EvaluationActivity", "========== 开始同步评价到服务器 ==========")
        android.util.Log.d("EvaluationActivity", "URL: $url")
        android.util.Log.d("EvaluationActivity", "teamId: '${evaluation.teamId}' (类型: ${evaluation.teamId::class.java.simpleName}, 长度: ${evaluation.teamId.length})")
        android.util.Log.d("EvaluationActivity", "teamName: ${evaluation.teamName}")
        android.util.Log.d("EvaluationActivity", "评价数量: ${evaluation.evaluations.size}")
        android.util.Log.d("EvaluationActivity", "评价环节: ${evaluation.evaluations.keys.joinToString(", ")}")
        android.util.Log.d("EvaluationActivity", "JSON数据: $json")
        android.util.Log.d("EvaluationActivity", "==========================================")
        
        val requestBody = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("EvaluationActivity", "同步评价到服务器失败: ${e.message}", e)
                // 网络失败不影响，数据已保存在本地，稍后可以重试
                runOnUiThread {
                    // 不显示错误提示，避免打扰用户
                    // 数据已保存在本地，可以稍后重试
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("EvaluationActivity", "同步评价响应 - code: ${response.code}, body: $responseBody")
                
                if (!response.isSuccessful) {
                    try {
                        val errorJson = gson.fromJson(responseBody, Map::class.java) as? Map<*, *>
                        val errorMessage = errorJson?.get("message") as? String ?: "未知错误"
                        
                        // 提取调试信息（如果有）
                        val debugInfo = errorJson?.get("debug_info") as? Map<*, *>
                        if (debugInfo != null) {
                            android.util.Log.e("EvaluationActivity", "========== 服务器错误详情 ==========")
                            android.util.Log.e("EvaluationActivity", "错误消息: $errorMessage")
                            android.util.Log.e("EvaluationActivity", "接收到的team_id: ${debugInfo["received_team_id"]}")
                            android.util.Log.e("EvaluationActivity", "数据库中的团队总数: ${debugInfo["total_teams"]}")
                            android.util.Log.e("EvaluationActivity", "示例team_id: ${debugInfo["sample_team_ids"]}")
                            android.util.Log.e("EvaluationActivity", "相似的team_id: ${debugInfo["similar_team_ids"]}")
                            android.util.Log.e("EvaluationActivity", "====================================")
                        } else {
                            android.util.Log.e("EvaluationActivity", "同步评价失败: $errorMessage (code: ${response.code})")
                        }
                        
                        // 服务器错误不影响，数据已保存在本地
                        // 可以稍后重试同步
                    } catch (e: Exception) {
                        android.util.Log.e("EvaluationActivity", "解析错误响应失败: ${e.message}", e)
                        android.util.Log.e("EvaluationActivity", "原始响应: $responseBody")
                    }
                    return
                }

                try {
                    val resultJson = gson.fromJson(responseBody, Map::class.java) as? Map<*, *>
                    val message = resultJson?.get("message") as? String ?: "评价同步成功"
                    android.util.Log.d("EvaluationActivity", "✅ 评价同步成功: $message")
                    
                    // 标记为已同步
                    storageManager.markAsSynced(evaluation.teamId)
                    
                    runOnUiThread {
                        Toast.makeText(this@EvaluationActivity, "✅ 评价已同步到服务器", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EvaluationActivity", "解析成功响应失败: ${e.message}", e)
                }
            }
        })
    }

    /**
     * 加载已保存的评价
     * 优先从本地加载，如果本地没有则尝试从服务器加载
     */
    private fun loadSavedEvaluation(teamId: String) {
        // 1. 先尝试从本地加载
        val localEvaluation = storageManager.loadEvaluation(teamId)
        if (localEvaluation != null) {
            android.util.Log.d("EvaluationActivity", "从本地加载评价: $teamId")
            evaluationData.clear()
            evaluationData.putAll(localEvaluation.evaluations)
            // 重新生成UI以显示已保存的评价
            generateEvaluationUI()
            
            // 如果本地有数据但未同步，尝试同步
            val pendingList = storageManager.getPendingSyncList()
            if (pendingList.contains(teamId)) {
                android.util.Log.d("EvaluationActivity", "发现待同步的评价，尝试同步: $teamId")
                syncToServer(localEvaluation)
            }
            return
        }
        
        // 2. 如果本地没有，尝试从服务器加载
        val serverUrl = getServerUrl()
        if (serverUrl.isEmpty()) {
            android.util.Log.d("EvaluationActivity", "服务器地址未配置，跳过从服务器加载")
            return
        }

        val url = "$serverUrl/api/evaluation/$teamId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 忽略错误，可能是首次评价
                android.util.Log.d("EvaluationActivity", "从服务器加载评价失败（可能是首次评价）: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    android.util.Log.d("EvaluationActivity", "从服务器加载评价失败: code=${response.code}")
                    return
                }

                try {
                    val responseBody = response.body?.string() ?: return
                    val json = gson.fromJson(responseBody, Map::class.java) as Map<*, *>
                    val evaluations = json["evaluations"] as? Map<*, *> ?: emptyMap<Any, Any>()
                    
                    runOnUiThread {
                        evaluationData.clear()
                        // 转换评价数据格式
                        evaluations.forEach { (stage, stageEvalData) ->
                            if (stage is String && stageEvalData is Map<*, *>) {
                                val positiveTags = (stageEvalData["positiveTags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val improvementTags = (stageEvalData["improvementTags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val otherComment = stageEvalData["otherComment"] as? String ?: ""
                                
                                evaluationData[stage] = StageEvaluation(
                                    stage = stage,
                                    positiveTags = positiveTags,
                                    improvementTags = improvementTags,
                                    otherComment = otherComment
                                )
                            }
                        }
                        
                        // 保存到本地（从服务器加载的数据也保存到本地）
                        if (evaluationData.isNotEmpty()) {
                            val evaluation = EvaluationData(
                                teamId = teamId,
                                teamName = currentTeam?.getDisplayName() ?: "",
                                evaluations = evaluationData.toMap(),
                                timestamp = System.currentTimeMillis()
                            )
                            storageManager.saveEvaluation(evaluation)
                            storageManager.markAsSynced(teamId)  // 从服务器加载的标记为已同步
                        }
                        
                        // 重新生成UI以显示已保存的评价
                        generateEvaluationUI()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EvaluationActivity", "解析服务器评价数据失败: ${e.message}", e)
                }
            }
        })
    }

    /**
     * 获取服务器地址
     */
    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("teacher_settings", MODE_PRIVATE)
        val savedUrl = prefs.getString("server_url", "")
        return savedUrl ?: "http://192.168.1.100:5000"
    }

    /**
     * dp转px
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}

