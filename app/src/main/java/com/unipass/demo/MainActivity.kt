package com.unipass.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.unipass.core.UniPassSDK
import com.unipass.core.types.*
import org.web3j.utils.Convert
import java.util.concurrent.CompletableFuture
import org.web3j.utils.Convert.toWei

class MainActivity : AppCompatActivity() {

    private lateinit var unipassInstance: UniPassSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView((R.layout.activity_main))

        unipassInstance = UniPassSDK(
            UniPassSDKOptions(
                context = this!!,
                redirectUrl = Uri.parse("unipassapp://com.unipass.wallet/redirect"),
                env = Environment.TESTNET
            )
        )
        unipassInstance.setResultUrl(intent.data)

        if(unipassInstance.isLogin()){
            val userAddressTextV = findViewById<TextView>(R.id.userAddress)
            userAddressTextV.text = unipassInstance.getAddress()
        }

        val loginBtn = findViewById<Button>(R.id.button_first)
        loginBtn.setOnClickListener { loginIn() }

        val logoutBtn = findViewById<Button>(R.id.button_second)
        logoutBtn.setOnClickListener { loginOut() }

        val signMsgBtn = findViewById<Button>(R.id.button_sign_message)
        signMsgBtn.setOnClickListener { signMsg() }

        val sendTransactionBtn = findViewById<Button>(R.id.button_send_transaction)
        sendTransactionBtn.setOnClickListener { sendTransaction() }
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
        Log.d("MainActivity_unipassAuth", "sign message button clicked")
        if (unipassInstance.isLogin()) {
            val editText = findViewById<EditText>(R.id.message_to_sign)
            var signInput = SignInput(unipassInstance.getAddress(), SignType.PersonalSign, editText.getText().toString())
            var signMsgCompletableFuture = unipassInstance.signMessage(signInput)
            signMsgCompletableFuture.whenComplete{ output, error ->
                if (error == null) {
                    Log.d("MainActivity_unipassAuth", "success")
                    val signatureText = findViewById<EditText>(R.id.personal_sign_signature)
                    signatureText.setText(output.signature)
                } else {
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
                }
            }
        }
    }

    fun sendTransaction() {
        Log.d("MainActivity_unipassAuth", "send transaction button clicked")
        if (unipassInstance.isLogin()) {

            var transactionInput = SendTransactionInput(unipassInstance.getAddress(),
                "0x7b5Bd7c9E3A0D0Ef50A9b3aCF5d1AcD58C3590d1",
                "0x" + toWei("0.001", Convert.Unit.ETHER).toBigIntegerExact().toString(16)
            )

            var signTxCompletableFuture = unipassInstance.sendTransaction(transactionInput)
            signTxCompletableFuture.whenComplete{ output, error ->
                if (error == null) {
                    Log.d("MainActivity_unipassAuth", "success")
                    val transactionHashText = findViewById<TextView>(R.id.transaction_hash)
                    transactionHashText.text = output.transactionHash
                } else {
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
                }
            }
        }
    }
}