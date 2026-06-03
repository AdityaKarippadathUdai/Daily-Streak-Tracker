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

data class OverallStreakStats(
    val activeDaysCount: Int,         // Total days with at least one completion
    val currentActiveStreak: Int,     // Consecutive days with at least 1 completion (ending today or yesterday)
    val longestActiveStreak: Int,     // Best historical active streak
    val perfectDaysCount: Int,        // Total days with ALL active habits completed
    val currentPerfectStreak: Int,    // Consecutive days with ALL active habits completed
    val longestPerfectStreak: Int,    // Best historical perfect days streak
    val todayCompletedCount: Int,     // Number of habits completed today
    val todayTotalCount: Int          // Total number of active habits today
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

    fun calculateOverallStreakDetails(
        challenges: List<Challenge>,
        completions: List<CompletionRecord>
    ): OverallStreakStats {
        val activeChallenges = challenges.filter { it.active }
        val allCompletedRecords = completions.filter { it.completed }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Group completions by date (yyyy-MM-dd)
        val completionsByDate = allCompletedRecords.groupBy { it.date }
        
        // Let's find dates where AT LEAST ONE challenge was completed
        val datesWithAtLeastOne = completionsByDate.keys.toSet()
        val totalActiveDays = datesWithAtLeastOne.size
        
        // Let's find dates where ALL active challenges were completed
        val perfectDates = mutableSetOf<String>()
        val activeCount = activeChallenges.size
        if (activeCount > 0) {
            completionsByDate.forEach { (date, recordsForDate) ->
                // Check how many of the ACTIVE challenges are completed on this date
                val activeCompletedOnDate = recordsForDate.map { it.challengeId }.intersect(activeChallenges.map { it.id }.toSet())
                if (activeCompletedOnDate.size >= activeCount) {
                    perfectDates.add(date)
                }
            }
        }
        val totalPerfectDays = perfectDates.size

        // Calculate streaks for AT LEAST ONE completion
        val (currentActiveStreak, longestActiveStreak) = calculateStreaksForDates(datesWithAtLeastOne)

        // Calculate streaks for PERFECT days
        val (currentPerfectStreak, longestPerfectStreak) = calculateStreaksForDates(perfectDates)

        // Today's status
        val todayStr = dateFormat.format(Date())
        val todayRecords = completionsByDate[todayStr]?.map { it.challengeId }?.intersect(activeChallenges.map { it.id }.toSet()) ?: emptySet()
        
        return OverallStreakStats(
            activeDaysCount = totalActiveDays,
            currentActiveStreak = currentActiveStreak,
            longestActiveStreak = longestActiveStreak,
            perfectDaysCount = totalPerfectDays,
            currentPerfectStreak = currentPerfectStreak,
            longestPerfectStreak = longestPerfectStreak,
            todayCompletedCount = todayRecords.size,
            todayTotalCount = activeCount
        )
    }

    private fun calculateStreaksForDates(dateStrings: Set<String>): Pair<Int, Int> {
        if (dateStrings.isEmpty()) return Pair(0, 0)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = dateStrings.mapNotNull {
            try { dateFormat.parse(it) } catch (e: Exception) { null }
        }.sorted()

        if (dates.isEmpty()) return Pair(0, 0)

        // Part 1: Historical Longest Streak
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: Date? = null
        val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

        for (date in dates) {
            if (previousDate == null) {
                tempStreak = 1
            } else {
                val diff = date.time - previousDate.time
                val daysDiff = Math.round(diff.toDouble() / ONE_DAY_MILLIS).toInt()

                if (daysDiff <= 1) {
                    tempStreak++
                } else {
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

        // Part 2: Current Streak
        val today = Calendar.getInstance().apply { zeroTime(this) }
        val yesterday = Calendar.getInstance().apply {
            zeroTime(this)
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val todayStr = dateFormat.format(today.time)
        val yesterdayStr = dateFormat.format(yesterday.time)

        val completedToday = dateStrings.contains(todayStr)
        val completedYesterday = dateStrings.contains(yesterdayStr)

        var currentStreak = 0
        if (completedToday || completedYesterday) {
            val activeCal = Calendar.getInstance().apply {
                time = if (completedToday) today.time else yesterday.time
            }
            while (true) {
                val activeStr = dateFormat.format(activeCal.time)
                if (dateStrings.contains(activeStr)) {
                    currentStreak++
                    activeCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        return Pair(currentStreak, maxOf(longestStreak, currentStreak))
    }
}
