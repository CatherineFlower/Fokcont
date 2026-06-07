package ru.fokcont.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppInstallReceiver : BroadcastReceiver() {

    private val distractingPackages = setOf(
        "com.google.android.youtube",
        "com.vkontakte.android",
        "org.telegram.messenger",
        "ru.rutube.app",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ok.android"
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            
            if (distractingPackages.contains(packageName)) {
                val prefs = context.getSharedPreferences("fokcont_prefs", Context.MODE_PRIVATE)
                val currentSet = prefs.getStringSet("distracting_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
                
                if (currentSet.add(packageName)) {
                    prefs.edit().putStringSet("distracting_apps", currentSet).apply()
                }
            }
        }
    }
}
