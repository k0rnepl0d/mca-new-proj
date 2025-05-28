package com.example.mcnews.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.mcnews.R
import com.example.mcnews.ui.auth.AuthViewModel
import com.example.mcnews.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private var hasCheckedAuth = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем авторизацию перед созданием UI
        authViewModel.checkAuthStatus()

        if (!authViewModel.isLoggedIn.value!!) {
            // Если не авторизован, сразу перенаправляем на LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController)

        // Наблюдаем за изменениями статуса авторизации
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn && hasCheckedAuth) {
                // Если пользователь вышел из системы
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            hasCheckedAuth = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_my_articles -> {
                // ДОБАВЛЕНИЕ: Навигация к "Ваши статьи"
                findNavController(R.id.nav_host_fragment).navigate(R.id.userArticlesFragment)
                true
            }
            R.id.action_profile -> {
                // Навигация к профилю
                findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment)
                true
            }
            R.id.action_logout -> {
                authViewModel.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
}