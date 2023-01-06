package com.unipass.core

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class UniPassActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setResult(RESULT_OK, intent);
        finish()
    }
}