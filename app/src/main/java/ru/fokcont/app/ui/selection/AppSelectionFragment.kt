package ru.fokcont.app.ui.selection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.fokcont.app.R
import ru.fokcont.app.databinding.FragmentAppSelectionBinding

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean
)

class AppSelectionFragment : Fragment(R.layout.fragment_app_selection) {

    private var _binding: FragmentAppSelectionBinding? = null
    private val binding get() = _binding!!
    
    private var allApps: List<AppItem> = emptyList()
    private var filteredApps: List<AppItem> = emptyList()
    private lateinit var adapter: AppAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAppSelectionBinding.bind(view)

        val prefs = requireContext().getSharedPreferences("fokcont_prefs", Context.MODE_PRIVATE)
        val savedApps = prefs.getStringSet("distracting_apps", emptySet()) ?: emptySet()

        val pm = requireContext().packageManager
        val distractingPackages = setOf(
            "com.google.android.youtube",
            "com.vkontakte.android",
            "org.telegram.messenger",
            "ru.rutube.app",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.ok.android"
        )

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        allApps = pm.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val packageName = appInfo.packageName
                
                val isDistractingByDefault = distractingPackages.contains(packageName)
                if (isDistractingByDefault && !savedApps.contains(packageName)) {
                    val currentSet = prefs.getStringSet("distracting_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
                    currentSet.add(packageName)
                    prefs.edit().putStringSet("distracting_apps", currentSet).apply()
                }
                
                AppItem(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(pm),
                    isSelected = savedApps.contains(packageName) || isDistractingByDefault
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name }

        filteredApps = allApps

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AppAdapter(filteredApps)
        binding.appsRecyclerView.adapter = adapter

        setupSearch()

        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        
        binding.saveButton.setOnClickListener {
            val selectedPackages = allApps.filter { it.isSelected }.map { it.packageName }.toSet()
            prefs.edit().putStringSet("distracting_apps", selectedPackages).apply()
            
            // Если мы пришли сюда из регистрации (нет предыдущего экрана в стеке или это первый запуск)
            // то переходим на главный экран. Иначе просто возвращаемся назад.
            if (findNavController().previousBackStackEntry == null) {
                findNavController().navigate(R.id.mainFragment)
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filter(query: String) {
        filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filteredApps)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AppAdapter(private var items: List<AppItem>) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_selection, parent, false)
            return ViewHolder(view)
        }

        fun updateList(newList: List<AppItem>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.icon.setImageDrawable(item.icon)
            holder.checkBox.setOnCheckedChangeListener(null) // Сброс слушателя перед установкой значения
            holder.checkBox.isChecked = item.isSelected
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)
            val checkBox: CheckBox = view.findViewById(R.id.appCheckBox)
        }
    }
}
