package com.unipass.core

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class UniPassActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.extras != null) {
            // activity created by others should been finish directly
            finish()
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setResult(RESULT_OK, intent);
        finish()
    }

    override fun onRestart() {
        super.onRestart()
        Handler(Looper.getMainLooper()).postDelayed({
            // in case user close browser directly
            finish()
        }, 300)
    }
}