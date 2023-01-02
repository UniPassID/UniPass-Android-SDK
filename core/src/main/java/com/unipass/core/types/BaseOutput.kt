package com.unipass.core.types

import com.google.gson.annotations.SerializedName

enum class OutputType {
    @SerializedName("UP_LOGIN")
    Login,

    @SerializedName("UP_LOGOUT")
    Logout,

    @SerializedName("UP_SIGN_MESSAGE")
    SignMessage,

    @SerializedName("UP_TRANSACTION")
    SendTransaction
}


open class BaseOutput(val type: OutputType?, val errorCode: Int? = null, val errorMsg: String? = null)
