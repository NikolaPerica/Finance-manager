package com.example.financemanager

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Category
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.ActivityTransactionFormBinding
import com.example.financemanager.databinding.DialogAddCategoryBinding
import com.example.financemanager.ui.CROATIAN
import com.example.financemanager.ui.applySystemBarsPadding
import com.example.financemanager.ui.shake
import com.example.financemanager.ui.staggerIn
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Shared screen for adding an income or an expense; subclasses only pick the type. */
abstract class TransactionFormActivity : AppCompatActivity() {

    protected abstract val transactionType: TransactionType

    private lateinit var binding: ActivityTransactionFormBinding
    private lateinit var db: AppDatabase
    private var categories: List<Category> = emptyList()

    // MaterialDatePicker works in UTC milliseconds, so format in UTC as well.
    private var selectedDateMillis = MaterialDatePicker.todayInUtcMilliseconds()
    private val storageFormat = utcFormat("yyyy-MM-dd", Locale.ROOT)
    private val displayFormat = utcFormat("d. MMMM yyyy.", CROATIAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTransactionFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Edge-to-edge windows aren't resized for the keyboard, so pad for it ourselves.
        binding.root.applySystemBarsPadding(includeIme = true)
        db = AppDatabase.getDatabase(applicationContext)

        savedInstanceState?.let { selectedDateMillis = it.getLong(KEY_DATE, selectedDateMillis) }

        applyTypeStyling()
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        updateDateField()
        binding.editDate.setOnClickListener { showDatePicker() }

        binding.inputCategory.setOnItemClickListener { _, _, _, _ -> binding.layoutCategory.error = null }
        binding.btnAddCategory.setOnClickListener { showAddCategoryDialog() }
        binding.editAmount.doAfterTextChanged { binding.amountError.isVisible = false }
        binding.btnSave.setOnClickListener { save() }

        loadCategories()

        if (savedInstanceState == null) {
            staggerIn(binding.formContent, startDelay = 120L, step = 50L)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_DATE, selectedDateMillis)
    }

    private fun applyTypeStyling() {
        val isIncome = transactionType == TransactionType.INCOME
        val accent = ContextCompat.getColor(this, if (isIncome) R.color.income else R.color.expense)
        val container = ContextCompat.getColor(
            this,
            if (isIncome) R.color.income_container else R.color.expense_container
        )

        binding.toolbar.title = getString(if (isIncome) R.string.novi_prihod else R.string.novi_rashod)
        binding.amountCard.setCardBackgroundColor(container)
        binding.amountIcon.setImageResource(
            if (isIncome) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward
        )
        binding.amountIcon.imageTintList = ColorStateList.valueOf(accent)
        binding.amountLabel.setTextColor(accent)
        binding.amountCurrency.setTextColor(accent)
    }

    private fun updateDateField() {
        binding.editDate.setText(displayFormat.format(selectedDateMillis))
    }

    private fun showDatePicker() {
        if (supportFragmentManager.findFragmentByTag(TAG_DATE_PICKER) != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.odaberite_datum)
            .setSelection(selectedDateMillis)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selectedDateMillis = selection
            updateDateField()
        }
        picker.show(supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun loadCategories(select: String? = null) {
        lifecycleScope.launch {
            categories = withContext(Dispatchers.IO) {
                db.categoryDao().getCategoriesByType(transactionType)
            }
            val names = categories.map { it.name }
            binding.inputCategory.setAdapter(
                ArrayAdapter(this@TransactionFormActivity, R.layout.item_dropdown, names)
            )
            binding.layoutCategory.helperText =
                if (names.isEmpty()) getString(R.string.no_categories_hint) else null
            if (select != null) {
                binding.inputCategory.setText(select, false)
                binding.layoutCategory.error = null
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nova_kategorija)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.odustani, null)
            .setPositiveButton(R.string.dodaj, null)
            .create()

        dialogBinding.editCategoryName.doAfterTextChanged {
            dialogBinding.layoutCategoryName.error = null
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.editCategoryName.text?.toString()?.trim().orEmpty()
                when {
                    name.isEmpty() ->
                        dialogBinding.layoutCategoryName.error = getString(R.string.error_category_name)
                    categories.any { it.name.equals(name, ignoreCase = true) } ->
                        dialogBinding.layoutCategoryName.error = getString(R.string.error_category_exists)
                    else -> {
                        dialog.dismiss()
                        addCategory(name)
                    }
                }
            }
            dialogBinding.editCategoryName.requestFocus()
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun addCategory(name: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.categoryDao().insertCategory(Category(0, name, transactionType))
            }
            loadCategories(select = name)
            Snackbar.make(binding.root, getString(R.string.category_added, name), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.btnSave)
                .show()
        }
    }

    private fun save() {
        val amount = binding.editAmount.text.toString().replace(',', '.').toDoubleOrNull()
        val category = binding.inputCategory.text.toString()

        var valid = true
        if (amount == null || amount <= 0.0) {
            binding.amountError.isVisible = true
            binding.amountCard.shake()
            valid = false
        }
        if (category.isBlank()) {
            binding.layoutCategory.error = getString(R.string.error_category)
            binding.layoutCategory.shake()
            valid = false
        }
        if (!valid || amount == null) return

        val transaction = Transaction(
            0,
            amount,
            storageFormat.format(selectedDateMillis),
            binding.editNote.text?.toString()?.trim().orEmpty(),
            category,
            transactionType
        )
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.transactionDao().insertTransaction(transaction)
            }
            finish()
        }
    }

    private companion object {
        const val KEY_DATE = "selected_date"
        const val TAG_DATE_PICKER = "date_picker"

        fun utcFormat(pattern: String, locale: Locale) =
            SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
}
