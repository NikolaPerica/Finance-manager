package com.example.financemanager

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.AddIncomeActivityBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var binding: AddIncomeActivityBinding

    private lateinit var edit_date: EditText
    private lateinit var btn_save: Button
    private lateinit var spinner_category: Spinner
    private lateinit var edit_amount: EditText
    private lateinit var edit_note: EditText
    private lateinit var fab_add_income_category: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddIncomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        // Initialize views
        edit_date = binding.editDate
        btn_save = binding.btnSave
        spinner_category = binding.spinnerCategory
        edit_amount = binding.editAmount
        edit_note = binding.editNote
        fab_add_income_category = binding.fabAddIncomeCategory

        // Set up the date EditText
        edit_date.setOnClickListener {
            showDatePickerDialog()
        }

        // Fetch income categories from the database and populate the spinner
        fetchIncomeCategories()

        // Save button click listener
        btn_save.setOnClickListener {
            saveIncome()
        }
        fab_add_income_category.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter category name")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("Add") { dialog, which ->
                val text = input.text.toString()
                val category = Category(0, text, TransactionType.INCOME)
                db.categoryDao().insertCategory(category)
                finish()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private fun showDatePickerDialog() {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            edit_date.setText(sdf.format(selectedDate.time))
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun fetchIncomeCategories() {
        val incomeCategories = db.categoryDao().getCategoriesByType(TransactionType.INCOME)
        val categoryNames = incomeCategories.map { category -> category.name }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ArrayList(categoryNames))
        spinner_category.adapter = adapter
    }

    private fun saveIncome() {
        val amount = edit_amount.text.toString().toDouble()
        val date = edit_date.text.toString()
        val note = edit_note.text.toString()
        val selectedCategory = spinner_category.selectedItem.toString()

        // Save the income as a transaction to the database
        val transaction = Transaction(0, amount, date, note, selectedCategory, TransactionType.INCOME)
        db.transactionDao().insertTransaction(transaction)
        finish()
    }
}
