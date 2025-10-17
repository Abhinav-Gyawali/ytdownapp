package com.mvdown.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mvdown.databinding.ItemDownloadBinding

class DownloadAdapter : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    private val downloads = mutableListOf<DownloadItem>()

    fun addDownload(item: DownloadItem) {
        downloads.add(0, item)
        notifyItemInserted(0)
    }

    fun updateProgress(downloadId: String, progress: Int, status: String) {
        val index = downloads.indexOfFirst { it.id == downloadId }
        if (index != -1) {
            downloads[index].progress = progress
            downloads[index].status = status
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(downloads[position])
    }

    override fun getItemCount() = downloads.size

    inner class ViewHolder(
        private val binding: ItemDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItem) {
            binding.tvTitle.text = item.title
            binding.tvStatus.text = item.status
            binding.progressBar.progress = item.progress
            binding.tvProgress.text = "${item.progress}%"
        }
    }
}

data class DownloadItem(
    val id: String,
    val title: String,
    var progress: Int = 0,
    var status: String = "Starting"
)