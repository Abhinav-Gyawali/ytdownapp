package com.mvdown.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mvdown.R
import com.mvdown.models.VideoFormat

class FormatsAdapter(private val onFormatClick: (VideoFormat) -> Unit) : RecyclerView.Adapter<FormatsAdapter.FormatViewHolder>() {
    private var formats: List<VideoFormat> = emptyList()

    class FormatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFormatInfo: TextView = view.findViewById(R.id.tvFormatInfo)
        val tvFormatSize: TextView = view.findViewById(R.id.tvFormatSize)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_format, parent, false)
        return FormatViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
        val format = formats[position]
        
        // Format resolution and extension
        val formatInfo = buildString {
            append(format.resolution ?: "Unknown")
            append(" - ")
            append(format.ext ?: "mp4")
        }
        holder.tvFormatInfo.text = formatInfo

        // Format size
        val formattedSize = format.filesize?.let { size ->
            when {
                size > 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
                size > 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                size > 1024 -> String.format("%.1f KB", size / 1024.0)
                else -> "$size B"
            }
        } ?: "Unknown size"
        holder.tvFormatSize.text = formattedSize

        holder.btnDownload.setOnClickListener { 
            holder.btnDownload.isEnabled = false
            holder.btnDownload.text = "Starting..."
            onFormatClick(format)
        }
    }

    override fun getItemCount() = formats.size

    fun updateFormats(newFormats: List<VideoFormat>) {
        formats = newFormats
        notifyDataSetChanged()
    }
}