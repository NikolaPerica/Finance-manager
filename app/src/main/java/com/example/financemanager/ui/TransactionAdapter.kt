package com.example.financemanager.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import com.example.financemanager.databinding.ItemTransactionBinding

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemTransactionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        val binding = holder.binding
        val context = binding.root.context
        val isIncome = transaction.type == TransactionType.INCOME

        val accent = ContextCompat.getColor(context, if (isIncome) R.color.income else R.color.expense)
        val container = ContextCompat.getColor(
            context,
            if (isIncome) R.color.income_container else R.color.expense_container
        )

        binding.iconContainer.backgroundTintList = ColorStateList.valueOf(container)
        binding.icon.setImageResource(if (isIncome) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward)
        binding.icon.imageTintList = ColorStateList.valueOf(accent)

        binding.category.text = transaction.category.ifBlank {
            context.getString(if (isIncome) R.string.prihod else R.string.rashod)
        }
        binding.subtitle.text = listOf(DateLabels.relative(context, transaction.date), transaction.note)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val sign = if (isIncome) "+" else "−"
        binding.amount.text = sign + MoneyFormat.format(transaction.amount)
        binding.amount.setTextColor(accent)
    }

    private object Diff : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}
