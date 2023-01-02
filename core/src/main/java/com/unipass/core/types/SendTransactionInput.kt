package com.unipass.core.types


data class SendTransactionInput (
        val from: String,
        val to: String,
        val value: String? = "0x",
        val data: String? = "0x",
        )
