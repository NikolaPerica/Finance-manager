package com.example.financemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.data.Reminder

class ReminderAdapter(
    private var reminders: List<Reminder>,
    private val clickListener: ReminderClickListener
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_reminder,
            parent,
            false
        )
        return ReminderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)
    }

    override fun getItemCount(): Int {
        return reminders.size
    }

    fun setData(data: List<Reminder>) {
        reminders = data
        notifyDataSetChanged()
    }

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val periodTextView: TextView = itemView.findViewById(R.id.periodTextView)

        fun bind(reminder: Reminder) {
            nameTextView.text = reminder.name
            amountTextView.text = reminder.amount.toString()
            periodTextView.text = reminder.periodType

            itemView.setOnClickListener {
                clickListener.onReminderClick(reminder)
            }

            itemView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                clickListener.onReminderDelete(reminder)
            }
        }
    }

    interface ReminderClickListener {
        fun onReminderClick(reminder: Reminder)
        fun onReminderDelete(reminder: Reminder)
    }
}
