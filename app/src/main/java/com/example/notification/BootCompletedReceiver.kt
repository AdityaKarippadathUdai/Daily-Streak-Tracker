package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val challenges = db.challengeDao().getAllChallenges().first()
                    for (challenge in challenges) {
                        if (challenge.active && challenge.reminderTime != null) {
                            AlarmScheduler.scheduleReminder(context, challenge)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
