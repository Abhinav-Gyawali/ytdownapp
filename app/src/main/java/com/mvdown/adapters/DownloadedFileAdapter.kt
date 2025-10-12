package com.mvdown.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mvdown.R
import com.mvdown.models.DownloadedFile
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DownloadedFileAdapter : RecyclerView.Adapter<DownloadedFileAdapter.ViewHolder>() {
    
    private var downloads: List<DownloadedFile> = emptyList()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    fun updateDownloads(newDownloads: List<DownloadedFile>) {
        downloads = newDownloads
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_downloaded_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = downloads[position]
        holder.bind(download)
    }
    
    override fun getItemCount() = downloads.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        
        fun bind(download: DownloadedFile) {
            tvTitle.text = download.title
            tvDate.text = dateFormatter.format(Date(download.downloadDate))
            tvSize.text = formatFileSize(download.size)
            
            card.setOnClickListener {
                // TODO: Handle click
            }
        }
        
        private fun formatFileSize(size: Long): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            
            return when {
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> String.format("%d Bytes", size)
            }
        }
    }
}