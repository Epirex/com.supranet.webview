package com.supranet.webview

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.preference.PreferenceManager

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
        val urlPreference = sharedPrefs.getString("url_preference", "http://www.supranet.ar")
        webView.loadUrl(urlPreference.toString())

        // check toolbar
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideToolbarPref = prefs.getBoolean("hide_toolbar", false)
        supportActionBar?.apply {
            if (hideToolbarPref) {
                hide()
            } else {
                show()
            }
        }
    }

    //val myWebView: WebView = findViewById(R.id.webview)
    //myWebView.setWebViewClient(WebViewClient())
    //myWebView.settings.javaScriptEnabled = true
    //val dato = intent.getStringExtra("direccion")
    //myWebView.loadUrl("https://${dato}")
}
