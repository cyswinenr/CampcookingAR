package com.campcooking.ar.data

/**
 * 视频数据模型
 */
data class Video(
    val id: Int,                    // 视频ID
    val title: String,              // 视频标题
    val description: String,        // 视频描述
    val duration: String,           // 时长（如："5:30"）
    val fileName: String,           // 视频文件名（含扩展名，存放在外部存储）
    val category: String = "通用",  // 分类
    val order: Int = 0              // 排序
)

