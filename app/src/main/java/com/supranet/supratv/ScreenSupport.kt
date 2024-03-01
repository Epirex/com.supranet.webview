package com.supranet.supratv

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class ScreenSupport : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_support)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}
