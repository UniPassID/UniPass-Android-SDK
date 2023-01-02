package com.unipass.core.types

import android.content.Context
import android.net.Uri

data class UniPassSDKOptions (
    var context: Context,
    var env: Environment,
    @Transient var redirectUrl: Uri? = null,
    var walletUrl: String? = null,
    var appSettings: AppSettings? = null
    )