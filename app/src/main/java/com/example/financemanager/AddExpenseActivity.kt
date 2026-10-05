package com.example.financemanager

import com.example.financemanager.data.TransactionType

class AddExpenseActivity : TransactionFormActivity() {
    override val transactionType = TransactionType.EXPENSE
}
