package com.example.financemanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner

class AddExpenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_expense_activity)
        val spinnerTrosak = findViewById<Spinner>(R.id.kategorijaTrosakOdabir)
        val items = arrayOf("Trosak 1", " Trosak 2", "Trosak 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinnerTrosak.setAdapter(adapter)
    }
}