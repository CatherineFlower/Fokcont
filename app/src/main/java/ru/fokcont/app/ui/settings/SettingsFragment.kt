package ru.fokcont.app.ui.settings

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        renderUsageStatus()

        binding.openUsageSettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        binding.privacyPolicyLink.setOnClickListener {
            val bundle = Bundle().apply { putString("policy_type", "privacy") }
            findNavController().navigate(R.id.action_settingsFragment_to_policyFragment, bundle)
        }

        binding.termsLink.setOnClickListener {
            val bundle = Bundle().apply { putString("policy_type", "terms") }
            findNavController().navigate(R.id.action_settingsFragment_to_policyFragment, bundle)
        }

        binding.selectAppsButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_appSelectionFragment)
        }

        binding.deleteAccountButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление аккаунта")
            .setMessage("Вы уверены? Все данные будут безвозвратно удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteEverything()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteEverything() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(requireContext())
                db.clearAllTables()
            }
            // Возврат на экран входа
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderUsageStatus()
    }

    private fun renderUsageStatus() {
        val granted = isUsageAccessGranted()
        binding.usageStatusText.text = if (granted) {
            "Доступ к статистике использования: разрешён\nПриложение может получать данные UsageStatsManager."
        } else {
            "Доступ к статистике использования: не разрешён\nДля полноценного анализа нужно включить Usage Access для Фокконт."
        }
    }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
