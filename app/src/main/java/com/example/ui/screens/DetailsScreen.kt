package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Challenge
import com.example.ui.viewmodel.ChallengeViewModel
import com.example.utils.StatsEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    challengeId: Int,
    viewModel: ChallengeViewModel,
    onNavigateBack: () -> Unit
) {
    val activeChallenges by viewModel.activeChallengesState.collectAsState()
    val archivedChallenges by viewModel.archivedChallengesState.collectAsState()
    val completions by viewModel.allCompletionRecords.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Find challenge in active or archived
    val challenge = remember(activeChallenges, archivedChallenges, challengeId) {
        (activeChallenges + archivedChallenges).firstOrNull { it.id == challengeId }
    }

    if (challenge == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Challenge details not found.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val challengeCompletions = remember(completions, challengeId) {
        completions.filter { it.challengeId == challengeId }
    }

    val stats = remember(challenge, challengeCompletions) {
        StatsEngine.calculateStats(challenge, challengeCompletions)
    }

    val themeColor = remember(challenge.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(challenge.colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit Challenge?") },
            text = { Text("This is permanent and will wipe all completion statistics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChallenge(challenge)
                        showDeleteConfirm = false
                        onNavigateBack()
                        Toast.makeText(context, "Challenge deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Challenge Insights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.toggleArchiveChallenge(challenge)
                            val word = if (challenge.active) "Archived" else "Restored"
                            Toast.makeText(context, "Challenge $word", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("archive_button")
                    ) {
                        Icon(
                            imageVector = if (challenge.active) Icons.Default.Archive else Icons.Default.Unarchive,
                            contentDescription = "Archive Toggle",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(themeColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(challenge.category),
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = challenge.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Category: ${challenge.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (challenge.description.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = challenge.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Today's Status Card & Interactive Button
            val todayCompletions by viewModel.todayCompletedState.collectAsState()
            val isCompletedToday = todayCompletions[challengeId] ?: false

            Button(
                onClick = { viewModel.toggleTodayCompletion(challengeId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("details_toggle_complete"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompletedToday) Color(0xFF10B981) else themeColor,
                    contentColor = if (isCompletedToday) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isCompletedToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isCompletedToday) "Completed Today! (Tap to Undo)" else "Mark Today as Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Streaks Panel Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatMetricCard(title = "🔥 Current Streak", value = "${stats.currentStreak} Days", subtitle = "Steady progress")
                }
                Column(modifier = Modifier.weight(1f)) {
                    StatMetricCard(title = "🏆 Longest Streak", value = "${stats.longestStreak} Days", subtitle = "All-time record")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatMetricCard(title = "📈 Target Duration", value = "${challenge.targetDays} Days", subtitle = "Target threshold")
                }
                Column(modifier = Modifier.weight(1f)) {
                    val pct = (stats.completionRate * 100).toInt()
                    StatMetricCard(title = "📊 Success Rate", value = "$pct%", subtitle = "Out of days active")
                }
            }

            // GitHub-Style Heatmap Contribution Graph (Grid)
            Text(
                text = "Habit Contribution Heatmap",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            HeatmapGraph(stats.monthlyHistory, themeColor)

            // Back-logging: Last 7 Days Manual Logs Toggler
            Text(
                text = "Rapid Historical Logger (Last 7 Days)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val cal = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                for (i in 0 until 7) {
                    val dateValue = cal.time
                    val dateStr = dateFormat.format(dateValue)
                    val dayLabel = dayFormat.format(dateValue)
                    val completed = stats.monthlyHistory[dateStr] == true
                    val isToday = i == 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (isToday) "Today" else dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (completed) themeColor else if (isToday) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.toggleDateCompletion(challengeId, dateStr) }
                                .testTag("hist_logger_${i}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed) {
                                Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(
                                    imageVector = if (isToday) Icons.Default.RadioButtonUnchecked else Icons.Default.Close,
                                    contentDescription = "Not log",
                                    tint = if (isToday) themeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatMetricCard(title: String, value: String, subtitle: String) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surfaceVariant
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun HeatmapGraph(history: Map<String, Boolean>, themeColor: Color) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    // Show 30 days contribution squares grouped together elegantly
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .testTag("heatmap_panel"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Days names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Heatmap history (Last 30 Days)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Sequence of last 30 days in chronological ascending order
            val last30DaysList = remember(history) {
                val list = mutableListOf<Pair<String, Boolean>>()
                val cal = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                for (i in 0 until 35) {
                    val dateStr = dateFormat.format(cal.time)
                    list.add(0, Pair(dateStr, history[dateStr] == true))
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                list
            }

            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(last30DaysList) { dayInfo ->
                    val isCompleted = dayInfo.second
                    val isToday = dayInfo.first == todayStr

                    val boxBg = when {
                        isCompleted -> themeColor
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> if (isDark) Color(0xFF101011) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    }

                    val borderModifier = if (isToday && !isCompleted) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(boxBg)
                            .then(borderModifier)
                            .testTag("day_square_${dayInfo.first}")
                    ) {
                        val dayNum = remember(dayInfo.first) {
                            try {
                                dayInfo.first.split("-").last().toInt().toString()
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        Text(
                            text = dayNum,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Missed ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF101011) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Today ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Completed ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(themeColor))
            }
        }
    }
}
