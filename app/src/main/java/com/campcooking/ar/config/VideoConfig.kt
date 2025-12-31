package com.campcooking.ar.config

import com.campcooking.ar.data.Video

/**
 * 视频配置管理类
 * 
 * 【重要】如何更新视频：
 * 1. 将视频文件（.mp4）放入 app/src/main/res/raw/ 文件夹
 * 2. 视频文件名使用小写字母和下划线，如：video_fire_skills.mp4
 * 3. 在下面的 videoList 中添加配置信息
 * 4. 重新编译运行即可
 */
object VideoConfig {
    
    /**
     * 获取所有视频列表
     */
    fun getVideoList(): List<Video> {
        return listOf(
            // 视频1：生火技巧
            Video(
                id = 1,
                title = "生火技巧",
                description = "学习如何快速安全地生火，掌握柴火的摆放和点火技巧。",
                duration = "1:27",
                resourceName = "video_fire_skills",
                category = "基础技能",
                order = 1
            ),
            
            // 视频2：砍鸡的方法
            Video(
                id = 2,
                title = "砍鸡的方法",
                description = "学习如何正确砍鸡",
                duration = "2:30",
                resourceName = "video_cut_chicken",
                category = "基础技能",
                order = 2
            ),
            
            // 视频3：砍排骨方法
            Video(
                id = 3,
                title = "砍排骨方法",
                description = "学习如何正确砍排骨。详细讲解烹饪前的准备工作、火候的掌握技巧、调味的正确方法。",
                duration = "3:15",
                resourceName = "video_chop_ribs",
                category = "基础技能",
                order = 3
            )
        )
    }
    
    /**
     * 根据ID获取视频
     */
    fun getVideoById(id: Int): Video? {
        return getVideoList().find { it.id == id }
    }
    
    /**
     * 根据分类获取视频列表
     */
    fun getVideosByCategory(category: String): List<Video> {
        return getVideoList().filter { it.category == category }
    }
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<String> {
        return getVideoList().map { it.category }.distinct()
    }
}
