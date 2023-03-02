package com.supranet.webview

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.setWebViewClient(WebViewClient())
        myWebView.settings.javaScriptEnabled = true
        myWebView.loadUrl("https://www.tiendac.ar/")
        val button1=findViewById<Button>(R.id.button1)
        button1.setOnClickListener {
            val intento1 = Intent(this, MainActivity2::class.java)
            startActivity(intento1)
        }

    }
}
