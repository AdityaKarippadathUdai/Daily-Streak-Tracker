package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.data.model.CompletionRecord
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChallengeViewModel
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: ChallengeViewModel) {
    val stats by viewModel.getGlobalStatsFlow().collectAsState(
        initial = viewModel.getGlobalStatsFlow().let {
            // Provide a fast default mock or rely on initial empty state values
            com.example.ui.viewmodel.GlobalStats(0, 0, 0f, 0, emptyList(), emptyList())
        }
    )

    val completions by viewModel.allCompletionRecords.collectAsState(initial = emptyList())

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Performance Hub",
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global Overview Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnalyticsMetricCard(
                        title = "TOTAL LOGS",
                        value = "${stats.totalCompletions}",
                        icon = Icons.Default.Assessment,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    AnalyticsMetricCard(
                        title = "ACTIVE HABITS",
                        value = "${stats.totalChallenges}",
                        icon = Icons.Default.Star,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val pct = (stats.completionRateToday * 100).toInt()
                    AnalyticsMetricCard(
                        title = "TODAY RATIO",
                        value = "$pct%",
                        icon = Icons.Default.Analytics,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    AnalyticsMetricCard(
                        title = "BEST STREAK",
                        value = "${stats.bestStreak}d",
                        icon = Icons.Default.Analytics,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Dynamic Personalized Insight text
            val favoriteCategory = remember(stats.categoryDistribution) {
                stats.categoryDistribution.maxByOrNull { it.completions }?.category ?: "None"
            }
            val insightMessage = remember(stats, favoriteCategory) {
                when {
                    stats.totalCompletedTodayCount() > 0 -> {
                        "Brilliant work! You completed the challenges today. Your favorite area of growth is $favoriteCategory!"
                    }
                    stats.totalChallenges > 0 -> {
                        "Log coordinates to preserve your $stats.bestStreak-day streak. Your top-performing discipline is $favoriteCategory."
                    }
                    else -> {
                        "Initialize your performance hub! Tap '+' below to select templates or build your customized roadmap."
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Insights",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Smart Recommendation: $insightMessage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Consecutive Days Completion Overall Streak Section
            val overall = stats.overallStreakDetails
            if (overall != null) {
                Text(
                    text = "Consecutive Completion Analysis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                val isDark = MaterialTheme.colorScheme.background.red < 0.1f
                val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
                val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .testTag("overall_streak_analysis_panel"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with descriptive subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Analysis Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Consecutive Days Tracking",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Analyzing consecutive active days and perfect habit cleared days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Statistics grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Active Streak Card (at least one completions)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "ACTIVE DAYS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${overall.currentActiveStreak}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "days current",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Longest: ${overall.longestActiveStreak} days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Total Active: ${overall.activeDaysCount} days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            // Perfect Streak Card (all active completions)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFBBF24).copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "PERFECT DAYS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    letterSpacing = 1.0.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${overall.currentPerfectStreak}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "days current",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Longest: ${overall.longestPerfectStreak} days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Total Perfect: ${overall.perfectDaysCount} days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            HabitHeatmap(completions = completions)

            // Canvas Line Chart: Weekly Progress Trend curves
            Text(
                text = "Weekly Activity Volume",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            WeeklyGridChart(stats.challengeProgressList)

            // Canvas Bar Chart: Category completions distribution
            Text(
                text = "Competency Breakdown (By Category)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            CategoryCompletesChart(stats.categoryDistribution)

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// Utility extension
fun com.example.ui.viewmodel.GlobalStats.totalCompletedTodayCount(): Int {
    return challengeProgressList.count { it.stats.monthlyHistory[java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())] == true }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class WeeklyChartItem(
    val dayName: String,
    val dateStr: String,
    val completionCount: Int,
    val totalActive: Int,
    val consistencyPercent: Float, // 0.0 to 1.0
    val isToday: Boolean
)

@Composable
fun WeeklyGridChart(progressList: List<com.example.ui.viewmodel.ChallengeProgress>) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    val chartData = remember(progressList) {
        val cal = Calendar.getInstance()
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        
        // Find start of week (Monday)
        val mondayCal = Calendar.getInstance()
        val dayOfWeek = mondayCal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        mondayCal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        val list = mutableListOf<WeeklyChartItem>()
        val totalActive = progressList.size
        
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(mondayCal.time)
            var count = 0
            for (progress in progressList) {
                if (progress.stats.monthlyHistory[dateStr] == true) {
                    count++
                }
            }
            val consistency = if (totalActive > 0) (count.toFloat() / totalActive.toFloat()) else 0f
            list.add(
                WeeklyChartItem(
                    dayName = dayNames[i],
                    dateStr = dateStr,
                    completionCount = count,
                    totalActive = totalActive,
                    consistencyPercent = consistency,
                    isToday = dateStr == todayStr
                )
            )
            mondayCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    var selectedIndex by remember { 
        mutableStateOf<Int?>(
            Calendar.getInstance().let { (it.get(Calendar.DAY_OF_WEEK) + 5) % 7 }
        ) 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .testTag("weekly_chart_panel"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Recharts Header: Legend & Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consistency Curve",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap nodes to inspect details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                // Recharts Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Consistency",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                        )
                        Text(
                            text = "Target Met",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph canvas area with interactive gestures
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()
                val spacingX = if (width > 0) width / 6f else 0f
                val paddingBottom = 20f
                val paddingTop = 15f
                val verticalRange = height - paddingBottom - paddingTop
                
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (spacingX > 0) {
                                    val idx = (offset.x / spacingX).roundToInt().coerceIn(0, 6)
                                    selectedIndex = idx
                                }
                            }
                        }
                ) {
                    
                    // 1. Draw Cartesian gridlines (Recharts strokeDasharray style)
                    val horizontalLinesCount = 4
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    for (i in 0..horizontalLinesCount) {
                        val y = paddingTop + (verticalRange * (i.toFloat() / horizontalLinesCount))
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 2f,
                            pathEffect = pathEffect
                        )
                    }
                    
                    // Draw vertical guides for each day
                    for (i in 0..6) {
                        val x = i * spacingX
                        drawLine(
                            color = gridColor,
                            start = Offset(x, paddingTop),
                            end = Offset(x, height - paddingBottom),
                            strokeWidth = 2f,
                            pathEffect = pathEffect
                        )
                    }

                    // 2. Draw selected day vertical highlight column (Recharts Tooltip background overlay)
                    selectedIndex?.let { idx ->
                        val activeX = idx * spacingX
                        drawRect(
                            color = lineColor.copy(alpha = 0.04f),
                            topLeft = Offset(activeX - (spacingX / 2.2f).coerceAtLeast(10f), paddingTop),
                            size = androidx.compose.ui.geometry.Size(
                                width = (spacingX / 1.1f).coerceAtLeast(20f),
                                height = verticalRange
                            )
                        )
                        drawLine(
                            color = lineColor.copy(alpha = 0.15f),
                            start = Offset(activeX, paddingTop),
                            end = Offset(activeX, height - paddingBottom),
                            strokeWidth = 2f
                        )
                    }

                    // Map chart data to physical screen coordinate offsets
                    val points = chartData.mapIndexed { idx, item ->
                        val x = idx * spacingX
                        val y = height - paddingBottom - (item.consistencyPercent * verticalRange)
                        Offset(x, y)
                    }

                    // 3. Draw Spline Area Gradient (Recharts <Area> gradient fill)
                    if (points.isNotEmpty()) {
                        val areaPath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prevPoint = points[i - 1]
                                val currentPoint = points[i]
                                val controlPoint1 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2f, prevPoint.y)
                                val controlPoint2 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2f, currentPoint.y)
                                cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    currentPoint.x, currentPoint.y
                                )
                            }
                            // Close the area path to bottom edges for gradient rendering
                            lineTo(points.last().x, height - paddingBottom)
                            lineTo(points.first().x, height - paddingBottom)
                            close()
                        }
                        
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // 4. Draw Thick Bezier Spline Line itself
                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prevPoint = points[i - 1]
                                val currentPoint = points[i]
                                val controlPoint1 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2f, prevPoint.y)
                                val controlPoint2 = Offset(prevPoint.x + (currentPoint.x - prevPoint.x) / 2f, currentPoint.y)
                                cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    currentPoint.x, currentPoint.y
                                )
                            }
                        }
                        
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }

                    // 5. Draw Anchor Nodes with active state pulses
                    points.forEachIndexed { idx, point ->
                        val isSelected = selectedIndex == idx
                        val outerRadius = if (isSelected) 10f else 6f
                        val innerRadius = if (isSelected) 4f else 3f
                        
                        // Outer glowing ring for selection
                        if (isSelected) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.25f),
                                radius = 18f,
                                center = point
                            )
                        }

                        drawCircle(
                            color = lineColor,
                            radius = outerRadius,
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = innerRadius,
                            center = point
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X-Axis text labels aligning Mon, Tue, Wed, Thu, Fri, Sat, Sun
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                chartData.forEachIndexed { idx, item ->
                    val isSelected = selectedIndex == idx
                    Text(
                        text = item.dayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Beautiful Recharts Hover Tooltip box displaying detailed analytics!
            selectedIndex?.let { idx ->
                val selectedItem = chartData.getOrNull(idx)
                if (selectedItem != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Style the Tooltip Box like a beautiful dark translucent popover
                    Surface(
                        color = if (isDark) Color(0xFF222225) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (selectedItem.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedItem.dayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selectedItem.isToday) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "TODAY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedItem.completionCount} of ${selectedItem.totalActive} habits completed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Right side: beautiful consistency badge
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${(selectedItem.consistencyPercent * 100).toInt()}%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "CONSISTENCY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCompletesChart(distributions: List<com.example.ui.viewmodel.CategoryStat>) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val cardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .testTag("category_chart_panel"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (distributions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No statistical insights to display yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                val maxCompleteness = remember(distributions) {
                    (distributions.maxOfOrNull { it.completions } ?: 1).coerceAtLeast(1)
                }

                distributions.take(6).forEach { stat ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${stat.completions} completions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val ratio = stat.completions.toFloat() / maxCompleteness.toFloat()
                        
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = getCategoryHexColor(stat.category),
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }
    }
}

// Map color dynamically
fun getCategoryHexColor(category: String): Color {
    return when(category.lowercase()) {
        "coding" -> Color(0xFF3B82F6)
        "reading" -> Color(0xFF8B5CF6)
        "fitness" -> Color(0xFF10B981)
        "health", "water" -> Color(0xFF06B6D4)
        "photography" -> Color(0xFFEF4444)
        "meditation" -> Color(0xFFEC4899)
        "learning" -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }
}

@Composable
fun HabitHeatmap(completions: List<CompletionRecord>) {
    var isYearly by remember { mutableStateOf(false) } // false = Monthly, true = Yearly
    val isDark = MaterialTheme.colorScheme.background.red < 0.1f
    val baseCardBg = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent

    // Group completion records by date YYYY-MM-DD
    val completionsByDate = remember(completions) {
        completions
            .filter { it.completed }
            .groupBy { it.date }
            .mapValues { it.value.size }
    }

    val maxCompletionsInADay = remember(completionsByDate) {
        (completionsByDate.values.maxOrNull() ?: 1).coerceAtLeast(1)
    }

    val todayCal = Calendar.getInstance()
    val todayWeekDay = when (todayCal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val numCols = if (isYearly) 53 else 5

    var selectedDateStr by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableStateOf<Int?>(null) }

    val primaryHexColor = MaterialTheme.colorScheme.primary

    // Precalculate cells list to prevent slot table corruption by calling remember inside dynamic loops
    val heatmapCells = remember(isYearly, todayWeekDay, completionsByDate, numCols) {
        val list = mutableListOf<HeatmapCellData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (col in 0 until numCols) {
            for (row in 0 until 7) {
                val daysAgo = (numCols - 1 - col) * 7 + (todayWeekDay - row)
                if (daysAgo < 0) {
                    list.add(HeatmapCellData(col, row, daysAgo, "", 0, true))
                } else {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
                    val cellDate = dateFormat.format(c.time)
                    val count = completionsByDate[cellDate] ?: 0
                    list.add(HeatmapCellData(col, row, daysAgo, cellDate, count, false))
                }
            }
        }
        list
    }

    val groupedCells = remember(heatmapCells) {
        heatmapCells.groupBy { it.col }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .testTag("habit_heatmap_panel"),
        colors = CardDefaults.cardColors(containerColor = baseCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title and Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Habit Heatmap",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visualizing habits consistency",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Month/Year Toggle Row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF222224) else Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!isYearly) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isYearly = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Monthly",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isYearly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isYearly) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isYearly = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Yearly",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isYearly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Year selection headers (e.g. Month names "Jan", "Feb" etc.)
            val monthLabelList = remember(isYearly, todayWeekDay) {
                val labels = mutableListOf<Pair<Int, String>>()
                val cal = Calendar.getInstance()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                
                if (isYearly) {
                    for (col in 0 until 53) {
                        val daysAgo = (52 - col) * 7 + todayWeekDay
                        val colCal = Calendar.getInstance()
                        colCal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                        val monthName = monthFormat.format(colCal.time)
                        
                        if (labels.isEmpty() || labels.last().second != monthName) {
                            labels.add(Pair(col, monthName))
                        }
                    }
                } else {
                    val startCal = Calendar.getInstance()
                    startCal.add(Calendar.DAY_OF_YEAR, -28)
                    labels.add(Pair(0, monthFormat.format(startCal.time)))
                    if (startCal.get(Calendar.MONTH) != todayCal.get(Calendar.MONTH)) {
                        labels.add(Pair(3, monthFormat.format(todayCal.time)))
                    }
                }
                labels
            }

            // Grid Container
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .padding(end = 8.dp, top = 20.dp)
                        .height(115.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Mon", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text("Wed", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text("Fri", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text("Sun", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }

                // Grid itself (maybe scrollable)
                val gridContent = @Composable {
                    Column {
                        // Month Headings row
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .then(if (isYearly) Modifier.width(910.dp) else Modifier.fillMaxWidth())
                        ) {
                            monthLabelList.forEach { labelInfo ->
                                val colIdx = labelInfo.first
                                val offset = (colIdx * 17).dp
                                Text(
                                    text = labelInfo.second,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = offset)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 7 Rows, grouped in columns
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until numCols) {
                                val colCells = groupedCells[col] ?: emptyList()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    colCells.forEach { cell ->
                                        if (cell.isPlaceholder) {
                                            Box(
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color.Transparent)
                                            )
                                        } else {
                                            val intensityFraction = if (maxCompletionsInADay > 0) cell.count.toFloat() / maxCompletionsInADay else 0f
                                            val cellColor = when {
                                                cell.count == 0 -> if (isDark) Color(0xFF222224) else Color(0xFFE2E8F0)
                                                intensityFraction <= 0.25f -> primaryHexColor.copy(alpha = 0.25f)
                                                intensityFraction <= 0.50f -> primaryHexColor.copy(alpha = 0.5f)
                                                intensityFraction <= 0.75f -> primaryHexColor.copy(alpha = 0.75f)
                                                else -> primaryHexColor
                                            }

                                            val isSelected = selectedDateStr == cell.dateStr
                                            val borderModifier = if (isSelected) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
                                            } else {
                                                Modifier
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(cellColor)
                                                    .then(borderModifier)
                                                    .clickable {
                                                        selectedDateStr = cell.dateStr
                                                        selectedCount = cell.count
                                                    }
                                                    .testTag("heatmap_cell_${cell.dateStr}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isYearly) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp)
                    ) {
                        gridContent()
                    }
                } else {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        gridContent()
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Legend and selection stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info / selection text
                if (selectedDateStr != null) {
                    val formattedSelectedDate = remember(selectedDateStr) {
                        try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateStr!!)
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(parsed!!)
                        } catch (e: Exception) {
                            selectedDateStr!!
                        }
                    }
                    Text(
                        text = "📅 $formattedSelectedDate: $selectedCount completed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "💡 Tap any box to inspect count",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Legend Less -> More
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Less", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF222224) else Color(0xFFE2E8F0)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryHexColor.copy(alpha = 0.25f)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryHexColor.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryHexColor.copy(alpha = 0.75f)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryHexColor))
                    Text("More", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

data class HeatmapCellData(
    val col: Int,
    val row: Int,
    val daysAgo: Int,
    val dateStr: String,
    val count: Int,
    val isPlaceholder: Boolean
)

