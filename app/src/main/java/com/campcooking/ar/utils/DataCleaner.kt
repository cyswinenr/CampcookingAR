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
     * 清理模式枚举
     */
    enum class ClearMode {
        /** 仅清理应用内数据（SharedPreferences），保留文件 */
        APP_ONLY,
        /** 完全清理（包括用户拍摄的照片和视频文件） */
        FULL_DELETE
    }
    
    /**
     * 清理所有应用数据
     * @param mode 清理模式：APP_ONLY 仅清理应用数据，FULL_DELETE 完全删除用户文件
     * @return true 表示清理成功，false 表示清理失败
     */
    fun clearAllData(mode: ClearMode = ClearMode.APP_ONLY): Boolean {
        return try {
            // 1. 清理 SharedPreferences 数据
            clearSharedPreferences()
            
            // 2. 根据模式决定是否删除文件
            if (mode == ClearMode.FULL_DELETE) {
                // 清理照片文件（私有目录，用户拍摄的照片）
                clearPhotoFiles()
                
                // 清理视频文件（私有目录，用户拍摄的视频）
                clearVideoFiles()
            }
            
            // 注意：Documents/CampcookingAR/Videos/ 中的教学视频永远不删除
            
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
        
        // 清理菜单数据
        context.getSharedPreferences("menu_data", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    
    /**
     * 清理所有照片文件（私有目录，用户拍摄的照片）
     */
    private fun clearPhotoFiles() {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        picturesDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    try {
                        if (file.isFile) {
                            file.delete()
                        } else if (file.isDirectory) {
                            deleteDirectory(file)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    /**
     * 清理所有视频文件（私有目录，用户拍摄的视频）
     * 注意：不清理 Documents/CampcookingAR/Videos/ 中的教学视频
     */
    private fun clearVideoFiles() {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        moviesDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    try {
                        if (file.isFile) {
                            file.delete()
                        } else if (file.isDirectory) {
                            deleteDirectory(file)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    /**
     * 递归删除目录及其所有内容
     */
    private fun deleteDirectory(directory: File): Boolean {
        return try {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
            directory.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取将要清理的数据统计信息
     * @param mode 清理模式
     * @return 包含文件数量等信息的字符串
     */
    fun getDataSummary(mode: ClearMode = ClearMode.APP_ONLY): String {
        val photoCount = getPhotoFileCount()
        val videoCount = getVideoFileCount()
        val hasProcessRecord = ProcessRecordManager(context).hasRecord()
        val hasTeamInfo = TeamInfoManager(context).hasData()
        val hasSummary = SummaryManager(context).hasData()
        val hasMenu = hasMenuData()
        
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
        if (hasMenu) {
            summary.append("• 菜单内容数据\n")
        }
        if (photoCount > 0) {
            summary.append("• $photoCount 张照片（用户拍摄）")
            if (mode == ClearMode.APP_ONLY) {
                summary.append("（仅从应用移除，文件保留）")
            }
            summary.append("\n")
        }
        if (videoCount > 0) {
            summary.append("• $videoCount 段视频（用户拍摄）")
            if (mode == ClearMode.APP_ONLY) {
                summary.append("（仅从应用移除，文件保留）")
            }
            summary.append("\n")
        }
        
        if (summary.toString() == "将清理以下数据：\n\n") {
            summary.append("• 无数据需要清理")
        }
        
        summary.append("\n\n")
        if (mode == ClearMode.APP_ONLY) {
            summary.append("⚠️ 注意：文件将保留在平板中，仅从应用内移除\n")
        } else {
            summary.append("⚠️ 注意：将删除用户拍摄的照片和视频，无法恢复！\n")
        }
        summary.append("✅ 教学视频（Documents/CampcookingAR/Videos/）不会被删除")
        
        return summary.toString()
    }
    
    /**
     * 获取照片文件数量（私有目录，用户拍摄的照片）
     */
    private fun getPhotoFileCount(): Int {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return picturesDir?.listFiles()?.size ?: 0
    }
    
    /**
     * 获取视频文件数量（私有目录，用户拍摄的视频）
     */
    private fun getVideoFileCount(): Int {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return moviesDir?.listFiles()?.size ?: 0
    }
    
    /**
     * 检查是否有菜单数据
     */
    private fun hasMenuData(): Boolean {
        val prefs = context.getSharedPreferences("menu_data", Context.MODE_PRIVATE)
        val soup = prefs.getString("soup", "") ?: ""
        val dishesJson = prefs.getString("dishes", "[]") ?: "[]"
        return soup.isNotEmpty() || dishesJson != "[]"
    }
}

