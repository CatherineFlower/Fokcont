package ru.fokcont.app.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import ru.fokcont.app.MainActivity
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.repository.SessionRepository
import android.app.AppOpsManager
import ru.fokcont.app.service.InterruptionOverlayService
import android.app.usage.UsageEvents

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastAppPackage: String? = null
    private var isRunning = false
    private var lastCheckTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startTracking()
        }
        return START_STICKY
    }

    private fun getForegroundApp(
        usageStatsManager: UsageStatsManager,
        fromTime: Long,
        toTime: Long
    ): String? {
        val events = usageStatsManager.queryEvents(fromTime, toTime)
        val event = UsageEvents.Event()

        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                foregroundPackage = event.packageName
            }
        }

        return foregroundPackage
    }

    private fun startTracking() {
        serviceScope.launch {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = SessionRepository(database.sessionDao())
            val eventDao = database.distractionEventDao()
            
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val prefs = applicationContext.getSharedPreferences("fokcont_prefs", Context.MODE_PRIVATE)

            lastCheckTime = System.currentTimeMillis()

            while (isRunning) {
                if (!hasUsageStatsPermission()) {
                    val active = repository.getActiveSession()
                    if (active != null) {
                        val endTime = System.currentTimeMillis()
                        repository.updateSession(active.copy(
                            endTime = endTime,
                            durationSec = (endTime - active.startTime) / 1000
                        ))
                    }
                    stopSelf()
                    break
                }

                val activeSession = repository.getActiveSession()
                if (activeSession != null) {
                    val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        powerManager.isInteractive
                    } else {
                        powerManager.isScreenOn
                    }

                    if (!isScreenOn) {
                        // Завершаем текущее событие если оно было
                        lastAppPackage?.let { pkg ->
                             val event = eventDao.getUnfinishedEvent(activeSession.id, pkg)
                             if (event != null) {
                                 eventDao.updateEvent(event.copy(durationMs = System.currentTimeMillis() - event.startTime))
                             }
                        }
                        lastAppPackage = "screen_off"
                        delay(2000)
                        continue
                    }

                    val currentTime = System.currentTimeMillis()

                    val currentApp = getForegroundApp(
                        usageStatsManager = usageStatsManager,
                        fromTime = currentTime - 10_000,
                        toTime = currentTime
                    )

                    if (currentApp != null && currentApp != applicationContext.packageName) {
                        if (lastAppPackage != currentApp) {
                            lastAppPackage?.let { oldPkg ->
                                val event = eventDao.getUnfinishedEvent(activeSession.id, oldPkg)
                                if (event != null) {
                                    eventDao.updateEvent(
                                        event.copy(durationMs = System.currentTimeMillis() - event.startTime)
                                    )
                                }
                            }

                            lastAppPackage = currentApp

                            val distractingAppsSet =
                                prefs.getStringSet("distracting_apps", emptySet()) ?: emptySet()

                            val isDistracting = distractingAppsSet.contains(currentApp)

                            val eventId = eventDao.insertEvent(
                                ru.fokcont.app.data.db.entity.DistractionEventEntity(
                                    sessionId = activeSession.id,
                                    packageName = currentApp,
                                    startTime = System.currentTimeMillis(),
                                    isConfirmedDistraction = false
                                )
                            )

                            if (isDistracting) {
                                launchInterruptionUI(currentApp, eventId.toInt())
                            }

                            val updatedSession = activeSession.copy(
                                switchCount = activeSession.switchCount + 1
                            )

                            repository.updateSession(updatedSession)
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun launchInterruptionUI(packageName: String, eventId: Int) {
        val appName = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }

        val intent = Intent(this, InterruptionOverlayService::class.java).apply {
            putExtra("app_name", appName)
            putExtra("target_package", packageName)
            putExtra("event_id", eventId.toInt())
        }

        startService(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Фокконт активен")
            .setContentText("Отслеживание фокус-сессии...")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false

        runBlocking(Dispatchers.IO) {
            try {
                val database = AppDatabase.getInstance(applicationContext)
                val repository = SessionRepository(database.sessionDao())
                val active = repository.getActiveSession()

                if (active != null) {
                    val endTime = System.currentTimeMillis()

                    repository.updateSession(
                        active.copy(
                            endTime = endTime,
                            durationSec = (endTime - active.startTime) / 1000
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 1
    }
}
