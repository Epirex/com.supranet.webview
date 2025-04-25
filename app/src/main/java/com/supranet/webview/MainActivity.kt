package com.supranet.webview

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.content.res.Configuration
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
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    companion object {
        const val BASE_URL = "http://supranet.ar"
    }

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var passwordDialog: Dialog
    private lateinit var serverSocket: ServerSocket
    private var previousUrl: String? = null
    private val handler = Handler()
    private var scheduledExecutorService: ScheduledExecutorService? = null
    private var scheduledFuture: ScheduledFuture<*>? = null
    private var isReceiverRegistered = false
    private var keyPressSequence = mutableListOf<Int>()
    private val requiredSequence = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_CENTER
    )
    private var sequenceStartTime: Long = 0
    private val sequenceTimeout = 5000L

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val refreshItem = menu?.findItem(R.id.action_refresh)
        refreshItem?.setOnMenuItemClickListener {
            reloadWebview()
            supportActionBar?.hide()
            true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                showPasswordDialog()
                true
            }
            R.id.action_home -> {
                loadBaseUrl()
                supportActionBar?.hide()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        loadBaseUrl()
        Toast.makeText(applicationContext, "Actualizando contenido...", Toast.LENGTH_SHORT)
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
        passwordDialog = Dialog(this).apply {
            setContentView(R.layout.password)
            setCancelable(true)
            setCanceledOnTouchOutside(true)

            setOnCancelListener {
                findViewById<EditText>(R.id.passwordEditText).text.clear()
            }
        }

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
        webView.setOnLongClickListener { true }
        webView.isLongClickable = false

        // Fondo temporal del webview, esta comentado para usarlo en casos especificos
        //webView.setBackgroundResource(R.drawable.fondo);
        //webView.setBackgroundColor(0x00000000);

        // Obtencion de datos de SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Cargar URL
        loadBaseUrl()

        // Aplicar configuraciones de zoom después de que la página termine de cargarse
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webSettings.useWideViewPort = true
                webSettings.displayZoomControls = false
                webSettings.builtInZoomControls = false
                webSettings.setSupportZoom(false)
            }

            // Para Android API < 23
            @Deprecated("Deprecated in Android API level 23")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (!isNetworkAvailable()) {
                    loadErrorPage()
                }
            }

            // Para Android API >= 23
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame && !isNetworkAvailable()) {
                    loadErrorPage()
                }
            }

            private fun loadErrorPage() {
                val orientation = resources.configuration.orientation
                val errorPage = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    "file:///android_asset/errorvertical/index.html"
                } else {
                    "file:///android_asset/errorhorizontal/index.html"
                }
                webView.loadUrl(errorPage)
            }
        }

        // Registrar el receptor de difusión para las acciones de cambio de conectividad
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)
        isReceiverRegistered = true

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

    private fun loadBaseUrl() {
        if (isNetworkAvailable()) {
            fetchUrlFromPlugin { fetchedUrl ->
                val urlToLoad = fetchedUrl ?: sharedPreferences.getString("url_preference", BASE_URL)
                webView.loadUrl(urlToLoad!!)
            }
        } else {
            val orientation = resources.configuration.orientation
            val errorPage = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                "file:///android_asset/errorvertical/index.html"
            } else {
                "file:///android_asset/errorhorizontal/index.html"
            }
            webView.loadUrl(errorPage)
        }
        checkTurns()
        stopRefreshTimer()
        startRefreshTimer()
    }

    private fun reloadWebview(){
        webView.reload()
        stopRefreshTimer()
        startRefreshTimer()
    }

    private fun startRefreshTimer() {
        val refreshIntervalPref = sharedPreferences.getString("refresh_interval", "30")?.toLong() ?: 30L
        if (refreshIntervalPref > 0) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            scheduledFuture = scheduledExecutorService?.scheduleAtFixedRate({
                runOnUiThread {
                    checkNetworkAndRefreshWebView()
                    if (checkTurnsForRefresh()) {
                        checkTurns()
                    }
                }
            }, refreshIntervalPref, refreshIntervalPref, TimeUnit.MINUTES)
        }
    }

    private fun stopRefreshTimer() {
        scheduledFuture?.cancel(true)
        scheduledExecutorService?.shutdownNow()
    }

    private fun checkTurnsForRefresh(): Boolean {
        val turnoMañanaActivo = sharedPreferences.getBoolean("turno_mañana", false)
        val turnoMediodiaActivo = sharedPreferences.getBoolean("turno_mediodia", false)
        val turnoTardeActivo = sharedPreferences.getBoolean("turno_tarde", false)
        val turnoNocheActivo = sharedPreferences.getBoolean("turno_noche", false)
        return turnoMañanaActivo || turnoMediodiaActivo || turnoTardeActivo || turnoNocheActivo
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(12345)
                while (!serverSocket.isClosed) {
                    val clientSocket = serverSocket.accept()
                    clientSocket.use { socket ->
                        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val receivedUrl = input.readLine()

                        withContext(Dispatchers.Main) {
                            webView.loadUrl(receivedUrl)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // No usar "Atrás"
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                return super.dispatchKeyEvent(event)
            }
            val currentTime = System.currentTimeMillis()

            if (keyPressSequence.isEmpty() || currentTime - sequenceStartTime > sequenceTimeout) {
                keyPressSequence.clear()
                sequenceStartTime = currentTime
            }

            keyPressSequence.add(event.keyCode)

            if (keyPressSequence.take(requiredSequence.size) == requiredSequence) {
                keyPressSequence.clear()
                showPasswordDialog()
            } else if (!requiredSequence.take(keyPressSequence.size).equals(keyPressSequence)) {
                keyPressSequence.clear()
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun checkNetworkAndRefreshWebView() {
        previousUrl = webView.url
        if (isNetworkAvailable()) {
            previousUrl?.let { webView.loadUrl(it) }
        } else {
            // Determinar orientación actual
            val orientation = resources.configuration.orientation
            val errorPage = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                "file:///android_asset/errorvertical/index.html"
            } else {
                "file:///android_asset/errorhorizontal/index.html"
            }
            webView.loadUrl(errorPage)
        }
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isConnected = isNetworkAvailable()
            if (isConnected) {
                handler.postDelayed({
                    checkNetworkAndRefreshWebView()
                    checkTurns()
                }, 5000)
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
        val urlPreference = sharedPreferences.getString("url_preference", BASE_URL)
        webView.loadUrl(urlPreference.toString())
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun fetchUrlFromPlugin(onResult: (String?) -> Unit) {
        val androidId = getAndroidId()

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val baseUrl = sharedPrefs.getString("wordpress_base_url", null)

        if (baseUrl.isNullOrEmpty()) {
            onResult(null)
            return
        }

        val pluginUrl = "$baseUrl/wp-json/tvboxs/v1/webview-url?device_id=$androidId"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(pluginUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val urlFromApi = json.optString("url", null)

                    withContext(Dispatchers.Main) {
                        if (urlFromApi != null) {
                            sharedPrefs.edit().putString("url_preference", urlFromApi).apply()
                        }
                        onResult(urlFromApi)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBaseUrl()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRefreshTimer()
        if (passwordDialog.isShowing) {
            passwordDialog.dismiss()
        }
        if (isReceiverRegistered) {
            unregisterReceiver(connectivityReceiver)
            isReceiverRegistered = false
        }
        if (::serverSocket.isInitialized) {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}