package com.campcooking.ar.data

/**
 * 媒体项 - 支持照片和视频
 */
data class MediaItem(
    val path: String,           // 文件路径
    val type: MediaType,        // 类型（照片或视频）
    val timestamp: Long = System.currentTimeMillis()  // 时间戳
)

/**
 * 媒体类型枚举
 */
enum class MediaType {
    PHOTO,      // 照片
    VIDEO       // 视频
}
