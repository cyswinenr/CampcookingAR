package com.campcooking.teacher.data

/**
 * 团队信息数据模型
 * 从服务器API获取的团队信息
 */
data class TeamInfo(
    val id: String,
    val teamName: String,
    val school: String,
    val grade: String,
    val className: String,
    val stoveNumber: String,
    val memberCount: Int,
    val memberNames: String,
    val groupLeader: String,  // 项目组长
    val groupCooking: String? = null,  // 烹饪组
    val groupSoupRice: String? = null,  // 汤饭组
    val groupFire: String? = null,  // 生火组
    val groupHealth: String? = null,  // 卫生组
    val submitTime: String? = null,
    val hasProcessRecord: Boolean = false,
    val hasSummary: Boolean = false,
    val completedStages: Int = 0,
    val totalStages: Int = 7
) {
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return "$school ${grade}年级 $className 炉号$stoveNumber"
    }
    
    /**
     * 获取分工信息文本
     */
    fun getDivisionText(): String {
        val divisions = mutableListOf<String>()
        if (groupLeader.isNotEmpty()) {
            divisions.add("组长：$groupLeader")
        }
        if (!groupCooking.isNullOrEmpty()) {
            divisions.add("烹饪组：$groupCooking")
        }
        if (!groupSoupRice.isNullOrEmpty()) {
            divisions.add("汤饭组：$groupSoupRice")
        }
        if (!groupFire.isNullOrEmpty()) {
            divisions.add("生火组：$groupFire")
        }
        if (!groupHealth.isNullOrEmpty()) {
            divisions.add("卫生组：$groupHealth")
        }
        return if (divisions.isEmpty()) "暂无分工信息" else divisions.joinToString("\n")
    }
    
    companion object {
        /**
         * 从服务器API数据创建TeamInfo
         */
        fun fromApiData(data: Map<String, Any?>): TeamInfo {
            // 获取分工信息（如果有）
            val division = data["division"] as? Map<String, Any?>
            
            return TeamInfo(
                id = data["id"] as? String ?: "",
                teamName = data["teamName"] as? String ?: "",
                school = data["school"] as? String ?: "",
                grade = data["grade"] as? String ?: "",
                className = data["className"] as? String ?: "",
                stoveNumber = data["stoveNumber"] as? String ?: "",
                memberCount = (data["memberCount"] as? Number)?.toInt() ?: 0,
                memberNames = data["memberNames"] as? String ?: "",
                groupLeader = data["groupLeader"] as? String ?: "",
                groupCooking = division?.get("groupCooking") as? String,
                groupSoupRice = division?.get("groupSoupRice") as? String,
                groupFire = division?.get("groupFire") as? String,
                groupHealth = division?.get("groupHealth") as? String,
                submitTime = data["submitTime"] as? String,
                hasProcessRecord = data["hasProcessRecord"] as? Boolean ?: false,
                hasSummary = data["hasSummary"] as? Boolean ?: false,
                completedStages = (data["completedStages"] as? Number)?.toInt() ?: 0,
                totalStages = (data["totalStages"] as? Number)?.toInt() ?: 7
            )
        }
    }
}

