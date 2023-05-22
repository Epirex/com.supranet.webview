package com.supranet.webview

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var saveButton: FloatingActionButton
    private lateinit var passwordDialog: Dialog

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
                showPasswordDialog()
                true
            }
            R.id.action_refresh -> {
                true
            }
            R.id.action_home -> {
                webView.loadUrl("https://looka.com/logo-maker")
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
        webView.settings.setAllowFileAccessFromFileURLs(true)
        webView.settings.setAllowUniversalAccessFromFileURLs(true)
        webView.setKeepScreenOn(true)

        // Cargar URL
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPrefs.getString("url_preference", "https://looka.com/logo-maker")
        webView.loadUrl(urlPreference.toString())

        // Configurar un WebViewClient para inyectar el CSS personalizado en cada página web cargada
        webView.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView?, url: String?) {
                    injectCSS()
                }
            private fun injectCSS() {
                try {
                    val inputStream = assets.open("looka.css")
                    val buffer = ByteArray(inputStream.available())
                    inputStream.read(buffer)
                    inputStream.close()
                    val encoded = Base64.encodeToString(buffer , Base64.NO_WRAP)
                    webView.loadUrl(
                        "javascript:(function() {" +
                                "var parent = document.getElementsByTagName('head').item(0);" +
                                "var style = document.createElement('style');" +
                                "style.type = 'text/css';" +
                                // Tell the browser to BASE64-decode the string into your script !!!
                                "style.innerHTML = window.atob('" + encoded + "');" +
                                "parent.appendChild(style)" +
                                "})()"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

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

        // Configura un temporizador para actualizar
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val refreshInterval = sharedPrefs.getString("refresh", "1")!!.toInt()

        // Configurar un temporizador para refrescar la página
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                webView.reload()
                handler.postDelayed(this, refreshInterval * 60 * 1000L)
            }
        }, refreshInterval * 60 * 1000L)

        saveButton = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
                webView.evaluateJavascript(
                    "(function() { return encodeURIComponent(new XMLSerializer().serializeToString(document.querySelector('svg'))); })();"
                ) { svg ->
                    val decodedSvg = Uri.decode(svg)
                    val contentSvg = decodedSvg.substring(1, decodedSvg.length - 1)
                    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "logo_$date.svg"
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    val outputStream = FileOutputStream(file)
                    outputStream.write(contentSvg.toByteArray(Charsets.UTF_8))
                    outputStream.close()
                    Toast.makeText(applicationContext, "Archivo guardado: $fileName", Toast.LENGTH_SHORT).show()
                }
            }

    // Crear el cuadro flotante
    passwordDialog = Dialog(this)
    passwordDialog.setContentView(R.layout.password)
    passwordDialog.setCancelable(false)

    // Configurar el botón de enviar
    val sendButton = passwordDialog.findViewById<Button>(R.id.buttonDone)
    sendButton.setOnClickListener { checkPassword() }

        val button1 = passwordDialog.findViewById<Button>(R.id.button1)
        val button2 = passwordDialog.findViewById<Button>(R.id.button2)
        val button3 = passwordDialog.findViewById<Button>(R.id.button3)
        val button4 = passwordDialog.findViewById<Button>(R.id.button4)
        val button5 = passwordDialog.findViewById<Button>(R.id.button5)
        val button6 = passwordDialog.findViewById<Button>(R.id.button6)
        val button7 = passwordDialog.findViewById<Button>(R.id.button7)
        val button8 = passwordDialog.findViewById<Button>(R.id.button8)
        val button9 = passwordDialog.findViewById<Button>(R.id.button9)
        val button0 = passwordDialog.findViewById<Button>(R.id.button0)
        val buttonClear = passwordDialog.findViewById<Button>(R.id.buttonClear)
        val buttonExit = passwordDialog.findViewById<Button>(R.id.buttonExit)
        val passwordEditText = passwordDialog.findViewById<EditText>(R.id.passwordEditText)

        button1.setOnClickListener {
            passwordEditText.append("1")
        }
        button2.setOnClickListener {
            passwordEditText.append("2")
        }
        button3.setOnClickListener {
            passwordEditText.append("3")
        }
        button4.setOnClickListener {
            passwordEditText.append("4")
        }
        button5.setOnClickListener {
            passwordEditText.append("5")
        }
        button6.setOnClickListener {
            passwordEditText.append("6")
        }
        button7.setOnClickListener {
            passwordEditText.append("7")
        }
        button8.setOnClickListener {
            passwordEditText.append("8")
        }

        button9.setOnClickListener {
            passwordEditText.append("9")
        }

        button0.setOnClickListener {
            passwordEditText.append("0")
        }
        buttonClear.setOnClickListener {
            val text = passwordEditText.text
            if (text.isNotEmpty()) {
                passwordEditText.text.delete(text.length - 1, text.length)
            }
        }
        buttonExit.setOnClickListener {
            passwordDialog.dismiss()
        }
}

private fun showPasswordDialog() {
    // Mostrar el cuadro flotante
    passwordDialog.show()
}

private fun checkPassword() {
    val passwordEditText = passwordDialog.findViewById<EditText>(R.id.passwordEditText)
    val password = passwordEditText.text.toString()

    // Verificar la contraseña
    if (password == "1234") {
        passwordDialog.dismiss()
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    } else {
        Toast.makeText(this, "¡Contraseña incorrecta!", Toast.LENGTH_SHORT).show()
    }
}
}