package com.supranet.supratv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
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

            lateinit var androidIdPreference: Preference

            // Asigna la preferencia correspondiente a la variable androidIdPreference
            androidIdPreference = findPreference("android_id_preference") ?: throw RuntimeException("Preference with key 'android_id_preference' not found")


            // Obtiene el Android ID
            val androidId = Settings.Secure.getString(requireActivity().contentResolver, Settings.Secure.ANDROID_ID)

            // Asigna el Android ID a la preferencia correspondiente
            androidIdPreference.summary = androidId

            val openSystemSettingsPreference = findPreference<Preference>("open_settings")
            openSystemSettingsPreference?.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                true
            }

            val appDetailsPreference = findPreference<Preference>("app_details_preference")
            appDetailsPreference?.setOnPreferenceClickListener {
                val packageName = requireContext().packageName
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
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