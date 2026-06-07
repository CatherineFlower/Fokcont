package ru.fokcont.app.ui.stats

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
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                binding.totalSessionsText.text = "Сессий: ${stats.totalSessions}"
                binding.totalDurationText.text = "Общее время: ${stats.totalDurationSec / 60} мин"
                binding.averageDurationText.text = "Средняя сессия: ${(stats.averageDurationSec / 60).toInt()} мин"
                binding.totalSwitchesText.text = "Переключений: ${stats.totalSwitchCount}"
            }
        }

        loadPieChartData()
        loadSessionHistory()
        viewModel.loadStats()
    }

    private fun loadPieChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val eventDao = db.distractionEventDao()
            val sessions = SessionRepository(db.sessionDao()).getSessionsSince(0)
            
            if (sessions.isEmpty()) {
                binding.pieChart.setData(emptyList())
                return@launch
            }

            val appUsageMap = mutableMapOf<String, Long>()
            var hasDistractions = false
            sessions.forEach { session ->
                val events = eventDao.getEventsForSession(session.id)
                events.filter { it.isConfirmedDistraction }.forEach { event ->
                    appUsageMap[event.packageName] = (appUsageMap[event.packageName] ?: 0L) + event.durationMs
                    hasDistractions = true
                }
            }
            
            if (!hasDistractions) {
                binding.pieChart.setData(listOf("Focus" to 0L)) 
            } else {
                val pieData = appUsageMap.toList().sortedByDescending { it.second }.take(5)
                binding.pieChart.setData(pieData)
            }
        }
    }

    private fun loadSessionHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val repository = SessionRepository(AppDatabase.getInstance(requireContext()).sessionDao())
            repository.getAllSessions().collect { sessions ->
                binding.sessionsRecyclerView.adapter = SessionAdapter(sessions)
            }
        }
    }

    inner class SessionAdapter(private val items: List<SessionEntity>) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {
        private val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title.ifBlank { "Рабочая сессия" }
            holder.details.text = "${dateFormat.format(Date(item.startTime))} • ${item.durationSec / 60} мин • ${item.switchCount} отв."
            holder.itemView.setOnClickListener {
                val bundle = Bundle().apply { putInt("session_id", item.id) }
                findNavController().navigate(R.id.action_statsFragment_to_sessionDetailsFragment, bundle)
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.sessionTitle)
            val details: TextView = view.findViewById(R.id.sessionDetails)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
