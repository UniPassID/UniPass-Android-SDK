package com.unipass.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.unipass.core.UniPassSDK
import com.unipass.core.types.LoginOutput
import com.unipass.core.types.Network
import com.unipass.core.types.SignInput
import com.unipass.core.types.UniPassSDKOptions
import java8.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {

    private lateinit var unipassInstance: UniPassSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView((R.layout.activity_main))

        unipassInstance = UniPassSDK(
            UniPassSDKOptions(
                context = this!!,
                redirectUrl = Uri.parse("unipassapp://com.unipass.wallet/redirect"),
                network = Network.TESTNET
            )
        )
        unipassInstance.setResultUrl(intent.data)

        val loginBtn = findViewById<Button>(R.id.button_first)
        loginBtn.setOnClickListener { loginIn() }

        val logoutBtn = findViewById<Button>(R.id.button_second)
        logoutBtn.setOnClickListener { loginOut() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        unipassInstance.setResultUrl(intent?.data)
    }

    fun loginIn() {
        var loginCompletableFuture: CompletableFuture<LoginOutput> = unipassInstance.login()
        loginCompletableFuture.whenComplete{ output, error ->
            if (error == null) {
                Log.d("MainActivity_unipassAuth", "success")
                val userAddressTextV = findViewById<TextView>(R.id.userAddress)
                userAddressTextV.text = output?.userInfo?.address
            } else {
                Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
            }
        }
    }

    fun loginOut() {
        var logoutCompletableFuture = unipassInstance.logout()
        logoutCompletableFuture.whenComplete{ _, error ->
            if (error == null) {
                Log.d("MainActivity_unipassAuth", "success")
                val userAddressTextV = findViewById<TextView>(R.id.userAddress)
                userAddressTextV.text = ""
            } else {
                Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
            }
        }
    }

    fun signMsg() {
        if (unipassInstance.isLogin()) {
            val editText = findViewById<EditText>(R.id.edit_text)
            var signInput = SignInput(unipassInstance.getAddress(), "signMessage", editText.getText().toString())
            var signMsgCompletableFuture = unipassInstance.signMessage(signInput)
            signMsgCompletableFuture.whenComplete{ output, error ->
                if (error == null) {
                    Log.d("MainActivity_unipassAuth", "success")
                    editText.setText(output.signature)
                } else {
                    Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
                }
            }
        }
    }
}