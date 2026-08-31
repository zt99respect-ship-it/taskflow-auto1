package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseManager
import com.example.data.repository.ScriptRepository
import com.example.data.repository.TaskRepository
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.TaskFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this, applicationScope)
        val firebaseManager = FirebaseManager()
        val taskRepository = TaskRepository(database.taskDao(), firebaseManager, applicationScope)
        val scriptRepository = ScriptRepository(
            context = applicationContext,
            scriptDao = database.scriptDao(),
            logDao = database.logDao(),
            firebaseManager = firebaseManager,
            externalScope = applicationScope
        )

        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(firebaseManager)
        )[AuthViewModel::class.java]

        val mainViewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(taskRepository, scriptRepository, firebaseManager)
        )[MainViewModel::class.java]

        setContent {
            // Request Notification permission for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted / denied handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            TaskFlowTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarkBg
                ) {
                    AppNavigation(
                        authViewModel = authViewModel,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}
