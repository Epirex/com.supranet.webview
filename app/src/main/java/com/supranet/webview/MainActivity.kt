package com.supranet.webview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val refreshItem = menu?.findItem(R.id.action_refresh)
        refreshItem?.setOnMenuItemClickListener {
            refreshWebView()
            true
        }
        return true
    }

    private fun refreshWebView() {
        val webView = findViewById<WebView>(R.id.webview)
        webView.reload()
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


    override fun onBackPressed() {

        // To execute back press
        // super.onBackPressed()

        // To do something else
        Toast.makeText(applicationContext, "Ya estas en la pantalla principal", Toast.LENGTH_SHORT).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Webview)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Configurar WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true

        // Cargar URL
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPrefs.getString("url_preference", "http://www.supranet.ar")
        webView.loadUrl(urlPreference.toString())

        // Cargar URL local
        val loadLocalHtml = sharedPreferences.getBoolean("enable_local", false)
        if (loadLocalHtml) {
            val file = File(getExternalFilesDir(null), "index.html")
            if (!file.exists()) {
                Toast.makeText(this, "El archivo HTML no existe!", Toast.LENGTH_SHORT).show()
                return
            }
            webView.loadDataWithBaseURL("file://${file.parent}/", file.readText(), "text/html", "UTF-8", null)
        }

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

        // Agregar el listener onTouch para mostrar u ocultar la ActionBar
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                // Obtiene las dimensiones de la pantalla
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels

                // Define la región del borde superior de la pantalla
                val topRegion = 50 // En píxeles

                // Verifica si la posición inicial del evento se encuentra dentro de la región del borde superior de la pantalla
                if (e1?.y ?: 0f < topRegion && e2?.y ?: 0f >= topRegion) {
                    // Muestra el ActionBar
                    supportActionBar?.show()

                    // Oculta el ActionBar después de 3 segundos
                    Handler().postDelayed({
                        supportActionBar?.hide()
                    }, 2500)
                }

                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })

        // Asigna el GestureDetector al WebView
        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

    }
    }