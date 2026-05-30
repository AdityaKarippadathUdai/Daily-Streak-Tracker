package com.example.data.model

data class ChallengeTemplate(
    val title: String,
    val description: String,
    val category: String,
    val iconName: String,
    val colorHex: String,
    val targetDays: Int,
    val reminderTime: String
)

object ChallengeTemplates {
    val templates = listOf(
        ChallengeTemplate(
            title = "100 Days of Coding",
            description = "Write code, debug, learn a new framework, or work on a side project for at least 30 minutes every day.",
            category = "Coding",
            iconName = "code",
            colorHex = "#3B82F6", // Blue
            targetDays = 100,
            reminderTime = "20:00"
        ),
        ChallengeTemplate(
            title = "30 Day Reading Challenge",
            description = "Read physical books, Kindle, or articles for 20 minutes daily to expand your mind.",
            category = "Reading",
            iconName = "book",
            colorHex = "#8B5CF6", // Purple
            targetDays = 30,
            reminderTime = "21:30"
        ),
        ChallengeTemplate(
            title = "Daily Walk Challenge",
            description = "Get outside and take a fresh walk for at least 15-30 minutes. Clear your head and stay active.",
            category = "Fitness",
            iconName = "fitness",
            colorHex = "#10B981", // Green
            targetDays = 30,
            reminderTime = "07:30"
        ),
        ChallengeTemplate(
            title = "Hydration Tracker",
            description = "Drink at least 8 glasses of water throughout the day to keep hydrated, responsive, and energetic.",
            category = "Health",
            iconName = "water",
            colorHex = "#06B6D4", // Cyan
            targetDays = 30,
            reminderTime = "12:00"
        ),
        ChallengeTemplate(
            title = "Mindfulness Meditation",
            description = "Settle yourself in quiet contemplation. Deep diaphragmatic belly-breathing for 10 minutes.",
            category = "Meditation",
            iconName = "meditation",
            colorHex = "#EC4899", // Pink
            targetDays = 30,
            reminderTime = "08:00"
        ),
        ChallengeTemplate(
            title = "Fitness Starter",
            description = "Complete a short bodyweight routine (pushups, squats, planks) to build solid muscle memory.",
            category = "Fitness",
            iconName = "gym",
            colorHex = "#F59E0B", // Orange
            targetDays = 30,
            reminderTime = "18:00"
        )
    )
}
