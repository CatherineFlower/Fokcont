package ru.fokcont.app.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.fokcont.app.R
import ru.fokcont.app.data.db.AppDatabase
import ru.fokcont.app.data.repository.NoteRepository
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.data.repository.UserRepository
import ru.fokcont.app.databinding.FragmentLoginBinding
import ru.fokcont.app.viewmodel.LoginState
import ru.fokcont.app.viewmodel.LoginViewModel
import ru.fokcont.app.viewmodel.ViewModelFactory

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        ViewModelFactory(
            userRepository = UserRepository(database.userDao()),
            sessionRepository = SessionRepository(database.sessionDao()),
            noteRepository = NoteRepository(database.noteDao())
        )
    }

    private var setupMode = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        viewModel.checkIfUserExists()

        binding.readPrivacyText.setOnClickListener {
            val bundle = Bundle().apply { putString("policy_type", "privacy") }
            findNavController().navigate(R.id.action_loginFragment_to_policyFragment, bundle)
        }

        binding.readTermsText.setOnClickListener {
            val bundle = Bundle().apply { putString("policy_type", "terms") }
            findNavController().navigate(R.id.action_loginFragment_to_policyFragment, bundle)
        }

        binding.loginButton.setOnClickListener {
            val pin = binding.pinEditText.text.toString().trim()
            if (pin.length < 4) {
                Toast.makeText(requireContext(), "PIN должен состоять минимум из 4 цифр", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (setupMode) {
                if (!binding.privacyCheckBox.isChecked || !binding.termsCheckBox.isChecked) {
                    Toast.makeText(requireContext(), "Необходимо принять согласия", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.setupPin(pin)
            } else {
                viewModel.verifyPin(pin)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                renderState(state)
            }
        }
    }

    private fun renderState(state: LoginState) {
        if (_binding == null) return
        when (state) {
            LoginState.Idle -> {
                setupMode = false
                binding.pinModeText.text = "Введите PIN-код"
                binding.loginButton.text = "Войти"
                binding.consentLayout.visibility = View.GONE
            }
            LoginState.NeedSetup -> {
                setupMode = true
                binding.pinModeText.text = "Создайте PIN-код"
                binding.loginButton.text = "Сохранить PIN"
                binding.consentLayout.visibility = View.VISIBLE
            }
            LoginState.Success -> {
                if (setupMode) {
                    findNavController().navigate(R.id.action_loginFragment_to_appSelectionFragment)
                } else {
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
            }
            LoginState.WrongPin -> {
                Toast.makeText(requireContext(), "Неверный PIN", Toast.LENGTH_SHORT).show()
            }
            LoginState.Loading -> {
                binding.loginButton.text = "Проверяем..."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
