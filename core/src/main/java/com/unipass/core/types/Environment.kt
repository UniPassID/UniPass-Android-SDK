package com.unipass.core.types
import com.google.gson.annotations.SerializedName

enum class Environment {
    @SerializedName("mainnet")
    MAINNET,

    @SerializedName("testnet")
    TESTNET,
}
