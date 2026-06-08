package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.CompletionRecord
import com.example.utils.StatsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitsWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "HabitsWidgetProvider"
        const val ACTION_TOGGLE_HABIT = "com.example.action.TOGGLE_HABIT"
        const val EXTRA_CHALLENGE_ID = "com.example.extra.CHALLENGE_ID"

        // Helper trigger to notify all widgets that database content was mutated
        fun triggerWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HabitsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            
            // Notify list collections to clear factory caching
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
            
            // Force general providers to re-render header values
            val updateIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        
        // Load database records inside an async coroutine to prevent blocking the main broadcast thread
        coroutineScope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val challenges = database.challengeDao().getAllChallenges().first()
                val records = database.completionRecordDao().getAllCompletionRecords().first()
                
                // Calculate actual metrics using the core mathematical stats engine
                val stats = StatsEngine.calculateOverallStreakDetails(challenges, records)
                val currentStreak = stats.currentActiveStreak
                val todayCompleted = stats.todayCompletedCount
                val todayTotal = stats.todayTotalCount

                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_main)

                    // 1. Setup Header Streak Info
                    views.setTextViewText(R.id.widget_streak_text, "Streak: $currentStreak🔥")
                    
                    // Display subtitle proportion if active habits exist
                    if (todayTotal > 0) {
                        views.setTextViewText(R.id.widget_empty_view, "No active habits today!")
                        views.setViewVisibility(R.id.widget_empty_view, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                    }

                    // 2. Setup adapter for Remote ListView (pointing to HabitsWidgetService)
                    val serviceIntent = Intent(context, HabitsWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        // Destined to avoid caching provider configurations
                        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                    }
                    views.setRemoteAdapter(R.id.widget_list, serviceIntent)

                    // 3. PendingIntent for opening the Main App when tapping title/header
                    val appIntent = Intent(context, MainActivity::class.java)
                    val pendingAppIntent = PendingIntent.getActivity(
                        context, 
                        0, 
                        appIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                    )
                    views.setOnClickPendingIntent(R.id.widget_streak_text, pendingAppIntent)
                    
                    // 4. Set template PendingIntent for toggling list elements on-tap
                    val toggleIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_HABIT
                    }
                    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingToggleIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        toggleIntent,
                        flag
                    )
                    views.setPendingIntentTemplate(R.id.widget_list, pendingToggleIntent)

                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating streaks for Home Widget update: ", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_TOGGLE_HABIT) {
            val challengeId = intent.getIntExtra(EXTRA_CHALLENGE_ID, -1)
            if (challengeId != -1) {
                val coroutineScope = CoroutineScope(Dispatchers.IO)
                coroutineScope.launch {
                    try {
                        val database = AppDatabase.getDatabase(context)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        
                        // Perform the database toggle
                        val existing = database.completionRecordDao().getCompletionForChallengeAndDate(challengeId, todayStr)
                        if (existing != null) {
                            database.completionRecordDao().deleteCompletionRecord(existing)
                            Log.d(TAG, "Unmarked habit $challengeId for today via Home Widget")
                        } else {
                            val record = CompletionRecord(challengeId = challengeId, date = todayStr)
                            database.completionRecordDao().insertCompletionRecord(record)
                            Log.d(TAG, "Marked habit $challengeId complete for today via Home Widget")
                        }

                        // Fully notify and update the layout representation of all widgets
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val component = ComponentName(context, HabitsWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
                        
                        // Notify underlying list view data factory to reload
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
                        
                        // Re-trigger update of static fields like current streaks
                        onUpdate(context, appWidgetManager, appWidgetIds)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to toggle completion from Widget trigger: ", e)
                    }
                }
            }
        }
    }
}
