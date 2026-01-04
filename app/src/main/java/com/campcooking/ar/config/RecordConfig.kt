package com.campcooking.ar.config

import com.campcooking.ar.data.CookingStage

/**
 * 过程记录的标签配置
 * 从教育角度设计，以正向鼓励为主
 */
object RecordConfig {

    // ==================== 进度要求配置 ====================
    const val MIN_PHOTOS_REQUIRED = 3          // 最少照片数量
    const val MIN_VIDEOS_REQUIRED = 1          // 最少视频数量
    
    // ==================== 视频录制配置 ====================
    const val MAX_VIDEO_DURATION_SECONDS = 30  // 视频最大录制时长（秒）
    
    /**
     * 成果展示阶段的特殊要求
     */
    const val SHOWCASE_GROUP_PHOTO_REQUIRED = 1    // 需要1张小组合照
    const val SHOWCASE_DISH_PHOTO_REQUIRED = 1     // 需要1张菜品合照
    const val SHOWCASE_SPEECH_VIDEO_REQUIRED = 1   // 需要1段语言表述视频

    /**
     * 各阶段的评价标签
     */
    val stageTagsMap = mapOf(
        CookingStage.PREPARATION to TagGroup(
            positive = listOf("准备充分", "分工明确", "工具齐全", "检查仔细"),
            problems = listOf("准备不足", "工具缺失", "分工不清")
        ),
        
        CookingStage.FIRE_MAKING to TagGroup(
            positive = listOf("速度很快", "柴火摆放好", "通风良好", "安全操作", "火势稳定"),
            problems = listOf("多次点火", "柴火潮湿", "烟雾太大", "火势不稳")
        ),
        
        CookingStage.COOKING_RICE to TagGroup(
            positive = listOf("水量正确", "火候控制好", "有及时退火", "没频繁掀盖", "软硬适中"),
            problems = listOf("煮糊了", "夹生", "水放多了", "水放少了")
        ),
        
        CookingStage.COOKING_DISHES to TagGroup(
            positive = listOf("刀工整齐", "调味恰当", "火候适中", "色香味好", "摆盘美观"),
            problems = listOf("炒糊了", "太咸/太淡", "不熟", "火候不对")
        ),
        
        CookingStage.SHOWCASE to TagGroup(
            positive = listOf("展示精彩", "分享到位", "讲解清晰", "成果突出", "团队协作"),
            problems = listOf("展示不足", "讲解不清", "准备不充分")
        ),
        
        CookingStage.CLEANING to TagGroup(
            positive = listOf("收拾干净", "分类整理", "工具归位", "场地整洁", "垃圾分类"),
            problems = listOf("收拾不及时", "场地脏乱", "工具散乱", "垃圾未清理")
        ),
        
        CookingStage.COMPLETED to TagGroup(
            positive = listOf("整体表现好", "团队配合好", "流程顺畅", "完成度高", "表现优秀"),
            problems = listOf("配合不足", "流程混乱", "完成度低")
        )
    )
    
    /**
     * 团队协作标签（适用于所有阶段）
     */
    val teamworkTags = listOf(
        "分工明确",
        "互相帮助",
        "沟通顺畅",
        "效率很高",
        "全员参与"
    )
    
    /**
     * 评分等级说明
     */
    val ratingDescriptions = mapOf(
        5 to RatingLevel("非常好", "我们做得很棒！", "⭐⭐⭐⭐⭐"),
        4 to RatingLevel("很好", "表现不错！", "⭐⭐⭐⭐"),
        3 to RatingLevel("还行", "还可以，继续努力", "⭐⭐⭐"),
        2 to RatingLevel("需努力", "下次要更认真", "⭐⭐"),
        1 to RatingLevel("待改进", "需要多练习", "⭐")
    )
    
    /**
     * 温馨提示语
     */
    val stageHints = mapOf(
        CookingStage.PREPARATION to "检查食材和工具，做好分工哦！",
        CookingStage.FIRE_MAKING to "注意安全，柴火要摆放整齐，留出通风口！",
        CookingStage.COOKING_RICE to "水量很重要，记得观察火候及时调整！",
        CookingStage.COOKING_DISHES to "掌握好火候，注意翻炒，让菜品色香味俱全！",
        CookingStage.SHOWCASE to "📸 请拍摄小组合照、完成菜品合照，并录制语言表述视频，展示你们的成果！",
        CookingStage.CLEANING to "记得清理场地，收拾工具，做好垃圾分类，爱护环境！",
        CookingStage.COMPLETED to "回顾整个野炊过程，总结整体表现，给自己一个评价吧！"
    )

    // ==================== 智能提示语配置 ====================
    /**
     * 获取成果展示阶段的特殊提示语
     */
    fun getShowcaseProgressHint(photoCount: Int, videoCount: Int): String {
        val hasGroupPhoto = photoCount >= 1  // 假设至少1张照片可以是小组合照
        val hasDishPhoto = photoCount >= 2   // 假设至少2张照片包含菜品合照
        val hasSpeechVideo = videoCount >= 1 // 至少1段视频是语言表述
        
        return when {
            !hasGroupPhoto && !hasDishPhoto && !hasSpeechVideo ->
                "📸 请拍摄：1张小组合照 + 1张菜品合照 + 1段语言表述视频"
            
            hasGroupPhoto && !hasDishPhoto && !hasSpeechVideo ->
                "✅ 小组合照已拍！还需要：1张菜品合照 + 1段语言表述视频"
            
            hasGroupPhoto && hasDishPhoto && !hasSpeechVideo ->
                "✅ 小组合照和菜品合照已拍！还需要：1段语言表述视频"
            
            hasGroupPhoto && !hasDishPhoto && hasSpeechVideo ->
                "✅ 小组合照和语言表述已录！还需要：1张菜品合照"
            
            !hasGroupPhoto && hasDishPhoto && hasSpeechVideo ->
                "✅ 菜品合照和语言表述已完成！还需要：1张小组合照"
            
            !hasGroupPhoto && hasDishPhoto && !hasSpeechVideo ->
                "✅ 菜品合照已拍！还需要：1张小组合照 + 1段语言表述视频"
            
            !hasGroupPhoto && !hasDishPhoto && hasSpeechVideo ->
                "✅ 语言表述已录！还需要：1张小组合照 + 1张菜品合照"
            
            else -> "🎉 太棒了！小组合照、菜品合照和语言表述都已完成！可以进行自我评价了！"
        }
    }
    
    /**
     * 根据进度显示不同的提示语
     */
    fun getProgressHint(photoCount: Int, videoCount: Int, stage: CookingStage? = null): String {
        // 成果展示阶段使用特殊提示语
        if (stage == CookingStage.SHOWCASE) {
            return getShowcaseProgressHint(photoCount, videoCount)
        }
        
        val photoTarget = MIN_PHOTOS_REQUIRED
        val videoTarget = MIN_VIDEOS_REQUIRED

        return when {
            // 还没开始
            photoCount == 0 && videoCount == 0 ->
                "💡 提示：开始记录吧！至少需要${photoTarget}张照片和${videoTarget}段视频哦"

            // 有照片但没视频
            photoCount > 0 && videoCount == 0 ->
                when {
                    photoCount < photoTarget -> "📸 已有${photoCount}张照片，还需要${photoTarget - photoCount}张，别忘了拍视频哦"
                    else -> "✅ 照片已达标！🎥 还需要1段视频就能完成本环节了"
                }

            // 有视频但没照片
            photoCount == 0 && videoCount > 0 ->
                "🎥 视频已录制！📸 还需要${photoTarget}张照片才能完成本环节哦"

            // 两者都有但未达标
            photoCount < photoTarget && videoCount < videoTarget ->
                "📸 还需要${photoTarget - photoCount}张照片 • 🎥 还需要${videoTarget - videoCount}段视频"

            // 照片达标但视频未达标
            photoCount >= photoTarget && videoCount < videoTarget ->
                "✅ 照片已完成！🎥 还需要${videoTarget - videoCount}段视频就能完成本环节了"

            // 视频达标但照片未达标
            photoCount < photoTarget && videoCount >= videoTarget ->
                "🎥 视频已完成！📸 还需要${photoTarget - photoCount}张照片就能完成本环节了"

            // 全部达标
            else -> "🎉 太棒了！本环节记录要求已全部完成，可以进行自我评价了！"
        }
    }

    /**
     * 鼓励反馈信息
     */
    fun getEncouragementMessage(photoCount: Int, videoCount: Int): String? {
        val photoTarget = MIN_PHOTOS_REQUIRED
        val videoTarget = MIN_VIDEOS_REQUIRED

        return when {
            // 达成全部目标
            photoCount >= photoTarget && videoCount >= videoTarget ->
                "🎉 恭喜！你已经完成了所有记录要求！表现得真棒！"

            // 达成照片目标
            photoCount >= photoTarget && videoCount == 0 ->
                "✨ 照片目标已达成！再拍一段视频就完美了！"

            // 达成视频目标
            videoCount >= videoTarget && photoCount == 0 ->
                "✨ 视频已录制完成！继续拍照吧，还差${photoTarget}张！"

            // 照片过半
            photoCount >= photoTarget / 2 && photoCount < photoTarget && videoCount == 0 ->
                "💪 照片已完成一半！加油，还差${photoTarget - photoCount}张！"

            // 第一张照片
            photoCount == 1 && videoCount == 0 ->
                "👍 很好！第一张照片已记录，继续保持！"

            // 第一段视频
            videoCount == 1 && photoCount == 0 ->
                "🎬 视频录制成功！现在开始拍照记录吧！"

            else -> null
        }
    }
}

/**
 * 标签组
 */
data class TagGroup(
    val positive: List<String>,     // 正面标签（优点）
    val problems: List<String>      // 问题标签
)

/**
 * 评分等级
 */
data class RatingLevel(
    val title: String,              // 等级名称
    val description: String,        // 描述语
    val stars: String               // 星级显示
)

