package com.kroslabs.lifecoach.data.repository

import com.kroslabs.lifecoach.data.database.LifeCoachDao
import com.kroslabs.lifecoach.data.model.*
import kotlinx.coroutines.flow.Flow

class LifeCoachRepository(private val dao: LifeCoachDao) {

    // User Profile
    fun getUserProfile(): Flow<UserProfile?> = dao.getUserProfile()
    suspend fun getUserProfileOnce(): UserProfile? = dao.getUserProfileOnce()
    suspend fun insertUserProfile(profile: UserProfile) = dao.insertUserProfile(profile)
    suspend fun updateUserProfile(profile: UserProfile) = dao.updateUserProfile(profile)

    // Life Paths
    fun getActivePaths(): Flow<List<LifePath>> = dao.getActivePaths()
    fun getAllPaths(): Flow<List<LifePath>> = dao.getAllPaths()
    suspend fun getPathById(id: Long): LifePath? = dao.getPathById(id)
    suspend fun insertPath(path: LifePath): Long = dao.insertPath(path)
    suspend fun updatePath(path: LifePath) = dao.updatePath(path)
    suspend fun deletePath(path: LifePath) = dao.deletePath(path)

    // Experiments
    fun getActiveExperiments(): Flow<List<Experiment>> = dao.getActiveExperiments()
    fun getExperimentsByPath(pathId: Long): Flow<List<Experiment>> = dao.getExperimentsByPath(pathId)
    suspend fun getExperimentById(id: Long): Experiment? = dao.getExperimentById(id)
    suspend fun insertExperiment(experiment: Experiment): Long = dao.insertExperiment(experiment)
    suspend fun updateExperiment(experiment: Experiment) = dao.updateExperiment(experiment)
    suspend fun deleteExperiment(experiment: Experiment) = dao.deleteExperiment(experiment)
    suspend fun getCompletedExperimentCount(pathId: Long): Int = dao.getCompletedExperimentCount(pathId)
    suspend fun getTotalExperimentCount(pathId: Long): Int = dao.getTotalExperimentCount(pathId)

    // Check-ins
    fun getCheckInsForExperiment(experimentId: Long): Flow<List<CheckIn>> =
        dao.getCheckInsForExperiment(experimentId)
    suspend fun getLatestCheckIn(experimentId: Long): CheckIn? = dao.getLatestCheckIn(experimentId)
    suspend fun insertCheckIn(checkIn: CheckIn): Long = dao.insertCheckIn(checkIn)

    // Journal Entries
    fun getAllJournalEntries(): Flow<List<JournalEntry>> = dao.getAllJournalEntries()
    fun getJournalEntriesByPath(pathId: Long): Flow<List<JournalEntry>> =
        dao.getJournalEntriesByPath(pathId)
    suspend fun getJournalEntriesSince(startTime: Long): List<JournalEntry> =
        dao.getJournalEntriesSince(startTime)
    suspend fun getJournalEntryById(id: Long): JournalEntry? = dao.getJournalEntryById(id)
    suspend fun insertJournalEntry(entry: JournalEntry): Long = dao.insertJournalEntry(entry)
    suspend fun updateJournalEntry(entry: JournalEntry) = dao.updateJournalEntry(entry)
    suspend fun deleteJournalEntry(entry: JournalEntry) = dao.deleteJournalEntry(entry)

    // API Usage
    fun getAllApiUsage(): Flow<List<ApiUsage>> = dao.getAllApiUsage()
    suspend fun getTotalCostSince(startTime: Long): Float = dao.getTotalCostSince(startTime) ?: 0f
    suspend fun getTotalTokensUsed(): Int = dao.getTotalTokensUsed() ?: 0
    suspend fun insertApiUsage(usage: ApiUsage) = dao.insertApiUsage(usage)

    // Analytics
    fun getAnalyticsForPath(pathId: Long): Flow<List<AnalyticsSnapshot>> =
        dao.getAnalyticsForPath(pathId)
    suspend fun insertAnalyticsSnapshot(snapshot: AnalyticsSnapshot) =
        dao.insertAnalyticsSnapshot(snapshot)

    // Data management
    suspend fun clearAllData() {
        dao.deleteAllCheckIns()
        dao.deleteAllExperiments()
        dao.deleteAllJournalEntries()
        dao.deleteAllAnalytics()
        dao.deleteAllApiUsage()
        dao.deleteAllPaths()
    }
}
