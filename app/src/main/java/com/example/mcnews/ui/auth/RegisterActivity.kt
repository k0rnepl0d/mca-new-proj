package com.example.mcnews.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mcnews.R
import com.example.mcnews.databinding.ActivityRegisterBinding
import com.example.mcnews.domain.model.User
import com.example.mcnews.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupObservers()
        setupClickListeners()
    }

    private fun setupSpinner() {
        val genders = arrayOf("Мужской", "Женский")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spGender.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Idle -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Зарегистрироваться"
                }
                is AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnRegister.text = "Регистрация..."
                }
                is AuthState.Success -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Зарегистрироваться"
                    Toast.makeText(this, "Успешная регистрация!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Зарегистрироваться"
                    Toast.makeText(this, "Ошибка: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.etBirthDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnRegister.setOnClickListener {
            if (validateFields()) {
                val user = User(
                    firstName = binding.etFirstName.text.toString().trim(),
                    lastName = binding.etLastName.text.toString().trim(),
                    middleName = binding.etMiddleName.text.toString().trim().takeIf { it.isNotEmpty() },
                    birthDate = selectedDate,
                    genderId = binding.spGender.selectedItemPosition + 1,
                    email = binding.etEmail.text.toString().trim(),
                    login = binding.etLogin.text.toString().trim()
                )

                val password = binding.etPassword.text.toString().trim()
                viewModel.register(user, password)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                binding.etBirthDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateFields(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val login = binding.etLogin.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        return when {
            firstName.isEmpty() -> {
                Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
                false
            }
            lastName.isEmpty() -> {
                Toast.makeText(this, "Введите фамилию", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                false
            }
            login.isEmpty() -> {
                Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
                false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                false
            }
            password.length < 6 -> { // ИСПРАВЛЕНИЕ: Добавляем проверку минимальной длины
                Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                false
            }
            selectedDate.isEmpty() -> {
                Toast.makeText(this, "Выберите дату рождения", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}