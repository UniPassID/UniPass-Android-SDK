package com.unipass.core.types

import com.google.gson.annotations.SerializedName

enum class OutputType {
    @SerializedName("login")
    Login,

    @SerializedName("logout")
    Logout,

    @SerializedName("sign_message")
    SignMessage,

    @SerializedName("send_transaction")
    SendTransaction
}


open class BaseOutput(val type: OutputType?, val error: String? = null)
