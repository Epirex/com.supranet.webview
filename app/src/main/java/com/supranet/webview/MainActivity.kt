package com.supranet.webview

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.util.Base64
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.*
import javax.mail.*
import javax.mail.Message
import javax.mail.internet.*


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var passwordDialog: Dialog
    private var lookaUrl: String? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val refreshItem = menu?.findItem(R.id.action_refresh)
        refreshItem?.setOnMenuItemClickListener {
            refreshWebView()
            true
        }
        val downloadItem = menu?.findItem(R.id.action_download)
        downloadItem?.setOnMenuItemClickListener {
            downloadSVG()
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
            R.id.action_download -> {
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

        // Eliminaremos el nombre de la App por el momento
        supportActionBar?.setDisplayShowTitleEnabled(false)
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
                lookaUrl = url
                injectCSS()
            }

            private fun injectCSS() {
                try {
                    val inputStream = assets.open("looka.css")
                    val buffer = ByteArray(inputStream.available())
                    inputStream.read(buffer)
                    inputStream.close()
                    val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
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
            webView.loadDataWithBaseURL(
                "file://${file.parent}/",
                file.readText(),
                "text/html",
                "UTF-8",
                null
            )
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

        // Configura un temporizador para actualizar
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
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

    private fun downloadSVG() {
        webView.evaluateJavascript(
            "(function() { return encodeURIComponent(new XMLSerializer().serializeToString(document.querySelector('svg'))); })();"
        ) { svg ->
            val decodedSvg = Uri.decode(svg)
            val contentSvg = decodedSvg.substring(1, decodedSvg.length - 1)
            val fileBytes = contentSvg.toByteArray(Charsets.UTF_8)

            // Calcular el peso del archivo SVG
            if (fileBytes.size < 1024) {
                Toast.makeText(
                    applicationContext,
                    "Por favor continua con los siguientes pasos para finalizar tu logo.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "logo_$date.svg"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                val outputStream = FileOutputStream(file)
                outputStream.write(fileBytes)
                outputStream.close()
                Toast.makeText(
                    applicationContext,
                    "Archivo guardado: $fileName",
                    Toast.LENGTH_SHORT
                ).show()

                // Ventana de correo
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("¡Que buen logo! ahora te lo enviaremos por correo")
                alertDialogBuilder.setMessage("Ingresa tu dirección de correo electrónico:")
                val input = EditText(this)
                alertDialogBuilder.setView(input)
                alertDialogBuilder.setPositiveButton("Enviar") { dialog, _ ->
                    val emailAddress = input.text.toString()
                    SendEmailTask(emailAddress, file).execute()
                    dialog.dismiss()
                }
                alertDialogBuilder.setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                alertDialogBuilder.show()
            }
        }
    }

    // Envio del correo electronico en segundo plano
    private inner class SendEmailTask(private val emailAddress: String, private val file: File) :
        AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                sendEmailWithAttachment(emailAddress, file)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun onPostExecute(success: Boolean) {
            if (success) {
                Toast.makeText(applicationContext, "Correo electrónico enviado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error al enviar el correo electrónico", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailWithAttachment(emailAddress: String, file: File) {
        val props = Properties()
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "465"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.ssl.enable"] = "true"

        // Remitente
        val username = "minceit.logo@gmail.com"
        val password = "rlazqoxkaqoplqnj"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(username))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress))
            message.subject = "Ministerio de Ciencia e Innovación Tecnologica: ¡Tu nuevo logo ya esta aqui!"

            // No me pregunten que es esto, lo saque de stackoverflow y me permitio enviar el archivo SVG
            val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
            CommandMap.setDefaultCommandMap(mc)

            val multipart = MimeMultipart()
            val messageBodyPart = MimeBodyPart()
            messageBodyPart.setText("¡Ya tienes disponible tu logo personalizado! Recuerda que tu logo se guardo en formato SVG, el mismo puede ser utilizado en cualquier editor (Recomendamos utilizar Canva) Si deseas visualizar tu logo ahora mismo puedes hacerlo desde tu navegador favorito a traves del siguiente URL: $lookaUrl")
            multipart.addBodyPart(messageBodyPart)

            val attachmentBodyPart = MimeBodyPart()
            val fileDataSource = FileDataSource(file)
            attachmentBodyPart.dataHandler = DataHandler(fileDataSource)
            attachmentBodyPart.fileName = MimeUtility.encodeText(file.name)
            multipart.addBodyPart(attachmentBodyPart)
            message.setContent(multipart)

            // Enviar el mensaje de correo electrónico
            Transport.send(message)

            runOnUiThread {
                Toast.makeText(applicationContext, "Correo electrónico enviado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: MessagingException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(applicationContext, "Error al enviar el correo electrónico", Toast.LENGTH_SHORT).show()
            }
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