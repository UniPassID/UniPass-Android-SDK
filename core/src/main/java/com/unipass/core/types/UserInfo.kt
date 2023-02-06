package com.unipass.core.types


data class UserInfo (
    var email: String?,
    var address: String,
    var newborn: Boolean = false,
    val message: String?,
    val signature: String?
)