package com.example.mcnews.ui.profile

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.databinding.FragmentProfileBinding
import com.example.mcnews.ui.auth.AuthViewModel
import com.example.mcnews.ui.auth.LoginActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    @Inject
    lateinit var apiService: ApiService

    private var currentUser: Any? = null
    private var selectedImageUri: Uri? = null

    // Activity result launchers
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { handleImageSelection(it) }
        }
    }

    // Permission launcher для сохранения файлов
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            generateProfilePdf()
        } else {
            Toast.makeText(requireContext(), "Разрешение на запись файлов отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
        setupClickListeners()
        observeAuthState()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val user = apiService.getCurrentUser()
                currentUser = user

                // Заполняем поля
                binding.etFirstName.setText(user.firstName)
                binding.etLastName.setText(user.lastName)
                binding.etMiddleName.setText(user.middleName ?: "")
                binding.etEmail.setText(user.email)
                binding.etLogin.setText(user.login)

                // Загружаем аватар если есть
                user.photo?.let { base64Photo ->
                    displayAvatar(base64Photo)
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки профиля: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnChangeAvatar.setOnClickListener {
            checkPermissionAndShowImageDialog()
        }

        binding.btnRemoveAvatar.setOnClickListener {
            clearAvatar()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnToPdf.setOnClickListener {
            checkStoragePermissionAndGeneratePdf()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun checkStoragePermissionAndGeneratePdf() {
        // На Android 10+ (API 29+) не нужно разрешение для сохранения в общую папку
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateProfilePdf()
        } else {
            // Для старых версий Android проверяем разрешение
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) {
                generateProfilePdf()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun generateProfilePdf() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // Получаем PDF от API
                val response = apiService.getProfilePdf()

                if (response.isSuccessful) {
                    val pdfBytes = response.body()?.bytes()
                    if (pdfBytes != null) {
                        savePdfFile(pdfBytes)
                    } else {
                        Snackbar.make(binding.root, "Ошибка: PDF файл пуст", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    Snackbar.make(binding.root, "Ошибка генерации PDF: ${response.code()}", Snackbar.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка генерации PDF: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun savePdfFile(pdfBytes: ByteArray) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "profile_$timestamp.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Для Android 10+ используем MediaStore
                savePdfToMediaStore(pdfBytes, fileName)
            } else {
                // Для старых версий сохраняем в Downloads
                savePdfToDownloads(pdfBytes, fileName)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Ошибка сохранения файла: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun savePdfToMediaStore(pdfBytes: ByteArray, fileName: String) {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { pdfUri ->
            resolver.openOutputStream(pdfUri)?.use { outputStream ->
                outputStream.write(pdfBytes)
                outputStream.flush()
            }

            showPdfSavedDialog(fileName, pdfUri)
        }
    }

    private fun savePdfToDownloads(pdfBytes: ByteArray, fileName: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use { outputStream ->
            outputStream.write(pdfBytes)
            outputStream.flush()
        }

        // Уведомляем систему о новом файле
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        showPdfSavedDialog(fileName, uri)
    }

    private fun showPdfSavedDialog(fileName: String, uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("PDF сохранен")
            .setMessage("Файл '$fileName' сохранен в папку Downloads")
            .setPositiveButton("Открыть") { _, _ ->
                openPdfFile(uri)
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun openPdfFile(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Нет приложения для открытия PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndShowImageDialog() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите источник")
            .setItems(arrayOf("Галерея", "Камера")) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> {
                        selectedImageUri = createImageUri()
                        cameraLauncher.launch(selectedImageUri)
                    }
                }
            }
            .show()
    }

    private fun createImageUri(): Uri {
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "profile_image_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Сжимаем изображение
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            displayAvatar(base64)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayAvatar(base64: String) {
        try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgAvatar.setImageBitmap(bitmap)
            binding.btnRemoveAvatar.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка отображения аватара", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAvatar() {
        binding.imgAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
        binding.btnRemoveAvatar.visibility = View.GONE
        selectedImageUri = null
    }

    private fun saveProfile() {
        lifecycleScope.launch {
            try {
                binding.btnSaveProfile.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                val firstName = binding.etFirstName.text.toString().trim()
                val lastName = binding.etLastName.text.toString().trim()
                val middleName = binding.etMiddleName.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                    Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Подготавливаем multipart запрос
                val firstNamePart = firstName.toRequestBody("text/plain".toMediaTypeOrNull())
                val lastNamePart = lastName.toRequestBody("text/plain".toMediaTypeOrNull())
                val middleNamePart = middleName.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())

                // Обрабатываем фото если выбрано новое
                val photoPart = selectedImageUri?.let { uri ->
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    bytes?.let {
                        val requestBody = it.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("photo", "avatar.jpg", requestBody)
                    }
                }

                apiService.updateProfile(
                    firstName = firstNamePart,
                    lastName = lastNamePart,
                    middleName = middleNamePart,
                    email = emailPart,
                    photo = photoPart
                )

                Toast.makeText(requireContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка обновления профиля: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnSaveProfile.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.example.mcnews.R.layout.dialog_change_password, null)

        AlertDialog.Builder(requireContext())
            .setTitle("Изменить пароль")
            .setView(dialogView)
            .setPositiveButton("Изменить") { _, _ ->
                changePassword(dialogView)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun changePassword(dialogView: View) {
        val etOldPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.mcnews.R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.mcnews.R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.mcnews.R.id.etConfirmPassword)

        val oldPassword = etOldPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                apiService.changePassword(oldPassword, newPassword)
                Toast.makeText(requireContext(), "Пароль изменен", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка изменения пароля: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выйти из аккаунта?")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                authViewModel.logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun observeAuthState() {
        authViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (!isLoggedIn) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImageSourceDialog()
        } else {
            Toast.makeText(requireContext(), "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}