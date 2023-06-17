package com.example.financemanager
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.financemanager.data.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterDialog : DialogFragment() {
    private lateinit var categorySpinner: Spinner
    private lateinit var categoryAdapter: ArrayAdapter<Category>
    private var selectedCategory: Category? = null
    private var listener: OnFilterListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_filter, null)

        categorySpinner = view.findViewById(R.id.categorySpinner)
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<Category>())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Filter by Category")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val category = categorySpinner.selectedItem as Category?
                listener?.onFilterSelected(category)
            }
            .setNegativeButton("Cancel") { _, _ -> }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onResume() {
        super.onResume()
        // Update the category list when the dialog is shown
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                listener?.getAllCategories()
            }
            categoryAdapter.clear()
            if (categories != null) {
                categoryAdapter.addAll(categories)
            }
            categoryAdapter.notifyDataSetChanged()
        }
    }

    fun setOnFilterListener(listener: OnFilterListener) {
        this.listener = listener
    }

    interface OnFilterListener {
        suspend fun getAllCategories(): List<Category>
        fun onFilterSelected(category: Category?)
    }

    companion object {
        fun newInstance(): FilterDialog {
            return FilterDialog()
        }
    }
}
