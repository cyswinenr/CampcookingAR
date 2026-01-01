package com.campcooking.ar.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.ar.R
import com.campcooking.ar.data.MediaItem
import com.campcooking.ar.data.MediaType
import com.campcooking.ar.databinding.ItemPhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 照片/视频列表适配器 - 专业版
 *
 * 功能：
 * - 显示文件信息（大小、时长、时间）
 * - 优化图片加载（采样、缩放）
 * - 视频缩略图生成
 * - 查看/播放功能
 * - 删除功能
 */
class PhotoListAdapter(
    private var mediaItems: MutableList<MediaItem>,
    private val onViewClick: (MediaItem) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoListAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem, position: Int) {
            val file = File(mediaItem.path)

            // 设置媒体信息
            binding.mediaTypeView.text = if (mediaItem.type == MediaType.PHOTO) "📷 照片" else "🎥 视频"
            binding.mediaTimeView.text = formatTimestamp(mediaItem.timestamp)

            when (mediaItem.type) {
                MediaType.PHOTO -> {
                    loadOptimizedPhoto(file)
                    binding.videoIconView.visibility = android.view.View.GONE
                    binding.videoDurationView.visibility = android.view.View.GONE
                    binding.viewButton.text = "查看"
                    binding.viewButton.setIconResource(android.R.drawable.ic_menu_view)
                }
                MediaType.VIDEO -> {
                    loadVideoThumbnail(file)
                    binding.videoIconView.visibility = android.view.View.VISIBLE
                    binding.videoDurationView.visibility = android.view.View.VISIBLE
                    binding.viewButton.text = "播放"
                    binding.viewButton.setIconResource(android.R.drawable.ic_media_play)

                    // 获取并显示视频时长
                    val duration = getVideoDuration(file)
                    binding.videoDurationView.text = formatDuration(duration)
                }
            }

            // 显示文件大小
            binding.mediaInfoView.text = formatFileSize(file.length())

            // 查看按钮点击
            binding.viewButton.setOnClickListener {
                onViewClick(mediaItem)
            }

            // 删除按钮点击
            binding.deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }

        /**
         * 优化照片加载（避免OOM）
         */
        private fun loadOptimizedPhoto(file: File) {
            try {
                if (!file.exists()) {
                    binding.photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    return
                }

                // 获取图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                // 计算采样率
                val reqWidth = 200
                val reqHeight = 200
                var inSampleSize = 1

                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2

                    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                // 加载采样后的图片
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // 减少内存占用
                }

                val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                if (bitmap != null) {
                    binding.photoImageView.setImageBitmap(bitmap)
                } else {
                    binding.photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        /**
         * 加载视频缩略图
         */
        private fun loadVideoThumbnail(file: File) {
            android.util.Log.d("PhotoListAdapter", "开始加载视频缩略图: ${file.absolutePath}")
            android.util.Log.d("PhotoListAdapter", "文件存在: ${file.exists()}, 大小: ${file.length()} bytes")

            try {
                if (!file.exists()) {
                    android.util.Log.e("PhotoListAdapter", "视频文件不存在!")
                    loadDefaultThumbnail()
                    return
                }

                // 检查文件大小
                if (file.length() < 100) {
                    android.util.Log.e("PhotoListAdapter", "视频文件过小: ${file.length()} bytes")
                    loadDefaultThumbnail()
                    return
                }

                var thumbnail: Bitmap? = null

                // 方法1: 使用ThumbnailUtils
                android.util.Log.d("PhotoListAdapter", "尝试方法1: ThumbnailUtils")
                thumbnail = ThumbnailUtils.createVideoThumbnail(
                    file.absolutePath,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                android.util.Log.d("PhotoListAdapter", "方法1结果: ${thumbnail != null}")

                // 方法2: 如果ThumbnailUtils失败，使用MediaMetadataRetriever
                if (thumbnail == null) {
                    android.util.Log.d("PhotoListAdapter", "尝试方法2: MediaMetadataRetriever")
                    var retriever: MediaMetadataRetriever? = null
                    try {
                        retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)

                        // 获取第一帧
                        val bitmap = retriever.frameAtTime
                        android.util.Log.d("PhotoListAdapter", "MediaMetadataRetriever获取帧: ${bitmap != null}")

                        if (bitmap != null) {
                            // 缩放到合适大小
                            val targetWidth = 200
                            val targetHeight = 200
                            thumbnail = ThumbnailUtils.extractThumbnail(
                                bitmap,
                                targetWidth,
                                targetHeight
                            )
                            android.util.Log.d("PhotoListAdapter", "缩放后的缩略图: ${thumbnail != null}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PhotoListAdapter", "MediaMetadataRetriever失败: ${e.message}", e)
                    } finally {
                        retriever?.release()
                    }
                }

                // 方法3: 使用ContentResolver（最可靠的方法）
                if (thumbnail == null) {
                    android.util.Log.d("PhotoListAdapter", "尝试方法3: ContentResolver")
                    try {
                        val context = binding.root.context
                        val contentResolver = context.contentResolver

                        // 查询视频缩略图
                        val projection = arrayOf(
                            MediaStore.Video.Thumbnails.DATA
                        )

                        val cursor = contentResolver.query(
                            MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                            projection,
                            "${MediaStore.Video.Thumbnails.VIDEO_ID} = ?",
                            arrayOf(getVideoId(context, file.absolutePath).toString()),
                            null
                        )

                        cursor?.use {
                            if (it.moveToFirst()) {
                                val thumbPath = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA))
                                android.util.Log.d("PhotoListAdapter", "找到缩略图路径: $thumbPath")

                                if (thumbPath != null && File(thumbPath).exists()) {
                                    thumbnail = BitmapFactory.decodeFile(thumbPath)
                                    android.util.Log.d("PhotoListAdapter", "从文件加载缩略图: ${thumbnail != null}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PhotoListAdapter", "ContentResolver失败: ${e.message}", e)
                    }
                }

                // 设置缩略图或默认图
                if (thumbnail != null) {
                    android.util.Log.d("PhotoListAdapter", "成功加载视频缩略图!")
                    binding.photoImageView.setImageBitmap(thumbnail)
                } else {
                    android.util.Log.w("PhotoListAdapter", "所有方法都失败，使用默认缩略图")
                    loadDefaultThumbnail()
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoListAdapter", "加载视频缩略图异常: ${e.message}", e)
                loadDefaultThumbnail()
            }
        }

        /**
         * 获取视频ID
         */
        private fun getVideoId(context: Context, videoPath: String): Long {
            try {
                val projection = arrayOf(MediaStore.Video.Media._ID)
                val selection = "${MediaStore.Video.Media.DATA} = ?"
                val selectionArgs = arrayOf(videoPath)

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoListAdapter", "获取视频ID失败: ${e.message}", e)
            }
            return -1
        }

        /**
         * 加载默认缩略图（视频缩略图加载失败时使用）
         */
        private fun loadDefaultThumbnail() {
            android.util.Log.d("PhotoListAdapter", "加载默认视频缩略图")

            // 创建渐变背景（从深灰到黑色）
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bitmap)

            // 绘制渐变背景
            val gradient = android.graphics.LinearGradient(
                0f, 0f, 0f, 200f,
                0xFF424242.toInt(), 0xFF212121.toInt(),
                android.graphics.Shader.TileMode.CLAMP
            )
            val paint = android.graphics.Paint().apply {
                shader = gradient
            }
            canvas.drawRect(0f, 0f, 200f, 200f, paint)

            // 绘制视频图标
            val iconPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 180
                isAntiAlias = true
            }

            // 绘制播放三角形
            val path = android.graphics.Path().apply {
                val centerX = 100f
                val centerY = 100f
                val size = 40f

                moveTo(centerX - size * 0.4f, centerY - size * 0.5f)
                lineTo(centerX - size * 0.4f, centerY + size * 0.5f)
                lineTo(centerX + size * 0.5f, centerY)
                close()
            }
            canvas.drawPath(path, iconPaint)

            binding.photoImageView.setImageBitmap(bitmap)
            android.util.Log.d("PhotoListAdapter", "默认缩略图已设置")
        }

        /**
         * 获取视频时长（毫秒）
         */
        private fun getVideoDuration(file: File): Long {
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                time?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }

        /**
         * 格式化时长（毫秒 -> MM:SS）
         */
        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        /**
         * 格式化文件大小
         */
        private fun formatFileSize(size: Long): String {
            if (size < 1024) return "$size B"
            val kb = size / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format("%.1f MB", mb)
        }

        /**
         * 格式化时间戳
         */
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(mediaItems[position], position)
    }

    override fun getItemCount() = mediaItems.size

    /**
     * 更新媒体项列表
     */
    fun updateMediaItems(newMediaItems: MutableList<MediaItem>) {
        mediaItems = newMediaItems
        notifyDataSetChanged()
    }

    /**
     * 保留旧方法以保持向后兼容
     */
    fun updatePhotos(newPhotos: MutableList<String>) {
        mediaItems = newPhotos.map { path ->
            MediaItem(path, MediaType.PHOTO)
        }.toMutableList()
        notifyDataSetChanged()
    }
}
