package com.campcooking.ar.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 炉号管理类
 * 管理首页设置的炉号，设置后需要密码才能修改
 */
class StoveNumberManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "stove_number_prefs"
        private const val KEY_STOVE_NUMBER = "stove_number"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val PASSWORD = "81438316"  // 修改密码
    }
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 设置炉号（首次设置）
     */
    fun setStoveNumber(stoveNumber: String) {
        sharedPreferences.edit()
            .putString(KEY_STOVE_NUMBER, stoveNumber)
            .putBoolean(KEY_IS_LOCKED, true)  // 设置后自动锁定
            .apply()
    }
    
    /**
     * 获取已设置的炉号
     */
    fun getStoveNumber(): String? {
        return sharedPreferences.getString(KEY_STOVE_NUMBER, null)
    }
    
    /**
     * 检查炉号是否已设置并锁定
     */
    fun isStoveNumberLocked(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOCKED, false) && getStoveNumber() != null
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String): Boolean {
        return password == PASSWORD
    }
    
    /**
     * 修改炉号（需要密码验证）
     */
    fun updateStoveNumber(newStoveNumber: String, password: String): Boolean {
        if (!verifyPassword(password)) {
            return false
        }
        setStoveNumber(newStoveNumber)
        return true
    }
    
    /**
     * 清除炉号设置（需要密码验证）
     */
    fun clearStoveNumber(password: String): Boolean {
        if (!verifyPassword(password)) {
            return false
        }
        sharedPreferences.edit()
            .remove(KEY_STOVE_NUMBER)
            .putBoolean(KEY_IS_LOCKED, false)
            .apply()
        return true
    }
    
    /**
     * 检查是否有已设置的炉号
     */
    fun hasStoveNumber(): Boolean {
        return getStoveNumber() != null && !getStoveNumber().isNullOrBlank()
    }
}

