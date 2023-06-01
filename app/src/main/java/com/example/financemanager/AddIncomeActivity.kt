package com.example.financemanager

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.CategoryDao
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.AddIncomeActivityBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var binding: AddIncomeActivityBinding
    private lateinit var categoryDao: CategoryDao
    private lateinit var edit_date: EditText
    private lateinit var btn_save: Button
    private lateinit var spinner_category: Spinner
    private lateinit var edit_amount: EditText
    private lateinit var edit_note: EditText
    private lateinit var fab_add_income_category: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        Toast.makeText(applicationContext, "OnCreateEntered", Toast.LENGTH_SHORT).show()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = preferences.getString("preference_key_theme", "light")

        // Apply the theme
        when (theme) {
            "light" -> setTheme(R.style.Theme_FinanceManager)
            "dark" -> setTheme(R.style.Theme_FinanceManagerDark)
            else -> setTheme(R.style.Theme_FinanceManager)
        }
        super.onCreate(savedInstanceState)
        binding = AddIncomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(applicationContext)
        val database = AppDatabase.getDatabase(this)
        categoryDao = database.categoryDao()

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
        btn_save?.setOnClickListener {
            saveIncome()
        }
        fab_add_income_category?.setOnClickListener{
            Toast.makeText(applicationContext, "FAB income pressed", Toast.LENGTH_SHORT).show()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter category name")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("Add") { dialog, which ->
                val text = input.text.toString()
                val category = Category(0, text, TransactionType.INCOME)

                // Perform database operation on a background thread
                GlobalScope.launch(Dispatchers.IO) {
                    db.categoryDao().insertCategory(category)
                }

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
        val spinnerCategory = findViewById<Spinner>(R.id.spinner_category)
        GlobalScope.launch(Dispatchers.Main) {
            val expenseCategories = withContext(Dispatchers.IO) {
                db.categoryDao().getCategoriesByType(TransactionType.INCOME)
            }
            val categoryNames = expenseCategories.map { category -> category.name }
            val adapter = ArrayAdapter<String>(this@AddIncomeActivity, android.R.layout.simple_spinner_item, categoryNames)
            spinnerCategory.adapter = adapter
        }
    }

    private fun saveIncome() {
        val amount = edit_amount.text.toString().toDouble()
        val date = edit_date.text.toString()
        val note = edit_note.text.toString()
        val selectedCategory = spinner_category.selectedItem.toString()

        // Save the income as a transaction to the database
        val transaction = Transaction(0, amount, date, note, selectedCategory, TransactionType.INCOME)
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().insertTransaction(transaction)
        }

        finish()
    }
}
