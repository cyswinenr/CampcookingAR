package com.campcooking.ar.utils

import android.content.Context
import com.campcooking.ar.data.CookingStage
import com.campcooking.ar.data.ProcessRecord
import com.campcooking.ar.data.StageRecord
import com.campcooking.ar.data.TeamInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 过程记录数据管理器
 * 使用 SharedPreferences + Gson 实现数据持久化
 */
class ProcessRecordManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        "process_record_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_PROCESS_RECORD = "process_record"
    }

    /**
     * 保存过程记录
     */
    fun saveProcessRecord(record: ProcessRecord) {
        try {
            val json = gson.toJson(record)
            sharedPreferences.edit()
                .putString(KEY_PROCESS_RECORD, json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 加载过程记录
     */
    fun loadProcessRecord(): ProcessRecord? {
        try {
            val json = sharedPreferences.getString(KEY_PROCESS_RECORD, null)
            if (json != null) {
                val type = object : TypeToken<ProcessRecord>() {}.type
                val record = gson.fromJson<ProcessRecord>(json, type)

                // 迁移旧数据：将photos转换为mediaItems
                record?.let { processRecord ->
                    processRecord.stages.values.forEach { stageRecord ->
                        // 如果mediaItems为空但photos有数据，进行迁移
                        if (stageRecord.mediaItems.isEmpty() && stageRecord.photos.isNotEmpty()) {
                            stageRecord.photos.forEach { photoPath ->
                                stageRecord.mediaItems.add(
                                    com.campcooking.ar.data.MediaItem(
                                        path = photoPath,
                                        type = com.campcooking.ar.data.MediaType.PHOTO
                                    )
                                )
                            }
                        }
                    }
                }

                return record
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 出错也清除记录
            clearProcessRecord()
        }
        return null
    }

    /**
     * 清除过程记录
     */
    fun clearProcessRecord() {
        sharedPreferences.edit()
            .remove(KEY_PROCESS_RECORD)
            .apply()
    }

    /**
     * 检查是否有保存的记录
     */
    fun hasRecord(): Boolean {
        return sharedPreferences.contains(KEY_PROCESS_RECORD)
    }
}

