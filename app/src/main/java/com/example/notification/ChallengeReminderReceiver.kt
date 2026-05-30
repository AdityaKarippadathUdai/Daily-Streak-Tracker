package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "daily_challenge_reminders"
        const val CHANNEL_NAME = "Daily Challenge Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val challengeId = intent.getIntExtra("challengeId", -1)
        val challengeTitle = intent.getStringExtra("challengeTitle") ?: "Your Challenge"

        if (challengeId == -1) return

        // 1. Post local notification
        showNotification(context, challengeId, challengeTitle)

        // 2. Reschedule notification for tomorrow
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val challenge = db.challengeDao().getChallengeById(challengeId)
            if (challenge != null && challenge.active && challenge.reminderTime != null) {
                AlarmScheduler.scheduleReminder(context, challenge)
            }
        }
    }

    private fun showNotification(context: Context, challengeId: Int, challengeTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android Oreo (8.0) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily alarms reminding you to log progress and maintain streaks."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open MainActivity when clicked
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, challengeId, clickIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System alarm drawable
            .setContentTitle("🔥 Don't Break Your Streak")
            .setContentText("Complete your $challengeTitle challenge today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        notificationManager.notify(challengeId, notification)
    }
}
