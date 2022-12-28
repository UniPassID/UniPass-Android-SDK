package com.unipass.core.types

class UserCancelledException : Exception("User cancelled.")

class UnKnownException(errorStr: String) : Exception(errorStr)
