package com.unipass.core.types

import com.google.gson.annotations.SerializedName

enum class SignType(val value: String) {
    @SerializedName("PersonalSign")
    PersonalSign("PersonalSign"),

    @SerializedName("SignTypedData")
    SignTypedData("SignTypedData"),
}


data class SignInput (
    val from: String,
    val type: SignType,
    val msg: String
)