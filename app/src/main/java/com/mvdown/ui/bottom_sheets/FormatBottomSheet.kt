package com.mvdown.ui.bottom_sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvdown.R
import com.mvdown.manager.DownloadManager
import com.mvdown.models.DownloadEvent
import com.mvdown.models.VideoFormat
import com.mvdown.ui.adapters.FormatsAdapter
import com.mvdown.ui.dialogs.DownloadProgressDialog

class FormatBottomSheet : BottomSheetDialogFragment() {
    private lateinit var rvFormats: RecyclerView
    private lateinit var adapter: FormatsAdapter
    private var formats: List<VideoFormat> = emptyList()
    private var url: String = ""
    private lateinit var downloadManager: DownloadManager
    private var progressDialog: DownloadProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_format, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadManager = DownloadManager(requireContext())
        
        rvFormats = view.findViewById(R.id.rvFormats)
        rvFormats.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = FormatsAdapter { format ->
            startDownload(format)
        }
        adapter.updateFormats(formats)
        rvFormats.adapter = adapter
        
        observeDownloadEvents()
    }

    private fun startDownload(format: VideoFormat) {
        println("🎬 Starting download...")
        println("📝 URL: $url")
        println("📝 Format ID: ${format.formatId}")
        
        dismiss()
        
        progressDialog = DownloadProgressDialog(requireContext()).apply {
            show()
        }
        
        downloadManager.startDownload(url, format.formatId, lifecycleScope)
    }
    
    private fun observeDownloadEvents() {
        println("👀 Observing download events...")
        downloadManager.downloadEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is DownloadEvent.Progress -> {
                    println("📊 Progress - Status: ${event.status}, Percent: ${event.percent}")
                    progressDialog?.updateProgress(event)
                }
                is DownloadEvent.Done -> {
                    println("✅ Download completed!")
                    println("📝 Title: ${event.title}")
                    println("📝 Filename: ${event.filename}")
                    progressDialog?.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Download complete: ${event.filename}",
                        Toast.LENGTH_LONG
                    ).show()
                    downloadManager.disconnect()
                }
                is DownloadEvent.Error -> {
                    println("❌ Download error: ${event.error}")
                    progressDialog?.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${event.error}",
                        Toast.LENGTH_LONG
                    ).show()
                    downloadManager.disconnect()
                }
            }
        }
    }

    fun setFormats(formats: List<VideoFormat>) {
        this.formats = formats
        if (::adapter.isInitialized) {
            adapter.updateFormats(formats)
        }
    }
    
    fun setUrl(url: String) {
        this.url = url
    }

    companion object {
        fun newInstance(formats: List<VideoFormat>, url: String): FormatBottomSheet {
            return FormatBottomSheet().apply {
                setFormats(formats)
                setUrl(url)
            }
        }
    }
}