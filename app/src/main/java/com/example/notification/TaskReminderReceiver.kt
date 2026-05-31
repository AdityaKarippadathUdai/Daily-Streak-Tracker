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

class TaskReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "scheduled_task_reminders"
        const val CHANNEL_NAME = "Scheduled Task Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("taskId", -1)
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Your Task"

        if (taskId == -1) return

        // 1. Post local notification
        showNotification(context, taskId, taskTitle)

        // 2. Reschedule notification for tomorrow if not completed
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val task = db.taskDao().getTaskById(taskId)
            if (task != null && !task.completed && task.reminderTime != null) {
                AlarmScheduler.scheduleTaskReminder(context, task)
            }
        }
    }

    private fun showNotification(context: Context, taskId: Int, taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android Oreo (8.0) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to complete scheduled tasks before they expire."
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
        val pendingIntent = PendingIntent.getActivity(context, taskId + 100000, clickIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System alarm drawable
            .setContentTitle("⏰ Task Reminder")
            .setContentText("Don't forget to complete your task: $taskTitle!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        notificationManager.notify(taskId + 100000, notification)
    }
}
