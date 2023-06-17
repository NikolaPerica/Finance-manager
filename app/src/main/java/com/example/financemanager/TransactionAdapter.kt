
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.Transaction

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    private var transactionList: List<Transaction> = emptyList()

    fun setData(transactions: List<Transaction>) {
        transactionList = transactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueTextView: TextView = itemView.findViewById(R.id.textViewValue)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        private val noteTextView: TextView = itemView.findViewById(R.id.textViewNote)

        fun bind(transaction: Transaction) {
            valueTextView.text = transaction.amount.toString()
            dateTextView.text = transaction.date
            categoryTextView.text = transaction.category
            noteTextView.text = transaction.note
        }
    }
}
