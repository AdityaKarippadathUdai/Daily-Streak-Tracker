package com.example.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.Challenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitsWidgetFactory(applicationContext)
    }
}

class HabitsWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var habitsList: List<Challenge> = emptyList()
    private var completionMap: Map<Int, Boolean> = emptyMap()

    override fun onCreate() {
        // Initialization
    }

    override fun onDestroy() {
        // Cleanup
    }

    override fun getCount(): Int {
        return habitsList.size
    }

    // Called on a background binder thread - perfect for fetching SQLite entries synchronously using runBlocking
    override fun onDataSetChanged() {
        try {
            runBlocking(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)
                // Filter only active habits
                val allChallenges = db.challengeDao().getAllChallenges().first()
                habitsList = allChallenges.filter { it.active }

                // Get completions for today
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val map = mutableMapOf<Int, Boolean>()
                for (habit in habitsList) {
                    val rec = db.completionRecordDao().getCompletionForChallengeAndDate(habit.id, todayStr)
                    map[habit.id] = (rec != null)
                }
                completionMap = map
            }
        } catch (e: Exception) {
            Log.e("HabitsWidgetFactory", "Error updating widget list dataset: ", e)
        }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= habitsList.size) return null

        val habit = habitsList[position]
        val completed = completionMap[habit.id] == true

        // Inflate custom card layout
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.widget_item_title, habit.title)
        views.setTextViewText(R.id.widget_item_category, habit.category.uppercase(Locale.getDefault()))

        // Parse custom brand hex color safely
        val categoryColor = try {
            val validatedHex = if (habit.colorHex.startsWith("#")) habit.colorHex else "#${habit.colorHex}"
            Color.parseColor(validatedHex)
        } catch (e: Exception) {
            Color.parseColor("#60A5FA") // Default neon blue fallback
        }
        views.setTextColor(R.id.widget_item_category, categoryColor)

        // Select correct checkbox image source
        val iconRes = if (completed) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
        views.setImageViewResource(R.id.widget_item_checkbox, iconRes)

        // Set up the click PendingIntent template fill-in values
        val fillInIntent = Intent().apply {
            putExtra(HabitsWidgetProvider.EXTRA_CHALLENGE_ID, habit.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null // Standard loading view
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return if (position in habitsList.indices) habitsList[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
