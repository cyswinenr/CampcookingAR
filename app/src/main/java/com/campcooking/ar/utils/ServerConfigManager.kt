package com.campcooking.ar.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 服务器配置管理器
 * 管理服务器IP地址等配置信息
 */
class ServerConfigManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "server_config_prefs"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val DEFAULT_SERVER_IP = "172.16.70.101"  // 默认IP
        private const val DEFAULT_PORT = 5000  // 默认端口
    }
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取服务器IP地址
     */
    fun getServerIp(): String {
        return sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP
    }
    
    /**
     * 保存服务器IP地址
     */
    fun saveServerIp(ip: String) {
        sharedPreferences.edit()
            .putString(KEY_SERVER_IP, ip.trim())
            .apply()
    }
    
    /**
     * 获取服务器端口
     */
    fun getServerPort(): Int {
        return sharedPreferences.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }
    
    /**
     * 保存服务器端口
     */
    fun saveServerPort(port: Int) {
        sharedPreferences.edit()
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }
    
    /**
     * 获取服务器完整地址
     */
    fun getServerUrl(): String {
        val ip = getServerIp()
        val port = getServerPort()
        return "http://$ip:$port"
    }
    
    /**
     * 保存服务器配置（IP和端口）
     */
    fun saveServerConfig(ip: String, port: Int) {
        sharedPreferences.edit()
            .putString(KEY_SERVER_IP, ip.trim())
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }
    
    /**
     * 检查IP地址格式是否有效
     */
    fun isValidIp(ip: String): Boolean {
        val ipPattern = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        return ipPattern.matches(ip.trim())
    }
    
    /**
     * 检查端口号是否有效（1-65535）
     */
    fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }
}

