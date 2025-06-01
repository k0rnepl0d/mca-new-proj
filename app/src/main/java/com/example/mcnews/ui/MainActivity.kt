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

        authViewModel.checkAuthStatus()

        if (!authViewModel.isLoggedIn.value!!) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController)

        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn && hasCheckedAuth) {
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
                findNavController(R.id.nav_host_fragment).navigate(R.id.userArticlesFragment)
                true
            }
            R.id.action_profile -> {
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