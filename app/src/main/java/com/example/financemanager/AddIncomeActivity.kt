package com.example.financemanager

import android.app.DatePickerDialog
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
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
    private lateinit var mainFabIncome: ExtendedFloatingActionButton
    private lateinit var fabAddIncomeCategory: FloatingActionButton
    private lateinit var fabRemoveIncomeCategory: FloatingActionButton

    private var isFabIncomeMenuOpen = true


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
        mainFabIncome = findViewById(R.id.mainFabIncome)
        fabAddIncomeCategory = findViewById(R.id.fab_add_income_category)
        fabRemoveIncomeCategory = findViewById(R.id.fab_remove_income_category)

        mainFabIncome.isExtended = false
        closeFabMenu()
        mainFabIncome.setOnClickListener {
            if (isFabIncomeMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

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
        fabRemoveIncomeCategory.setOnClickListener {
            showDeleteCategoryDialog()
        }
    }
    private fun openFabMenu() {
        fabAddIncomeCategory.show()
        fabRemoveIncomeCategory.show()
        isFabIncomeMenuOpen = true
    }

    private fun closeFabMenu() {
        fabAddIncomeCategory.hide()
        fabRemoveIncomeCategory.hide()
        isFabIncomeMenuOpen = false
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
    private suspend fun checkIfCategoryUsed(categoryName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val transactions = db.transactionDao().getTransactionsByCategory(categoryName)
            transactions.isNotEmpty()
        }
    }



    private suspend fun getCategoriesFromDatabase(): List<Category> {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoriesByType(TransactionType.INCOME)
        }
    }


    private fun showDeleteCategoryDialog() {
        lifecycleScope.launch {
            val categoriesList = getCategoriesFromDatabase()

            val categoryNames = categoriesList.map { it.name }.toTypedArray()

            val dialog = AlertDialog.Builder(this@AddIncomeActivity)
                .setTitle("Delete Category")
                .setItems(categoryNames) { _, position ->
                    val selectedCategory = categoriesList[position]
                    val selectedCategoryName = selectedCategory.name

                    lifecycleScope.launch {
                        val isCategoryUsed = checkIfCategoryUsed(selectedCategoryName)

                        if (isCategoryUsed) {
                            showErrorDialog("Cannot delete category as it is used in transactions.")
                        } else {
                            showConfirmationDialog(selectedCategory)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            val isCategoryUsed = checkIfCategoryUsed(category.name)

            if (isCategoryUsed) {
                showErrorDialog("Cannot delete category as it is used in transactions.")
            } else {
                withContext(Dispatchers.IO) {
                    db.categoryDao().deleteCategory(category)
                }
                Toast.makeText(this@AddIncomeActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                fetchIncomeCategories()
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val errorDialog = AlertDialog.Builder(this@AddIncomeActivity)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()

        errorDialog.show()
    }





    private fun showConfirmationDialog(category: Category) {
        val confirmationDialog = AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete the category '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the category from the categories table
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .create()

        confirmationDialog.show()
    }



}
//data class Category(val id: Int, val name: String)