package ru.fokcont.app.ui.history

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.db.entity.SessionEntity
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.databinding.FragmentHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        val repository = SessionRepository(AppDatabase.getInstance(requireContext()).sessionDao())
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllSessions().collect { sessions ->
                renderSessions(sessions)
            }
        }
    }

    private fun renderSessions(sessions: List<SessionEntity>) {
        val container = binding.historyContainer
        while (container.childCount > 2) {
            container.removeViewAt(2)
        }

        binding.emptyHistoryText.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE

        sessions.forEach { session ->
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(session.startTime))
            val durationMin = session.durationSec / 60
            val text = "${session.title}\n$date\nДлительность: $durationMin мин · Переключений: ${session.switchCount}\n${session.note.ifBlank { "Без заметки" }}"

            val item = TextView(requireContext()).apply {
                setText(text)
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setBackgroundResource(R.drawable.bg_card)
                setPadding(28, 24, 28, 24)
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = 16
                layoutParams = params
            }
            container.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
