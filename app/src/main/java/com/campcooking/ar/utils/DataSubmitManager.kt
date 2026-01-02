package com.campcooking.ar.utils

import android.content.Context
import android.util.Log
import com.campcooking.ar.data.ProcessRecord
import com.campcooking.ar.data.TeamInfo
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 数据提交管理器
 * 负责将学生端数据发送到教师端服务器
 */
class DataSubmitManager(private val context: Context) {
    
    private val gson = Gson()
    private val serverConfig = ServerConfigManager(context)
    private val teamInfoManager = TeamInfoManager(context)
    private val processRecordManager = ProcessRecordManager(context)
    private val summaryManager = SummaryManager(context)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "DataSubmitManager"
    }
    
    /**
     * 提交团队信息到服务器
     * @param teamInfo 团队信息
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun submitTeamInfo(
        teamInfo: TeamInfo,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // 在后台线程执行
        Thread {
            try {
                // 构建数据包（只包含团队信息）
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
                    "summaryData" to null,
                    "exportTime" to System.currentTimeMillis()
                )
                
                // 转换为JSON
                val json = gson.toJson(dataPackage)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                // 构建请求
                val serverUrl = serverConfig.getServerUrl()
                val request = Request.Builder()
                    .url("$serverUrl/api/submit")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "提交团队信息到: $serverUrl/api/submit")
                
                // 发送请求
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "提交成功: $responseBody")
                    onSuccess?.invoke()
                } else {
                    val errorMsg = "服务器错误: ${response.code}"
                    Log.e(TAG, errorMsg)
                    onError?.invoke(errorMsg)
                }
                
            } catch (e: IOException) {
                val errorMsg = "网络连接失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onError?.invoke(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "提交失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onError?.invoke(errorMsg)
            }
        }.start()
    }
    
    /**
     * 提交完整数据到服务器（包含团队信息、过程记录、课后总结）
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun submitAllData(
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // 在后台线程执行
        Thread {
            try {
                // 获取所有数据
                val teamInfo = teamInfoManager.loadTeamInfo()
                if (teamInfo == null) {
                    onError?.invoke("团队信息不存在")
                    return@Thread
                }
                
                val processRecord = processRecordManager.loadProcessRecord()
                val summaryData = summaryManager.loadSummary()
                
                // 获取团队分工数据
                val teamDivision = loadTeamDivision()
                
                // 构建数据包
                val dataPackage = mutableMapOf<String, Any?>(
                    "teamInfo" to mapOf(
                        "school" to teamInfo.school,
                        "grade" to teamInfo.grade,
                        "className" to teamInfo.className,
                        "stoveNumber" to teamInfo.stoveNumber,
                        "memberCount" to teamInfo.memberCount,
                        "memberNames" to teamInfo.memberNames
                    ),
                    "exportTime" to System.currentTimeMillis()
                )
                
                // 添加团队分工
                if (teamDivision != null && teamDivision.isNotEmpty()) {
                    dataPackage["teamDivision"] = teamDivision
                    Log.d(TAG, "✅ 包含团队分工数据: $teamDivision")
                } else {
                    dataPackage["teamDivision"] = null
                    Log.w(TAG, "⚠️ 未找到团队分工数据")
                }
                
                // 添加过程记录
                if (processRecord != null) {
                    dataPackage["processRecord"] = convertProcessRecordToMap(processRecord)
                } else {
                    dataPackage["processRecord"] = null
                }
                
                // 添加课后总结
                if (summaryData != null) {
                    dataPackage["summaryData"] = mapOf(
                        "answer1" to summaryData.answer1,
                        "answer2" to summaryData.answer2,
                        "answer3" to summaryData.answer3,
                        "photos1" to summaryData.photos1,
                        "photos2" to summaryData.photos2,
                        "photos3" to summaryData.photos3
                    )
                } else {
                    dataPackage["summaryData"] = null
                }
                
                // 转换为JSON
                val json = gson.toJson(dataPackage)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                // 构建请求
                val serverUrl = serverConfig.getServerUrl()
                val request = Request.Builder()
                    .url("$serverUrl/api/submit")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "提交完整数据到: $serverUrl/api/submit")
                
                // 发送请求
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "提交成功: $responseBody")
                    onSuccess?.invoke()
                } else {
                    val errorMsg = "服务器错误: ${response.code}"
                    Log.e(TAG, errorMsg)
                    onError?.invoke(errorMsg)
                }
                
            } catch (e: IOException) {
                val errorMsg = "网络连接失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onError?.invoke(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "提交失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                onError?.invoke(errorMsg)
            }
        }.start()
    }
    
    /**
     * 将ProcessRecord转换为Map（用于JSON序列化）
     */
    private fun convertProcessRecordToMap(processRecord: ProcessRecord): Map<String, Any?> {
        val stagesMap = mutableMapOf<String, Map<String, Any?>>()
        
        processRecord.stages.forEach { (stage, stageRecord) ->
            stagesMap[stage.name] = mapOf(
                "stage" to stage.name,
                "startTime" to stageRecord.startTime,
                "endTime" to stageRecord.endTime,
                "photos" to stageRecord.photos,
                "mediaItems" to stageRecord.mediaItems.map { mediaItem ->
                    mapOf(
                        "path" to mediaItem.path,
                        "type" to mediaItem.type.name,
                        "timestamp" to mediaItem.timestamp
                    )
                },
                "selfRating" to stageRecord.selfRating,
                "selectedTags" to stageRecord.selectedTags,
                "notes" to stageRecord.notes,
                "problemNotes" to stageRecord.problemNotes,
                "isCompleted" to stageRecord.isCompleted
            )
        }
        
        return mapOf(
            "teamInfo" to mapOf(
                "school" to processRecord.teamInfo.school,
                "grade" to processRecord.teamInfo.grade,
                "className" to processRecord.teamInfo.className,
                "stoveNumber" to processRecord.teamInfo.stoveNumber,
                "memberCount" to processRecord.teamInfo.memberCount,
                "memberNames" to processRecord.teamInfo.memberNames
            ),
            "startTime" to processRecord.startTime,
            "endTime" to processRecord.endTime,
            "stages" to stagesMap,
            "currentStage" to processRecord.currentStage.name,
            "overallNotes" to processRecord.overallNotes
        )
    }
    
    /**
     * 加载团队分工数据
     */
    private fun loadTeamDivision(): Map<String, String>? {
        try {
            val prefs = context.getSharedPreferences("team_division_prefs", android.content.Context.MODE_PRIVATE)
            val division = mutableMapOf<String, String>()
            
            val groupLeader = prefs.getString("groupLeader", "") ?: ""
            val groupCooking = prefs.getString("groupCooking", "") ?: ""
            val groupSoupRice = prefs.getString("groupSoupRice", "") ?: ""
            val groupFire = prefs.getString("groupFire", "") ?: ""
            val groupHealth = prefs.getString("groupHealth", "") ?: ""
            
            Log.d(TAG, "加载团队分工 - groupLeader: $groupLeader, groupCooking: $groupCooking, groupSoupRice: $groupSoupRice, groupFire: $groupFire, groupHealth: $groupHealth")
            
            if (groupLeader.isNotBlank()) division["groupLeader"] = groupLeader
            if (groupCooking.isNotBlank()) division["groupCooking"] = groupCooking
            if (groupSoupRice.isNotBlank()) division["groupSoupRice"] = groupSoupRice
            if (groupFire.isNotBlank()) division["groupFire"] = groupFire
            if (groupHealth.isNotBlank()) division["groupHealth"] = groupHealth
            
            val result = if (division.isNotEmpty()) division else null
            Log.d(TAG, "团队分工数据: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "加载团队分工失败: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 测试服务器连接
     */
    fun testConnection(
        serverIp: String? = null,
        serverPort: Int? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        Thread {
            try {
                val ip = serverIp ?: serverConfig.getServerIp()
                val port = serverPort ?: serverConfig.getServerPort()
                val url = "http://$ip:$port/api/status"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke("服务器响应错误: ${response.code}")
                }
            } catch (e: Exception) {
                onError?.invoke("连接失败: ${e.message}")
            }
        }.start()
    }
}

