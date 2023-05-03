package com.supranet.webview

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class StartActivityOnBootReceiver : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    setLauncher(context)
                }
            }

            private fun setLauncher(context: Context) {
                val pm = context.packageManager
                val componentName = ComponentName(context, MainActivity::class.java)
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(homeIntent)
            }
        }
