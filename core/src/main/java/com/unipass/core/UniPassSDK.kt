package com.unipass.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.GsonBuilder
import com.unipass.core.types.*
import java.util.*
import java8.util.concurrent.CompletableFuture

class UniPassSDK(uniPassSDKOptions: UniPassSDKOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    private val initParams: Map<String, Any>
    private val context: Context

    private var userInfo: UserInfo? = null
    private var walletUrl: Uri

    private lateinit var currentAction: OutputType

    private var loginCompletableFuture: CompletableFuture<LoginOutput> = CompletableFuture()
    private var logoutCompletableFuture: CompletableFuture<LogoutOutput> = CompletableFuture()
    private var signMessageCompletableFuture: CompletableFuture<SignOutput> = CompletableFuture()
    private var sendTransactionCompletableFuture: CompletableFuture<SendTransactionOutput> =
        CompletableFuture()


    init {
        val initParams = mutableMapOf(
            "network" to uniPassSDKOptions.network.name.lowercase(Locale.ROOT)
        )
        if (uniPassSDKOptions.redirectUrl != null) initParams["redirectUrl"] =
            uniPassSDKOptions.redirectUrl.toString()

        if (uniPassSDKOptions.walletUrl == null || uniPassSDKOptions.walletUrl?.isEmpty() == true) {
            if (uniPassSDKOptions.network == Network.MAINNET) {
//                uniPassSDKOptions.walletUrl = "https://wallet.unipass.id"
                uniPassSDKOptions.walletUrl = "http://localhost:1901/"
            } else {
//                uniPassSDKOptions.walletUrl = "https://testnet.wallet.unipass.id"
                uniPassSDKOptions.walletUrl = "http://localhost:1901/"
            }
        }
        walletUrl = Uri.parse(uniPassSDKOptions.walletUrl)

        this.initParams = initParams
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
            "init" to initParams,
            "params" to params
        )
        val validParams = paramMap.filterValues { it != null }
        val hash = gson.toJson(validParams).toByteArray(Charsets.UTF_8).toBase64URLString()

        val url = Uri.Builder().scheme(walletUrl.scheme)
            .encodedAuthority(walletUrl.encodedAuthority)
            .encodedPath(walletUrl.encodedPath)
            .appendPath(path)
            .appendQueryParameter("redirectUrl", initParams["redirectUrl"].toString())
            .fragment(hash)
            .build()

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

        if (output.error?.isNotBlank() == true) {
            completeFutureWithException(
                UnKnownException(
                    output.error ?: "Something went wrong"
                )
            )
        }

        when (output.type) {
            OutputType.Login -> {
                output = gson.fromJson(
                    decodeBase64URLString(hash).toString(Charsets.UTF_8),
                    LoginOutput::class.java
                )
                SharedPreferenceUtil.saveItem(context, SharedPreferenceUtil.SESSION_KEY, gson.toJson(output))
                loginCompletableFuture.complete(output as LoginOutput)
            }
            OutputType.Logout -> {
                logoutCompletableFuture.complete(null)
            }
            OutputType.SignMessage -> {
                signMessageCompletableFuture.complete(output as SignOutput)
            }
            OutputType.SendTransaction -> {
                sendTransactionCompletableFuture.complete(output as SendTransactionOutput)
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
//        val params = mutableMapOf<string, any>(
//            "message" to signInput.message
//        )
//        val testp = mutableMapOf<string, any>()
        request("sign-message", OutputType.SignMessage)

        signMessageCompletableFuture = CompletableFuture()
        return signMessageCompletableFuture
    }

    fun signTypedData(signInput: SignInput): CompletableFuture<SignOutput> {
        request("sign-message", OutputType.SignMessage)

        signMessageCompletableFuture = CompletableFuture()
        return signMessageCompletableFuture
    }

    fun sendTransaction(sendTransactionInput: SendTransactionInput): CompletableFuture<SendTransactionOutput> {
        request("send-transaction", OutputType.SignMessage)

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