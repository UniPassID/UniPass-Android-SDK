package com.unipass.core.types

data class LoginOption (
    val connectType: ConnectType,
    val authorize: Boolean,
    val returnEmail: Boolean
)