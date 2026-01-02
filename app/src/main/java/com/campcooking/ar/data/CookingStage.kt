package com.campcooking.ar.data

/**
 * 野炊流程阶段枚举
 */
enum class CookingStage(
    val displayName: String,
    val emoji: String,
    val description: String,
    val order: Int
) {
    PREPARATION(
        displayName = "准备阶段",
        emoji = "📋",
        description = "准备食材和工具",
        order = 1
    ),
    FIRE_MAKING(
        displayName = "生火",
        emoji = "🔥",
        description = "搭建灶台并点燃柴火",
        order = 2
    ),
    COOKING_RICE(
        displayName = "煮饭",
        emoji = "🍚",
        description = "淘米煮饭",
        order = 3
    ),
    COOKING_DISHES(
        displayName = "炒菜",
        emoji = "🥘",
        description = "清洗切配并炒制菜品",
        order = 4
    ),
    SHOWCASE(
        displayName = "成果展示",
        emoji = "🎉",
        description = "展示成果和分享",
        order = 5
    ),
    CLEANING(
        displayName = "卫生清洁",
        emoji = "🧹",
        description = "清理和整理",
        order = 6
    ),
    COMPLETED(
        displayName = "整体表现",
        emoji = "✅",
        description = "用餐和收拾",
        order = 7
    );
    
    companion object {
        /**
         * 获取所有阶段列表（按顺序）
         */
        fun getAllStages(): List<CookingStage> {
            return values().sortedBy { it.order }
        }
        
        /**
         * 根据order获取阶段
         */
        fun getStageByOrder(order: Int): CookingStage? {
            return values().firstOrNull { it.order == order }
        }
    }
}

