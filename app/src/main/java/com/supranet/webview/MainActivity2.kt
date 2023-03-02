package com.supranet.webview

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.supranet.webview.databinding.ActivityMain2Binding
import android.widget.Button

class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}

class CambiarURL : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val button2 = findViewById<Button>(R.id.button2)
        button2.setOnClickListener{
            val intento2 = Intent(this, MainActivity::class.java)
            startActivity(intento2)
        }
    }
}