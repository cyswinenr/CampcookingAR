package com.campcooking.ar.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.ar.data.MediaItem
import com.campcooking.ar.data.MediaType
import com.campcooking.ar.databinding.ItemPhotoBinding
import java.io.File

/**
 * 照片/视频列表适配器
 */
class PhotoListAdapter(
    private var mediaItems: MutableList<MediaItem>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoListAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem, position: Int) {
            val file = File(mediaItem.path)

            when (mediaItem.type) {
                MediaType.PHOTO -> {
                    // 加载照片
                    if (file.exists()) {
                        val uri = Uri.fromFile(file)
                        binding.photoImageView.setImageURI(uri)
                    }
                    // 隐藏视频相关UI
                    binding.videoIconView.visibility = android.view.View.GONE
                    binding.videoLabelView.visibility = android.view.View.GONE

                    // 点击照片查看大图
                    binding.photoImageView.setOnClickListener {
                        // TODO: 实现查看大图功能
                    }
                }
                MediaType.VIDEO -> {
                    // 加载视频缩略图
                    loadVideoThumbnail(mediaItem.path)
                    // 显示视频标识
                    binding.videoIconView.visibility = android.view.View.VISIBLE
                    binding.videoLabelView.visibility = android.view.View.VISIBLE

                    // 点击视频播放
                    binding.photoImageView.setOnClickListener {
                        playVideo(mediaItem.path)
                    }
                }
            }

            // 删除按钮点击
            binding.deletePhotoButton.setOnClickListener {
                onDeleteClick(position)
            }
        }

        /**
         * 加载视频缩略图
         */
        private fun loadVideoThumbnail(videoPath: String) {
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    // 使用ThumbnailUtils获取视频缩略图
                    val thumbnail = ThumbnailUtils.createVideoThumbnail(
                        videoPath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                    if (thumbnail != null) {
                        binding.photoImageView.setImageBitmap(thumbnail)
                    } else {
                        // 如果无法获取缩略图，使用深灰色背景
                        loadDefaultVideoBackground()
                    }
                } else {
                    // 文件不存在，显示错误背景
                    loadDefaultVideoBackground()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 出错时使用默认背景
                loadDefaultVideoBackground()
            }
        }

        /**
         * 加载默认视频背景（当缩略图无法加载时）
         */
        private fun loadDefaultVideoBackground() {
            // 创建一个深灰色的背景
            val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(0xFF424242.toInt()) // 深灰色
            binding.photoImageView.setImageBitmap(bitmap)
        }

        /**
         * 播放视频
         */
        private fun playVideo(videoPath: String) {
            try {
                val file = File(videoPath)

                // 检查文件是否存在
                if (!file.exists()) {
                    android.widget.Toast.makeText(
                        binding.root.context,
                        "❌ 视频文件不存在\n路径: $videoPath",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // 检查文件大小
                val fileSizeKB = file.length() / 1024
                if (fileSizeKB < 10) {
                    android.widget.Toast.makeText(
                        binding.root.context,
                        "⚠️ 视频文件过小 (${fileSizeKB}KB)\n可能录制失败，请重新录制",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // 使用FileProvider获取URI
                val uri = FileProvider.getUriForFile(
                    binding.root.context,
                    binding.root.context.packageName + ".fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // 检查是否有应用可以播放视频
                if (intent.resolveActivity(binding.root.context.packageManager) != null) {
                    binding.root.context.startActivity(intent)
                } else {
                    android.widget.Toast.makeText(
                        binding.root.context,
                        "❌ 未找到视频播放应用",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    binding.root.context,
                    "❌ 播放视频失败\n${e.javaClass.simpleName}: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
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
        // 将String列表转换为MediaItem列表
        mediaItems = newPhotos.map { path ->
            MediaItem(path, MediaType.PHOTO)
        }.toMutableList()
        notifyDataSetChanged()
    }
}

