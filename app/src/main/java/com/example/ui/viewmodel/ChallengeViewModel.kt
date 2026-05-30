package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Challenge
import com.example.data.model.ChallengeTemplate
import com.example.data.model.CompletionRecord
import com.example.data.repository.ChallengeRepository
import com.example.notification.AlarmScheduler
import com.example.utils.ChallengeStats
import com.example.utils.StatsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChallengeViewModel"
    private val repository: ChallengeRepository
    private val sharedPrefs: SharedPreferences =
        application.getSharedPreferences("daily_challenge_prefs", Context.MODE_PRIVATE)

    // Flow of all challenges and records
    val allChallenges: Flow<List<Challenge>>
    val allCompletionRecords: Flow<List<CompletionRecord>>

    // Live combined UI states
    val activeChallengesState: StateFlow<List<Challenge>>
    val archivedChallengesState: StateFlow<List<Challenge>>
    val todayCompletedState: StateFlow<Map<Int, Boolean>> // challengeId -> completed today
    val completionsListState: StateFlow<List<CompletionRecord>>

    // Theme state: "light", "dark", "system"
    private val _themeState = MutableStateFlow(sharedPrefs.getString("theme", "system") ?: "system")
    val themeState: StateFlow<String> = _themeState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChallengeRepository(database.challengeDao(), database.completionRecordDao())

        allChallenges = repository.allChallenges
        allCompletionRecords = repository.allCompletionRecords

        // Active challenges
        activeChallengesState = allChallenges.map { list ->
            list.filter { it.active }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Archived challenges
        archivedChallengesState = allChallenges.map { list ->
            list.filter { !it.active }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Map of challengeId -> today completion status
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        todayCompletedState = allCompletionRecords.map { list ->
            list.filter { it.date == todayStr && it.completed }
                .associate { it.challengeId to true }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        completionsListState = allCompletionRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun setTheme(theme: String) {
        sharedPrefs.edit().putString("theme", theme).apply()
        _themeState.value = theme
    }

    // Toggle today's completion
    fun toggleTodayCompletion(challengeId: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModelScope.launch(Dispatchers.IO) {
            val marked = repository.toggleCompletion(challengeId, todayStr)
            Log.d(TAG, "Completion for $challengeId toggled to $marked for date $todayStr")
        }
    }

    // Toggle specific historical date completion
    fun toggleDateCompletion(challengeId: Int, dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCompletion(challengeId, dateStr)
        }
    }

    // Insert new challenge
    fun createChallenge(
        title: String,
        description: String,
        category: String,
        iconName: String,
        colorHex: String,
        reminderTime: String?,
        targetDays: Int,
        startDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val challenge = Challenge(
                title = title,
                description = description,
                category = category,
                iconName = iconName,
                colorHex = colorHex,
                reminderTime = reminderTime,
                targetDays = targetDays,
                startDate = startDate,
                endDate = null,
                active = true
            )
            val newId = repository.insertChallenge(challenge)
            val updatedChallenge = challenge.copy(id = newId.toInt())

            // Schedule reminder notification after DB write
            if (reminderTime != null) {
                AlarmScheduler.scheduleReminder(getApplication(), updatedChallenge)
            }
        }
    }

    // Add challenge from Template
    fun addChallengeFromTemplate(template: ChallengeTemplate) {
        createChallenge(
            title = template.title,
            description = template.description,
            category = template.category,
            iconName = template.iconName,
            colorHex = template.colorHex,
            reminderTime = template.reminderTime,
            targetDays = template.targetDays
        )
    }

    // Update challenge
    fun updateChallenge(challenge: Challenge) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChallenge(challenge)
            
            // Reschedule notification
            if (challenge.active && challenge.reminderTime != null) {
                AlarmScheduler.scheduleReminder(getApplication(), challenge)
            } else {
                AlarmScheduler.cancelReminder(getApplication(), challenge.id)
            }
        }
    }

    // Toggle Archive status
    fun toggleArchiveChallenge(challenge: Challenge) {
        val updated = challenge.copy(active = !challenge.active, updatedAt = System.currentTimeMillis())
        updateChallenge(updated)
    }

    // Delete challenge
    fun deleteChallenge(challenge: Challenge) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelReminder(getApplication(), challenge.id)
            repository.deleteChallenge(challenge)
        }
    }

    // Get specific challenge stats in Flow
    fun getStatsForChallenge(challenge: Challenge, completions: List<CompletionRecord>): ChallengeStats {
        return StatsEngine.calculateStats(challenge, completions.filter { it.challengeId == challenge.id })
    }

    // Live global statistics
    fun getGlobalStatsFlow(): Flow<GlobalStats> {
        return combine(allChallenges, allCompletionRecords) { challenges, records ->
            val active = challenges.filter { it.active }
            val totalChallengesCount = active.size

            if (active.isEmpty()) {
                return@combine GlobalStats(0, 0, 0f, 0, emptyList(), emptyList())
            }

            var totalCompletions = 0
            var maxStreak = 0
            val challengeStreaks = mutableListOf<Int>()
            val categoryCompletions = mutableMapOf<String, Int>()

            for (challenge in active) {
                val comp = records.filter { it.challengeId == challenge.id }
                val stats = StatsEngine.calculateStats(challenge, comp)
                totalCompletions += stats.totalCompletions
                if (stats.longestStreak > maxStreak) {
                    maxStreak = stats.longestStreak
                }
                challengeStreaks.add(stats.currentStreak)

                val originalCat = challenge.category
                categoryCompletions[originalCat] = (categoryCompletions[originalCat] ?: 0) + stats.totalCompletions
            }

            val averageStreak = if (challengeStreaks.isNotEmpty()) challengeStreaks.average().toFloat() else 0f
            val activeIds = active.map { it.id }.toSet()
            val totalPossibilitiesToday = active.size
            val completedToday = records.filter { it.challengeId in activeIds && it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }.size
            val completionRateToday = if (totalPossibilitiesToday > 0) completedToday.toFloat() / totalPossibilitiesToday else 0f

            GlobalStats(
                totalChallenges = totalChallengesCount,
                totalCompletions = totalCompletions,
                completionRateToday = completionRateToday,
                bestStreak = maxStreak,
                categoryDistribution = categoryCompletions.map { CategoryStat(it.key, it.value) },
                challengeProgressList = active.map { challenge ->
                    val comp = records.filter { it.challengeId == challenge.id }
                    val stats = StatsEngine.calculateStats(challenge, comp)
                    ChallengeProgress(challenge, stats)
                }
            )
        }
    }

    // Live Achievements Flow
    fun getAchievementsFlow(): Flow<List<UiAchievement>> {
        return getGlobalStatsFlow().map { stats ->
            val activeChalCount = stats.totalChallenges
            val totalCompletions = stats.totalCompletions
            val bestStreak = stats.bestStreak

            listOf(
                UiAchievement(
                    id = "first_completion",
                    title = "First Completion",
                    description = "Take the first step. Mark your very first daily challenge as complete.",
                    iconName = "star",
                    unlocked = totalCompletions >= 1,
                    progress = if (totalCompletions >= 1) 1f else 0f,
                    targetText = "0/1"
                ),
                UiAchievement(
                    id = "streak_7",
                    title = "7 Day Streak",
                    description = "Forming a habit. Keep a streak going for seven consecutive days.",
                    iconName = "local_fire_department",
                    unlocked = bestStreak >= 7,
                    progress = (bestStreak.toFloat() / 7f).coerceAtMost(1f),
                    targetText = "$bestStreak/7 Days"
                ),
                UiAchievement(
                    id = "streak_30",
                    title = "30 Day Streak",
                    description = "Unstoppable force. Reach a month-long streak of consecutive completions.",
                    iconName = "workspace_premium",
                    unlocked = bestStreak >= 30,
                    progress = (bestStreak.toFloat() / 30f).coerceAtMost(1f),
                    targetText = "$bestStreak/30 Days"
                ),
                UiAchievement(
                    id = "completions_100",
                    title = "Centurion Master",
                    description = "Complete 100 historical records in total across your challenges.",
                    iconName = "military_tech",
                    unlocked = totalCompletions >= 100,
                    progress = (totalCompletions.toFloat() / 100f).coerceAtMost(1f),
                    targetText = "$totalCompletions/100 Logs"
                ),
                UiAchievement(
                    id = "streak_365",
                    title = "Yearlong Legend",
                    description = "Extreme dedication. Maintain an incredible 365-day streak.",
                    iconName = "emoji_events",
                    unlocked = bestStreak >= 365,
                    progress = (bestStreak.toFloat() / 365f).coerceAtMost(1f),
                    targetText = "$bestStreak/365 Days"
                ),
                UiAchievement(
                    id = "five_active",
                    title = "Multitasker Hero",
                    description = "Juggle the grind. Have five active challenges running concurrently.",
                    iconName = "layers",
                    unlocked = activeChalCount >= 5,
                    progress = (activeChalCount.toFloat() / 5f).coerceAtMost(1f),
                    targetText = "$activeChalCount/5 Active"
                )
            )
        }
    }

    // Export progress to JSON string
    fun exportBackup(): String {
        return try {
            val json = JSONObject()
            
            // We read and serialize in a blocking manner since SharedPreferences or simple lists are fast
            viewModelScope.launch(Dispatchers.IO) {
                // Background operations handled if needed, but returning string means doing it synchronously or via cached flow
            }
            
            // Get current values from flows
            val challenges = activeChallengesState.value + archivedChallengesState.value
            val completions = completionsListState.value

            val chalArray = JSONArray()
            challenges.forEach {
                val obj = JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("description", it.description)
                    put("category", it.category)
                    put("iconName", it.iconName)
                    put("colorHex", it.colorHex)
                    put("startDate", it.startDate)
                    put("endDate", it.endDate ?: JSONObject.NULL)
                    put("reminderTime", it.reminderTime ?: JSONObject.NULL)
                    put("targetDays", it.targetDays)
                    put("active", it.active)
                }
                chalArray.put(obj)
            }

            val compArray = JSONArray()
            completions.forEach {
                val obj = JSONObject().apply {
                    put("challengeId", it.challengeId)
                    put("date", it.date)
                    put("completed", it.completed)
                }
                compArray.put(obj)
            }

            json.put("challenges", chalArray)
            json.put("completions", compArray)
            json.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup", e)
            ""
        }
    }

    // Import progress from JSON string
    fun importBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val chalArray = root.getJSONArray("challenges")
            val compArray = root.getJSONArray("completions")

            viewModelScope.launch(Dispatchers.IO) {
                // Clear and schedule alarms for existing challenges
                val currentChallenges = activeChallengesState.value
                currentChallenges.forEach { AlarmScheduler.cancelReminder(getApplication(), it.id) }

                // Repopulate
                for (i in 0 until chalArray.length()) {
                    val obj = chalArray.getJSONObject(i)
                    val reminder = if (obj.isNull("reminderTime")) null else obj.getString("reminderTime")
                    val challenge = Challenge(
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        category = obj.getString("category"),
                        iconName = obj.getString("iconName"),
                        colorHex = obj.getString("colorHex"),
                        startDate = obj.getLong("startDate"),
                        endDate = if (obj.isNull("endDate")) null else obj.getLong("endDate"),
                        reminderTime = reminder,
                        targetDays = obj.getInt("targetDays"),
                        active = obj.optBoolean("active", true)
                    )
                    val newId = repository.insertChallenge(challenge)
                    if (challenge.active && reminder != null) {
                        AlarmScheduler.scheduleReminder(getApplication(), challenge.copy(id = newId.toInt()))
                    }
                }

                for (i in 0 until compArray.length()) {
                    val obj = compArray.getJSONObject(i)
                    repository.setCompletionStatus(
                        challengeId = obj.getInt("challengeId"),
                        date = obj.getString("date"),
                        completed = obj.getBoolean("completed")
                    )
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing backup", e)
            false
        }
    }

    // Wipe/reset everything
    fun resetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            val active = activeChallengesState.value
            active.forEach { AlarmScheduler.cancelReminder(getApplication(), it.id) }

            // Re-instantiate DB cleanly to clear tables or just delete manually
            val db = AppDatabase.getDatabase(getApplication())
            db.clearAllTables()
        }
    }
}

// Stats representation
data class GlobalStats(
    val totalChallenges: Int,
    val totalCompletions: Int,
    val completionRateToday: Float,
    val bestStreak: Int,
    val categoryDistribution: List<CategoryStat>,
    val challengeProgressList: List<ChallengeProgress>
)

data class CategoryStat(val category: String, val completions: Int)
data class ChallengeProgress(val challenge: Challenge, val stats: ChallengeStats)

data class UiAchievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val unlocked: Boolean,
    val progress: Float, // 0.0 to 1.0
    val targetText: String
)
