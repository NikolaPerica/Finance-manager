package com.example.financemanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prihod=findViewById<Button>(R.id.prihod)
        val rashod=findViewById<Button>(R.id.rashod)
        val stanjeRacuna=findViewById<TextView>(R.id.trenutnoStanje)

        prihod.setOnClickListener {
            Toast.makeText(applicationContext, "Prihod stisnut", Toast.LENGTH_SHORT).show()
            stanjeRacuna.text="30"
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }

        rashod.setOnClickListener {
            Toast.makeText(applicationContext, "Rashod stisnut", Toast.LENGTH_SHORT).show()
            stanjeRacuna.text="0"
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

    }
}