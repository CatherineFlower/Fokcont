package ru.fokcont.app

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.fokcont.app.data.db.AppDatabase

class FokcontApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            finishActiveSessionSafely()

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun finishActiveSessionSafely() {
        runBlocking(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val activeSession = db.sessionDao().getActiveSession()

                if (activeSession != null && activeSession.startTime > 0L) {
                    val endTime = System.currentTimeMillis()

                    db.sessionDao().updateSession(
                        activeSession.copy(
                            endTime = endTime,
                            durationSec = ((endTime - activeSession.startTime) / 1000)
                                .coerceAtLeast(0)
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}