package com.example.financemanager

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.enums.Align
import com.anychart.enums.LegendLayout
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class GraphActivity : AppCompatActivity() {

    private lateinit var chartView: AnyChartView
    private lateinit var groupingOptionSpinner: Spinner
    private lateinit var graphTypeSpinner: Spinner
    private lateinit var transactionTypeRadioGroup: RadioGroup
    private lateinit var monthYearLayout: LinearLayout
    private lateinit var typeLayout: LinearLayout
    private lateinit var reset: Button
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedGroupingOption = GroupingOption.values()[0]
    private var selectedGraphType = GraphType.values()[0]
    private var selectedTransactionType = TransactionType.ALL


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        chartView = findViewById(R.id.chartView)
        groupingOptionSpinner = findViewById(R.id.groupingOptionSpinner)
        graphTypeSpinner = findViewById(R.id.graphTypeSpinner)
        transactionTypeRadioGroup = findViewById(R.id.transactionTypeRadioGroup)
        monthYearLayout = findViewById(R.id.monthYearLayout)
        typeLayout=findViewById(R.id.typeLayout)
        reset=findViewById(R.id.resetMonthYear)

        val pie = AnyChart.pie()
        val line = AnyChart.line()
        val column = AnyChart.column()

        val datePickerButton = findViewById<Button>(R.id.datePickerButton)
        datePickerButton.setOnClickListener {
            showMonthYearPickerDialog()
        }

        reset.setOnClickListener {
            val toastText = "Selected Month: $selectedMonth, Year: $selectedYear"
            Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
            selectedMonth=0
            selectedYear=0
        }




        val graphTypeOptions = GraphType.values().map { it.name }
        val graphTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, graphTypeOptions)
        graphTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        graphTypeSpinner.adapter = graphTypeAdapter
        graphTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGraphType = GraphType.values()[position]
                updateChart(selectedGroupingOption, selectedGraphType, selectedTransactionType, selectedMonth, selectedYear, chartView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val groupingOptions = GroupingOption.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupingOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupingOptionSpinner.adapter = adapter
        groupingOptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGroupingOption = GroupingOption.values()[position]
                updateChart(selectedGroupingOption, selectedGraphType, selectedTransactionType, selectedMonth, selectedYear, chartView)

                if (selectedGroupingOption == GroupingOption.MONTH) {
                    monthYearLayout.visibility = View.GONE
                    selectedYear=0
                    selectedMonth=0
                } else {
                    monthYearLayout.visibility = View.VISIBLE
                }
                if (selectedGroupingOption == GroupingOption.TYPE) {
                    typeLayout.visibility = View.GONE
                    selectedTransactionType = TransactionType.ALL
                } else {
                    typeLayout.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        transactionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransactionType = when (checkedId) {
                R.id.radioIncome -> TransactionType.INCOME
                R.id.radioExpense -> TransactionType.EXPENSE
                R.id.radioAll -> TransactionType.ALL
                else -> TransactionType.ALL
            }
            updateChart(selectedGroupingOption, selectedGraphType, selectedTransactionType, selectedMonth, selectedYear, chartView)
        }

        chartView.setChart(pie)
    }



    private fun showMonthYearPickerDialog() {
        val calendar = Calendar.getInstance()

        // Set the initial selected year and month
        val initialYear = selectedYear.takeIf { it != 0 } ?: calendar.get(Calendar.YEAR)
        val initialMonth = selectedMonth.takeIf { it != 0 } ?: calendar.get(Calendar.MONTH)

        // Set up the constraints for the picker
        val constraints = CalendarConstraints.Builder()
            .setStart(calendar.apply { set(Calendar.MONTH, Calendar.JANUARY) }.timeInMillis)
            .setEnd(calendar.apply { set(Calendar.MONTH, Calendar.DECEMBER) }.timeInMillis)
            .build()

        // Create the date picker dialog
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Month and Year")
            .setCalendarConstraints(constraints)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        // Set the positive button click listener to get the selected year and month
        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.timeInMillis = selection

            selectedYear = selectedCalendar.get(Calendar.YEAR)
            selectedMonth = selectedCalendar.get(Calendar.MONTH)
            selectedMonth+=1
            // Call the updateChart function here to update the chart with the selected year and month
            updateChart(selectedGroupingOption, selectedGraphType, selectedTransactionType, selectedYear, selectedMonth, chartView)
        }


        // Show the date picker dialog
        datePicker.show(supportFragmentManager, "MonthYearPicker")

    }





    private fun setGraphToLineChart(
        selectedGroupingOption: GroupingOption,
        selectedTransactionType: TransactionType,
        myChart: AnyChartView
    ) {
        myChart.removeAllViews()

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView)

        val line = AnyChart.line()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase(selectedTransactionType, selectedMonth, selectedYear)
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory(transactions)
                    GroupingOption.YEAR -> sumDataByYear(transactions)
                    GroupingOption.MONTH -> sumDataByMonth(transactions)
                    GroupingOption.DATE -> sumDataByDate(transactions)
                    GroupingOption.TYPE -> sumDataByType(transactions)
                    GroupingOption.DAY -> sumDataByDay(transactions)
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

    private fun setGraphToPieChart(
        selectedGroupingOption: GroupingOption,
        selectedTransactionType: TransactionType,
        myChart: AnyChartView
    ) {
        myChart.removeAllViews()

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView)

        val pie = AnyChart.pie()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase(selectedTransactionType, selectedMonth, selectedYear)
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory(transactions)
                    GroupingOption.YEAR -> sumDataByYear(transactions)
                    GroupingOption.MONTH -> sumDataByMonth(transactions)
                    GroupingOption.DATE -> sumDataByDate(transactions)
                    GroupingOption.TYPE -> sumDataByType(transactions)
                    GroupingOption.DAY -> sumDataByDay(transactions)
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

    private fun setGraphToColumnChart(
        selectedGroupingOption: GroupingOption,
        selectedTransactionType: TransactionType,
        myChart: AnyChartView
    ) {
        myChart.removeAllViews()

        val newChartView = AnyChartView(this)
        myChart.addView(newChartView)

        val column = AnyChart.column()

        lifecycleScope.launch {
            val transactions: List<Transaction> = withContext(Dispatchers.IO) {
                getTransactionsFromDatabase(selectedTransactionType, selectedMonth, selectedYear)
            }

            val groupedData: Map<String, Double> = withContext(Dispatchers.Default) {
                when (selectedGroupingOption) {
                    GroupingOption.CATEGORY -> sumDataByCategory(transactions)
                    GroupingOption.YEAR -> sumDataByYear(transactions)
                    GroupingOption.MONTH -> sumDataByMonth(transactions)
                    GroupingOption.DATE -> sumDataByDate(transactions)
                    GroupingOption.TYPE -> sumDataByType(transactions)
                    GroupingOption.DAY -> sumDataByDay(transactions)
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
        selectedTransactionType: TransactionType,
        selectedMonth: Int,
        selectedYear: Int,
        chartView: AnyChartView
    ) {
        when (selectedGraphType) {
            GraphType.PIE -> setGraphToPieChart(selectedGroupingOption, selectedTransactionType, chartView)
            GraphType.COLUMN -> setGraphToColumnChart(selectedGroupingOption, selectedTransactionType, chartView)
            GraphType.LINE -> setGraphToLineChart(selectedGroupingOption, selectedTransactionType, chartView)
        }
    }

    private suspend fun getTransactionsFromDatabase(
        selectedTransactionType: TransactionType,
        selectedMonth: Int,
        selectedYear: Int
    ): List<Transaction> {
        return withContext(Dispatchers.IO) {
            val allTransactions: List<Transaction> = AppDatabase.getDatabase(applicationContext)
                .transactionDao().getAllTransactions()

            val filteredTransactions = when (selectedTransactionType) {
                TransactionType.INCOME -> allTransactions.filter { it.type == TransactionType.INCOME }
                TransactionType.EXPENSE -> allTransactions.filter { it.type == TransactionType.EXPENSE }
                TransactionType.ALL -> allTransactions
            }

            if (selectedMonth != 0 && selectedYear != 0) {
                val formattedMonth = String.format("%02d", selectedMonth)
                val formattedYear = selectedYear.toString()
                val filteredByMonthAndYear = filteredTransactions.filter {
                    it.date.startsWith("$formattedYear-$formattedMonth")
                }
                filteredByMonthAndYear
            } else {
                filteredTransactions
            }
        }
    }


    private suspend fun sumDataByCategory(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
            transactions
                .groupBy { it.category }
                .mapValues { (_, transactionsByCategory) ->
                    transactionsByCategory.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByYear(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
            transactions
                .groupBy { it.date.substring(0, 4) }
                .mapValues { (_, transactionsByYear) ->
                    transactionsByYear.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByMonth(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
            transactions
                .groupBy { it.date.substring(5, 7) }
                .mapValues { (_, transactionsByMonth) ->
                    transactionsByMonth.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByDate(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
            transactions
                .groupBy { it.date }
                .mapValues { (_, transactionsByDate) ->
                    transactionsByDate.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByType(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
            transactions
                .groupBy { it.type.name }
                .mapValues { (_, transactionsByType) ->
                    transactionsByType.sumOf { it.amount }
                }
        }
    }

    private suspend fun sumDataByDay(transactions: List<Transaction>): Map<String, Double> {
        return withContext(Dispatchers.Default) {
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
    CATEGORY,
    YEAR,
    MONTH,
    DATE,
    TYPE,
    DAY
}

