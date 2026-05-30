package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChallengeViewModel
import com.example.ui.viewmodel.UiAchievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: ChallengeViewModel) {
    val achievements by viewModel.getAchievementsFlow().collectAsState(initial = emptyList())

    val totalUnlocked = achievements.count { it.unlocked }
    val progressPercent = if (achievements.isNotEmpty()) (totalUnlocked.toFloat() / achievements.size) else 0f

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Milestones & Badges",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                .padding(horizontal = 16.dp)
        ) {
            // General Milestones Summary card
            val isDark = MaterialTheme.colorScheme.background.red < 0.1f
            val summaryBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surfaceVariant
            val summaryBorder = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, summaryBorder, RoundedCornerShape(16.dp))
                    .testTag("achievements_summary"),
                colors = CardDefaults.cardColors(
                    containerColor = summaryBg
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievements Trophy",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level Completed: $totalUnlocked / ${achievements.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFF59E0B),
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            Text(
                text = "My Showcases",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Grid Layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("achievements_grid"),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(achievements, key = { it.id }) { achievement ->
                    AchievementGridCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementGridCard(achievement: UiAchievement) {
    val glowColor = if (achievement.unlocked) Color(0xFFF59E0B) else Color.Gray
    val contentAlpha = if (achievement.unlocked) 1f else 0.4f

    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val baseCardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val lockedCardBg = if (isDark) Color(0xFF101011) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    val borderCol = if (achievement.unlocked) {
        glowColor.copy(alpha = 0.6f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_card_${achievement.id}")
            .border(
                width = 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) baseCardBg else lockedCardBg
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Badge Circle
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (achievement.unlocked) glowColor.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAchievementIcon(achievement.iconName),
                    contentDescription = null,
                    tint = if (achievement.unlocked) glowColor else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Info
            Text(
                text = achievement.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (achievement.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = achievement.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier.height(26.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress towards unlocked achievement
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { achievement.progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = glowColor,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (achievement.unlocked) "✦ Unlocked ✦" else achievement.targetText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.unlocked) glowColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

fun getAchievementIcon(name: String): ImageVector {
    return when(name) {
        "star" -> Icons.Default.Star
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "military_tech" -> Icons.Default.MilitaryTech
        "emoji_events" -> Icons.Default.EmojiEvents
        "layers" -> Icons.Default.Layers
        else -> Icons.Default.Verified
    }
}
