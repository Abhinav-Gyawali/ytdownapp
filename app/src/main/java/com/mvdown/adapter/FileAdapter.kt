package com.mvdown.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mvdown.databinding.ItemFileBinding
import com.mvdown.model.FileItem

class FileAdapter(
    private val onDownload: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : ListAdapter<FileItem, FileAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        Log.d("FileAdapter", "onCreateViewHolder called")
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("FileAdapter", "onBindViewHolder: position=$position, item=$item")
        if (item != null) {
            holder.bind(item)
        }
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d("FileAdapter", "getItemCount: $count")
        return count
    }

    inner class ViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: FileItem) {
            Log.d("FileAdapter", "Binding file: ${file.name}")
            
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)
            binding.tvFileType.text = file.type.uppercase()

            binding.btnDownload.setOnClickListener {
                Log.d("FileAdapter", "Download clicked: ${file.name}")
                onDownload(file.name)
            }

            binding.btnDelete.setOnClickListener {
                Log.d("FileAdapter", "Delete clicked: ${file.name}")
                onDelete(file.name)
            }
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            var unitIndex = 0

            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }

            return String.format("%.2f %s", size, units[unitIndex])
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}