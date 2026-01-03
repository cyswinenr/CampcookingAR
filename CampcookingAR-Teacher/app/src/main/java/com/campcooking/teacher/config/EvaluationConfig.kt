package com.campcooking.teacher.config

/**
 * 教师评价标签配置
 * 按照教育教学维度设计，每个环节包含10个优点标签和10个改进标签
 */
object EvaluationConfig {
    
    /**
     * 评价标签组
     */
    data class EvaluationTagGroup(
        val positive: List<String>,     // 做得好的地方（10个）
        val improvements: List<String>  // 需要改进的地方（10个）
    )
    
    /**
     * 各环节的评价标签配置
     */
    val stageEvaluationTags = mapOf(
        "PREPARATION" to EvaluationTagGroup(
            positive = listOf(
                "准备充分，工具齐全",
                "分工明确，职责清晰",
                "检查仔细，无遗漏",
                "安全意识强，防护到位",
                "团队协作好，配合默契",
                "计划周全，考虑细致",
                "材料准备充足，质量好",
                "时间安排合理，效率高",
                "沟通顺畅，信息传达准确",
                "组织有序，流程清晰"
            ),
            improvements = listOf(
                "准备不足，工具缺失",
                "分工不清，职责混乱",
                "检查不仔细，有遗漏",
                "安全意识薄弱，防护不足",
                "团队配合不够，各自为政",
                "计划不周全，考虑不周",
                "材料准备不足，质量差",
                "时间安排不合理，效率低",
                "沟通不畅，信息传达有误",
                "组织混乱，流程不清"
            )
        ),
        
        "FIRE_MAKING" to EvaluationTagGroup(
            positive = listOf(
                "生火速度快，效率高",
                "柴火摆放合理，通风良好",
                "火势控制稳定，持续燃烧",
                "安全操作规范，无危险",
                "技巧掌握熟练，操作流畅",
                "观察仔细，及时调整",
                "团队配合好，分工明确",
                "环保意识强，用柴合理",
                "耐心细致，不急于求成",
                "经验丰富，处理得当"
            ),
            improvements = listOf(
                "生火速度慢，多次点火",
                "柴火摆放不当，通风不畅",
                "火势控制不稳，容易熄灭",
                "安全操作不规范，有隐患",
                "技巧掌握不熟练，操作生疏",
                "观察不够仔细，调整不及时",
                "团队配合不够，分工不清",
                "环保意识薄弱，用柴浪费",
                "缺乏耐心，急于求成",
                "经验不足，处理不当"
            )
        ),
        
        "COOKING_RICE" to EvaluationTagGroup(
            positive = listOf(
                "水量控制准确，软硬适中",
                "火候掌握好，不糊不夹生",
                "及时翻动，受热均匀",
                "时间把握准确，恰到好处",
                "观察仔细，及时调整",
                "技巧熟练，操作规范",
                "团队配合好，分工明确",
                "耐心细致，不急于求成",
                "经验丰富，处理得当",
                "成果质量好，口感佳"
            ),
            improvements = listOf(
                "水量控制不准，过软或过硬",
                "火候掌握不好，糊了或夹生",
                "翻动不及时，受热不均",
                "时间把握不准，过生或过熟",
                "观察不够仔细，调整不及时",
                "技巧不熟练，操作不规范",
                "团队配合不够，分工不清",
                "缺乏耐心，急于求成",
                "经验不足，处理不当",
                "成果质量差，口感不佳"
            )
        ),
        
        "COOKING_DISHES" to EvaluationTagGroup(
            positive = listOf(
                "刀工整齐，切配规范",
                "调味恰当，口味适中",
                "火候控制好，不糊不生",
                "色香味俱全，卖相好",
                "摆盘美观，有创意",
                "技巧熟练，操作流畅",
                "团队配合好，分工明确",
                "卫生意识强，操作规范",
                "创新意识强，有特色",
                "成果质量好，口感佳"
            ),
            improvements = listOf(
                "刀工不整齐，切配不规范",
                "调味不当，过咸或过淡",
                "火候控制不好，糊了或不熟",
                "色香味不佳，卖相差",
                "摆盘不美观，缺乏创意",
                "技巧不熟练，操作生疏",
                "团队配合不够，分工不清",
                "卫生意识薄弱，操作不规范",
                "缺乏创新，没有特色",
                "成果质量差，口感不佳"
            )
        ),
        
        "SHOWCASE" to EvaluationTagGroup(
            positive = listOf(
                "展示精彩，内容丰富",
                "分享到位，表达清晰",
                "讲解流畅，逻辑清楚",
                "成果突出，亮点明显",
                "团队协作好，配合默契",
                "创意十足，有特色",
                "准备充分，展示完整",
                "互动积极，氛围好",
                "总结到位，反思深刻",
                "表现优秀，值得表扬"
            ),
            improvements = listOf(
                "展示不足，内容简单",
                "分享不到位，表达不清",
                "讲解不流畅，逻辑混乱",
                "成果不突出，亮点不明显",
                "团队协作不够，配合不默契",
                "缺乏创意，没有特色",
                "准备不充分，展示不完整",
                "互动不积极，氛围差",
                "总结不到位，反思不深刻",
                "表现一般，需要改进"
            )
        ),
        
        "CLEANING" to EvaluationTagGroup(
            positive = listOf(
                "收拾干净，无残留",
                "分类整理，有条理",
                "工具归位，摆放整齐",
                "场地整洁，环境好",
                "垃圾分类，环保意识强",
                "团队配合好，效率高",
                "检查仔细，无遗漏",
                "责任心强，认真负责",
                "习惯良好，持续保持",
                "成果显著，值得表扬"
            ),
            improvements = listOf(
                "收拾不干净，有残留",
                "分类不整理，混乱无序",
                "工具不归位，摆放不整齐",
                "场地不整洁，环境差",
                "垃圾分类不当，环保意识薄弱",
                "团队配合不够，效率低",
                "检查不仔细，有遗漏",
                "责任心不强，不够认真",
                "习惯不好，不能持续",
                "成果不佳，需要改进"
            )
        ),
        
        "COMPLETED" to EvaluationTagGroup(
            positive = listOf(
                "整体表现优秀，值得表扬",
                "团队配合默契，协作好",
                "流程顺畅，执行到位",
                "完成度高，质量好",
                "学习态度认真，积极投入",
                "安全意识强，无事故",
                "环保意识强，爱护环境",
                "创新意识强，有特色",
                "总结反思深刻，有收获",
                "综合能力提升明显"
            ),
            improvements = listOf(
                "整体表现一般，需要改进",
                "团队配合不够，协作差",
                "流程不顺畅，执行不到位",
                "完成度低，质量差",
                "学习态度不认真，投入不足",
                "安全意识薄弱，有隐患",
                "环保意识薄弱，不够爱护环境",
                "缺乏创新，没有特色",
                "总结反思不深刻，收获少",
                "综合能力提升不明显"
            )
        )
    )
    
    /**
     * 获取所有环节列表（按顺序）
     */
    fun getAllStages(): List<String> {
        return listOf(
            "PREPARATION",
            "FIRE_MAKING",
            "COOKING_RICE",
            "COOKING_DISHES",
            "SHOWCASE",
            "CLEANING",
            "COMPLETED"
        )
    }
    
    /**
     * 获取环节显示名称
     */
    fun getStageDisplayName(stage: String): String {
        return when (stage) {
            "PREPARATION" -> "准备阶段"
            "FIRE_MAKING" -> "生火"
            "COOKING_RICE" -> "煮饭"
            "COOKING_DISHES" -> "炒菜"
            "SHOWCASE" -> "成果展示"
            "CLEANING" -> "卫生清洁"
            "COMPLETED" -> "整体表现"
            else -> stage
        }
    }
    
    /**
     * 获取环节图标
     */
    fun getStageEmoji(stage: String): String {
        return when (stage) {
            "PREPARATION" -> "📋"
            "FIRE_MAKING" -> "🔥"
            "COOKING_RICE" -> "🍚"
            "COOKING_DISHES" -> "🥘"
            "SHOWCASE" -> "🎉"
            "CLEANING" -> "🧹"
            "COMPLETED" -> "✅"
            else -> "📝"
        }
    }
    
    /**
     * 获取指定环节的评价标签
     */
    fun getEvaluationTags(stage: String): EvaluationTagGroup? {
        return stageEvaluationTags[stage]
    }
}

