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
        val btnDownload: Button = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_format, parent, false)
        return FormatViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
        val format = formats[position]
        holder.tvFormatInfo.text = "${format.resolution ?: "Unknown"} - ${format.ext ?: "mp4"} (${format.filesize?.let { "${it/1024/1024} MB" } ?: "Unknown size"})"
        holder.btnDownload.setOnClickListener { onFormatClick(format) }
    }

    override fun getItemCount() = formats.size

    fun updateFormats(newFormats: List<VideoFormat>) {
        formats = newFormats
        notifyDataSetChanged()
    }
}