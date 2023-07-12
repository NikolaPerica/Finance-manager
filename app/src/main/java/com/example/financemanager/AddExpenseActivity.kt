package com.example.financemanager

//import androidx.preference.PreferenceManager
import android.app.DatePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.CategoryDao
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.AddExpenseActivityBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: AddExpenseActivityBinding
    private lateinit var categoryDao: CategoryDao
    private lateinit var db: AppDatabase
    private lateinit var mainFab: ExtendedFloatingActionButton
    private lateinit var fabAddExpenseCategory: FloatingActionButton
    private lateinit var fabRemoveExpenseCategory: FloatingActionButton

    private var isFabMenuOpen = true
    private lateinit var btn_save: Button
    private lateinit var fab_add_expense_category: FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
     //   Toast.makeText(applicationContext, "OnCreateEntered", Toast.LENGTH_SHORT).show()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = preferences.getString("preference_key_theme", "light")

        // Apply the theme
        when (theme) {
            "light" -> setTheme(R.style.Theme_FinanceManager)
            "dark" -> setTheme(R.style.Theme_FinanceManagerDark)
            else -> setTheme(R.style.Theme_FinanceManager)
        }

        super.onCreate(savedInstanceState)
        binding = AddExpenseActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.add_expense_activity)
        db = AppDatabase.getDatabase(applicationContext)
        mainFab = findViewById(R.id.mainFab)
        fabAddExpenseCategory = findViewById(R.id.fab_add_expense_category)
        fabRemoveExpenseCategory = findViewById(R.id.fab_remove_expense_category)

        mainFab.isExtended = false
        closeFabMenu()
        mainFab.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        val database = AppDatabase.getDatabase(this)
        categoryDao = database.categoryDao()
        btn_save = findViewById(R.id.btn_save)
        fab_add_expense_category = findViewById(R.id.fab_add_expense_category)

        val edit_date =findViewById<EditText>(R.id.edit_date)
        // Set up the date EditText
        edit_date.setOnClickListener {
            showDatePickerDialog()
        }

        // Fetch expense categories from the database and populate the spinner
        fetchExpenseCategories()


        // Save button click listener
        btn_save?.setOnClickListener {
            saveExpense()
        }
        fab_add_expense_category?.setOnClickListener{
          //  Toast.makeText(applicationContext, "FAB expense pressed", Toast.LENGTH_SHORT).show()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter category name")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("Add") { dialog, which ->
                val text = input.text.toString()
                val category = Category(0, text, TransactionType.EXPENSE)

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

        fabRemoveExpenseCategory.setOnClickListener {
            showDeleteCategoryDialog()
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
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().insertTransaction(transaction)
        }
        finish()
    }
    private fun openFabMenu() {
        fabAddExpenseCategory.show()
        fabRemoveExpenseCategory.show()
        isFabMenuOpen = true
    }

    private fun closeFabMenu() {
        fabAddExpenseCategory.hide()
        fabRemoveExpenseCategory.hide()
        isFabMenuOpen = false
    }

    private suspend fun checkIfCategoryUsed(categoryName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val transactions = db.transactionDao().getTransactionsByCategory(categoryName)
            transactions.isNotEmpty()
        }
    }



    private suspend fun getCategoriesFromDatabase(): List<Category> {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoriesByType(TransactionType.EXPENSE)
        }
    }


    private fun showDeleteCategoryDialog() {
        lifecycleScope.launch {
            val categoriesList = getCategoriesFromDatabase()

            val categoryNames = categoriesList.map { it.name }.toTypedArray()

            val dialog = AlertDialog.Builder(this@AddExpenseActivity)
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
              //  Toast.makeText(this@AddExpenseActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                fetchExpenseCategories()
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val errorDialog = AlertDialog.Builder(this@AddExpenseActivity)
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
data class Category(val id: Int, val name: String)