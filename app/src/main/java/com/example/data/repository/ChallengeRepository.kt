package com.example.data.repository

import com.example.data.database.ChallengeDao
import com.example.data.database.CompletionRecordDao
import com.example.data.model.Challenge
import com.example.data.model.CompletionRecord
import kotlinx.coroutines.flow.Flow

class ChallengeRepository(
    private val challengeDao: ChallengeDao,
    private val completionRecordDao: CompletionRecordDao
) {
    val allChallenges: Flow<List<Challenge>> = challengeDao.getAllChallenges()
    val allCompletionRecords: Flow<List<CompletionRecord>> = completionRecordDao.getAllCompletionRecords()

    suspend fun getChallengeById(id: Int): Challenge? {
        return challengeDao.getChallengeById(id)
    }

    suspend fun insertChallenge(challenge: Challenge): Long {
        return challengeDao.insertChallenge(challenge)
    }

    suspend fun updateChallenge(challenge: Challenge) {
        challengeDao.updateChallenge(challenge)
    }

    suspend fun deleteChallenge(challenge: Challenge) {
        // First delete its completions, then the challenge itself
        completionRecordDao.deleteCompletionsForChallenge(challenge.id)
        challengeDao.deleteChallenge(challenge)
    }

    suspend fun toggleCompletion(challengeId: Int, date: String): Boolean {
        val existing = completionRecordDao.getCompletionForChallengeAndDate(challengeId, date)
        return if (existing != null) {
            completionRecordDao.deleteCompletionRecord(existing)
            false // Unmarked
        } else {
            val record = CompletionRecord(challengeId = challengeId, date = date)
            completionRecordDao.insertCompletionRecord(record)
            true // Marked complete
        }
    }

    suspend fun setCompletionStatus(challengeId: Int, date: String, completed: Boolean) {
        val existing = completionRecordDao.getCompletionForChallengeAndDate(challengeId, date)
        if (completed && existing == null) {
            val record = CompletionRecord(challengeId = challengeId, date = date)
            completionRecordDao.insertCompletionRecord(record)
        } else if (!completed && existing != null) {
            completionRecordDao.deleteCompletionRecord(existing)
        }
    }

    fun getCompletionsByChallenge(challengeId: Int): Flow<List<CompletionRecord>> {
        return completionRecordDao.getCompletionsByChallenge(challengeId)
    }
}
