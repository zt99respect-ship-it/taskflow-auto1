package com.example.ui.automation

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

data class ColabRepo(val name: String, val url: String)

// Simple SharedPreferences storage for repositories
class ColabStorage(context: Context) {
    private val prefs = context.getSharedPreferences("colab_repos", Context.MODE_PRIVATE)
    
    fun saveRepo(name: String, url: String) {
        prefs.edit().putString(name, url).apply()
    }
    
    fun getRepos(): List<ColabRepo> {
        val map = prefs.all
        return map.map { ColabRepo(it.key, it.value as String) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColabHubScreen(navController: NavController) {
    val context = LocalContext.current
    val storage = remember { ColabStorage(context) }
    var repos by remember { mutableStateOf(storage.getRepos()) }
    
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("") }
    var newRepoUrl by remember { mutableStateOf("https://colab.research.google.com/") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentUrl == null) "مستودعات كولاب" else "جوجل كولاب", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentUrl != null) currentUrl = null
                        else navController.popBackStack() 
                    }) {
                        Icon(if (currentUrl != null) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUrl == null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Repo")
                }
            }
        }
    ) { padding ->
        if (currentUrl == null) {
            // List view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Text("المشاريع والمستودعات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (repos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لم تقم بإضافة أي مشاريع كولاب بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(repos) { repo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentUrl = repo.url },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(repo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(repo.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة مشروع كولاب") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newRepoName,
                                onValueChange = { newRepoName = it },
                                label = { Text("اسم المشروع") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newRepoUrl,
                                onValueChange = { newRepoUrl = it },
                                label = { Text("الرابط (URL)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newRepoName.isNotBlank() && newRepoUrl.isNotBlank()) {
                                storage.saveRepo(newRepoName, newRepoUrl)
                                repos = storage.getRepos()
                                showAddDialog = false
                                newRepoName = ""
                                newRepoUrl = "https://colab.research.google.com/"
                            }
                        }) {
                            Text("حفظ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        } else {
            // WebView mode
            AndroidView(
                factory = {
                    WebView(it).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // User agent spoofing to avoid Google sign-in blocking in WebViews
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(currentUrl!!)
                    }
                },
                update = {
                    it.loadUrl(currentUrl!!)
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}
