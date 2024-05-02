package com.supranet.supratv

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsStreaming : AppCompatActivity() {

    override fun onBackPressed() {
        val intent = Intent(this, Streaming::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, AdvertisingSettingsFragment()).commit()

        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.hide()
        }
    }

    class AdvertisingSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.streaming_preferences, rootKey)

            val baseAdvertisingTimePreference: EditTextPreference? = findPreference("base_advertising_time")
            baseAdvertisingTimePreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }

            val mixedStreamingTimePreference: EditTextPreference? = findPreference("mixed_streaming_time")
            mixedStreamingTimePreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }

            val mixedAdvertisingTimePreference: EditTextPreference? = findPreference("mixed_advertising_time")
            mixedAdvertisingTimePreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }
        }
    }
}