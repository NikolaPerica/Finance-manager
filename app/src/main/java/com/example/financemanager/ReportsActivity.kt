package com.example.financemanager

import TransactionAdapter
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.ActivityReportsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class ReportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportsBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase
    private var selectedDate: String? = null
    private var selectedCategory: Category? = null
    private var selectedType: TransactionType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDatabase = AppDatabase.getDatabase(this)

        setupRecyclerView()

        binding.filterByDateButton.setOnClickListener {
            showDatePickerDialog()
        }

        binding.filterByCategoryButton.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.filterByTypeButton.setOnClickListener {
            showTypeFilterDialog()
        }

        loadData()
    }

    private fun loadData() {
        val date: String? = selectedDate
        val category: String? = selectedCategory?.name
        val type: String? = selectedType?.name

        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                appDatabase.transactionDao().getFilteredTransactions(date, category, type)
            }
            transactionAdapter.setData(transactions)
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedCategory = null
            selectedType = null
            loadData()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun showCategoryFilterDialog() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                appDatabase.categoryDao().getAllCategories()
            }
            val categoryNames = categories.map { it.name }

            val builder = AlertDialog.Builder(this@ReportsActivity)
                .setTitle("Filter by Category")
                .setItems(categoryNames.toTypedArray()) { _, index ->
                    selectedDate = null
                    selectedCategory = categories[index]
                    selectedType = null
                    loadData()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.show()
        }
    }


    private fun showTypeFilterDialog() {
        val transactionTypes = TransactionType.values().map { it.name }

        val builder = AlertDialog.Builder(this)
            .setTitle("Filter by Type")
            .setItems(transactionTypes.toTypedArray()) { _, index ->
                selectedDate = null
                selectedCategory = null
                selectedType = TransactionType.valueOf(transactionTypes[index])
                loadData()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.show()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@ReportsActivity)
            adapter = transactionAdapter
        }
    }
}
