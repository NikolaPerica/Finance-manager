package com.example.financemanager

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prihod=findViewById<Button>(R.id.prihod)
        val rashod=findViewById<Button>(R.id.rashod)
        val stanjeRacuna=findViewById<TextView>(R.id.trenutnoStanje)
        val selectedDate = getCurrentDateAsString()
        db = AppDatabase.getDatabase(applicationContext)
        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                db.transactionDao().getTransactionsByDate(selectedDate)
            }

            val totalAmount = transactions.sumOf { transaction ->
                if (transaction.type == TransactionType.EXPENSE) {
                    -transaction.amount
                } else {
                    transaction.amount
                }
            }

            //val euroFormat = NumberFormat.getCurrencyInstance(Locale("en", "EUR"))
            //val formattedAmount = euroFormat.format(totalAmount)

            stanjeRacuna.text = "${totalAmount} \u20AC"

        }




        prihod.setOnClickListener {
            Toast.makeText(applicationContext, "Prihod stisnut", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }

        rashod.setOnClickListener {
            //Toast.makeText(applicationContext, "Rashod stisnut", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateAsString(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return currentDate.format(formatter)
    }
}