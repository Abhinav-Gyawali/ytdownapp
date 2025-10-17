package com.mvdown.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mvdown.databinding.ItemFormatBinding
import com.mvdown.model.Format
import com.mvdown.model.FormatResponse

class FormatAdapter(
    private val onFormatSelected: (Format) -> Unit
) : RecyclerView.Adapter<FormatAdapter.ViewHolder>() {

    private val formats = mutableListOf<Format>()
    private var selectedPosition = -1

    fun submitFormats(response: FormatResponse) {
        formats.clear()
        
        // Add quick options
        formats.add(Format("best-audio", "mp3", null, 320.0, null, "Best Audio"))
        formats.add(Format("best-video", "mp4", "Best", null, null, "Best Video"))
        
        // Add video formats
        response.videoFormats?.let { formats.addAll(it) }
        
        // Add audio formats
        response.audioFormats?.let { formats.addAll(it) }
        
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFormatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(formats[position], position == selectedPosition)
    }

    override fun getItemCount() = formats.size

    inner class ViewHolder(
        private val binding: ItemFormatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(format: Format, isSelected: Boolean) {
            binding.root.isSelected = isSelected
            
            binding.tvFormatType.text = if (format.resolution != null) "VIDEO" else "AUDIO"
            
            val label = when {
                format.resolution != null -> "${format.resolution} ${format.ext}"
                format.abr != null -> "${format.abr.toInt()} kbps ${format.ext}"
                else -> format.formatNote ?: format.formatId
            }
            binding.tvFormatLabel.text = label
            
            format.formatNote?.let {
                binding.tvFormatNote.text = it
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onFormatSelected(format)
            }
        }
    }
}
