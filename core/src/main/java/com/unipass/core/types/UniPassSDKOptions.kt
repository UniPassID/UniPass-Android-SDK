package com.unipass.core.types

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

data class UniPassSDKOptions (
    var context: Context,
    var activity: AppCompatActivity,
    var env: Environment,
    @Transient var redirectUrl: Uri? = null,
    var walletUrl: String? = null,
    var appSettings: AppSettings? = null
)