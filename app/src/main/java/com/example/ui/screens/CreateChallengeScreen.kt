package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChallengeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    viewModel: ChallengeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form inputs state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Coding") }
    var targetDays by remember { mutableStateOf("30") }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") } // Blue default
    var reminderTime by remember { mutableStateOf<String?>("08:00") } // HH:mm format

    // Validation errors state
    var titleError by remember { mutableStateOf<String?>(null) }
    var targetDaysError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Coding", "Reading", "Fitness", "Health", "Photography", "Meditation", "Learning")
    val colors = listOf(
        "#3B82F6", // Blue
        "#8B5CF6", // Purple
        "#10B981", // Green
        "#06B6D4", // Cyan
        "#F59E0B", // Orange
        "#EC4899", // Pink
        "#EF4444"  // Red
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Create Challenge",
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
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = null
                },
                label = { Text("Challenge Title") },
                placeholder = { Text("e.g. Read 15 Pages") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenge_title_input"),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Explain your habit parameters") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenge_description_input"),
                maxLines = 3
            )

            // Category Chips Selection
            Column {
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Scrollable categories selection row
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.take(4).forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        modifier = Modifier.testTag("chip_cat_$cat")
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.drop(4).forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        modifier = Modifier.testTag("chip_cat_$cat")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Target Days Limit Input
            OutlinedTextField(
                value = targetDays,
                onValueChange = {
                    targetDays = it
                    if (it.toIntOrNull() != null && it.toInt() > 0) targetDaysError = null
                },
                label = { Text("Target Days Duration") },
                placeholder = { Text("e.g. 30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = targetDaysError != null,
                supportingText = targetDaysError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenge_target_days_input"),
                singleLine = true
            )

            // Color Swatches Picker
            Column {
                Text(
                    text = "Color Accent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { col ->
                        val parsedColor = remember(col) { Color(android.graphics.Color.parseColor(col)) }
                        val isSelected = selectedColorHex == col
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = col }
                                .testTag("color_circle_$col")
                        )
                    }
                }
            }

            // Reminder Time Picker Row
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
                        .testTag("reminder_time_card"),
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
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Reminder Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (reminderTime != null) "Remind me daily at $reminderTime" else "No reminder alarm configured",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (reminderTime != null) {
                            TextButton(onClick = { reminderTime = null }) {
                                Text("Disable")
                            }
                        } else {
                            Text(
                                text = "Setup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    // Modern form validation
                    var hasError = false
                    if (title.isBlank()) {
                        titleError = "Please input a valid title identifier"
                        hasError = true
                    }
                    
                    val tDays = targetDays.toIntOrNull()
                    if (tDays == null || tDays <= 0) {
                        targetDaysError = "Please specify a positive number of target days (e.g. 30)"
                        hasError = true
                    }

                    if (!hasError) {
                        viewModel.createChallenge(
                            title = title.trim(),
                            description = description.trim(),
                            category = selectedCategory,
                            iconName = selectedCategory.lowercase(),
                            colorHex = selectedColorHex,
                            reminderTime = reminderTime,
                            targetDays = tDays!!
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_challenge_button")
            ) {
                Text(
                    text = "Launch Challenge",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
