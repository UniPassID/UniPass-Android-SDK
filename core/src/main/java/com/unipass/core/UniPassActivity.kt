package com.unipass.core

import android.content.Intent
import android.os.Build
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

        val componentName =  intent?.resolveActivity(packageManager)
        val className = componentName?.className
        // Make sure that the class name used matches the expected one.
        if (className.equals("com.unipass.core.UniPassActivity")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Remove URI permissions granted in untrusted Intents.
                intent?.removeFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent?.removeFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            intent?.putExtra("resolverId", resolverId)
            setResult(RESULT_OK, intent);
        }
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