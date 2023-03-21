package com.supranet.webview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()

        // Configurar WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Cargar URL
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPrefs.getString("url_preference", "https://www.google.com")
        webView.loadUrl(urlPreference.toString())
    }

    //val myWebView: WebView = findViewById(R.id.webview)
    //myWebView.setWebViewClient(WebViewClient())
    //myWebView.settings.javaScriptEnabled = true
    //val dato = intent.getStringExtra("direccion")
    //myWebView.loadUrl("https://${dato}")
}
