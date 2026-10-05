package com.example.financemanager

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.ActivityMainBinding
import com.example.financemanager.ui.CROATIAN
import com.example.financemanager.ui.EmphasizedDecelerate
import com.example.financemanager.ui.MoneyFormat
import com.example.financemanager.ui.TransactionAdapter
import com.example.financemanager.ui.staggerIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private val adapter = TransactionAdapter()

    private var displayedBalance = 0.0
    private var balanceAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(applicationContext)

        binding.greeting.text = getString(greetingForNow())
        binding.todayDate.text = SimpleDateFormat("EEE, d. MMM", CROATIAN)
            .format(Calendar.getInstance().time)
            .replaceFirstChar { it.titlecase(CROATIAN) }

        // Keep the decorative shapes inside the rounded hero card.
        binding.balanceCard.clipToOutline = true
        binding.trenutnoStanje.text = MoneyFormat.format(0.0)

        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter

        binding.prihod.setOnClickListener {
            startActivity(Intent(this, AddIncomeActivity::class.java))
        }
        binding.rashod.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        if (savedInstanceState == null) {
            staggerIn(binding.content)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    override fun onDestroy() {
        balanceAnimator?.cancel()
        super.onDestroy()
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                db.transactionDao().getAllTransactions()
            }

            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            binding.incomeTotal.text = MoneyFormat.format(income)
            binding.expenseTotal.text = MoneyFormat.format(expense)
            animateBalanceTo(income - expense)

            val sorted = transactions.sortedWith(
                compareByDescending<Transaction> { it.date }.thenByDescending { it.id }
            )
            adapter.submitList(sorted)
            binding.transactionCount.text = sorted.size.toString()
            binding.recyclerTransactions.isVisible = sorted.isNotEmpty()
            binding.emptyState.isVisible = sorted.isEmpty()
        }
    }

    /** Counts the balance up/down to its new value instead of jumping. */
    private fun animateBalanceTo(target: Double) {
        balanceAnimator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        if (target == displayedBalance) {
            binding.trenutnoStanje.text = MoneyFormat.format(target)
            return
        }
        balanceAnimator = ValueAnimator.ofFloat(displayedBalance.toFloat(), target.toFloat()).apply {
            duration = 900L
            interpolator = EmphasizedDecelerate
            addUpdateListener {
                displayedBalance = (it.animatedValue as Float).toDouble()
                binding.trenutnoStanje.text = MoneyFormat.format(displayedBalance)
            }
            doOnEnd {
                displayedBalance = target
                binding.trenutnoStanje.text = MoneyFormat.format(target)
            }
            start()
        }
    }

    private fun greetingForNow(): Int = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_day
        else -> R.string.greeting_evening
    }
}
