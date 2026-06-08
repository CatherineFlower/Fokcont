package ru.fokcont.app.ui.session

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.db.entity.SessionEntity
import ru.fokcont.app.data.repository.NoteRepository
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.data.repository.UserRepository
import ru.fokcont.app.databinding.FragmentSessionBinding
import ru.fokcont.app.service.TrackingService
import ru.fokcont.app.viewmodel.SessionState
import ru.fokcont.app.viewmodel.SessionViewModel
import ru.fokcont.app.viewmodel.ViewModelFactory
import java.util.Calendar

class SessionFragment : Fragment(R.layout.fragment_session) {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var sessionStartTime = 0L

    private val draftPrefs by lazy {
        requireContext().getSharedPreferences("session_draft", Context.MODE_PRIVATE)
    }

    private fun restoreDraft() {
        val savedTitle = draftPrefs.getString("title", "")
        val savedNote = draftPrefs.getString("note", "")

        if (!savedTitle.isNullOrBlank()) {
            binding.sessionTitleEditText.setText(savedTitle)
        }

        if (!savedNote.isNullOrBlank()) {
            binding.noteEditText.setText(savedNote)
        }
    }

    private fun saveDraft() {
        draftPrefs.edit()
            .putString("title", binding.sessionTitleEditText.text.toString())
            .putString("note", binding.noteEditText.text.toString())
            .apply()
    }

    private fun clearDraft() {
        draftPrefs.edit().clear().apply()
    }

    private val viewModel: SessionViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        ViewModelFactory(
            userRepository = UserRepository(database.userDao()),
            sessionRepository = SessionRepository(database.sessionDao()),
            noteRepository = NoteRepository(database.noteDao())
        )
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${requireContext().packageName}")
        )
        startActivity(intent)

        Toast.makeText(
            requireContext(),
            "Разрешите приложению Фокконт показывать окна поверх других приложений",
            Toast.LENGTH_LONG
        ).show()
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (viewModel.sessionState.value != SessionState.Running || sessionStartTime <= 0L) {
                binding.timerText.text = "00:00"
                return
            }

            val elapsedSec = ((System.currentTimeMillis() - sessionStartTime) / 1000).coerceAtLeast(0)
            val minutes = elapsedSec / 60
            val seconds = elapsedSec % 60

            binding.timerText.text = "%02d:%02d".format(minutes, seconds)
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSessionBinding.bind(view)
        restoreDraft()

        viewModel.checkActiveSession()

        binding.startStopButton.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                Toast.makeText(requireContext(), "Необходимо разрешение на отслеживание", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                return@setOnClickListener
            }
            if (!hasOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            val state = viewModel.sessionState.value
            if (state == SessionState.Running) {
                stopTrackingService()
                viewModel.finishSession(binding.noteEditText.text.toString())
            } else {
                viewModel.startSession(binding.sessionTitleEditText.text.toString().ifBlank { "Рабочая сессия" })
                startTrackingService()
            }
        }

        binding.addSwitchButton.setOnClickListener {
            if (viewModel.sessionState.value == SessionState.Running) {
                viewModel.incrementSwitchCount()
            } else {
                Toast.makeText(requireContext(), "Сначала начните сессию", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSession.collect { session ->
                binding.switchCountText.text = "Переключений: ${session?.switchCount ?: 0}"
                
                // Считаем подтвержденные отвлечения из базы
                if (session != null) {
                    val db = AppDatabase.getInstance(requireContext())
                    val confirmedCount = db.distractionEventDao().getEventsForSession(session.id)
                        .count { it.isConfirmedDistraction }
                    binding.distractionCountText.text = "Отвлечений: $confirmedCount"
                    
                    if (viewModel.sessionState.value == SessionState.Running) {
                        sessionStartTime = session.startTime
                    }
                } else {
                    binding.distractionCountText.text = "Отвлечений: 0"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collect { state ->
                when (state) {
                    SessionState.Idle -> {
                        binding.startStopButton.text = "Начать"
                        sessionStartTime = 0L
                        binding.timerText.text = "00:00"
                        timerHandler.removeCallbacks(timerRunnable)
                    }
                    SessionState.Running -> {
                        binding.startStopButton.text = "Завершить"
                        if (sessionStartTime == 0L) sessionStartTime = System.currentTimeMillis()
                        timerHandler.removeCallbacks(timerRunnable)
                        timerHandler.post(timerRunnable)
                    }
                    SessionState.Finished -> {
                        binding.startStopButton.text = "Начать новую сессию"
                        sessionStartTime = 0L
                        binding.timerText.text = "00:00"
                        timerHandler.removeCallbacks(timerRunnable)

                        binding.sessionTitleEditText.setText("")
                        binding.noteEditText.setText("")
                        clearDraft()

                        Toast.makeText(requireContext(), "Сессия сохранена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startTrackingService() {
        val intent = Intent(requireContext(), TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopTrackingService() {
        val intent = Intent(requireContext(), TrackingService::class.java)
        requireContext().stopService(intent)
    }

    override fun onDestroyView() {
        timerHandler.removeCallbacks(timerRunnable)
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        viewModel.checkActiveSession()

        if (viewModel.sessionState.value == SessionState.Running && sessionStartTime > 0L) {
            timerHandler.post(timerRunnable)
        } else {
            binding.timerText.text = "00:00"
        }
    }

    override fun onPause() {
        saveDraft()
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
