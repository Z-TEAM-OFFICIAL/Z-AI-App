package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.ZegaRepository
import com.example.data.auth.AuthRepository
import com.example.data.database.AppDatabase
import com.example.ui.screens.ZegaHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ZegaViewModel
import com.example.ui.viewmodel.ZegaViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize local SQLite Room database & DAOs
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ZegaRepository(database.zegaDao(), database.userAccountDao(), database.vfsDao())
        val authRepository = AuthRepository(database.userAccountDao(), applicationContext)
        
        // 2. Instantiate ZegaViewModel using the Factory pattern
        val viewModel: ZegaViewModel by viewModels {
            ZegaViewModelFactory(repository, authRepository)
        }

        // 3. Enable edge-to-edge drawing
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ZegaHomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
