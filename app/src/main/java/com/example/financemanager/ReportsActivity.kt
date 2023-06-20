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
import java.text.SimpleDateFormat
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

        binding.filterByWeekButton.setOnClickListener {
            selectWeekAndLoadData()

        }

        binding.filterByMonthButton.setOnClickListener {
            selectMonthAndLoadData()
        }


        loadData()
    }


    private fun loadData() {
        val category: String? = selectedCategory?.name
        val type: String? = selectedType?.name
        val date: String? = selectedDate

        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                appDatabase.transactionDao().getFilteredTransactions(date, category, type)
            }

            transactionAdapter.setData(transactions)
        }
    }

    private fun selectWeekAndLoadData() {
        val calendar = Calendar.getInstance()

        val weekStartDatePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, monthOfYear, dayOfMonth)

                val weekStartDate = selectedCalendar.getStartDateOfWeek().toFormattedDateString()
                val weekEndDate = selectedCalendar.getEndDateOfWeek().toFormattedDateString()

                lifecycleScope.launch {
                    val transactions = withContext(Dispatchers.IO) {
                        appDatabase.transactionDao().getFilteredTransactionsByWeek(weekStartDate, weekEndDate)
                    }

                    transactionAdapter.setData(transactions)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        weekStartDatePickerDialog.datePicker.firstDayOfWeek = Calendar.SUNDAY
        weekStartDatePickerDialog.show()
    }

    private fun Calendar.getStartDateOfWeek(): Calendar {
        val calendar = clone() as Calendar
        calendar.firstDayOfWeek = Calendar.SUNDAY
        while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.firstDayOfWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        return calendar
    }

    private fun Calendar.getEndDateOfWeek(): Calendar {
        val calendar = clone() as Calendar
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        return calendar
    }

    private fun selectMonthAndLoadData() {
        val calendar = Calendar.getInstance()

        val monthYearPickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, _ ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, monthOfYear, 1)

                val monthStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)

                selectedCalendar.add(Calendar.MONTH, 1)
                selectedCalendar.add(Calendar.DAY_OF_MONTH, -1)

                val monthEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)

                lifecycleScope.launch {
                    val transactions = withContext(Dispatchers.IO) {
                        appDatabase.transactionDao().getFilteredTransactionsByMonth(monthStartDate, monthEndDate)
                    }

                    transactionAdapter.setData(transactions)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.getActualMinimum(Calendar.DAY_OF_MONTH)
        )

        monthYearPickerDialog.datePicker.minDate = getStartOfYear(calendar).timeInMillis
        monthYearPickerDialog.datePicker.maxDate = getEndOfYear(calendar).timeInMillis
        monthYearPickerDialog.datePicker.calendarViewShown = false
        monthYearPickerDialog.show()
    }


    private fun Calendar.toFormattedDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(time)
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
                if (selectedType == TransactionType.ALL) {
                    selectedType = null // Set selectedType to null for "ALL" option
                }
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


