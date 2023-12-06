package com.supranet.webview

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var passwordDialog: Dialog
    private lateinit var serverSocket: ServerSocket

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val refreshItem = menu?.findItem(R.id.action_refresh)
        refreshItem?.setOnMenuItemClickListener {
            refreshWebView()
            supportActionBar?.hide()
            true
        }
        return true
    }

    private fun refreshWebView() {
        webView.reload()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                showPasswordDialog()
                true
            }
            R.id.action_home -> {
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                val urlPreference =
                    sharedPrefs.getString("url_preference", "http://supranet.ar")
                webView.loadUrl(urlPreference.toString())
                supportActionBar?.hide()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        Toast.makeText(applicationContext, "Ya estas en la pantalla principal", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Webview)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Abrir conexion con la App control remoto
        initServerSocket()

        // Obtener el ANDROID_ID del dispositivo
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Mantener pantalla siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // URL del servidor
        val url = "http://supranet.ar/webview/devices.txt"

        // Crear una instancia de la clase AsyncTask para realizar la solicitud HTTP en segundo plano
        val networkTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Unit, Unit, Boolean>() {

            override fun doInBackground(vararg params: Unit?): Boolean {
                val serverUrl = URL(url)
                val connection = serverUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.doInput = true
                val stream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = StringBuffer()

                var inputLine: String?
                while (reader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }

                // Buscar el ID único del dispositivo en la respuesta
                return response.toString().contains(androidId)
            }

            override fun onPostExecute(result: Boolean) {
                if (result) {
                } else {
                    // Si el ID no se encuentra en la respuesta, mostrar un mensaje de error
                    val intent = Intent(applicationContext, ScreenSupport::class.java)
                    startActivity(intent)
                    Toast.makeText(this@MainActivity, "Error en la licencia", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Ejecutar la tarea asincrónica (Desactivado por ahora)
        //networkTask.execute()

        // Crear el cuadro flotante
        passwordDialog = Dialog(this)
        passwordDialog.setContentView(R.layout.password)
        passwordDialog.setCancelable(false)

        // Botones, muchos botones
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
        val sendButton = passwordDialog.findViewById<Button>(R.id.buttonDone)
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
            passwordEditText.text.clear()
            passwordDialog.dismiss()
        }
        sendButton.setOnClickListener {
            checkPassword()
            passwordEditText.text.clear()
        }

        // Configurar WebView
        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.domStorageEnabled = true

        // Cargar URL
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val urlPreference = sharedPreferences.getString("url_preference", "http://supranet.ar")
        webView.loadUrl(urlPreference.toString())

        // Aplicar configuraciones de zoom después de que la página termine de cargarse
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                webSettings.useWideViewPort = true
                webSettings.displayZoomControls = false
                webSettings.builtInZoomControls = false
                webSettings.setSupportZoom(false)
            }
        }

        // Ocultar el ActionBar
        val hideToolbarPref = sharedPreferences.getBoolean("hide_toolbar", false)
        supportActionBar?.apply {
            if (hideToolbarPref) {
                hide()
            } else {
                show()
            }
        }

        // Agregar el listener onTouch para mostrar u ocultar la ActionBar
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
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

        // Aplica un custom CSS
        val customCss = sharedPreferences.getBoolean("custom_css", false)
        if (customCss) {
            val fileName = "supranet.css"
            val url = "http://supranet.ar/css/supranet.css"

            // Create a DownloadManager request for the CSS file
            val downloadRequest = DownloadManager.Request(Uri.parse(url))
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                .setTitle(fileName)
                .setDescription("Downloading $fileName")
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(downloadRequest)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        val cssFile = File(downloadDir, fileName)
                        webView.evaluateJavascript(
                            "(function() { " +
                                    "   var css = document.createElement('link');" +
                                    "   css.setAttribute('rel', 'stylesheet');" +
                                    "   css.setAttribute('type', 'text/css');" +
                                    "   css.setAttribute('href', 'file://${cssFile.absolutePath}');" +
                                    "   document.head.appendChild(css);" +
                                    "})();"
                        ) { result ->
                        }
                    }
                }
            }
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Configura un temporizador para actualizar
        val refreshIntervalPref = sharedPreferences.getString("refresh_interval", "0")
        val refreshInterval = refreshIntervalPref!!.toInt()

        val handler = Handler(Looper.getMainLooper())
        if (refreshInterval > 0) {
            handler.postDelayed(object : Runnable {
                override fun run() {
                    webView.reload()
                    handler.postDelayed(this, refreshInterval * 60 * 1000L)
                }
            }, refreshInterval * 60 * 1000L)
        }

        // Cargar URL local
        val loadLocalHtml = sharedPreferences.getBoolean("enable_local", false)
        if (loadLocalHtml) {
            val file = File(getExternalFilesDir(null), "index.html")
            if (!file.exists()) {
                Toast.makeText(this, "El archivo HTML no existe!", Toast.LENGTH_SHORT).show()
                return
            }
            webView.loadDataWithBaseURL(
                "file://${file.parent}/",
                file.readText(),
                "text/html",
                "UTF-8",
                null
            )
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
        if (password == "3434") {
            passwordDialog.dismiss()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, "¡Contraseña incorrecta!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initServerSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(12345)
                while (true) {
                    val clientSocket = serverSocket.accept()
                    val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val receivedUrl = input.readLine()

                    runOnUiThread {
                        webView.loadUrl(receivedUrl)
                    }

                    clientSocket.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}