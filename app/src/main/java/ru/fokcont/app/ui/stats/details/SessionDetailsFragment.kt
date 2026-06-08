package ru.fokcont.app.ui.stats.details

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.databinding.FragmentSessionDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class SessionDetailsFragment : Fragment(R.layout.fragment_session_details) {

    private var _binding: FragmentSessionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSessionDetailsBinding.bind(view)

        val sessionId = arguments?.getInt("session_id") ?: return
        loadSessionDetails(sessionId)

        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.deleteButton.setOnClickListener { showDeleteConfirmation(sessionId) }
    }

    private fun loadSessionDetails(sessionId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val sessionRepo = SessionRepository(db.sessionDao())
            val eventDao = db.distractionEventDao()

            val session = sessionRepo.getSessionById(sessionId) ?: return@launch
            val events = eventDao.getEventsForSession(sessionId)

            binding.sessionTitleText.text = session.title.ifBlank { "Сессия" }
            val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.sessionTimeText.text = "${df.format(Date(session.startTime))} • ${session.durationSec / 60} мин"

            binding.noteText.text = if (session.note.isNullOrBlank()) {
                "Заметок нет"
            } else {
                session.note
            }

            val pm = requireContext().packageManager
            val groupedEvents = events.groupBy { it.packageName }

            binding.detailsContainer.removeAllViews()

            groupedEvents.forEach { (pkg, appEvents) ->
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent == null && pkg != "com.google.android.youtube") return@forEach

                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }

                val totalDurationMs = appEvents.filter { it.isConfirmedDistraction }.sumOf { it.durationMs }
                val confirmedEvents = appEvents.filter { it.isConfirmedDistraction }
                val nonConfirmedEvents = appEvents.filter { !it.isConfirmedDistraction }

                val appView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    background = requireContext().getDrawable(R.drawable.bg_card)
                    setPadding(32, 32, 32, 32)
                }
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 32)
                appView.layoutParams = params

                val titleView = TextView(requireContext()).apply {
                    text = appName
                    textSize = 18f
                    background = requireContext().getDrawable(R.drawable.bg_text_overlay)
                    setPadding(24, 8, 24, 8)
                    setTextColor(requireContext().getColor(R.color.primary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                appView.addView(titleView)

                val summaryView = TextView(requireContext()).apply {
                    text = "Суммарно: ${confirmedEvents.size} раз, ${totalDurationMs / 60000} мин ${ (totalDurationMs % 60000) / 1000 } сек"
                    textSize = 14f
                    setPadding(0, 8, 0, 0)
                }
                appView.addView(summaryView)

                confirmedEvents.forEachIndexed { index, event ->
                    val eventView = TextView(requireContext()).apply {
                        text = "${index + 1}. ${event.durationMs / 1000} сек"
                        textSize = 13f
                        setPadding(16, 4, 0, 0)
                    }
                    appView.addView(eventView)
                }

                if (nonConfirmedEvents.isNotEmpty()) {
                    val nonConfirmedTitle = TextView(requireContext()).apply {
                        text = "Открытия без фиксации отвлечения:"
                        textSize = 13f
                        setPadding(0, 16, 0, 0)
                        setTextColor(requireContext().getColor(R.color.text_secondary))
                    }
                    appView.addView(nonConfirmedTitle)

                    nonConfirmedEvents.forEach { event ->
                        val eventView = TextView(requireContext()).apply {
                            text = "• ${event.durationMs / 1000} сек"
                            textSize = 12f
                            setPadding(16, 4, 0, 0)
                            setTextColor(requireContext().getColor(R.color.text_secondary))
                        }
                        appView.addView(eventView)
                    }
                }

                binding.detailsContainer.addView(appView)
            }
        }
    }

    private fun showDeleteConfirmation(sessionId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить сессию")
            .setMessage("Вы уверены, что хотите удалить эту сессию?")
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    SessionRepository(AppDatabase.getInstance(requireContext()).sessionDao()).deleteSession(sessionId)
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity
}
