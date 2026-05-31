package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: ChallengeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf<Date>(Date(System.currentTimeMillis() + 86400000)) } // default: tomorrow
    var titleError by remember { mutableStateOf<String?>(null) }
    var reminderTime by remember { mutableStateOf<String?>(null) } // HH:mm format, null if no reminder

    val formattedDeadline = remember(deadlineDate) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(deadlineDate)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Create Simple Task",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = null
                },
                label = { Text("Task Title") },
                placeholder = { Text("e.g. Finish chemistry project") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Add any specific notes or requirements...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input"),
                maxLines = 4
            )

            // Deadline Date Picker Panel
            Column {
                Text(
                    text = "Completion Deadline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isDark = MaterialTheme.colorScheme.background.red < 0.1f
                val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surfaceVariant
                val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

                Card(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.time = deadlineDate
                        val currentYear = calendar.get(Calendar.YEAR)
                        val currentMonth = calendar.get(Calendar.MONTH)
                        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(Calendar.YEAR, year)
                                selectedCal.set(Calendar.MONTH, month)
                                selectedCal.set(Calendar.DAY_OF_MONTH, day)
                                // Standard end of that day as deadline
                                selectedCal.set(Calendar.HOUR_OF_DAY, 23)
                                selectedCal.set(Calendar.MINUTE, 59)
                                selectedCal.set(Calendar.SECOND, 59)
                                deadlineDate = selectedCal.time
                            },
                            currentYear,
                            currentMonth,
                            currentDay
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .testTag("task_deadline_card"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Deadline Calendar Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Complete before $formattedDeadline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Daily Task Reminder Picker
            Column {
                Text(
                    text = "Daily Reminder Time",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isDark = MaterialTheme.colorScheme.background.red < 0.1f
                val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surfaceVariant
                val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

                Card(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMin = calendar.get(Calendar.MINUTE)
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val timeStr = String.format("%02d:%02d", hour, minute)
                                reminderTime = timeStr
                            },
                            currentHour,
                            currentMin,
                            true
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .testTag("task_reminder_time_card"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (reminderTime != null) Icons.Default.NotificationsActive else Icons.Default.AccessTime,
                                contentDescription = "Reminder Icon",
                                tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (reminderTime != null) "Remind me daily at $reminderTime" else "No reminder alarm configured",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (reminderTime != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        if (reminderTime != null) {
                            TextButton(
                                onClick = { reminderTime = null },
                                modifier = Modifier.testTag("clear_task_reminder_button")
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = "Set Time",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Launch Task Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Please input a valid task title"
                    } else {
                        viewModel.createTask(
                            title = title.trim(),
                            description = description.trim(),
                            deadline = deadlineDate.time,
                            reminderTime = reminderTime
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button")
            ) {
                Text(
                    text = "Add Task",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
