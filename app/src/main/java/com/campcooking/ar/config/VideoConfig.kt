package com.campcooking.ar.config

import com.campcooking.ar.data.Video

/**
 * 视频配置管理类
 * 
 * 【重要】如何更新视频：
 * 
 * 方案：外部存储（Documents 文件夹）
 * 1. 通过 USB 连接平板到电脑
 * 2. 将视频文件复制到：内部存储/Documents/CampcookingAR/Videos/
 * 3. 视频文件名可以使用中文，如：生火技巧.mp4
 * 4. 在下面的 videoList 中添加配置信息
 * 5. 重新编译运行即可
 * 
 * 优点：
 * - ✅ 编译速度快（视频不在项目中）
 * - ✅ 更新视频不需要重新编译（只需复制文件）
 * - ✅ 支持中文文件名
 * - ✅ APK 文件小
 */
object VideoConfig {
    
    /**
     * 视频文件夹路径（相对于 Documents 目录）
     */
    const val VIDEO_FOLDER = "CampcookingAR/Videos"
    
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
                fileName = "video_fire_skills.mp4",
                category = "基础技能",
                order = 1
            ),
            
            // 视频2：砍鸡的方法
            Video(
                id = 2,
                title = "砍鸡的方法",
                description = "学习如何正确砍鸡",
                duration = "1:00",
                fileName = "video_cut_chicken.mp4",
                category = "基础技能",
                order = 2
            ),
            
            // 视频3：砍排骨方法
            Video(
                id = 3,
                title = "砍排骨方法",
                description = "砍排骨方法",
                duration = "1:00",
                fileName = "video_chop_ribs.mp4",
                category = "基础技能",
                order = 3
            ),
            
            // 视频4：野炊砍柴方法
            Video(
                id = 4,
                title = "野炊砍柴方法",
                description = "野炊砍柴方法",
                duration = "1:00",
                fileName = "野炊砍柴.mp4",
                category = "基础技能",
                order = 4
            ),
            
            // 视频5：番茄炒鸡蛋
            Video(
                id = 5,
                title = "番茄炒鸡蛋",
                description = "番茄炒鸡蛋",
                duration = "1:00",
                fileName = "番茄炒鸡蛋.mp4",
                category = "菜式",
                order = 5
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
