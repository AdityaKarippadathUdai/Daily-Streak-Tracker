package com.example.utils

import com.example.data.model.Challenge
import com.example.data.model.CompletionRecord
import java.text.SimpleDateFormat
import java.util.*

data class ChallengeStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val completionRate: Float, // 0.0 to 1.0
    val monthlyHistory: Map<String, Boolean>, // Key: "yyyy-MM-dd" -> Completed (true) or Missed (false)
    val weeklyProgress: List<Pair<String, Boolean>> // Days of current week
)

object StatsEngine {

    fun calculateStats(
        challenge: Challenge,
        completions: List<CompletionRecord>
    ): ChallengeStats {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val completedDatesSet = completions.map { it.date }.toSet()
        val totalCompletions = completedDatesSet.size

        // Calculate streaks
        val (currentStreak, longestStreak) = calculateStreaks(completedDatesSet)

        // Calculate completion rate based on days since start date
        val todayCalendar = Calendar.getInstance()
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = challenge.startDate
        }
        
        // Zero out HMS to do proper day calculation
        zeroTime(todayCalendar)
        zeroTime(startCalendar)

        val daysDelta = ((todayCalendar.timeInMillis - startCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
        val targetDays = if (daysDelta <= 0) 1 else daysDelta
        val completionRate = (totalCompletions.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

        // Monthly Heatmap History (last 30 days)
        val monthlyHistory = mutableMapOf<String, Boolean>()
        val tempCal = Calendar.getInstance()
        for (i in 0 until 30) {
            val dateStr = dateFormat.format(tempCal.time)
            monthlyHistory[dateStr] = completedDatesSet.contains(dateStr)
            tempCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Weekly Progress
        val weeklyProgress = mutableListOf<Pair<String, Boolean>>()
        val weekCal = Calendar.getInstance()
        // Find starting Monday or Sunday of this week (let's go back 7 days)
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(weekCal.time)
            // Label representing day of week, e.g. "Mon"
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(weekCal.time)
            weeklyProgress.add(0, Pair(dayLabel, completedDatesSet.contains(dateStr)))
            weekCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return ChallengeStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletions = totalCompletions,
            completionRate = completionRate,
            monthlyHistory = monthlyHistory,
            weeklyProgress = weeklyProgress
        )
    }

    private fun calculateStreaks(completedDates: Set<String>): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Parse and sort dates ascending
        val dates = completedDates.mapNotNull {
            try { dateFormat.parse(it) } catch (e: Exception) { null }
        }.sorted()

        if (dates.isEmpty()) return Pair(0, 0)

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: Date? = null

        val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

        // Calculate historical longest streak
        for (date in dates) {
            if (previousDate == null) {
                tempStreak = 1
            } else {
                val diff = date.time - previousDate.time
                val daysDiff = Math.round(diff.toDouble() / ONE_DAY_MILLIS).toInt()

                if (daysDiff <= 1) {
                    tempStreak++
                } else if (daysDiff > 1) {
                    if (tempStreak > longestStreak) {
                        longestStreak = tempStreak
                    }
                    tempStreak = 1
                }
            }
            previousDate = date
        }
        if (tempStreak > longestStreak) {
            longestStreak = tempStreak
        }

        // Calculate current streak backward from today
        val today = Calendar.getInstance().apply { zeroTime(this) }
        val yesterday = Calendar.getInstance().apply {
            zeroTime(this)
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val todayStr = dateFormat.format(today.time)
        val yesterdayStr = dateFormat.format(yesterday.time)

        val completedToday = completedDates.contains(todayStr)
        val completedYesterday = completedDates.contains(yesterdayStr)

        if (completedToday || completedYesterday) {
            val activeCal = Calendar.getInstance().apply {
                time = if (completedToday) today.time else yesterday.time
            }
            currentStreak = 0
            while (true) {
                val activeStr = dateFormat.format(activeCal.time)
                if (completedDates.contains(activeStr)) {
                    currentStreak++
                    activeCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else {
            currentStreak = 0
        }

        return Pair(currentStreak, maxOf(longestStreak, currentStreak))
    }

    private fun zeroTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
}
