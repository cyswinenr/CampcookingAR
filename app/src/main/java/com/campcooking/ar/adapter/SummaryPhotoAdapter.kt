package com.campcooking.ar.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.campcooking.ar.R
import java.io.File

/**
 * 课后总结图片适配器
 * 用于显示每个问题关联的图片网格
 */
class SummaryPhotoAdapter(
    private var photos: MutableList<String>,
    private val onPhotoClick: (String) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SummaryPhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoPath = photos[position]
        
        // 加载图片
        loadOptimizedPhoto(holder.imageView, photoPath)
        
        // 点击查看大图
        holder.imageView.setOnClickListener {
            onPhotoClick(photoPath)
        }
        
        // 删除按钮点击
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount() = photos.size

    /**
     * 更新图片列表
     */
    fun updatePhotos(newPhotos: List<String>) {
        photos = newPhotos.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * 添加图片
     */
    fun addPhoto(photoPath: String) {
        photos.add(photoPath)
        notifyItemInserted(photos.size - 1)
    }

    /**
     * 删除图片
     */
    fun removePhoto(position: Int) {
        if (position in photos.indices) {
            photos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photos.size - position)
        }
    }

    /**
     * 优化照片加载（避免OOM）
     */
    private fun loadOptimizedPhoto(imageView: ImageView, photoPath: String) {
        try {
            val file = File(photoPath)
            if (!file.exists()) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                return
            }

            // 获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(photoPath, options)

            // 计算采样率
            val reqWidth = 240
            val reqHeight = 240
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 解码图片
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(photoPath, options)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

