package com.supranet.supratv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Logica para guardar una nueva URL
            val urlPreference = findPreference<EditTextPreference>("url_preference")
            urlPreference?.setOnPreferenceChangeListener { preference, newValue ->
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                sharedPrefs.edit().putString("url_preference", newValue.toString()).apply()
                true
            }

            // Asigna la preferencia correspondiente a la variable androidIdPreference
            lateinit var androidIdPreference: Preference
            androidIdPreference = findPreference("android_id_preference") ?: throw RuntimeException("Preference with key 'android_id_preference' not found")


            // Obtiene el Android ID
            val androidId = Settings.Secure.getString(requireActivity().contentResolver, Settings.Secure.ANDROID_ID)

            // Asigna el Android ID a la preferencia correspondiente
            androidIdPreference.summary = androidId

            // Abrir configuraciones del sistema
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

            // Configuracion de horarios
            val scheduleSettingsPreference = findPreference<Preference>("schedule_settings")
            scheduleSettingsPreference?.setOnPreferenceClickListener {
                fragmentManager?.beginTransaction()?.replace(android.R.id.content, ScheduleSettingsFragment())?.addToBackStack(null)?.commit()
                true
            }

            // Salir de la App
            val exitAppPreference = findPreference<Preference>("exit_app")
            exitAppPreference?.setOnPreferenceClickListener {
                clearDefaultLauncherPreferences()
                activity?.finishAffinity()
                true
            }
        }

        private fun clearDefaultLauncherPreferences() {
            val pm = requireActivity().packageManager
            pm.clearPackagePreferredActivities(requireActivity().packageName)
        }

        class ScheduleSettingsFragment : PreferenceFragmentCompat() {
            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                setPreferencesFromResource(R.xml.schedule_preferences, rootKey)

                // URL turno mañana
                val turnoMañanaUrlPreference = findPreference<EditTextPreference>("turno_mañana_url")
                turnoMañanaUrlPreference?.setOnPreferenceChangeListener { preference, newValue ->
                    val url = newValue.toString()
                    saveUrlToSharedPreferences(requireContext(), "turno_mañana_url", url)
                    true
                }
                // Horario turno mañana
                val turnoMañanaTimePreference = findPreference<Preference>("turno_mañana_time")
                turnoMañanaTimePreference?.setOnPreferenceClickListener {
                    val context = requireContext()
                    showDualTimePickerDialog(context, "turno_mañana_time")
                    true
                }

                // URL turno mediodia
                val turnoMediodiaUrlPreference = findPreference<EditTextPreference>("turno_mediodia_url")
                turnoMediodiaUrlPreference?.setOnPreferenceChangeListener { preference, newValue ->
                    val url = newValue.toString()
                    saveUrlToSharedPreferences(requireContext(), "turno_mediodia_url", url)
                    true
                }
                // Horario turno mediodia
                val turnoMediodiaPreference = findPreference<Preference>("turno_mediodia_time")
                turnoMediodiaPreference?.setOnPreferenceClickListener {
                    val context = requireContext()
                    showDualTimePickerDialog(context, "turno_mediodia_time")
                    true
                }

                // URL turno tarde
                val turnoTardeUrlPreference = findPreference<EditTextPreference>("turno_tarde_url")
                turnoTardeUrlPreference?.setOnPreferenceChangeListener { preference, newValue ->
                    val url = newValue.toString()
                    saveUrlToSharedPreferences(requireContext(), "turno_tarde_url", url)
                    true
                }
                // Horario turno tarde
                val turnoTardePreference = findPreference<Preference>("turno_tarde_time")
                turnoTardePreference?.setOnPreferenceClickListener {
                    val context = requireContext()
                    showDualTimePickerDialog(context, "turno_tarde_time")
                    true
                }

                // URL turno noche
                val turnoNocheUrlPreference = findPreference<EditTextPreference>("turno_noche_url")
                turnoNocheUrlPreference?.setOnPreferenceChangeListener { preference, newValue ->
                    val url = newValue.toString()
                    saveUrlToSharedPreferences(requireContext(), "turno_noche_url", url)
                    true
                }
                // Horario turno noche
                val turnoNochePreference = findPreference<Preference>("turno_noche_time")
                turnoNochePreference?.setOnPreferenceClickListener {
                    val context = requireContext()
                    showDualTimePickerDialog(context, "turno_noche_time")
                    true
                }
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
// Cuadro de configuracion de horarios para los turnos
private fun showDualTimePickerDialog(context: Context, key: String) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_dual_time_picker, null)
    val timePicker1 = dialogView.findViewById<TimePicker>(R.id.timePicker1)
    val timePicker2 = dialogView.findViewById<TimePicker>(R.id.timePicker2)
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val savedTime = sharedPrefs.getString(key, null)
    val (hour1, minute1, hour2, minute2) = if (savedTime != null) {
        savedTime.split(":"," - ",":").map { it.toInt() }
    } else {
        when (key) {
            "turno_mañana_time" -> listOf(8, 0, 12, 0)
            "turno_mediodia_time" -> listOf(12, 0, 16, 0)
            "turno_tarde_time" -> listOf(16, 0, 20, 0)
            "turno_noche_time" -> listOf(20, 0, 8, 0)
            else -> listOf(0, 0, 0, 0)
        }
    }

    timePicker1.hour = hour1
    timePicker1.minute = minute1
    timePicker2.hour = hour2
    timePicker2.minute = minute2

    val builder = AlertDialog.Builder(context)
        .setTitle("Configurar Horarios")
        .setView(dialogView)
        .setPositiveButton("Guardar") { dialog, _ ->
            val newHour1 = timePicker1.hour
            val newMinute1 = timePicker1.minute
            val newHour2 = timePicker2.hour
            val newMinute2 = timePicker2.minute

            val formattedTime = String.format("%02d:%02d - %02d:%02d", newHour1, newMinute1, newHour2, newMinute2)
            saveStringToSharedPreferences(context, key, formattedTime)

            dialog.dismiss()
        }
        .setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

    val dialog = builder.create()
    dialog.show()
}

private fun saveStringToSharedPreferences(context: Context, key: String, value: String) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    sharedPrefs.edit().putString(key, value).apply()
}

private fun saveUrlToSharedPreferences(context: Context, key: String, url: String) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    sharedPrefs.edit().putString(key, url).apply()
}