package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Challenge
import com.example.data.model.ChallengeTemplate
import com.example.data.model.ChallengeTemplates
import com.example.data.model.CompletionRecord
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.viewmodel.ChallengeViewModel
import com.example.utils.StatsEngine
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChallengeViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToCreateTask: () -> Unit
) {
    val activeChallenges by viewModel.activeChallengesState.collectAsState()
    val completions by viewModel.allCompletionRecords.collectAsState(initial = emptyList())
    val todayCompletions by viewModel.todayCompletedState.collectAsState()
    val allTasks by viewModel.allTasksState.collectAsState()
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf("habits") } // "habits" or "tasks"
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Coding", "Reading", "Fitness", "Health", "Photography", "Meditation", "Learning")

    var selectedTaskFilter by remember { mutableStateOf("All") } // "All", "Pending", "Completed", "Overdue"
    val taskFilters = listOf("All", "Pending", "Completed", "Overdue")

    val filteredChallenges = remember(activeChallenges, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            activeChallenges
        } else {
            activeChallenges.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    val filteredTasks = remember(allTasks, selectedTaskFilter) {
        val now = System.currentTimeMillis()
        when (selectedTaskFilter) {
            "Pending" -> allTasks.filter { !it.completed && it.deadline >= now }
            "Completed" -> allTasks.filter { it.completed }
            "Overdue" -> allTasks.filter { !it.completed && it.deadline < now }
            else -> allTasks
        }
    }

    // Compute Streak progress and summaries
    val totalActiveCount = activeChallenges.size
    val totalCompletedCountToday = activeChallenges.count { todayCompletions[it.id] == true }
    val progressToday = if (totalActiveCount > 0) totalCompletedCountToday.toFloat() / totalActiveCount else 0F

    // Overall Streak: Maximum current streak among all active challenges
    val bestCurrentStreak = remember(activeChallenges, completions) {
        if (activeChallenges.isEmpty()) 0
        else {
            activeChallenges.maxOfOrNull { challenge ->
                val comp = completions.filter { it.challengeId == challenge.id }
                StatsEngine.calculateStats(challenge, comp).currentStreak
            } ?: 0
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Column {
                            val todayDateStr = remember {
                                val cal = Calendar.getInstance()
                                val sdf = java.text.SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                                sdf.format(cal.time)
                            }
                            Text(
                                text = todayDateStr.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA),
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = if (currentTab == "habits") "Daily Challenges" else "Scheduled Tasks",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                                .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentTab == "habits") {
                        onNavigateToCreate()
                    } else {
                        onNavigateToCreateTask()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_item_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (currentTab == "habits") "Create Challenge" else "Create Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val templates = remember(selectedCategoryFilter) {
            if (selectedCategoryFilter == "All") {
                ChallengeTemplates.templates
            } else {
                ChallengeTemplates.templates.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Segment Tab Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = if (MaterialTheme.colorScheme.background.red < 0.1f) Color(0xFF161618) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Habits Tab
                val isHabits = currentTab == "habits"
                val habitsBg = if (isHabits) MaterialTheme.colorScheme.primary else Color.Transparent
                val habitsText = if (isHabits) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(habitsBg)
                        .clickable { currentTab = "habits" }
                        .testTag("tab_toggle_habits"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Habit Challenges",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = habitsText
                    )
                }

                // Tasks Tab
                val isTasks = currentTab == "tasks"
                val tasksBg = if (isTasks) MaterialTheme.colorScheme.primary else Color.Transparent
                val tasksText = if (isTasks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tasksBg)
                        .clickable { currentTab = "tasks" }
                        .testTag("tab_toggle_tasks"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "One-off Tasks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tasksText
                    )
                }
            }

            if (currentTab == "habits") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_scrollable_container"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top spacer
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 1. Stats Panel
                    item {
                        StreakHeaderCard(
                            currentStreak = bestCurrentStreak,
                            completedTodayCount = totalCompletedCountToday,
                            totalChallengesCount = totalActiveCount,
                            progressFraction = progressToday
                        )
                    }

                    // 2. Categories Filter Chips
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = (selectedCategoryFilter == category),
                                    onClick = { selectedCategoryFilter = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("chip_$category")
                                )
                            }
                        }
                    }

                    // 3. Challenges or Empty state
                    if (filteredChallenges.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .testTag("empty_onboarding_container"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = "No Challenges",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Step Up to the Challenge!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Create a custom habit challenge or start instantly with one of the popular templates below:",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Recommended Templates",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }

                        items(templates) { template ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addChallengeFromTemplate(template)
                                            Toast.makeText(context, "Added challenge: ${template.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("template_${template.title.replace(" ", "_")}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val templateColor = remember {
                                            try {
                                                Color(android.graphics.Color.parseColor(template.colorHex))
                                            } catch (e: Exception) {
                                                Color(0xFF3B82F6)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(templateColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(template.category),
                                                contentDescription = null,
                                                tint = templateColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = template.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = template.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Add Template",
                                            tint = templateColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    } else {
                        items(filteredChallenges, key = { it.id }) { challenge ->
                            val challengeCompletions = completions.filter { it.challengeId == challenge.id }
                            val isTodayCompleted = todayCompletions[challenge.id] ?: false
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                SwipeableChallengeCard(
                                    challenge = challenge,
                                    completions = challengeCompletions,
                                    isCompletedToday = isTodayCompleted,
                                    onCardClick = { onNavigateToDetails(challenge.id) },
                                    onToggleComplete = { viewModel.toggleTodayCompletion(challenge.id) }
                                )
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                MiniHeatmapView(completions = completions)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            } else {
                // One-off Tasks Tab Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tasks_scrollable_container"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TaskHeaderCard(
                            totalTasks = allTasks.size,
                            completedTasks = allTasks.count { it.completed }
                        )
                    }

                    // Task filter chips Row
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(taskFilters) { filter ->
                                FilterChip(
                                    selected = (selectedTaskFilter == filter),
                                    onClick = { selectedTaskFilter = filter },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("task_chip_$filter")
                                )
                            }
                        }
                    }

                    if (filteredTasks.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 40.dp)
                                    .testTag("empty_tasks_container"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "No Tasks",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All Clear!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedTaskFilter == "All") 
                                        "No tasks have been added yet. Click the + button to launch your first targeted task."
                                        else "No tasks found matching high-priority filter: $selectedTaskFilter.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                TaskItemCard(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StreakHeaderCard(
    currentStreak: Int,
    completedTodayCount: Int,
    totalChallengesCount: Int,
    progressFraction: Float
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val bgModifier = if (isDark) {
        Modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(bgModifier)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CURRENT STREAK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$currentStreak",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "DAYS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD)
                    )
                    Text(
                        text = "🔥",
                        fontSize = 24.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "COMPLETION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    completions: List<CompletionRecord>,
    isCompletedToday: Boolean,
    onCardClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val stats = remember(challenge, completions) {
        StatsEngine.calculateStats(challenge, completions)
    }

    val themeColor = remember(challenge.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(challenge.colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
    val opValue = if (isCompletedToday) 0.8f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .graphicsLayer { alpha = opValue }
            .testTag("challenge_card_${challenge.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = themeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = themeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(challenge.category),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = challenge.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (challenge.description.isNotBlank()) {
                    Text(
                        text = challenge.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    val progressFraction = if (challenge.targetDays > 0) stats.totalCompletions.toFloat() / challenge.targetDays else 0f
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(themeColor)
                        )
                    }
                    
                    Text(
                        text = "${stats.totalCompletions}/${challenge.targetDays}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = themeColor
                    )
                    
                    if (isCompletedToday) {
                        Text(
                            text = "REACHED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color(0xFF10B981)
                        )
                    } else if (challenge.reminderTime != null) {
                        Text(
                            text = challenge.reminderTime,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("complete_toggle_${challenge.id}")
            ) {
                if (isCompletedToday) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed Today",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(2.dp, themeColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(themeColor, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// Utility mapper function
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "coding" -> Icons.Default.Code
        "reading" -> Icons.Default.MenuBook
        "fitness" -> Icons.Default.DirectionsRun
        "gym" -> Icons.Default.FitnessCenter
        "health", "water" -> Icons.Default.Favorite
        "photography" -> Icons.Default.CameraAlt
        "meditation" -> Icons.Default.SelfImprovement
        "learning" -> Icons.Default.Lightbulb
        else -> Icons.Default.Stars
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "coding" -> "💻"
        "reading" -> "📖"
        "fitness" -> "🏃"
        "gym" -> "🏋️"
        "health", "water" -> "💧"
        "photography" -> "📷"
        "meditation" -> "🧘"
        "learning" -> "💡"
        else -> "⭐️"
    }
}

@Composable
fun MiniHeatmapView(completions: List<CompletionRecord>) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardColor = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(11.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAST 4 WEEKS ACTIVITY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
                )
                Text(
                    text = "+14% vs Last Month",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            val last28DaysFlags = remember(completions) {
                val cal = Calendar.getInstance()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dates = mutableListOf<String>()
                for (i in 0 until 28) {
                    dates.add(0, dateFormat.format(cal.time))
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                
                dates.map { dateStr ->
                    completions.any { it.date == dateStr }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (week in 0 until 4) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (day in 0 until 7) {
                            val index = week * 7 + day
                            val isCompleted = if (index < last28DaysFlags.size) last28DaysFlags[index] else false
                            val baseColor = Color(0xFF3B82F6)
                            
                            val tileColor = when {
                                isCompleted -> baseColor
                                (index % 5 == 0 && completions.isEmpty()) -> baseColor.copy(alpha = 0.6f)
                                (index % 7 == 3 && completions.isEmpty()) -> baseColor.copy(alpha = 0.3f)
                                (index % 9 == 1 && completions.isEmpty()) -> baseColor.copy(alpha = 0.8f)
                                else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(tileColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableChallengeCard(
    challenge: Challenge,
    completions: List<CompletionRecord>,
    isCompletedToday: Boolean,
    onCardClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!isCompletedToday) {
                        onToggleComplete()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isCompletedToday) {
                        onToggleComplete()
                    }
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (!isCompletedToday) Color(0xFF10B981) else Color.Transparent
                SwipeToDismissBoxValue.EndToStart -> if (isCompletedToday) Color(0xFFEF4444) else Color.Transparent
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (!isCompletedToday) Icons.Default.Check else null
                SwipeToDismissBoxValue.EndToStart -> if (isCompletedToday) Icons.Default.Undo else null
                else -> null
            }
            val text = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (!isCompletedToday) "Complete" else ""
                SwipeToDismissBoxValue.EndToStart -> if (isCompletedToday) "Undo" else ""
                else -> ""
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(icon, contentDescription = text, tint = Color.Black)
                            Text(text, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                        } else {
                            Text(text, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Icon(icon, contentDescription = text, tint = Color.White)
                        }
                    }
                }
            }
        },
        content = {
            ChallengeCard(
                challenge = challenge,
                completions = completions,
                isCompletedToday = isCompletedToday,
                onCardClick = onCardClick,
                onToggleComplete = onToggleComplete
            )
        },
        modifier = modifier
    )
}

@Composable
fun TaskHeaderCard(
    totalTasks: Int,
    completedTasks: Int
) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("task_header_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.background(cardBg).padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "TASK PIPELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalTasks == 0) "No tasks yet!" else "$completedTasks of $totalTasks Completed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (totalTasks == 0) "Create a task with a deadline below to start tracking!" 
                               else "Focus and cross these items off before their deadlines hit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB),
                        trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                        strokeWidth = 6.dp,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: com.example.data.model.Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF121214) else MaterialTheme.colorScheme.surface
    val borderCol = if (task.completed) {
        Color(0xFF10B981).copy(alpha = 0.3f)
    } else {
        if (System.currentTimeMillis() > task.deadline) Color(0xFFEF4444).copy(alpha = 0.3f)
        else if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    }

    val stateColor = if (task.completed) {
        Color(0xFF10B981)
    } else {
        if (System.currentTimeMillis() > task.deadline) Color(0xFFEF4444)
        else MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom check target checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(stateColor.copy(alpha = 0.1f))
                    .border(2.dp, stateColor, CircleShape)
                    .clickable { onToggleComplete() }
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed Check icon",
                        tint = stateColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (task.completed) 0.6f else 1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Deadline text
                val sdfValue = remember { java.text.SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", java.util.Locale.getDefault()) }
                val deadlineStr = remember(task.deadline) { sdfValue.format(java.util.Date(task.deadline)) }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val now = System.currentTimeMillis()
                    val isOverdue = !task.completed && now > task.deadline
                    
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else if (isOverdue) Icons.Default.Warning else Icons.Default.AccessTime,
                        contentDescription = "Status Icon",
                        tint = stateColor,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (task.completed) {
                            "Completed on ${sdfValue.format(java.util.Date(task.completedAt ?: task.createdAt))}"
                        } else {
                            if (isOverdue) "Overdue (deadline was $deadlineStr)" else "Deadline: $deadlineStr"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stateColor
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("task_delete_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
