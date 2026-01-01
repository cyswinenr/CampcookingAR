package com.campcooking.ar.utils

import android.content.Context
import android.content.SharedPreferences
import com.campcooking.ar.data.TeamInfo

/**
 * 团队信息管理类
 * 使用 SharedPreferences 持久化保存团队信息
 */
class TeamInfoManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "team_info_prefs"
        private const val KEY_SCHOOL = "school"
        private const val KEY_GRADE = "grade"
        private const val KEY_CLASS = "class"
        private const val KEY_STOVE = "stove"
        private const val KEY_MEMBER_COUNT = "member_count"
        private const val KEY_MEMBER_NAMES = "member_names"
        private const val KEY_HAS_DATA = "has_data"
    }
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 保存团队信息
     */
    fun saveTeamInfo(teamInfo: TeamInfo) {
        sharedPreferences.edit().apply {
            putString(KEY_SCHOOL, teamInfo.school)
            putString(KEY_GRADE, teamInfo.grade)
            putString(KEY_CLASS, teamInfo.className)
            putString(KEY_STOVE, teamInfo.stoveNumber)
            putInt(KEY_MEMBER_COUNT, teamInfo.memberCount)
            putString(KEY_MEMBER_NAMES, teamInfo.memberNames)
            putBoolean(KEY_HAS_DATA, true)
            apply()
        }
    }
    
    /**
     * 加载团队信息
     */
    fun loadTeamInfo(): TeamInfo? {
        if (!hasData()) {
            return null
        }
        
        return TeamInfo().apply {
            school = sharedPreferences.getString(KEY_SCHOOL, "") ?: ""
            grade = sharedPreferences.getString(KEY_GRADE, "") ?: ""
            className = sharedPreferences.getString(KEY_CLASS, "") ?: ""
            stoveNumber = sharedPreferences.getString(KEY_STOVE, "") ?: ""
            memberCount = sharedPreferences.getInt(KEY_MEMBER_COUNT, 0)
            memberNames = sharedPreferences.getString(KEY_MEMBER_NAMES, "") ?: ""
        }
    }
    
    /**
     * 清除保存的数据
     */
    fun clearTeamInfo() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * 检查是否有保存的数据
     */
    fun hasData(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAS_DATA, false)
    }
    
    /**
     * 获取成员姓名列表
     */
    fun getMemberNamesList(): List<String> {
        val namesString = sharedPreferences.getString(KEY_MEMBER_NAMES, "") ?: ""
        return if (namesString.isNotEmpty()) {
            namesString.split("、").filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
}

