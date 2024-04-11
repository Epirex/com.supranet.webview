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
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var passwordDialog: Dialog
    private lateinit var serverSocket: ServerSocket
    private var previousUrl: String? = null
    private val handler = Handler()
    private var scheduledExecutorService: ScheduledExecutorService? = null
    private var scheduledFuture: ScheduledFuture<*>? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val refreshItem = menu?.findItem(R.id.action_refresh)
        refreshItem?.setOnMenuItemClickListener {
            checkNetworkAndRefreshWebView()
            checkTurns()
            stopRefreshTimer()
            startRefreshTimer()
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
                checkNetworkAndRefreshWebView()
                checkTurns()
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

        // Establecer la dirección IP como título del Action Bar
        val ipAddress = getLocalIpAddress()
        supportActionBar?.title = "IP: $ipAddress"

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

        // Fondo temporal del webview, esta comentado para usarlo en casos especificos
        //webView.setBackgroundResource(R.drawable.fondo);
        //webView.setBackgroundColor(0x00000000);

        // Obtencion de datos de SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Verificar si hay turnos activos
        checkTurns()
        startRefreshTimer()

        // Cargar URL
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

        checkNetworkAndRefreshWebView()
        // Registrar el receptor de difusión para las acciones de cambio de conectividad
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        // Ocultar el ActionBar
        val hideToolbarPref = sharedPreferences.getBoolean("hide_toolbar", true)
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

    private fun startRefreshTimer() {
        val refreshIntervalPref = sharedPreferences.getString("refresh_interval", "30")?.toLong() ?: 30L
        if (refreshIntervalPref > 0) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            scheduledFuture = scheduledExecutorService?.scheduleAtFixedRate({
                runOnUiThread {
                    checkNetworkAndRefreshWebView()
                    checkTurns()
                }
            }, refreshIntervalPref, refreshIntervalPref, TimeUnit.MINUTES)
        }
    }

    private fun stopRefreshTimer() {
        scheduledFuture?.cancel(true)
        scheduledExecutorService?.shutdownNow()
    }

    private fun showPasswordDialog() {
        if (!isFinishing) {
            passwordDialog.show()
        }
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

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return null
    }

    // Al presionar el boton volver en el control remoto de la TVBOX
    // se recargara la pagina actual, esta funcion sera para casos de emergencia
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            checkNetworkAndRefreshWebView()
            checkTurns()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkNetworkAndRefreshWebView() {
        previousUrl = webView.url
        if (isNetworkAvailable()) {
            previousUrl?.let { webView.loadUrl(it) }
        } else {
            // añadire los elementos mas tarde, aun no lo termine
            webView.loadUrl("file:///android_asset/error.html")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isConnected = isNetworkAvailable()
            if (isConnected) {
                handler.postDelayed({
                    checkNetworkAndRefreshWebView()
                    checkTurns()
                }, 5000) // Retraso de 5 segundos (5000 milisegundos)
            } else {
                checkNetworkAndRefreshWebView()
            }
        }
    }

    private fun checkTurns(){
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        // Revisamos si hay algun turno activo
        val turnoMañanaActivo = sharedPreferences.getBoolean("turno_mañana", false)
        val turnoMediodiaActivo = sharedPreferences.getBoolean("turno_mediodia", false)
        val turnoTardeActivo = sharedPreferences.getBoolean("turno_tarde", false)
        val turnoNocheActivo = sharedPreferences.getBoolean("turno_noche", false)

        // Función auxiliar para determinar si la hora actual está dentro del rango especificado
        fun estaEnRango(horaInicio: Int, minutoInicio: Int, horaFin: Int, minutoFin: Int): Boolean {
            val inicio = horaInicio * 60 + minutoInicio
            val fin = horaFin * 60 + minutoFin
            val actual = currentHour * 60 + currentMinute
            return actual in inicio..fin
        }

        // Verificar y cargar URL para cada turno si está activo y en su horario
        if (turnoMañanaActivo) {
            val horario = sharedPreferences.getString("turno_mañana_time", "08:00 - 12:00")!!.split(" - ")
            val inicio = horario[0].split(":").map { it.toInt() }
            val fin = horario[1].split(":").map { it.toInt() }
            if (estaEnRango(inicio[0], inicio[1], fin[0], fin[1])) {
                sharedPreferences.getString("turno_mañana_url", "")?.let { webView.loadUrl(it) }
                return
            }
        }

        if (turnoMediodiaActivo) {
            val horario = sharedPreferences.getString("turno_mediodia_time", "12:00 - 16:00")!!.split(" - ")
            val inicio = horario[0].split(":").map { it.toInt() }
            val fin = horario[1].split(":").map { it.toInt() }
            if (estaEnRango(inicio[0], inicio[1], fin[0], fin[1])) {
                sharedPreferences.getString("turno_mediodia_url", "")?.let { webView.loadUrl(it) }
                return
            }
        }

        if (turnoTardeActivo) {
            val horario = sharedPreferences.getString("turno_tarde_time", "16:00 - 20:00")!!.split(" - ")
            val inicio = horario[0].split(":").map { it.toInt() }
            val fin = horario[1].split(":").map { it.toInt() }
            if (estaEnRango(inicio[0], inicio[1], fin[0], fin[1])) {
                sharedPreferences.getString("turno_tarde_url", "")?.let { webView.loadUrl(it) }
                return
            }
        }

        if (turnoNocheActivo) {
            val horario = sharedPreferences.getString("turno_noche_time", "20:00 - 08:00")!!.split(" - ")
            val inicio = horario[0].split(":").map { it.toInt() }
            val fin = horario[1].split(":").map { it.toInt() }
            // Para el turno de noche, que cruza la medianoche, se maneja un caso especial
            if (currentHour >= inicio[0] || currentHour < fin[0] || (currentHour == fin[0] && currentMinute < fin[1])) {
                sharedPreferences.getString("turno_noche_url", "")?.let { webView.loadUrl(it) }
                return
            }
        }

        // si no hay turnos activos, cargar la URL por defecto
        val urlPreference = sharedPreferences.getString("url_preference", "http://supranet.ar")
        webView.loadUrl(urlPreference.toString())
    }

    override fun onResume() {
        super.onResume()
        checkTurns()
        stopRefreshTimer()
        startRefreshTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRefreshTimer()
        if (passwordDialog.isShowing) {
            passwordDialog.dismiss()
        }
        unregisterReceiver(connectivityReceiver)
        if (::serverSocket.isInitialized) {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}