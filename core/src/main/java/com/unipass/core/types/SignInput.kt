package com.unipass.core.types

data class SignInput (
    var address: String,
    var signType: String? = null,
    var message: String? = null
)