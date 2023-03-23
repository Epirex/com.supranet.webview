package com.supranet.webview

import androidx.appcompat.app.ActionBar
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var actionBar: ActionBar
    private lateinit var preferences: SharedPreferences

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "show_window_action_bar") {
            if (preferences.getBoolean("show_window_action_bar", false)) {
                showActionBar()
            } else {
                hideActionBar()
            }
        }
    }

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

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        actionBar = supportActionBar!!

        preferences.registerOnSharedPreferenceChangeListener(listener)

        if (preferences.getBoolean("show_toolbar", false)) {
            showActionBar()
        }

        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ ->
            if (preferences.getBoolean("show_toolbar", false)) {
                showActionBar()
            }
            true
        }
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
        super.onDestroy()
    }

    private fun showActionBar() {
        actionBar.show()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ hideActionBar() }, 3000)
    }

    private fun hideActionBar() {
        actionBar.hide()
    }

}

    //val myWebView: WebView = findViewById(R.id.webview)
    //myWebView.setWebViewClient(WebViewClient())
    //myWebView.settings.javaScriptEnabled = true
    //val dato = intent.getStringExtra("direccion")
    //myWebView.loadUrl("https://${dato}")