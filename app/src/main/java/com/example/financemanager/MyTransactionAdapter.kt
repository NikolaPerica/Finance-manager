package com.example.financemanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType

class MyTransactionAdapter : RecyclerView.Adapter<MyTransactionAdapter.TransactionViewHolder>() {

    private var transactions: List<Transaction> = emptyList()
    private var onItemClickListener: ((Transaction) -> Unit)? = null

    interface OnItemClickListener {
        fun onItemClick(transaction: Transaction)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        private val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
        private val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val transaction = transactions[position]
                    onItemClickListener?.invoke(transaction)
                }
            }
        }

        fun bind(transaction: Transaction) {
            textViewName.text = transaction.note
            textViewAmount.text = transaction.amount.toString()
            textViewCategory.text = transaction.category

            when (transaction.type) {
                TransactionType.INCOME -> {
                    textViewAmount.setTextColor(Color.GREEN)
                }
                TransactionType.EXPENSE -> {
                    textViewAmount.setTextColor(Color.RED)
                }
                else -> {}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    fun updateData(transactions: List<Transaction>) {
        this.transactions = transactions.sortedByDescending { it.id }
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Transaction) -> Unit) {
        onItemClickListener = listener
    }
}




