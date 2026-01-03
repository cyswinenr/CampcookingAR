package com.campcooking.teacher.data

/**
 * 评价数据模型
 * 存储教师对每个团队的评价信息
 */
data class EvaluationData(
    val teamId: String,
    val teamName: String,
    val evaluations: Map<String, StageEvaluation> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取指定环节的评价
     */
    fun getStageEvaluation(stage: String): StageEvaluation? {
        return evaluations[stage]
    }
    
    /**
     * 检查是否所有环节都已评价
     */
    fun isAllStagesEvaluated(): Boolean {
        val allStages = listOf(
            "PREPARATION",
            "FIRE_MAKING",
            "COOKING_RICE",
            "COOKING_DISHES",
            "SHOWCASE",
            "CLEANING",
            "COMPLETED"
        )
        return allStages.all { stage ->
            evaluations[stage]?.let { 
                it.positiveTags.isNotEmpty() || 
                it.improvementTags.isNotEmpty() || 
                it.otherComment.isNotEmpty()
            } ?: false
        }
    }
}

/**
 * 单个环节的评价
 */
data class StageEvaluation(
    val stage: String,
    val positiveTags: List<String> = emptyList(),  // 选中的优点标签
    val improvementTags: List<String> = emptyList(),  // 选中的改进标签
    val otherComment: String = ""  // 其它评价文字
) {
    /**
     * 检查是否有评价内容
     */
    fun hasContent(): Boolean {
        return positiveTags.isNotEmpty() || 
               improvementTags.isNotEmpty() || 
               otherComment.isNotEmpty()
    }
}

