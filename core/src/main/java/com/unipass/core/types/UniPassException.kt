package com.unipass.core.types

class UserCancelledException(): Exception("User cancelled.")

class UserInterruptedException(): Exception("User interrupted.")

class UnKnownException(errorStr: String) : Exception(errorStr)