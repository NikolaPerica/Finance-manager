package com.example.financemanager

import android.app.DatePickerDialog
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
import com.example.financemanager.databinding.AddExpenseActivityBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.financemanager.data.CategoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var categoryDao: CategoryDao
    private lateinit var db: AppDatabase
    private lateinit var binding: AddExpenseActivityBinding

//    val edit_date =findViewById<EditText>(R.id.edit_date)
//    val btn_save=findViewById<Button>(R.id.btn_save)
//    val spinner_category=findViewById<Spinner>(R.id.spinner_category)
//    val edit_amount=findViewById<EditText>(R.id.edit_amount)
//    val edit_note=findViewById<EditText>(R.id.edit_note)
//    val fab_add_expense_category=findViewById<FloatingActionButton>(R.id.fab_add_expense_category)
    
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
        val btn_save=findViewById<Button>(R.id.btn_save)
        val fab_add_expense=findViewById<FloatingActionButton>(R.id.fab_add_expense_category)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_expense_activity)
        db = AppDatabase.getDatabase(applicationContext)

        val database = AppDatabase.getDatabase(this)
        categoryDao = database.categoryDao()
        val edit_date =findViewById<EditText>(R.id.edit_date)
        // Set up the date EditText
        edit_date.setOnClickListener {
            showDatePickerDialog()
        }

        // Fetch expense categories from the database and populate the spinner
        fetchExpenseCategories()

        fab_add_expense?.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter category name")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("Add") { dialog, which ->
                val text = input.text.toString()
                val category = Category(0, text, TransactionType.EXPENSE)
                db.categoryDao().insertCategory(category)
                finish()
            }

            builder.setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

            builder.show()
        }
        // Save button click listener
        btn_save?.setOnClickListener {
            saveExpense()
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
            val edit_date =findViewById<EditText>(R.id.edit_date)
            edit_date.setText(sdf.format(selectedDate.time))
        }, year, month, day)
        datePickerDialog.show()
    }

   /* private fun fetchExpenseCategories() {
        val spinner_category=findViewById<Spinner>(R.id.spinner_category)
        val expenseCategories = db.categoryDao().getCategoriesByType(TransactionType.EXPENSE)
        val categoryNames = expenseCategories.map { category -> category.name }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ArrayList(categoryNames))
        spinner_category.adapter = adapter
    }*/
    private fun fetchExpenseCategories() {
        val spinnerCategory = findViewById<Spinner>(R.id.spinner_category)
        GlobalScope.launch(Dispatchers.Main) {
            val expenseCategories = withContext(Dispatchers.IO) {
                db.categoryDao().getCategoriesByType(TransactionType.EXPENSE)
            }
            val categoryNames = expenseCategories.map { category -> category.name }
            val adapter = ArrayAdapter<String>(this@AddExpenseActivity, android.R.layout.simple_spinner_item, categoryNames)
            spinnerCategory.adapter = adapter
        }
    }



    private fun saveExpense() {
        val edit_date =findViewById<EditText>(R.id.edit_date)
        val edit_amount=findViewById<EditText>(R.id.edit_amount)
        val edit_note=findViewById<EditText>(R.id.edit_note)
        val amount = edit_amount.text.toString().toDouble()
        val date = edit_date.text.toString()
        val note = edit_note.text.toString()
        val spinner_category=findViewById<Spinner>(R.id.spinner_category)
        val selectedCategory = spinner_category.selectedItem.toString()

        // Save the expense as a transaction to the database
        val transaction = Transaction(0, amount, date, note, selectedCategory, TransactionType.EXPENSE)
        db.transactionDao().insertTransaction(transaction)
        finish()
    }

}
