package com.supranet.webview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.setWebViewClient(WebViewClient())
        myWebView.settings.javaScriptEnabled = true
        myWebView.loadUrl("https://www.tiendac.ar/")

    }
}