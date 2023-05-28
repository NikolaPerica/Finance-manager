package com.example.financemanager

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity


class AddIncomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_income_activity)

        val spinner = findViewById<Spinner>(R.id.kategorijaOdabir)
        val items = arrayOf("Kategorija 1", " Kategorija 2", "Kategorija 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.setAdapter(adapter)

    }
}