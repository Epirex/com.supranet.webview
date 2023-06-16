package com.supranet.webview

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

class Streaming : AppCompatActivity() {

    private lateinit var urls: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.streaming)

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
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE // Hacer visible la vista de ProgressBar

        val m3uFileUrl = "http://poster.com.ar/tvo.m3u" // URL del archivo m3u

        // Descargar y leer el archivo m3u en segundo plano
        object : AsyncTask<String, Void, String>() {
            override fun doInBackground(vararg urls: String): String {
                val url = urls[0]
                val stringBuilder = StringBuilder()

                try {
                    val urlConnection = URL(url).openConnection()
                    val bufferedReader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                        stringBuilder.append("\n")
                    }
                    bufferedReader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return stringBuilder.toString()
            }

            override fun onPostExecute(result: String) {
                urls = parseM3uUrls(result)
                if (urls.isNotEmpty()) {
                    val videoUri = Uri.parse(urls[0]) // Carga el primer canal por defecto
                    videoView.setVideoURI(videoUri)
                }
            }
        }.execute(m3uFileUrl)

        videoView.setOnPreparedListener {
            progressBar.visibility = View.GONE // Ocultar la vista de ProgressBar cuando el video esté preparado
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

        val controller = MediaController(this)
        videoView.setMediaController(controller)
        var currentVideoUri: Uri? = null
        controller.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                // Cambiar al siguiente canal
                val currentIndex = urls.indexOf(currentVideoUri.toString())
                val nextIndex = (currentIndex + 1) % urls.size
                val nextUri = Uri.parse(urls[nextIndex])
                videoView.setVideoURI(nextUri)
                currentVideoUri = nextUri // Actualizar la URI actual al siguiente canal
                return@setOnKeyListener true
            } else if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                // Cambiar al canal anterior
                val currentIndex = urls.indexOf(currentVideoUri.toString())
                val prevIndex = if (currentIndex - 1 < 0) urls.size - 1 else currentIndex - 1
                val prevUri = Uri.parse(urls[prevIndex])
                videoView.setVideoURI(prevUri)
                currentVideoUri = prevUri // Actualizar la URI actual al canal anterior
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

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
        timer.schedule(task, 0, 1 * 120 * 1000)
    }

    private fun parseM3uUrls(m3uContent: String): List<String> {
        val urls = mutableListOf<String>()
        val lines = m3uContent.lines()
        for (line in lines) {
            if (line.startsWith("http")) {
                urls.add(line)
            }
        }
        return urls
    }

    override fun onDestroy() {
        super.onDestroy()
        System.gc()
    }
}


