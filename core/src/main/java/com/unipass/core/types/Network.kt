package com.unipass.core.types
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

enum class Network {
    @SerializedName("mainnet")
    MAINNET,

    @SerializedName("testnet")
    TESTNET,
}
