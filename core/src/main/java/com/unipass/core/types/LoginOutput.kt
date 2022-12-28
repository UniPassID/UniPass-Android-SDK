package com.unipass.core.types

class LoginOutput: BaseOutput(OutputType.Login) {
    val userInfo: UserInfo? = null
    val newborn: Boolean? = false
}

class LogoutOutput: BaseOutput(OutputType.Logout)