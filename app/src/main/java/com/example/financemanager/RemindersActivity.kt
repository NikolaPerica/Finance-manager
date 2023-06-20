package com.example.financemanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Reminder
import com.example.financemanager.databinding.ActivityRemindersBinding
import com.example.financemanager.databinding.DialogAddReminderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemindersActivity : AppCompatActivity(), ReminderAdapter.ReminderClickListener {
    private lateinit var appDatabase: AppDatabase
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var remindersRecyclerView: RecyclerView
    private lateinit var binding: ActivityRemindersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDatabase = AppDatabase.getDatabase(this)
        remindersRecyclerView = binding.remindersRecyclerView // Move this line here
        val addReminderButton = binding.addReminderButton

        reminderAdapter = ReminderAdapter(emptyList(), this)
        remindersRecyclerView.layoutManager = LinearLayoutManager(this) // Move this line here
        remindersRecyclerView.adapter = reminderAdapter

        loadReminders()

        addReminderButton.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun loadReminders() {
        lifecycleScope.launch {
            val reminders = withContext(Dispatchers.IO) {
                appDatabase.reminderDao().getAllReminders()
            }
            reminderAdapter.setData(reminders)
        }
    }


    private fun showAddReminderDialog() {
        val dialogBinding = DialogAddReminderBinding.inflate(LayoutInflater.from(this))

        // Initialize dialog views
        val nameEditText: EditText = dialogBinding.nameEditText
        val amountEditText: EditText = dialogBinding.amountEditText
        val periodTypeSpinner: Spinner = dialogBinding.periodTypeSpinner
        val dateEditText: EditText = dialogBinding.dateEditText

        // Set up the period type spinner with array resource
        val periodTypes = resources.getStringArray(R.array.period_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodTypeSpinner.adapter = adapter

        // Set up the date picker dialog for the date selection
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, monthOfYear, dayOfMonth)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.format(selectedCalendar.time)
                dateEditText.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set click listener for the date edit text to show the date picker dialog
        dateEditText.setOnClickListener {
            datePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Reminder")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val amount = amountEditText.text.toString().toDouble()
                val periodType = periodTypeSpinner.selectedItem.toString()
                val date = dateEditText.text.toString()

                if (name.isNotEmpty() && amount >= 0) {
                    val newReminder = Reminder(name = name, amount = amount, periodType = periodType, date = date)

                    // Save the new reminder to the database
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            appDatabase.reminderDao().insertReminder(newReminder)
                        }
                        // Refresh the reminder list
                        loadReminders()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }


    override fun onReminderClick(reminder: Reminder) {
        val dialogBinding = DialogAddReminderBinding.inflate(LayoutInflater.from(this))

        // Initialize dialog views
        val nameEditText: EditText = dialogBinding.nameEditText
        val amountEditText: EditText = dialogBinding.amountEditText
        val periodTypeSpinner: Spinner = dialogBinding.periodTypeSpinner
        val dateEditText: EditText = dialogBinding.dateEditText

        // Set the initial values of the dialog views with the clicked reminder's data
        nameEditText.setText(reminder.name)
        amountEditText.setText(reminder.amount.toString())
        val periodTypes = resources.getStringArray(R.array.period_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodTypeSpinner.adapter = adapter
        val periodTypePosition = periodTypes.indexOf(reminder.periodType)
        periodTypeSpinner.setSelection(periodTypePosition)
        dateEditText.setText(reminder.date)

        // Set up the date picker dialog for the date selection
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, monthOfYear, dayOfMonth)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.format(selectedCalendar.time)
                dateEditText.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set click listener for the date edit text to show the date picker dialog
        dateEditText.setOnClickListener {
            datePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Reminder")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val amount = amountEditText.text.toString().toDouble()
                val periodType = periodTypeSpinner.selectedItem.toString()
                val date = dateEditText.text.toString()

                if (name.isNotEmpty() && amount >= 0) {
                    val updatedReminder = Reminder(
                        id = reminder.id,
                        name = name,
                        amount = amount,
                        periodType = periodType,
                        date = date
                    )

                    // Update the reminder in the database
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            appDatabase.reminderDao().updateReminder(updatedReminder)
                        }
                        // Refresh the reminder list
                        loadReminders()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }


    override fun onReminderDelete(reminder: Reminder) {
        val confirmationDialog = AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appDatabase.reminderDao().deleteReminder(reminder)
                    }
                    // Refresh the reminder list
                    loadReminders()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        confirmationDialog.show()
    }

}
