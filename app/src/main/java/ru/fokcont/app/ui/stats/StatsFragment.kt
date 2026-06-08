package ru.fokcont.app.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.db.entity.SessionEntity
import ru.fokcont.app.data.repository.NoteRepository
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.data.repository.UserRepository
import ru.fokcont.app.databinding.FragmentStatsBinding
import ru.fokcont.app.viewmodel.StatsViewModel
import ru.fokcont.app.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val chartColors = listOf(
        Color.rgb(84, 128, 109),
        Color.rgb(224, 82, 82),
        Color.rgb(89, 142, 186),
        Color.rgb(218, 170, 75),
        Color.rgb(142, 107, 181)
    )

    private val viewModel: StatsViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        ViewModelFactory(
            userRepository = UserRepository(database.userDao()),
            sessionRepository = SessionRepository(database.sessionDao()),
            noteRepository = NoteRepository(database.noteDao())
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sessionsRecyclerView.isNestedScrollingEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                binding.totalSessionsText.text = "Сессий: ${stats.totalSessions}"
                binding.totalDurationText.text = "Общее время: ${stats.totalDurationSec / 60} мин"
                binding.averageDurationText.text = "Средняя сессия: ${(stats.averageDurationSec / 60).toInt()} мин"
                binding.totalSwitchesText.text = "Переключений: ${stats.totalSwitchCount}"
            }
        }

        loadStatsScreenData()
        viewModel.loadStats()
    }

    private fun loadStatsScreenData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val eventDao = db.distractionEventDao()
            val sessionRepository = SessionRepository(db.sessionDao())
            val sessions = sessionRepository.getSessionsSince(0)

            val distractionsBySession = mutableMapOf<Int, Int>()
            val appDistractionMap = mutableMapOf<String, Int>()

            sessions.forEach { session ->
                val confirmedEvents = eventDao
                    .getEventsForSession(session.id)
                    .filter { it.isConfirmedDistraction }

                distractionsBySession[session.id] = confirmedEvents.size

                confirmedEvents.forEach { event ->
                    appDistractionMap[event.packageName] =
                        (appDistractionMap[event.packageName] ?: 0) + 1
                }
            }

            val totalDistractions = appDistractionMap.values.sum()
            binding.totalDistractionsText.text = "Отвлечений: $totalDistractions"

            val totalSwitches = sessions.sumOf { it.switchCount }

            val focusCount = (totalSwitches - totalDistractions).coerceAtLeast(0)

            val distractionItems = appDistractionMap.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { entry ->
                    getAppName(entry.key) to entry.value.toLong()
                }

            val chartItems = buildList {
                if (focusCount > 0) {
                    add("Фокус" to focusCount.toLong())
                }

                addAll(distractionItems)
            }

            binding.pieChart.setData(chartItems)

            val sortedSessions = sessions.sortedByDescending { it.startTime }

            binding.sessionsRecyclerView.adapter =
                SessionAdapter(sortedSessions, distractionsBySession)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = requireContext().packageManager.getApplicationInfo(packageName, 0)
            requireContext().packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }

    inner class SessionAdapter(
        private val items: List<SessionEntity>,
        private val distractionsBySession: Map<Int, Int>
    ) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val distractionCount = distractionsBySession[item.id] ?: 0

            holder.title.text = item.title.ifBlank { "Рабочая сессия" }
            holder.details.text =
                "${dateFormat.format(Date(item.startTime))} • ${item.durationSec / 60} мин • $distractionCount отвл."

            holder.itemView.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt("session_id", item.id)
                }
                findNavController().navigate(
                    R.id.action_statsFragment_to_sessionDetailsFragment,
                    bundle
                )
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.sessionTitle)
            val details: TextView = view.findViewById(R.id.sessionDetails)
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatsScreenData()
        viewModel.loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}