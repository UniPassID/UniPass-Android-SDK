package com.unipass.core.types

interface UnipassCallBack <T>{
    fun success(output: T?)
    fun failure(exception: Exception)
}
