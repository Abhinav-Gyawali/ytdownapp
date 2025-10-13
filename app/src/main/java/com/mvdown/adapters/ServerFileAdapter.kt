package com.mvdown.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mvdown.R
import com.mvdown.models.FileInfo

class ServerFileAdapter(
    private val onDownloadClick: (FileInfo) -> Unit
) : RecyclerView.Adapter<ServerFileAdapter.ViewHolder>() {
    
    private var files: List<FileInfo> = emptyList()
    
    fun updateFiles(newFiles: List<FileInfo>) {
        files = newFiles
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)
    }
    
    override fun getItemCount() = files.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card)
        private val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        private val tvFileType: TextView = itemView.findViewById(R.id.tvFileType)
        private val btnDownload: MaterialButton = itemView.findViewById(R.id.btnDownload)
        
        fun bind(file: FileInfo) {
            tvFileName.text = file.name
            tvFileSize.text = formatFileSize(file.size)
            tvFileType.text = file.type.uppercase()
            
            // Set icon based on file type
            val iconRes = when (file.type) {
                "video" -> R.drawable.ic_video
                "audio" -> R.drawable.ic_audio
                else -> R.drawable.ic_downloads
            }
            ivFileIcon.setImageResource(iconRes)
            
            btnDownload.setOnClickListener {
                btnDownload.isEnabled = false
                btnDownload.text = "Downloading..."
                onDownloadClick(file)
                
                // Re-enable after 2 seconds
                itemView.postDelayed({
                    btnDownload.isEnabled = true
                    btnDownload.text = "Download"
                }, 2000)
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
