package com.kroslabs.lifecoach.data.database

import androidx.room.*
import com.kroslabs.lifecoach.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeCoachDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // Life Paths
    @Query("SELECT * FROM life_paths WHERE isActive = 1 ORDER BY viabilityScore DESC")
    fun getActivePaths(): Flow<List<LifePath>>

    @Query("SELECT * FROM life_paths ORDER BY createdAt DESC")
    fun getAllPaths(): Flow<List<LifePath>>

    @Query("SELECT * FROM life_paths WHERE id = :id")
    suspend fun getPathById(id: Long): LifePath?

    @Insert
    suspend fun insertPath(path: LifePath): Long

    @Update
    suspend fun updatePath(path: LifePath)

    @Delete
    suspend fun deletePath(path: LifePath)

    // Experiments
    @Query("SELECT * FROM experiments WHERE status = 'ACTIVE' ORDER BY endDate ASC")
    fun getActiveExperiments(): Flow<List<Experiment>>

    @Query("SELECT * FROM experiments WHERE pathId = :pathId ORDER BY createdAt DESC")
    fun getExperimentsByPath(pathId: Long): Flow<List<Experiment>>

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getExperimentById(id: Long): Experiment?

    @Query("SELECT COUNT(*) FROM experiments WHERE pathId = :pathId AND status = 'COMPLETED'")
    suspend fun getCompletedExperimentCount(pathId: Long): Int

    @Query("SELECT COUNT(*) FROM experiments WHERE pathId = :pathId")
    suspend fun getTotalExperimentCount(pathId: Long): Int

    @Insert
    suspend fun insertExperiment(experiment: Experiment): Long

    @Update
    suspend fun updateExperiment(experiment: Experiment)

    @Delete
    suspend fun deleteExperiment(experiment: Experiment)

    // Check-ins
    @Query("SELECT * FROM check_ins WHERE experimentId = :experimentId ORDER BY createdAt DESC")
    fun getCheckInsForExperiment(experimentId: Long): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE experimentId = :experimentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCheckIn(experimentId: Long): CheckIn?

    @Insert
    suspend fun insertCheckIn(checkIn: CheckIn): Long

    // Journal Entries
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE pathId = :pathId ORDER BY createdAt DESC")
    fun getJournalEntriesByPath(pathId: Long): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE createdAt >= :startTime ORDER BY createdAt DESC")
    suspend fun getJournalEntriesSince(startTime: Long): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryById(id: Long): JournalEntry?

    @Insert
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)

    // API Usage
    @Query("SELECT * FROM api_usage ORDER BY createdAt DESC")
    fun getAllApiUsage(): Flow<List<ApiUsage>>

    @Query("SELECT SUM(estimatedCostCents) FROM api_usage WHERE createdAt >= :startTime")
    suspend fun getTotalCostSince(startTime: Long): Float?

    @Query("SELECT SUM(inputTokens + outputTokens) FROM api_usage")
    suspend fun getTotalTokensUsed(): Int?

    @Insert
    suspend fun insertApiUsage(usage: ApiUsage)

    // Analytics
    @Query("SELECT * FROM analytics WHERE pathId = :pathId ORDER BY snapshotDate DESC LIMIT 30")
    fun getAnalyticsForPath(pathId: Long): Flow<List<AnalyticsSnapshot>>

    @Insert
    suspend fun insertAnalyticsSnapshot(snapshot: AnalyticsSnapshot)

    // Data management
    @Query("DELETE FROM experiments")
    suspend fun deleteAllExperiments()

    @Query("DELETE FROM life_paths")
    suspend fun deleteAllPaths()

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    @Query("DELETE FROM api_usage")
    suspend fun deleteAllApiUsage()

    @Query("DELETE FROM analytics")
    suspend fun deleteAllAnalytics()

    @Query("DELETE FROM check_ins")
    suspend fun deleteAllCheckIns()
}
