package ru.fokcont.app.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main) {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainBinding.bind(view)

        binding.startSessionButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_sessionFragment)
        }
        binding.historyButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_historyFragment)
        }
        binding.statsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_statsFragment)
        }
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        loadDashboard()
    }

    private fun loadDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val repository = SessionRepository(AppDatabase.getInstance(requireContext()).sessionDao())
            val totalSwitches = repository.getTotalSwitchCount()
            val totalDurationMin = repository.getTotalDurationSec() / 60
            val activeSession = repository.getActiveSession()

            binding.todaySwitchesText.text = "Переключений: $totalSwitches"
            binding.focusTimeText.text = "Фокус-время: $totalDurationMin мин"
            binding.sessionStatusText.text = if (activeSession == null) {
                "Активная сессия: нет"
            } else {
                "Активная сессия: ${activeSession.title}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
