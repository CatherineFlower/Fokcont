package ru.fokcont.app.service

import android.app.AlertDialog
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import ru.fokcont.app.R
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.fokcont.app.data.db.AppDatabase
import kotlinx.coroutines.withContext

class InterruptionOverlayService : Service() {

    private var overlayView: android.view.View? = null
    private lateinit var windowManager: WindowManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var eventId: Int = -1
    private var targetPackage: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra("app_name") ?: "отвлекающее приложение"

        eventId = intent?.getIntExtra("event_id", -1) ?: -1
        targetPackage = intent?.getStringExtra("target_package") ?: ""

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            showOverlay(appName)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun markEventAsDistractionAndClose() {
        serviceScope.launch {
            try {
                if (eventId != -1) {
                    val db = AppDatabase.getInstance(applicationContext)
                    val eventDao = db.distractionEventDao()

                    val activeSession = db.sessionDao().getActiveSession()

                    if (activeSession != null) {
                        val event = eventDao
                            .getEventsForSession(activeSession.id)
                            .find { it.id == eventId }

                        if (event != null) {
                            eventDao.updateEvent(
                                event.copy(isConfirmedDistraction = true)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                closeOverlay()
            }
        }
    }

    private fun showOverlayMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Готово") { _, _ ->
                closeOverlay()
                returnToHomeScreen()
            }
            .show()
    }

    private fun showNoteDialog() {
        val editText = EditText(this).apply {
            hint = "Например: захотелось открыть YouTube вместо задачи"
            minLines = 3
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        AlertDialog.Builder(this)
            .setTitle("Заметка к сессии")
            .setMessage("Запишите, что именно отвлекло вас сейчас.")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val noteText = editText.text.toString().trim()

                if (noteText.isNotEmpty()) {
                    appendNoteToActiveSession(noteText)
                } else {
                    closeOverlay()
                    returnToHomeScreen()
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                closeOverlay()
                returnToHomeScreen()
            }
            .show()
    }

    private fun appendNoteToActiveSession(noteText: String) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val sessionDao = db.sessionDao()
            val activeSession = sessionDao.getActiveSession()

            if (activeSession != null) {
                val oldNote = activeSession.note.orEmpty().trim()
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())

                val newLine = "[$time] $noteText"

                val updatedNote = if (oldNote.isBlank()) {
                    newLine
                } else {
                    "$oldNote\n$newLine"
                }

                sessionDao.updateSession(
                    activeSession.copy(note = updatedNote)
                )
            }

            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@InterruptionOverlayService,
                    "Заметка добавлена к сессии",
                    Toast.LENGTH_SHORT
                ).show()

                closeOverlay()
                returnToHomeScreen()
            }
        }
    }

    private fun returnToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun showOverlay(appName: String) {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_interruption, null)

        val titleText = overlayView!!.findViewById<TextView>(R.id.overlayTitleText)
        val descText = overlayView!!.findViewById<TextView>(R.id.overlayDescText)
        val workButton = overlayView!!.findViewById<Button>(R.id.overlayWorkButton)
        val continueButton = overlayView!!.findViewById<Button>(R.id.overlayContinueButton)

        titleText.text = "Момент осознанности"
        descText.text = "Вы собирались открыть $appName. Хотите сделать паузу или вернуться к задаче?"

        workButton.setOnClickListener {
            closeOverlay()
            returnToHomeScreen()
        }

        /*breathButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Дыхательное упражнение")
                .setMessage(
                    """
            Вдох — 4 сек
            
            Задержка — 4 сек
            
            Выдох — 8 сек
            
            Повторите цикл 5 раз.
            """.trimIndent()
                )
                .setPositiveButton("Выполнено") { _, _ ->
                    closeOverlay()
                    returnToHomeScreen()
                }
                .show()
        }

        audioButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Пауза")
                .setMessage(
                    "Закройте глаза на 30 секунд и сосредоточьтесь на дыхании, услышьте приятный звук волн или шорох листьев."
                )
                .setPositiveButton("Готово") { _, _ ->
                    closeOverlay()
                    returnToHomeScreen()
                }
                .show()
        }

        notesButton.setOnClickListener {
            showNoteDialog()
        }*/

        continueButton.setOnClickListener {
            markEventAsDistractionAndClose()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER
        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            stopSelf()
        }
    }

    private fun closeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        closeOverlay()
        super.onDestroy()
    }
}