package com.example.financemanager

import com.example.financemanager.data.TransactionType

class AddIncomeActivity : TransactionFormActivity() {
    override val transactionType = TransactionType.INCOME
}
