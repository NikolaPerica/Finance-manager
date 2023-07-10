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
import com.anychart.core.Chart
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
    private lateinit var graphTypeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chartView = findViewById(R.id.chartView)
        groupingOptionSpinner = findViewById(R.id.groupingOptionSpinner)
        graphTypeSpinner=findViewById(R.id.graphTypeSpinner)


        val pie = AnyChart.pie()
        val line = AnyChart.line()
        val column = AnyChart.column()

        //var myChart = ""
        var myChart: Chart? = null


        var selectedGroupingOption = GroupingOption.values()[0]
        var selectedGraphType = GraphType.values()[0]

        val graphTypeOptions = GraphType.values().map { it.name }
        val graphTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, graphTypeOptions)
        graphTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        graphTypeSpinner.adapter = graphTypeAdapter
        graphTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGraphType = GraphType.values()[position]
                updateChart(selectedGroupingOption, selectedGraphType, chartView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

// Set up the grouping option spinner
        val groupingOptions = GroupingOption.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupingOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupingOptionSpinner.adapter = adapter
        groupingOptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGroupingOption = GroupingOption.values()[position]
                updateChart(selectedGroupingOption, selectedGraphType, chartView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        chartView.setChart(pie)
    }

    private fun setGraphToLineChart(selectedGroupingOption: GroupingOption, myChart: AnyChartView) {
        myChart.removeAllViews() // Remove existing view

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView) // Add new chart view

        val line = AnyChart.line()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase()
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory()
                    GroupingOption.YEAR -> sumDataByYear()
                    GroupingOption.MONTH -> sumDataByMonth()
                    GroupingOption.DATE -> sumDataByDate()
                    GroupingOption.TYPE -> sumDataByType()
                    GroupingOption.DAY -> sumDataByDay()
                }
            }

            val dataList = mutableListOf<DataEntry>()
            groupedData.forEach { (group, sum) ->
                val dataEntry = ValueDataEntry(group, sum)
                dataList.add(dataEntry)
            }

            line.data(dataList)
            line.labels().position("outside")

            line.legend().title().enabled(true)
            line.legend().title()
                .text(selectedGroupingOption.toString())
                .padding(0.0, 0.0, 10.0, 0.0)

            line.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER)

            newChartView.setChart(line)
        }
    }

    private fun setGraphToPieChart(selectedGroupingOption: GroupingOption, myChart: AnyChartView) {
        myChart.removeAllViews() // Remove existing view

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView) // Add new chart view

        val pie = AnyChart.pie()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase()
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory()
                    GroupingOption.YEAR -> sumDataByYear()
                    GroupingOption.MONTH -> sumDataByMonth()
                    GroupingOption.DATE -> sumDataByDate()
                    GroupingOption.TYPE -> sumDataByType()
                    GroupingOption.DAY -> sumDataByDay()
                }
            }

            val dataList = mutableListOf<DataEntry>()
            groupedData.forEach { (group, sum) ->
                val dataEntry = ValueDataEntry(group, sum)
                dataList.add(dataEntry)
            }

            pie.data(dataList)
            pie.labels().position("outside")

            pie.legend().title().enabled(true)
            pie.legend().title()
                .text(selectedGroupingOption.toString())
                .padding(0.0, 0.0, 10.0, 0.0)

            pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER)

            newChartView.setChart(pie)
        }
    }

    private fun setGraphToColumnChart(selectedGroupingOption: GroupingOption, myChart: AnyChartView) {
        myChart.removeAllViews() // Remove existing view

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView) // Add new chart view

        val column = AnyChart.column()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase()
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory()
                    GroupingOption.YEAR -> sumDataByYear()
                    GroupingOption.MONTH -> sumDataByMonth()
                    GroupingOption.DATE -> sumDataByDate()
                    GroupingOption.TYPE -> sumDataByType()
                    GroupingOption.DAY -> sumDataByDay()
                }
            }

            val dataList = mutableListOf<DataEntry>()
            groupedData.forEach { (group, sum) ->
                val dataEntry = ValueDataEntry(group, sum)
                dataList.add(dataEntry)
            }

            column.data(dataList)
            column.labels().position("outside")

            column.legend().title().enabled(true)
            column.legend().title()
                .text(selectedGroupingOption.toString())
                .padding(0.0, 0.0, 10.0, 0.0)

            column.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER)

            newChartView.setChart(column)
        }
    }



    private fun updateChart(
        selectedGroupingOption: GroupingOption,
        selectedGraphType: GraphType,
        chartView: AnyChartView
    ) {
        when (selectedGraphType) {
            GraphType.PIE -> setGraphToPieChart(selectedGroupingOption, chartView)
            GraphType.COLUMN -> setGraphToColumnChart(selectedGroupingOption, chartView)
            GraphType.LINE -> setGraphToLineChart(selectedGroupingOption, chartView)
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
enum class GraphType {
    PIE,
    COLUMN,
    LINE
}


enum class GroupingOption {
    CATEGORY, YEAR, MONTH, DATE, TYPE, DAY
}