
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.Transaction
import com.example.financemanager.data.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            val newDate=changeDateFormatUsingDateTimeFormatter(transaction.date)
            valueTextView.text = transaction.amount.toString()
            dateTextView.text = newDate
            categoryTextView.text = transaction.category
            noteTextView.text = transaction.note

            val context = itemView.context
            val textColor = when (transaction.type) {
                TransactionType.INCOME -> {
                    valueTextView.setTextColor(Color.GREEN)
                }
                TransactionType.EXPENSE -> {
                    valueTextView.setTextColor(Color.RED)
                }

                else -> {}
            }

        }
    }

    fun changeDateFormatUsingDateTimeFormatter(inputDate: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        val date: LocalDate = LocalDate.parse(inputDate, inputFormat)
        return outputFormat.format(date)
    }
}
