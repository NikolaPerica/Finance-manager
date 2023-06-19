package com.example.financemanager

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemindersActivity : AppCompatActivity(), ReminderAdapter.ReminderClickListener {
    private lateinit var appDatabase: AppDatabase
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var remindersRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
        val addReminderButton = findViewById<Button>(R.id.addReminderButton)
        appDatabase = AppDatabase.getDatabase(this)

        reminderAdapter = ReminderAdapter(emptyList(), this)
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
        // Implement your logic to show the dialog for adding a new reminder
    }

    override fun onReminderClick(reminder: Reminder) {
        // Handle click on a reminder item
        // For example, open an edit dialog for the selected reminder
    }

    override fun onReminderDelete(reminder: Reminder) {
        // Handle delete action for the selected reminder
        // For example, prompt the user for confirmation and delete the reminder from the database
    }
}
