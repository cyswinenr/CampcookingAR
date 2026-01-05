package com.campcooking.ar.utils

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 学生名单管理器
 * 负责从服务器获取学生名单
 */
class StudentListManager(private val context: Context) {
    
    private val serverConfig = ServerConfigManager(context)
    
    // HTTP客户端（使用较短的超时时间，因为只是获取文本数据）
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "StudentListManager"
    }
    
    /**
     * 根据炉号获取学生名单
     * @param stoveNumber 炉号（如"1号炉"、"1"等）
     * @param callback 回调函数，返回学生姓名列表
     */
    fun getStudentListByStove(
        stoveNumber: String,
        callback: (success: Boolean, names: List<String>?, error: String?) -> Unit
    ) {
        // 在后台线程执行网络请求
        Thread {
            try {
                val serverUrl = serverConfig.getServerUrl()
                val url = "$serverUrl/api/student_list/$stoveNumber"
                
                Log.d(TAG, "请求学生名单: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        // 解析JSON响应
                        val json = org.json.JSONObject(responseBody)
                        val status = json.getString("status")
                        
                        if (status == "success") {
                            val namesArray = json.getJSONArray("names")
                            val names = mutableListOf<String>()
                            
                            for (i in 0 until namesArray.length()) {
                                names.add(namesArray.getString(i))
                            }
                            
                            Log.d(TAG, "成功获取学生名单: ${names.size} 人")
                            
                            // 在主线程执行回调
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                callback(true, names, null)
                            }
                        } else {
                            val message = json.optString("message", "未知错误")
                            Log.w(TAG, "获取学生名单失败: $message")
                            
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                callback(false, null, message)
                            }
                        }
                    } else {
                        Log.e(TAG, "响应体为空")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            callback(false, null, "服务器响应为空")
                        }
                    }
                } else {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    Log.e(TAG, "HTTP错误: $errorMsg")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(false, null, errorMsg)
                    }
                }
                
                response.close()
            } catch (e: IOException) {
                Log.e(TAG, "网络请求失败: ${e.message}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(false, null, "网络连接失败: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取学生名单异常: ${e.message}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(false, null, "解析失败: ${e.message}")
                }
            }
        }.start()
    }
    
    /**
     * 测试服务器连接
     */
    fun testConnection(callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            try {
                val serverUrl = serverConfig.getServerUrl()
                val url = "$serverUrl/api/student_list"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (response.isSuccessful) {
                        callback(true, "连接成功")
                    } else {
                        callback(false, "HTTP ${response.code}")
                    }
                }
                
                response.close()
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(false, e.message ?: "连接失败")
                }
            }
        }.start()
    }
}

