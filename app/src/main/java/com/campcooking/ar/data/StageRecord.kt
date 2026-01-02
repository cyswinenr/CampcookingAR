package com.campcooking.ar.data

/**
 * 每个阶段的记录数据
 */
data class StageRecord(
    val stage: CookingStage,
    val startTime: Long = 0L,              // 开始时间戳
    var endTime: Long? = null,             // 结束时间戳
    val photos: MutableList<String> = mutableListOf(),  // 照片路径列表（保留兼容）
    val mediaItems: MutableList<MediaItem> = mutableListOf(),  // 媒体项列表（照片+视频）
    var selfRating: Int = 0,               // 自我评分 (1-5星)
    val selectedTags: MutableList<String> = mutableListOf(),  // 选择的标签
    var notes: String = "",                 // 可选备注（做得好的地方的其它）
    var problemNotes: String = "",          // 需要改进的地方的其它
    var isCompleted: Boolean = false        // 是否完成
) {
    /**
     * 获取该阶段的用时（分钟）
     */
    fun getDurationMinutes(): Int {
        if (startTime == 0L || endTime == null) return 0
        return ((endTime!! - startTime) / 1000 / 60).toInt()
    }
    
    /**
     * 获取用时的显示文本
     */
    fun getDurationText(): String {
        val minutes = getDurationMinutes()
        return when {
            minutes == 0 -> "进行中..."
            minutes < 60 -> "${minutes}分钟"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins == 0) "${hours}小时"
                else "${hours}小时${mins}分钟"
            }
        }
    }
    
    /**
     * 获取评分的显示文本
     */
    fun getRatingText(): String {
        return when (selfRating) {
            5 -> "非常好"
            4 -> "很好"
            3 -> "还行"
            2 -> "需努力"
            1 -> "待改进"
            else -> "未评价"
        }
    }
}

