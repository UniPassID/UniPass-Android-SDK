package com.unipass.core.types


data class Transaction (
        val from: String,
        val to: String,
        val value: String? = "0x0",
        val data: String? = "0x",
        )

data class SendTransactionInput (
    val transactionList: List<Transaction>
)