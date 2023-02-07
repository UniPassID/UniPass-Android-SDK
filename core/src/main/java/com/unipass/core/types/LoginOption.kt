package com.unipass.core.types

data class LoginOption (
    val connectType: ConnectType? = ConnectType.BOTH,
    val authorize: Boolean? = false,
    val returnEmail: Boolean? = false
)