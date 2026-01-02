package com.campcooking.ar.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 课后总结数据管理器
 * 使用 SharedPreferences + Gson 持久化保存总结数据
 */
class SummaryManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "summary_prefs"
        private const val KEY_ANSWER1 = "answer1"
        private const val KEY_ANSWER2 = "answer2"
        private const val KEY_ANSWER3 = "answer3"
        private const val KEY_PHOTOS1 = "photos1"
        private const val KEY_PHOTOS2 = "photos2"
        private const val KEY_PHOTOS3 = "photos3"
    }
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * 保存总结数据
     */
    fun saveSummary(
        answer1: String, 
        answer2: String, 
        answer3: String,
        photos1: List<String> = emptyList(),
        photos2: List<String> = emptyList(),
        photos3: List<String> = emptyList()
    ): Boolean {
        return try {
            sharedPreferences.edit().apply {
                putString(KEY_ANSWER1, answer1)
                putString(KEY_ANSWER2, answer2)
                putString(KEY_ANSWER3, answer3)
                
                // 使用Gson序列化图片列表
                putString(KEY_PHOTOS1, gson.toJson(photos1))
                putString(KEY_PHOTOS2, gson.toJson(photos2))
                putString(KEY_PHOTOS3, gson.toJson(photos3))
                
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 加载总结数据
     */
    fun loadSummary(): SummaryData? {
        val answer1 = sharedPreferences.getString(KEY_ANSWER1, "") ?: ""
        val answer2 = sharedPreferences.getString(KEY_ANSWER2, "") ?: ""
        val answer3 = sharedPreferences.getString(KEY_ANSWER3, "") ?: ""
        
        // 加载图片列表
        val photos1 = loadPhotoList(KEY_PHOTOS1)
        val photos2 = loadPhotoList(KEY_PHOTOS2)
        val photos3 = loadPhotoList(KEY_PHOTOS3)
        
        // 如果所有答案都为空，返回null
        if (answer1.isEmpty() && answer2.isEmpty() && answer3.isEmpty() 
            && photos1.isEmpty() && photos2.isEmpty() && photos3.isEmpty()) {
            return null
        }
        
        return SummaryData(answer1, answer2, answer3, photos1, photos2, photos3)
    }
    
    /**
     * 加载图片列表
     */
    private fun loadPhotoList(key: String): List<String> {
        return try {
            val json = sharedPreferences.getString(key, null)
            if (json != null) {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 清除总结数据
     */
    fun clearSummary() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * 检查是否有保存的数据
     */
    fun hasData(): Boolean {
        val answer1 = sharedPreferences.getString(KEY_ANSWER1, "") ?: ""
        val answer2 = sharedPreferences.getString(KEY_ANSWER2, "") ?: ""
        val answer3 = sharedPreferences.getString(KEY_ANSWER3, "") ?: ""
        val photos1 = loadPhotoList(KEY_PHOTOS1)
        val photos2 = loadPhotoList(KEY_PHOTOS2)
        val photos3 = loadPhotoList(KEY_PHOTOS3)
        
        return answer1.isNotEmpty() || answer2.isNotEmpty() || answer3.isNotEmpty()
            || photos1.isNotEmpty() || photos2.isNotEmpty() || photos3.isNotEmpty()
    }
}

/**
 * 总结数据模型
 */
data class SummaryData(
    val answer1: String,
    val answer2: String,
    val answer3: String,
    val photos1: List<String> = emptyList(),
    val photos2: List<String> = emptyList(),
    val photos3: List<String> = emptyList()
)

