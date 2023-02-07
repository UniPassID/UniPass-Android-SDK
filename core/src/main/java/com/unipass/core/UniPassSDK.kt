package com.unipass.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.GsonBuilder
import com.unipass.core.types.*

class UniPassSDK(uniPassSDKOptions: UniPassSDKOptions) {

    private val gson = GsonBuilder().disableHtmlEscaping().create()
    private val appSettings: MutableMap<String, Any> = mutableMapOf(
        "chain" to "polygon",
        "theme" to "dark",
    )
    private val context: Context
    private val activity: ComponentActivity
    private var resultLauncher: ActivityResultLauncher<Intent>
    private var userInfo: UserInfo? = null
    private var walletUrl: Uri
    private var redirectUrl: String? = ""
    private var supportLoginType: ConnectType? = ConnectType.BOTH

    private lateinit var currentAction: OutputType
    private lateinit var loginCallBack: UnipassCallBack<LoginOutput>
    private lateinit var logoutCallBack: UnipassCallBack<Void>
    private lateinit var signMsgCallBack: UnipassCallBack<SignOutput>
    private lateinit var sendTransactionCallBack: UnipassCallBack<SendTransactionOutput>

    init {
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
        this.activity = uniPassSDKOptions.activity

        resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            setResultUrl(it.data?.data)
        }

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
        var hashByteArray = gson.toJson(validParams).toByteArray(Charsets.UTF_8)
        var hash = hashByteArray.toBase64URLString()

        val uri = Uri.Builder().scheme(walletUrl.scheme)
            .encodedAuthority(walletUrl.encodedAuthority)
            .encodedPath(walletUrl.encodedPath)
            .appendPath(path)

        if (outputType == OutputType.Login) {
            uri.appendQueryParameter("connectType", supportLoginType.toString().lowercase())
        }

        uri.appendQueryParameter("redirectUrl", redirectUrl)
            .fragment(hash)

        val url = uri.build()

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
    private fun setResultUrl(uri: Uri?) {
        if (uri == null) {
            completeFutureWithException(UserInterruptedException())
            return
        }
        val hash = uri?.fragment ?: return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            completeFutureWithException(UnKnownException(error))
            return
        }
        var  output = gson.fromJson(
            decodeBase64URLString(hash).toString(Charsets.UTF_8),
            BaseOutput::class.java
        )

        if (output.errorCode != null) {
            when (output.errorCode) {
                401 -> {
                    completeFutureWithException(UserCancelledException())
                }
                else -> {
                    completeFutureWithException(
                        UnKnownException(
                            output.errorMsg ?: "Something went wrong"
                        )
                    )
                }
            }
            return
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

    fun login(callBack: UnipassCallBack<LoginOutput>, loginOption: LoginOption = LoginOption(ConnectType.BOTH, false, false)) {
        supportLoginType = loginOption.connectType
        loginCallBack = callBack
        resultLauncher.launch(Intent(context, UniPassActivity::class.java))
        val params = mutableMapOf<String, Any>(
            "authorize" to loginOption.authorize,
            "returnEmail" to loginOption.returnEmail,
        )
        request("connect", OutputType.Login, params)
    }

    fun login(connectType: ConnectType, callBack: UnipassCallBack<LoginOutput>) {
        supportLoginType = connectType
        login(callBack)
    }

    fun logout(callBack: UnipassCallBack<Void>, deep: Boolean = true) {
        // delete local storage
        SharedPreferenceUtil.deleteItem(context, SharedPreferenceUtil.SESSION_KEY)
        logoutCallBack = callBack
        if (deep) {
            resultLauncher.launch(Intent(context, UniPassActivity::class.java))
            request("logout", OutputType.Logout)
        } else {
            logoutCallBack.success(null)
        }
    }

    fun signMessage(signInput: SignInput, callBack: UnipassCallBack<SignOutput>, redirectUrl: Uri? = null) {
        signMsgCallBack = callBack
        resultLauncher.launch(Intent(context, UniPassActivity::class.java))
        val params = mutableMapOf<String, Any>(
            "from" to signInput.from,
            "type" to signInput.type.toString(),
            "msg" to signInput.msg,
        )
        request("sign-message", OutputType.SignMessage, params)
    }

    fun sendTransaction(sendTransactionInput: SendTransactionInput, callBack: UnipassCallBack<SendTransactionOutput>) {
        sendTransactionCallBack = callBack
        resultLauncher.launch(Intent(context, UniPassActivity::class.java))
        val params = mutableMapOf<String, Any>(
            "from" to sendTransactionInput.from,
            "to" to sendTransactionInput.to,
            "value" to sendTransactionInput.value.toString(),
            "data" to sendTransactionInput.data.toString()
        )
        request("send-transaction", OutputType.SendTransaction, params)
    }

    fun setChainType(chain: ChainType) {
        appSettings["chain"] = chain
    }

    fun setTheme(theme: UniPassTheme) {
        appSettings["theme"] = theme
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