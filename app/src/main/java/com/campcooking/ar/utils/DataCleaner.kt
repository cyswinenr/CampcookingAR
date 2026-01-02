package com.campcooking.ar.utils

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * 数据清理工具类
 * 用于清理所有应用内部数据，复原应用状态
 */
class DataCleaner(private val context: Context) {
    
    /**
     * 清理所有应用数据
     * @return true 表示清理成功，false 表示清理失败
     */
    fun clearAllData(): Boolean {
        return try {
            // 1. 清理 SharedPreferences 数据
            clearSharedPreferences()
            
            // 2. 清理照片文件
            clearPhotoFiles()
            
            // 3. 清理视频文件
            clearVideoFiles()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 清理所有 SharedPreferences 数据
     */
    private fun clearSharedPreferences() {
        // 清理过程记录数据
        ProcessRecordManager(context).clearProcessRecord()
        
        // 清理团队信息数据
        TeamInfoManager(context).clearTeamInfo()
        
        // 清理团队分工数据
        context.getSharedPreferences("team_division_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        // 清理课后总结数据
        SummaryManager(context).clearSummary()
    }
    
    /**
     * 清理所有照片文件
     */
    private fun clearPhotoFiles() {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        picturesDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    /**
     * 清理所有视频文件
     */
    private fun clearVideoFiles() {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        moviesDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    /**
     * 获取将要清理的数据统计信息
     * @return 包含文件数量等信息的字符串
     */
    fun getDataSummary(): String {
        val photoCount = getPhotoFileCount()
        val videoCount = getVideoFileCount()
        val hasProcessRecord = ProcessRecordManager(context).hasRecord()
        val hasTeamInfo = TeamInfoManager(context).hasData()
        val hasSummary = SummaryManager(context).hasData()
        
        val summary = StringBuilder()
        summary.append("将清理以下数据：\n\n")
        
        if (hasProcessRecord) {
            summary.append("• 过程记录数据\n")
        }
        if (hasTeamInfo) {
            summary.append("• 团队信息数据\n")
        }
        if (hasSummary) {
            summary.append("• 课后总结数据\n")
        }
        if (photoCount > 0) {
            summary.append("• $photoCount 张照片\n")
        }
        if (videoCount > 0) {
            summary.append("• $videoCount 段视频\n")
        }
        
        if (summary.toString() == "将清理以下数据：\n\n") {
            summary.append("• 无数据需要清理")
        }
        
        return summary.toString()
    }
    
    /**
     * 获取照片文件数量
     */
    private fun getPhotoFileCount(): Int {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return picturesDir?.listFiles()?.size ?: 0
    }
    
    /**
     * 获取视频文件数量
     */
    private fun getVideoFileCount(): Int {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return moviesDir?.listFiles()?.size ?: 0
    }
}

