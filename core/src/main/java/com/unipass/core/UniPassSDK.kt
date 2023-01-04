package com.unipass.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.GsonBuilder
import com.unipass.core.types.*

class UniPassSDK(uniPassSDKOptions: UniPassSDKOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private val appSettings: Map<String, Any>
    private val context: Context

    private var userInfo: UserInfo? = null
    private var walletUrl: Uri
    private var redirectUrl: String? = ""
    private lateinit var currentAction: OutputType

    private lateinit var loginCallBack: UnipassCallBack<LoginOutput>
    private lateinit var logoutCallBack: UnipassCallBack<Void>
    private lateinit var signMsgCallBack: UnipassCallBack<SignOutput>
    private lateinit var sendTransactionCallBack: UnipassCallBack<SendTransactionOutput>

    init {
        appSettings = mutableMapOf(
            "chain" to "polygon",
            "theme" to "dark",
        )
        if(uniPassSDKOptions.appSettings != null){
            if(uniPassSDKOptions.appSettings!!.chain != null) appSettings["chain"] =
                uniPassSDKOptions.appSettings!!.chain!!.toString()
            if(uniPassSDKOptions.appSettings!!.theme != null) appSettings["theme"] =
                uniPassSDKOptions.appSettings!!.theme!!.toString()
            if(uniPassSDKOptions.appSettings!!.appName != null) appSettings["appName"] = uniPassSDKOptions.appSettings!!.appName!!
            if(uniPassSDKOptions.appSettings!!.appIcon != null) appSettings["appIcon"] = uniPassSDKOptions.appSettings!!.appIcon!!
        }

        if (uniPassSDKOptions.redirectUrl != null) redirectUrl =
            uniPassSDKOptions.redirectUrl.toString()

        if (uniPassSDKOptions.walletUrl == null || uniPassSDKOptions.walletUrl?.isEmpty() == true) {
            if (uniPassSDKOptions.env == Environment.MAINNET) {
                uniPassSDKOptions.walletUrl = "https://wallet.unipass.id"
            } else {
                uniPassSDKOptions.walletUrl = "https://testnet.wallet.unipass.id"
            }
        }
        walletUrl = Uri.parse(uniPassSDKOptions.walletUrl)

        this.context = uniPassSDKOptions.context

        // load session from local storage
        loadSession()
    }

    private fun loadSession() {
        val sessionInfo = SharedPreferenceUtil.getItem(context, SharedPreferenceUtil.SESSION_KEY)
        if (sessionInfo != null && sessionInfo.isNotEmpty()) {
            userInfo = gson.fromJson(sessionInfo, UserInfo::class.java)
        }
    }

    /**
     * open url with params in web browser
     */
    private fun request(
        path: String,
        outputType: OutputType,
        params: Map<String, Any>? = null
    ) {
        currentAction = outputType

        val paramMap = mapOf(
            "type" to outputType,
            "payload" to params,
            "appSetting" to appSettings,
            )
        val validParams = paramMap.filterValues { it != null }
        val hash = gson.toJson(validParams).toByteArray(Charsets.UTF_8).toBase64URLString()

        val url = Uri.Builder().scheme(walletUrl.scheme)
            .encodedAuthority(walletUrl.encodedAuthority)
            .encodedPath(walletUrl.encodedPath)
            .appendPath(path)
            .appendQueryParameter("redirectUrl", redirectUrl)
            .fragment(hash)
            .build()

        println("go to url: $url")

        val defaultBrowser = context.getDefaultBrowser()
        val customTabsBrowsers = context.getCustomTabsBrowsers()

        if (customTabsBrowsers.contains(defaultBrowser)) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(defaultBrowser)
            customTabs.launchUrl(context, url)
        } else if (customTabsBrowsers.isNotEmpty()) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(customTabsBrowsers[0])
            customTabs.launchUrl(context, url)
        } else {
            // Open in browser externally
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        }
    }

    private fun completeFutureWithException(exception: Exception) {
        when (currentAction) {
            OutputType.Login -> loginCallBack.failure(exception)
            OutputType.Logout -> logoutCallBack.failure(exception)
            OutputType.SignMessage -> signMsgCallBack.failure(exception)
            OutputType.SendTransaction -> sendTransactionCallBack.failure(exception)
        }
    }

    /**
     * receive redirect url params and set result for request
     */
    fun setResultUrl(uri: Uri?) {
        val hash = uri?.fragment ?: return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            completeFutureWithException(UnKnownException(error))
        }
        var  output = gson.fromJson(
            decodeBase64URLString(hash).toString(Charsets.UTF_8),
            BaseOutput::class.java
        )

        if (output.errorCode != null) {
            completeFutureWithException(
                UnKnownException(
                    output.errorMsg ?: "Something went wrong"
                )
            )
        }

        when (output.type) {
            OutputType.Login -> {
                val loginOutput = gson.fromJson(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    LoginOutput::class.java
                ) as LoginOutput
                SharedPreferenceUtil.saveItem(context, SharedPreferenceUtil.SESSION_KEY, gson.toJson(loginOutput.userInfo))
                this.loadSession()
                loginCallBack.success(loginOutput)
            }
            OutputType.Logout -> {
                logoutCallBack.success(null)
            }
            OutputType.SignMessage -> {
                val signMessageOutput = gson.fromJson<SignOutput>(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    SignOutput::class.java
                )
                signMsgCallBack.success(signMessageOutput)
            }
            OutputType.SendTransaction -> {
                val sendTransactionOutput = gson.fromJson<SendTransactionOutput>(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    SendTransactionOutput::class.java
                )
                sendTransactionCallBack.success(sendTransactionOutput)
            }
            else -> {}
        }
    }

    fun login(callBack: UnipassCallBack<LoginOutput>) {
        request("connect", OutputType.Login)
        loginCallBack = callBack
    }

    fun logout(callBack: UnipassCallBack<Void>) {
        // delete local storage
        SharedPreferenceUtil.deleteItem(context, SharedPreferenceUtil.SESSION_KEY)

        request("logout", OutputType.Logout)
        logoutCallBack = callBack
    }

    fun signMessage(signInput: SignInput, callBack: UnipassCallBack<SignOutput>) {
        val params = mutableMapOf<String, Any>(
            "from" to signInput.from,
            "type" to signInput.type.toString(),
            "msg" to signInput.msg,
        )
        request("sign-message", OutputType.SignMessage, params)
        signMsgCallBack = callBack
    }

    fun sendTransaction(sendTransactionInput: SendTransactionInput, callBack: UnipassCallBack<SendTransactionOutput>) {

        val params = mutableMapOf<String, Any>(
            "from" to sendTransactionInput.from,
            "to" to sendTransactionInput.to,
            "value" to sendTransactionInput.value.toString(),
            "data" to sendTransactionInput.data.toString()
        )
        request("send-transaction", OutputType.SendTransaction, params)

        sendTransactionCallBack = callBack
    }

    fun getUserInfo(): UserInfo {
        assertLogin()
        return userInfo!!
    }

    private fun assertLogin() {
        if (!isLogin()) throw AssertionError("User not login")
    }

    fun isLogin(): Boolean {
        return userInfo != null
    }

    fun getAddress(): String {
        assertLogin()
        return getUserInfo().address
    }
}