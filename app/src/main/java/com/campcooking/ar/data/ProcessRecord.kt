package com.campcooking.ar.data

/**
 * 完整的过程记录数据
 */
data class ProcessRecord(
    val teamInfo: TeamInfo,                 // 团队信息
    val startTime: Long = System.currentTimeMillis(),  // 开始时间
    var endTime: Long? = null,              // 结束时间
    val stages: MutableMap<CookingStage, StageRecord> = mutableMapOf(),  // 各阶段记录
    var currentStage: CookingStage = CookingStage.PREPARATION,  // 当前阶段
    var overallNotes: String = ""           // 总体备注
) {
    /**
     * 获取或创建阶段记录
     */
    fun getOrCreateStageRecord(stage: CookingStage): StageRecord {
        return stages.getOrPut(stage) {
            StageRecord(stage = stage)
        }
    }
    
    /**
     * 开始一个新阶段
     */
    fun startStage(stage: CookingStage) {
        val record = getOrCreateStageRecord(stage)
        if (record.startTime == 0L) {
            stages[stage] = record.copy(startTime = System.currentTimeMillis())
        }
        currentStage = stage
    }
    
    /**
     * 完成当前阶段
     */
    fun completeCurrentStage() {
        val record = stages[currentStage]
        if (record != null && !record.isCompleted) {
            record.endTime = System.currentTimeMillis()
            record.isCompleted = true
        }
    }
    
    /**
     * 进入下一阶段
     */
    fun moveToNextStage(): Boolean {
        completeCurrentStage()
        
        val allStages = CookingStage.getAllStages()
        val currentIndex = allStages.indexOf(currentStage)
        
        return if (currentIndex < allStages.size - 1) {
            val nextStage = allStages[currentIndex + 1]
            startStage(nextStage)
            true
        } else {
            // 已经是最后一个阶段
            endTime = System.currentTimeMillis()
            false
        }
    }
    
    /**
     * 获取总用时（分钟）
     */
    fun getTotalDurationMinutes(): Int {
        val end = endTime ?: System.currentTimeMillis()
        return ((end - startTime) / 1000 / 60).toInt()
    }
    
    /**
     * 获取总体评分（所有阶段的平均分）
     */
    fun getOverallRating(): Float {
        val completedStages = stages.values.filter { it.isCompleted && it.selfRating > 0 }
        if (completedStages.isEmpty()) return 0f
        return completedStages.map { it.selfRating }.average().toFloat()
    }
    
    /**
     * 获取完成的阶段数
     */
    fun getCompletedStagesCount(): Int {
        return stages.values.count { it.isCompleted }
    }
}

