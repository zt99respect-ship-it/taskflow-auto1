package com.example.ui.automation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LogEntity
import com.example.data.local.entity.ScriptEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationHubScreen(
    mainViewModel: MainViewModel
) {
    val scripts by mainViewModel.allScripts.collectAsStateWithLifecycle()
    val logs by mainViewModel.executionLogs.collectAsStateWithLifecycle()
    val isExecuting by mainViewModel.isExecuting.collectAsStateWithLifecycle()
    val terminalOutput by mainViewModel.activeTerminalOutput.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Scripts, 1: Live Terminal, 2: Logs History
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var scriptToEdit by remember { mutableStateOf<ScriptEntity?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = {
                        scriptToEdit = null
                        showAddDialog = true
                    },
                    containerColor = CyanPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_script_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Script")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "مركز الأتمتة",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Termux, Bash, Python & HTTP Webhook Dispatcher",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Top Navigation Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = CyanPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Scripts (${scripts.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Terminal", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("History (${logs.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                0 -> {
                    // Script List View
                    ScriptsListView(
                        scripts = scripts,
                        selectedTypeFilter = selectedTypeFilter,
                        onTypeFilterChange = { selectedTypeFilter = it },
                        isExecuting = isExecuting,
                        onRunScript = { script ->
                            mainViewModel.runScript(script, triggerType = "MANUAL")
                            activeTab = 1 // Switch to terminal to see live run!
                        },
                        onEditScript = { script ->
                            scriptToEdit = script
                            showAddDialog = true
                        },
                        onDeleteScript = { script -> mainViewModel.deleteScript(script) },
                        onToggleQuickAction = { script ->
                            mainViewModel.updateScript(script.copy(isQuickAction = !script.isQuickAction))
                        }
                    )
                }
                1 -> {
                    // Live Terminal Screen
                    TerminalView(
                        output = terminalOutput,
                        isExecuting = isExecuting,
                        onClearLogs = { mainViewModel.clearLogs() },
                        onCopyLogs = {
                            clipboardManager.setText(AnnotatedString(terminalOutput))
                            Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                2 -> {
                    // Logs History List
                    LogsHistoryView(
                        logs = logs,
                        onClearHistory = { mainViewModel.clearLogs() }
                    )
                }
            }
        }
    }

    // Add / Edit Script Dialog
    if (showAddDialog) {
        ScriptDialog(
            scriptToEdit = scriptToEdit,
            onDismiss = { showAddDialog = false },
            onSave = { script ->
                if (scriptToEdit == null) {
                    mainViewModel.createScript(
                        name = script.name,
                        description = script.description,
                        type = script.type,
                        content = script.content,
                        webhookMethod = script.webhookMethod,
                        webhookHeaders = script.webhookHeaders,
                        webhookPayload = script.webhookPayload,
                        scheduleType = script.scheduleType,
                        dailyHour = script.dailyHour,
                        dailyMinute = script.dailyMinute,
                        isQuickAction = script.isQuickAction
                    )
                } else {
                    mainViewModel.updateScript(script)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScriptsListView(
    scripts: List<ScriptEntity>,
    selectedTypeFilter: String,
    onTypeFilterChange: (String) -> Unit,
    isExecuting: Boolean,
    onRunScript: (ScriptEntity) -> Unit,
    onEditScript: (ScriptEntity) -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onToggleQuickAction: (ScriptEntity) -> Unit
) {
    val filterTypes = listOf("ALL", "BASH", "PYTHON", "WEBHOOK", "TERMUX_INTENT")
    val filtered = if (selectedTypeFilter == "ALL") {
        scripts
    } else {
        scripts.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTypes) { type ->
                val isSelected = selectedTypeFilter == type
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyanLight else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.clickable { onTypeFilterChange(type) }
                ) {
                    Text(
                        text = when (type) {
                            "TERMUX_INTENT" -> "Termux"
                            "WEBHOOK" -> "Webhook"
                            "PYTHON" -> "Python"
                            "BASH" -> "Bash"
                            else -> "All Types"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No scripts found. Tap the '+' button below to add your first automation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        isExecuting = isExecuting,
                        onRun = { onRunScript(script) },
                        onEdit = { onEditScript(script) },
                        onDelete = { onDeleteScript(script) },
                        onToggleQuickAction = { onToggleQuickAction(script) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptCard(
    script: ScriptEntity,
    isExecuting: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleQuickAction: () -> Unit
) {
    val accentColor = when (script.type.uppercase()) {
        "BASH" -> EmeraldSuccess
        "PYTHON" -> IndigoAccent
        "WEBHOOK" -> CyanPrimary
        "TERMUX_INTENT" -> AmberWarning
        else -> CyanPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("script_card_${script.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = script.type,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Schedule: ${script.scheduleType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleQuickAction, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (script.isQuickAction) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Quick Action",
                            tint = if (script.isQuickAction) AmberWarning else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanLight, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = script.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            if (script.description.isNotBlank()) {
                Text(
                    text = script.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalBg)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = script.content.take(120),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = TerminalGreen,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (script.lastRunStatus.uppercase()) {
                        "SUCCESS" -> EmeraldSuccess
                        "FAILED", "ERROR" -> RoseError
                        "RUNNING" -> AmberWarning
                        else -> Color.Gray
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Status: ${script.lastRunStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }

                // Run Now Button
                Button(
                    onClick = onRun,
                    enabled = !isExecuting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("run_script_button_${script.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تشغيل الآن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TerminalView(
    output: String,
    isExecuting: Boolean,
    onClearLogs: () -> Unit,
    onCopyLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Live Terminal Console",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (isExecuting) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = TerminalGreen
                    )
                }
            }

            Row {
                IconButton(onClick = onCopyLogs) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Output", tint = CyanLight)
                }
                IconButton(onClick = onClearLogs) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Logs", tint = RoseError)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Screen Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(TerminalBg)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "taskflow@root:~$ ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TerminalCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = TerminalGreen
                )
            }
        }
    }
}

@Composable
fun LogsHistoryView(
    logs: List<LogEntity>,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Execution History Logs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("مسح السجل", color = RoseError)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No execution history recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogHistoryCard(log = log)
                }
            }
        }
    }
}

@Composable
fun LogHistoryCard(log: LogEntity) {
    val isSuccess = log.status.equals("SUCCESS", ignoreCase = true)
    val statusColor = if (isSuccess) EmeraldSuccess else RoseError

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.scriptName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = "${log.executionDurationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanLight
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Trigger: ${log.triggerType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateStr = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalBg)
                    .padding(8.dp)
            ) {
                Text(
                    text = log.output.take(200),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = if (isSuccess) TerminalGreen else Color(0xFFF87171),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptDialog(
    scriptToEdit: ScriptEntity?,
    onDismiss: () -> Unit,
    onSave: (ScriptEntity) -> Unit
) {
    var name by remember { mutableStateOf(scriptToEdit?.name ?: "") }
    var description by remember { mutableStateOf(scriptToEdit?.description ?: "") }
    var type by remember { mutableStateOf(scriptToEdit?.type ?: "BASH") }
    var content by remember { mutableStateOf(scriptToEdit?.content ?: "") }
    var webhookMethod by remember { mutableStateOf(scriptToEdit?.webhookMethod ?: "POST") }
    var webhookHeaders by remember { mutableStateOf(scriptToEdit?.webhookHeaders ?: "{\"Content-Type\":\"application/json\"}") }
    var webhookPayload by remember { mutableStateOf(scriptToEdit?.webhookPayload ?: "{\"action\":\"trigger\"}") }
    var scheduleType by remember { mutableStateOf(scriptToEdit?.scheduleType ?: "MANUAL") }
    var dailyHour by remember { mutableIntStateOf(scriptToEdit?.dailyHour ?: 8) }
    var dailyMinute by remember { mutableIntStateOf(scriptToEdit?.dailyMinute ?: 0) }
    var isQuickAction by remember { mutableStateOf(scriptToEdit?.isQuickAction ?: true) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var scheduleDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    val types = listOf("BASH", "PYTHON", "WEBHOOK", "TERMUX_INTENT")
    val scheduleTypes = listOf("MANUAL", "HOURLY", "DAILY", "SCHEDULED_TIME", "FCM_TRIGGER")
    val httpMethods = listOf("POST", "GET", "PUT", "DELETE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (scriptToEdit == null) "Add Automation Script" else "Edit Script",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم النص / سير العمل") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("script_name_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف قصير") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Script Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع الأتمتة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        types.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    type = item
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Content / Script Code / URL
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = {
                        Text(
                            when (type) {
                                "WEBHOOK" -> "Webhook URL (https://...)"
                                "TERMUX_INTENT" -> "Termux Command / Script Name"
                                "PYTHON" -> "Python Code Script"
                                else -> "Bash Command / Shell Script"
                            }
                        )
                    },
                    minLines = if (type == "WEBHOOK") 2 else 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("script_content_input")
                )

                // Webhook specific fields
                if (type == "WEBHOOK") {
                    ExposedDropdownMenuBox(
                        expanded = methodDropdownExpanded,
                        onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = webhookMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("طريقة HTTP") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            httpMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        webhookMethod = method
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = webhookPayload,
                        onValueChange = { webhookPayload = it },
                        label = { Text("حمولة JSON (اختياري)") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Schedule Type
                ExposedDropdownMenuBox(
                    expanded = scheduleDropdownExpanded,
                    onExpandedChange = { scheduleDropdownExpanded = !scheduleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = scheduleType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الجدولة / المُشغِّل") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = scheduleDropdownExpanded,
                        onDismissRequest = { scheduleDropdownExpanded = false }
                    ) {
                        scheduleTypes.forEach { schedule ->
                            DropdownMenuItem(
                                text = { Text(schedule) },
                                onClick = {
                                    scheduleType = schedule
                                    scheduleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Action Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تثبيت في الإجراءات السريعة بالرئيسية", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Switch(
                        checked = isQuickAction,
                        onCheckedChange = { isQuickAction = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && content.isNotBlank()) {
                        val result = scriptToEdit?.copy(
                            name = name,
                            description = description,
                            type = type,
                            content = content,
                            webhookMethod = webhookMethod,
                            webhookHeaders = webhookHeaders,
                            webhookPayload = webhookPayload,
                            scheduleType = scheduleType,
                            dailyHour = dailyHour,
                            dailyMinute = dailyMinute,
                            isQuickAction = isQuickAction
                        ) ?: ScriptEntity(
                            name = name,
                            description = description,
                            type = type,
                            content = content,
                            webhookMethod = webhookMethod,
                            webhookHeaders = webhookHeaders,
                            webhookPayload = webhookPayload,
                            scheduleType = scheduleType,
                            dailyHour = dailyHour,
                            dailyMinute = dailyMinute,
                            isQuickAction = isQuickAction
                        )
                        onSave(result)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("save_script_button")
            ) {
                Text("حفظ النص", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
