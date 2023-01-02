package com.unipass.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.GsonBuilder
import com.unipass.core.types.*
import java.util.concurrent.CompletableFuture

class UniPassSDK(uniPassSDKOptions: UniPassSDKOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private val appSettings: Map<String, Any>
    private val context: Context

    private var userInfo: UserInfo? = null
    private var walletUrl: Uri
    private var redirectUrl: String? = ""

    private lateinit var currentAction: OutputType

    private var loginCompletableFuture: CompletableFuture<LoginOutput> = CompletableFuture()
    private var logoutCompletableFuture: CompletableFuture<LogoutOutput> = CompletableFuture()
    private var signMessageCompletableFuture: CompletableFuture<SignOutput> = CompletableFuture()
    private var sendTransactionCompletableFuture: CompletableFuture<SendTransactionOutput> =
        CompletableFuture()


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
                throw Exception("not supported now")
            } else {
//                uniPassSDKOptions.walletUrl = "https://testnet.wallet.unipass.id"
                uniPassSDKOptions.walletUrl = "https://t.wallet.unipass.vip"
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
            OutputType.Login -> loginCompletableFuture.completeExceptionally(exception)
            OutputType.Logout -> logoutCompletableFuture.completeExceptionally(exception)
            OutputType.SignMessage -> signMessageCompletableFuture.completeExceptionally(exception)
            OutputType.SendTransaction -> sendTransactionCompletableFuture.completeExceptionally(
                exception
            )
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
                loginCompletableFuture.complete(loginOutput)
            }
            OutputType.Logout -> {
                logoutCompletableFuture.complete(null)
            }
            OutputType.SignMessage -> {
                val signMessageOutput = gson.fromJson<SignOutput>(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    SignOutput::class.java
                )
                signMessageCompletableFuture.complete(signMessageOutput)
            }
            OutputType.SendTransaction -> {
                val sendTransactionOutput = gson.fromJson<SendTransactionOutput>(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    SendTransactionOutput::class.java
                )
                sendTransactionCompletableFuture.complete(sendTransactionOutput)
            }
            else -> {}
        }
    }

    fun login(): CompletableFuture<LoginOutput> {
        request("connect", OutputType.Login)

        loginCompletableFuture = CompletableFuture()
        return loginCompletableFuture
    }

    fun logout(): CompletableFuture<LogoutOutput> {
        // delete local storage
        SharedPreferenceUtil.deleteItem(context, SharedPreferenceUtil.SESSION_KEY)

        request("logout", OutputType.Logout)
        logoutCompletableFuture = CompletableFuture()
        return logoutCompletableFuture
    }

    fun signMessage(signInput: SignInput): CompletableFuture<SignOutput> {
        val params = mutableMapOf<String, Any>(
            "from" to signInput.from,
            "type" to signInput.type.toString(),
            "msg" to signInput.msg,
        )
        request("sign-message", OutputType.SignMessage, params)

        signMessageCompletableFuture = CompletableFuture()
        return signMessageCompletableFuture
    }

    fun signTypedData(signInput: SignInput): CompletableFuture<SignOutput> {
        return this.signMessage(signInput)
    }

    fun sendTransaction(sendTransactionInput: SendTransactionInput): CompletableFuture<SendTransactionOutput> {

        val params = mutableMapOf<String, Any>(
            "from" to sendTransactionInput.from,
            "to" to sendTransactionInput.to,
            "value" to sendTransactionInput.value.toString(),
            "data" to sendTransactionInput.data.toString()
        )
        request("send-transaction", OutputType.SendTransaction, params)

        sendTransactionCompletableFuture = CompletableFuture()
        return sendTransactionCompletableFuture
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