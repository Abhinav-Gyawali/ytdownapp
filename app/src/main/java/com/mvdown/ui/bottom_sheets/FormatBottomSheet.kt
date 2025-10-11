package com.mvdown.ui.bottom_sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvdown.Format
import com.mvdown.R
import com.mvdown.ui.adapters.FormatsAdapter

class FormatBottomSheet : BottomSheetDialogFragment() {
    private lateinit var rvFormats: RecyclerView
    private lateinit var adapter: FormatsAdapter
    private var formats: List<Format> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_format, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFormats = view.findViewById(R.id.rvFormats)
        rvFormats.layoutManager = LinearLayoutManager(requireContext())
        adapter = FormatsAdapter { format ->
            // TODO: Handle format selection
        }
        adapter.updateFormats(formats)
        rvFormats.adapter = adapter
    }

    fun setFormats(formats: List<Format>) {
        this.formats = formats
        if (::adapter.isInitialized) {
            adapter.updateFormats(formats)
        }
    }

    companion object {
        fun newInstance(formats: List<Format>): FormatBottomSheet {
            return FormatBottomSheet().apply {
                setFormats(formats)
            }
        }
    }
}

            }            }

        }        }

    }    }

}}