package com.supranet.webview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class Streaming : AppCompatActivity() {

    private val port = 1234 // Puerto para la conexión

    private lateinit var serverSocket: ServerSocket
    private lateinit var socket: Socket
    private var inputStream: DataInputStream? = null
    private var isSocketInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.streaming)

        iniciarConexionSocket()

        // Forzar cierre de la aplicacion para liberar memoria
        val timerReset = Timer()
        val calendar = Calendar.getInstance()

        // Obtener la hora actual
        val horaActual = calendar.get(Calendar.HOUR_OF_DAY)
        val minutosActuales = calendar.get(Calendar.MINUTE)

        calendar.set(Calendar.HOUR_OF_DAY, 17)
        calendar.set(Calendar.MINUTE, 1)

        if (horaActual >= 17 && minutosActuales >= 1) {
            // Añadir un día a la fecha para programar el temporizador para mañana
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val fechaProgramada = calendar.time
        timerReset.schedule(object : TimerTask() {
            override fun run() {
                // Cerrar la aplicación
                System.exit(0)
            }
        }, fechaProgramada)

        val videoView = findViewById<VideoView>(R.id.video_view)
        val savedUrl = readSavedUrl()
        if (savedUrl != null) {
            // Si se ha guardado una URL anteriormente, utilizarla
            actualizarUrlTransmision(savedUrl)
        } else {
            val videoUri =
                Uri.parse("https://live-01-02-eltrece.vodgc.net/eltrecetv/index.m3u8?PlaylistM3UCL")
            videoView.setVideoURI(videoUri)
            videoView.start()
            iniciarConexionSocket()
        }
            // Configurar progressbar
            val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
            progressBar.visibility = View.VISIBLE // Hacer visible la vista de ProgressBar

            videoView.setOnPreparedListener {
                progressBar.visibility =
                    View.GONE // Ocultar la vista de ProgressBar cuando el video esté preparado
                videoView.start()
                val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationRepeat(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        progressBar.visibility = View.GONE
                    }
                })
                progressBar.startAnimation(fadeOutAnimation)
            }

        // El temporizador come memorias le digo yo.
        val timer = Timer()
        val task = object : TimerTask() {
            var isStreamingActivityShowing = false
            override fun run() {
                runOnUiThread {
                    if (isStreamingActivityShowing) {
                        val intent = Intent(this@Streaming, MainActivity::class.java)
                        startActivity(intent)
                        timer.cancel()
                        cancel()
                        finish()
                    } else {
                        isStreamingActivityShowing = true
                    }
                }
            }
        }
        timer.schedule(task, 0, 1 * 60 * 1000)
    }

    private fun actualizarUrlTransmision(url: String) {
        val videoView = findViewById<VideoView>(R.id.video_view)
        videoView.setVideoURI(Uri.parse(url))
        videoView.start()
        Log.d("StreamingApp", "URL de transmisión actualizada: $url")
        saveUrl(url)
    }

    private fun iniciarConexionSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(port)
                socket = serverSocket.accept()
                inputStream = DataInputStream(socket.getInputStream())

                isSocketInitialized = true

                Log.d("StreamingApp", "Conexión de socket establecida")

                // Recibir la URL enviada desde la aplicación de control remoto
                while (isSocketInitialized) {
                    val url = inputStream!!.readUTF()
                    runOnUiThread {
                        actualizarUrlTransmision(url)
                    }
                    Thread.sleep(100) // Pausa de 100 milisegundos
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("StreamingApp", "Error al establecer la conexión de socket: ${e.message}")
            }
        }.start()
    }

    private fun saveUrl(url: String) {
        try {
            val file = File(filesDir, "saved_url.txt")
            val outputStream = FileOutputStream(file)
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            writer.write(url)
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readSavedUrl(): String? {
        try {
            val file = File(filesDir, "saved_url.txt")
            if (file.exists()) {
                val inputStream = FileInputStream(file)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val savedUrl = reader.readLine()
                reader.close()
                return savedUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isSocketInitialized = false // Detener el ciclo de recepción de URL

        inputStream?.let {
            try {
                // Cerrar el flujo de entrada y el socket del servidor
                it.close()
                socket.close()
                serverSocket.close()
                Log.d("StreamingApp", "Conexion finalizada con exito")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("StreamingApp", "Error al cerrar la conexión de socket: ${e.message}")
            }
        }
    }
}


