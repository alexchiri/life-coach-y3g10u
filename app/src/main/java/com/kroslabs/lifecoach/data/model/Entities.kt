package com.kroslabs.lifecoach.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Long = 1,
    val valuesResponses: String = "", // JSON string of questionnaire responses
    val deepDiveResponses: String = "", // Extended questionnaire responses
    val onboardingCompleted: Boolean = false,
    val deepDiveCompleted: Boolean = false,
    val preferredNotificationTime: String = "09:00",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Notification preferences
    val notificationsEnabled: Boolean = true,
    val dailyCheckInReminder: Boolean = true,
    val weeklyReflectionReminder: Boolean = true,
    val experimentLifecycleAlerts: Boolean = true,
    val milestoneNotifications: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Entity(tableName = "life_paths")
data class LifePath(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val viabilityScore: Float = 0f, // 0-100
    val isActive: Boolean = true,
    val aiRationale: String = "", // Why Claude suggested this path
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "experiments",
    foreignKeys = [
        ForeignKey(
            entity = LifePath::class,
            parentColumns = ["id"],
            childColumns = ["pathId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pathId")]
)
data class Experiment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pathId: Long,
    val title: String,
    val purpose: String, // Why this matters (Purposeful)
    val actionSteps: String, // Specific actions (Actionable) - JSON array
    val durationDays: Int, // How long to run (Continuous)
    val trackingMethod: TrackingMethod, // How to track (Trackable)
    val status: ExperimentStatus = ExperimentStatus.ACTIVE,
    val progress: Float = 0f, // 0-100
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ExperimentStatus {
    ACTIVE, COMPLETED, PAUSED, ARCHIVED
}

enum class TrackingMethod {
    DAILY_CHECKIN, MILESTONE_COMPLETION, WEEKLY_REVIEW
}

@Entity(
    tableName = "check_ins",
    foreignKeys = [
        ForeignKey(
            entity = Experiment::class,
            parentColumns = ["id"],
            childColumns = ["experimentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("experimentId")]
)
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val experimentId: Long,
    val progressValue: Float, // 0-100 for sliders, or specific milestone value
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = LifePath::class,
            parentColumns = ["id"],
            childColumns = ["pathId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Experiment::class,
            parentColumns = ["id"],
            childColumns = ["experimentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("pathId"), Index("experimentId")]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val pathId: Long? = null,
    val experimentId: Long? = null,
    val aiInsights: String = "", // AI-generated insights
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_usage")
data class ApiUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCostCents: Float,
    val operationType: String, // e.g., "path_discovery", "experiment_generation", "reflection_analysis"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "analytics")
data class AnalyticsSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pathId: Long,
    val viabilityScore: Float,
    val experimentCompletionRate: Float,
    val totalCheckIns: Int,
    val snapshotDate: Long = System.currentTimeMillis()
)
