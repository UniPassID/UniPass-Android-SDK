package com.unipass.core

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class UniPassActivity : AppCompatActivity() {
    private var resolverId = 0;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resolverId = intent.getIntExtra("resolverId", 0)
        if (resolverId == 0) {
            // activity created by others should been finish directly
            finish()
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.putExtra("resolverId", resolverId)
        setResult(RESULT_OK, intent);
        finish()
    }

    override fun onRestart() {
        super.onRestart()
        Handler(Looper.getMainLooper()).postDelayed({
            // in case user close browser directly
            intent?.putExtra("resolverId", resolverId)
            finish()
        }, 300)
    }
}