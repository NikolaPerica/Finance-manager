package com.example.financemanager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: MyTransactionAdapter
    private lateinit var recyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prihod=findViewById<Button>(R.id.prihod)
        val rashod=findViewById<Button>(R.id.rashod)
        val podsjetnici=findViewById<Button>(R.id.buttonOpenReminders)
        //DateCheckWorker.scheduleDailyCheck(this)
        db = AppDatabase.getDatabase(this)
        recyclerView = findViewById(R.id.latestTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyTransactionAdapter()
        recyclerView.adapter = adapter
        lifecycleScope.launch {
            val transactions: LiveData<List<Transaction>> = db.transactionDao().getAllTransactionsLiveData()
            //adapter.updateData(transactions)
        }
        db.transactionsLiveData.observe(this) { transactions ->
            val transactionList = transactions.sortedByDescending { it.id }
            adapter.updateData(transactionList)
        }

        adapter = MyTransactionAdapter()
        adapter.setOnItemClickListener { transaction ->
            showTransactionDialog(transaction)
        }
        recyclerView.adapter = adapter





        val buttonOpenGraphs: Button = findViewById(R.id.buttonOpenGraphs)
        buttonOpenGraphs.setOnClickListener {
            val intent = Intent(this, GraphActivity::class.java)
            startActivity(intent)
        }

        val buttonOpenReports: Button = findViewById(R.id.buttonOpenReports)
        buttonOpenReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }

        podsjetnici.setOnClickListener {
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
        }

        prihod.setOnClickListener {
           // Toast.makeText(applicationContext, "Prihod stisnut", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }

        rashod.setOnClickListener {
            //Toast.makeText(applicationContext, "Rashod stisnut", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

    }


    private fun showTransactionDialog(transaction: Transaction) {
        // Create and show the dialog with the detailed transaction data
        val dialog = AlertDialog.Builder(this)
            .setTitle("Transaction Details")
            .setMessage("Amount: ${transaction.amount}\n" +
                    "Date: ${transaction.date}\n" +
                    "Note: ${transaction.note}\n" +
                    "Category: ${transaction.category}\n" +
                    "Type: ${transaction.type}")
            .setPositiveButton("OK") { _, _ ->
                // Handle OK button click if needed
            }
            .create()

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateAsString(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return currentDate.format(formatter)
    }
}