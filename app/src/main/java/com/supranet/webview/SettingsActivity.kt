package com.supranet.webview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.appcompat.widget.Toolbar
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val openSystemSettingsPreference = findPreference<Preference>("open_settings")
            openSystemSettingsPreference?.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                true
            }
        }

         override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference?.key == "url_preference") {
                val editTextPreference = preference as EditTextPreference
                editTextPreference.setOnPreferenceChangeListener { preference, newValue ->
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    sharedPrefs.edit().putString("url_preference", newValue.toString()).apply()
                    true
                }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}