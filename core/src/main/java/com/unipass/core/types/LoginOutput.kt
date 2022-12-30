package com.unipass.core.types

class LoginOutput: BaseOutput(OutputType.Login) {
    val userInfo: UserInfo? = null
}

class LogoutOutput: BaseOutput(OutputType.Logout)