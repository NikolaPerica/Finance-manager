package com.example.financemanager

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Pie
import com.anychart.enums.Align
import com.anychart.enums.LegendLayout
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GraphActivity : AppCompatActivity() {

    private lateinit var chartView: AnyChartView
    private lateinit var groupingOptionSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chartView = findViewById(R.id.chartView)
        groupingOptionSpinner = findViewById(R.id.groupingOptionSpinner)

        val pie = AnyChart.pie()

        // Set up the grouping option spinner
        val groupingOptions = GroupingOption.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupingOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupingOptionSpinner.adapter = adapter
        groupingOptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedGroupingOption = GroupingOption.values()[position]
                updateChart(selectedGroupingOption, pie)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        chartView.setChart(pie)
    }

    private fun updateChart(selectedGroupingOption: GroupingOption, pie: Pie) {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val transactions: List<Transaction> = getTransactionsFromDatabase()
                val groupedData: Map<String, Double> = when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory()
                    GroupingOption.YEAR -> sumDataByYear()
                    GroupingOption.MONTH -> sumDataByMonth()
                    GroupingOption.DATE -> sumDataByDate()
                    GroupingOption.TYPE -> sumDataByType()
                    GroupingOption.DAY -> sumDataByDay()
                }

                val dataList = mutableListOf<ValueDataEntry>()
                groupedData.forEach { (group, sum) ->
                    val valueDataEntry = ValueDataEntry(group, sum)
                    dataList.add(valueDataEntry)
                }
                dataList
            }

            pie.data(data as List<DataEntry>?)
            pie.labels().position("outside")

            pie.legend().title().enabled(true)
            pie.legend().title()
                .text(selectedGroupingOption.toString())
                .padding(0.0, 0.0, 10.0, 0.0)

            pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER)
        }
    }

        private suspend fun getTransactionsFromDatabase(): List<Transaction> {
            // Replace this with your actual implementation to fetch transaction data from the database
            return AppDatabase.getDatabase(applicationContext).transactionDao().getAllTransactions()
        }

    private suspend fun sumDataByCategory(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.category }
                .mapValues { (_, transactionsByCategory) ->
                    transactionsByCategory.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByYear(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.date.substring(0, 4) }
                .mapValues { (_, transactionsByYear) ->
                    transactionsByYear.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByMonth(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.date.substring(5, 7) }
                .mapValues { (_, transactionsByMonth) ->
                    transactionsByMonth.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByDate(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.date }
                .mapValues { (_, transactionsByDate) ->
                    transactionsByDate.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByType(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.type.name }
                .mapValues { (_, transactionsByType) ->
                    transactionsByType.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByDay(): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val transactions: List<Transaction> = getTransactionsFromDatabase()
            transactions
                .groupBy { it.date.substring(8, 10) }
                .mapValues { (_, transactionsByDay) ->
                    transactionsByDay.sumOf { it.amount }
                }
        }
    }



}



    enum class GroupingOption {
    CATEGORY, YEAR, MONTH, DATE, TYPE, DAY
}
