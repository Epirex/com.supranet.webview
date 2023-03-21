package com.supranet.webview

import android.app.PendingIntent.getActivity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.supranet.webview.databinding.ActivityMain2Binding
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.preference.PreferenceManager

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val urlEditText = findViewById<EditText>(R.id.urlEditText)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedUrl = sharedPreferences.getString("url", "https://www.ejemplo.com")
        urlEditText.setText(savedUrl)

        val saveButton = findViewById<Button>(R.id.button2)
        saveButton.setOnClickListener {
            val newUrl = urlEditText.text.toString()
            val editor = sharedPreferences.edit()
            editor.putString("url", newUrl)
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
