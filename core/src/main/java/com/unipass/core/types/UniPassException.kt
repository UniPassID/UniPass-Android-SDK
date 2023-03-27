package com.unipass.core.types

class UserCancelledException(): Exception("User cancelled.")

class UserInterruptedException(): Exception("User interrupted.")

class UserAddressInconsistentException(): Exception("Address inconsistent. User info may expired. Please log in again")

class UnKnownException(errorStr: String) : Exception(errorStr)