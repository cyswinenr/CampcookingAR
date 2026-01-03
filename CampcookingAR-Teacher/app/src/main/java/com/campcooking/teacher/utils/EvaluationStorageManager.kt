package com.campcooking.teacher.utils

import android.content.Context
import android.content.SharedPreferences
import com.campcooking.teacher.data.EvaluationData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 评价数据本地存储管理器
 * 使用 SharedPreferences + Gson 实现数据持久化
 */
class EvaluationStorageManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "teacher_evaluation_prefs"
        private const val KEY_EVALUATIONS = "evaluations"  // 存储所有评价的JSON
        private const val KEY_PENDING_SYNC = "pending_sync"  // 待同步的评价ID列表
    }
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * 保存评价数据到本地
     * @return 是否保存成功
     */
    fun saveEvaluation(evaluation: EvaluationData): Boolean {
        return try {
            // 获取所有已保存的评价
            val allEvaluations = loadAllEvaluations().toMutableMap()
            
            // 更新或添加当前评价
            allEvaluations[evaluation.teamId] = evaluation
            
            // 保存到SharedPreferences
            val json = gson.toJson(allEvaluations)
            sharedPreferences.edit()
                .putString(KEY_EVALUATIONS, json)
                .apply()
            
            // 标记为待同步
            markAsPendingSync(evaluation.teamId)
            
            android.util.Log.d("EvaluationStorageManager", "✅ 评价已保存到本地: ${evaluation.teamId}")
            true
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "保存评价失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 加载指定团队的评价
     */
    fun loadEvaluation(teamId: String): EvaluationData? {
        return try {
            val allEvaluations = loadAllEvaluations()
            allEvaluations[teamId]
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "加载评价失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 加载所有评价
     */
    fun loadAllEvaluations(): Map<String, EvaluationData> {
        return try {
            val json = sharedPreferences.getString(KEY_EVALUATIONS, null)
            if (json != null && json.isNotEmpty()) {
                val type = object : TypeToken<Map<String, EvaluationData>>() {}.type
                gson.fromJson<Map<String, EvaluationData>>(json, type) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "加载所有评价失败: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * 标记评价为已同步
     */
    fun markAsSynced(teamId: String) {
        try {
            val pending = getPendingSyncList().toMutableSet()
            pending.remove(teamId)
            savePendingSyncList(pending)
            android.util.Log.d("EvaluationStorageManager", "✅ 评价已标记为已同步: $teamId")
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "标记已同步失败: ${e.message}", e)
        }
    }
    
    /**
     * 标记评价为待同步
     */
    private fun markAsPendingSync(teamId: String) {
        try {
            val pending = getPendingSyncList().toMutableSet()
            pending.add(teamId)
            savePendingSyncList(pending)
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "标记待同步失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取待同步的评价列表
     */
    fun getPendingSyncList(): Set<String> {
        return try {
            val json = sharedPreferences.getString(KEY_PENDING_SYNC, null)
            if (json != null && json.isNotEmpty()) {
                val type = object : TypeToken<Set<String>>() {}.type
                gson.fromJson<Set<String>>(json, type) ?: emptySet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "获取待同步列表失败: ${e.message}", e)
            emptySet()
        }
    }
    
    /**
     * 保存待同步列表
     */
    private fun savePendingSyncList(pending: Set<String>) {
        try {
            val json = gson.toJson(pending)
            sharedPreferences.edit()
                .putString(KEY_PENDING_SYNC, json)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "保存待同步列表失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取待同步的评价数据
     */
    fun getPendingEvaluations(): List<EvaluationData> {
        return try {
            val pendingIds = getPendingSyncList()
            val allEvaluations = loadAllEvaluations()
            pendingIds.mapNotNull { allEvaluations[it] }
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "获取待同步评价失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 删除评价（谨慎使用）
     */
    fun deleteEvaluation(teamId: String): Boolean {
        return try {
            val allEvaluations = loadAllEvaluations().toMutableMap()
            allEvaluations.remove(teamId)
            
            val json = gson.toJson(allEvaluations)
            sharedPreferences.edit()
                .putString(KEY_EVALUATIONS, json)
                .apply()
            
            // 从待同步列表中移除
            val pending = getPendingSyncList().toMutableSet()
            pending.remove(teamId)
            savePendingSyncList(pending)
            
            android.util.Log.d("EvaluationStorageManager", "✅ 评价已删除: $teamId")
            true
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "删除评价失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 清除所有数据（谨慎使用）
     */
    fun clearAll(): Boolean {
        return try {
            sharedPreferences.edit()
                .remove(KEY_EVALUATIONS)
                .remove(KEY_PENDING_SYNC)
                .apply()
            android.util.Log.d("EvaluationStorageManager", "✅ 所有评价数据已清除")
            true
        } catch (e: Exception) {
            android.util.Log.e("EvaluationStorageManager", "清除数据失败: ${e.message}", e)
            false
        }
    }
}

