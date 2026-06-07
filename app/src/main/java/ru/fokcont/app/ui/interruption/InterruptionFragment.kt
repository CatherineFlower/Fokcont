package ru.fokcont.app.ui.interruption

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.databinding.FragmentInterruptionBinding

class InterruptionFragment : Fragment(R.layout.fragment_interruption) {

    private var _binding: FragmentInterruptionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInterruptionBinding.bind(view)

        val targetPackage = activity?.intent?.getStringExtra("target_package") ?: ""
        val appName = try {
            val pm = requireContext().packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(targetPackage, 0)).toString()
        } catch (e: Exception) {
            "приложение"
        }

        binding.interruptionDescText.text = "Вы собирались открыть $appName. Хотите сделать паузу или вернуться к задаче?"

        binding.workButton.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        binding.breathButton.setOnClickListener {
            showExerciseDialog("Дыхательное упражнение", "Сделайте глубокий вдох на 4 счета, задержите дыхание на 4 счета и медленно выдохните на 8 счетов. Повторите 5 раз.")
        }

        binding.audioButton.setOnClickListener {
            showExerciseDialog("Аудиофрагмент", "Воспроизведение успокаивающих звуков природы (шум леса и пение птиц)...")
        }

        binding.notesButton.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        binding.continueButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val eventId = activity?.intent?.getIntExtra("event_id", -1) ?: -1
        AlertDialog.Builder(requireContext())
            .setTitle("Зафиксировать отвлечение?")
            .setMessage("Вы решили продолжить работу с отвлекающим приложением. Мы запишем это как факт отвлечения в текущую сессию.")
            .setPositiveButton("Да, записываем") { _, _ ->
                recordDistractionAndClose(eventId)
            }
            .setNegativeButton("Нет, я передумал") { _, _ ->
                findNavController().navigate(R.id.mainFragment)
            }
            .show()
    }

    private fun showExerciseDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Завершить") { _, _ ->
                findNavController().navigate(R.id.mainFragment)
            }
            .show()
    }

    private fun recordDistractionAndClose(eventId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val repository = SessionRepository(db.sessionDao())
            val eventDao = db.distractionEventDao()
            
            val active = repository.getActiveSession()
            if (active != null) {
                repository.updateSession(active.copy(switchCount = active.switchCount + 1))
                if (eventId != -1) {
                    // Помечаем событие как подтвержденное отвлечение
                    val event = eventDao.getEventsForSession(active.id).find { it.id == eventId }
                    if (event != null) {
                        eventDao.updateEvent(event.copy(isConfirmedDistraction = true))
                    }
                }
            }
            activity?.moveTaskToBack(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
