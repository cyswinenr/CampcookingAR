package com.campcooking.ar.data

import java.io.Serializable

/**
 * 团队信息数据类
 * 用于存储野炊小组的基本信息
 */
data class TeamInfo(
    var school: String = "",           // 学校
    var grade: String = "",            // 年级
    var className: String = "",        // 班级
    var stoveNumber: String = "",      // 炉号
    var memberCount: Int = 0,          // 小组人数
    var memberNames: String = ""       // 人员姓名（用逗号或换行分隔）
) : Serializable {
    
    /**
     * 验证信息是否完整
     */
    fun isValid(): Boolean {
        return school.isNotBlank() &&
                grade.isNotBlank() &&
                className.isNotBlank() &&
                stoveNumber.isNotBlank() &&
                memberCount > 0 &&
                memberNames.isNotBlank()
    }
    
    /**
     * 获取团队完整名称
     */
    fun getTeamName(): String {
        return "$school ${grade}年级 $className 炉号$stoveNumber"
    }
}

