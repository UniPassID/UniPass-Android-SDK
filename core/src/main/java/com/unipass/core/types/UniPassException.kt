package com.unipass.core.types

class UserCancelledException(): Exception("User cancelled.")

class UserInterruptedException(): Exception("User interrupted.")

class UserAddressInconsistentException(): Exception("Address mismatch. Please reconnect to UniPass in the app for authentication.")

class UnKnownException(errorStr: String) : Exception(errorStr)