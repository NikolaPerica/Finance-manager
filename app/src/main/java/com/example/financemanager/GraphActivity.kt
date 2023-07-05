package com.example.financemanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.data.Mapping
import com.anychart.data.Set
import com.anychart.enums.TooltipPositionMode
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GraphActivity : AppCompatActivity() {

    private lateinit var chartView: AnyChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chartView = findViewById(R.id.chartView)

        // Set up the chart
        val chart = AnyChart.line()

        // Set the tooltip position mode
        chart.tooltip().positionMode(TooltipPositionMode.POINT)

        // Set the title
        chart.title("Transaction History")

        // Get the data from the database using lifecycleScope
        lifecycleScope.launch {
            val dataEntries = withContext(Dispatchers.IO) {
                val transactions: List<Transaction> = getTransactionsFromDatabase()
                transactions.map { transaction ->
                    ValueDataEntry(transaction.date, transaction.amount)
                }
            }

            // Create a data set and add the data entries
            val dataSet = Set.instantiate()
            dataSet.data(dataEntries)

            // Create a mapping and set the data set
            val mapping: Mapping = dataSet.mapAs("{ x: 'x', value: 'value' }")

            // Set the data mapping to the chart
            val series = chart.line(mapping)

            // Set the chart view
            chartView.setChart(chart)
        }
    }

    private suspend fun getTransactionsFromDatabase(): List<Transaction> {
        // Replace this with your actual implementation to fetch transaction data from the database
        return AppDatabase.getDatabase(applicationContext).transactionDao().getAllTransactions()
    }
}
