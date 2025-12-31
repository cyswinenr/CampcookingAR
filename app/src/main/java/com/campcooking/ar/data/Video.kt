package com.campcooking.ar.data

/**
 * 视频数据模型
 */
data class Video(
    val id: Int,                    // 视频ID
    val title: String,              // 视频标题
    val description: String,        // 视频描述
    val duration: String,           // 时长（如："5:30"）
    val resourceName: String,       // 资源文件名（不含扩展名）
    val category: String = "通用",  // 分类
    val order: Int = 0              // 排序
)

