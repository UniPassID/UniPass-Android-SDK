package com.unipass.demo

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
import org.web3j.utils.Convert.toWei

class MainActivity : AppCompatActivity() {

    private lateinit var unipassInstance: UniPassSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView((R.layout.activity_main))

        unipassInstance = UniPassSDK(
            UniPassSDKOptions(
                context = this,
                activity = this,
                redirectUrl = Uri.parse("unipassapptest://com.unipass.wallet/redirect"),
                env = Environment.TESTNET
            )
        )

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

    fun loginIn() {
        val self = this
        Log.d("MainActivity_auth", "login in button clicked")
        unipassInstance.login(object : UnipassCallBack<LoginOutput> {
            override fun success(output: LoginOutput?) {
                Log.d("MainActivity_auth", "success")
                val userAddressTextV = findViewById<TextView>(R.id.userAddress)
                userAddressTextV.text = output?.userInfo?.address
            }

            override fun failure(error: Exception) {
                Toast.makeText(self, error.message, Toast.LENGTH_SHORT).show()
                Log.d("MainActivity_auth", error.message ?: "Something went wrong")
            }
        })
    }

    fun loginOut() {
        val self = this
        Log.d("MainActivity_unipassAuth", "login out button clicked")
        unipassInstance.logout(object : UnipassCallBack<Void> {
            override fun success(output: Void?) {
                Log.d("MainActivity_unipassAuth", "success")
                val userAddressTextV = findViewById<TextView>(R.id.userAddress)
                userAddressTextV.text = ""
            }
            override fun failure(error: Exception) {
                Toast.makeText(self, error.message, Toast.LENGTH_SHORT).show()
                Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
            }
        })
    }

    fun signMsg() {
        val self = this
        Log.d("MainActivity_unipassAuth", "sign message button clicked")
        if (unipassInstance.isLogin()) {
            val editText = findViewById<EditText>(R.id.message_to_sign)
            var signInput = SignInput(unipassInstance.getAddress(), SignType.PersonalSign, editText.getText().toString())
            unipassInstance.signMessage(signInput, object : UnipassCallBack<SignOutput> {
                override fun success(output: SignOutput?) {
                    Log.d("MainActivity_unipassAuth", "success")
                    val signatureText = findViewById<EditText>(R.id.personal_sign_signature)
                    signatureText.setText(output?.signature)
                }
                override fun failure(error: Exception) {
                    Toast.makeText(self, error.message, Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
                }
            })
        }
    }

    fun sendTransaction() {
        Log.d("MainActivity_unipassAuth", "send transaction button clicked")
        var self = this
        if (unipassInstance.isLogin()) {
            var transactionInput = SendTransactionInput(unipassInstance.getAddress(),
                "0x7b5Bd7c9E3A0D0Ef50A9b3aCF5d1AcD58C3590d1",
                "0x" + toWei("0.001", Convert.Unit.ETHER).toBigIntegerExact().toString(16)
            )
            unipassInstance.sendTransaction(transactionInput, object : UnipassCallBack<SendTransactionOutput> {
                override fun success(output: SendTransactionOutput?) {
                    Log.d("MainActivity_unipassAuth", "success")
                    val transactionHashText = findViewById<TextView>(R.id.transaction_hash)
                    transactionHashText.text = output?.transactionHash
                }
                override fun failure(error: Exception) {
                    Toast.makeText(self, error.message, Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity_unipassAuth", error.message ?: "Something went wrong")
                }
            })
        }
    }
}