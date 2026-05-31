package com.example.data.database

import androidx.room.*
import com.example.data.model.Challenge
import com.example.data.model.CompletionRecord
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY createdAt DESC")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getChallengeById(id: Int): Challenge?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge): Long

    @Update
    suspend fun updateChallenge(challenge: Challenge)

    @Delete
    suspend fun deleteChallenge(challenge: Challenge)
}

@Dao
interface CompletionRecordDao {
    @Query("SELECT * FROM completion_records ORDER BY timestamp DESC")
    fun getAllCompletionRecords(): Flow<List<CompletionRecord>>

    @Query("SELECT * FROM completion_records WHERE challengeId = :challengeId ORDER BY date ASC")
    fun getCompletionsByChallenge(challengeId: Int): Flow<List<CompletionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionRecord(record: CompletionRecord)

    @Delete
    suspend fun deleteCompletionRecord(record: CompletionRecord)

    @Query("DELETE FROM completion_records WHERE challengeId = :challengeId")
    suspend fun deleteCompletionsForChallenge(challengeId: Int)

    @Query("SELECT * FROM completion_records WHERE challengeId = :challengeId AND date = :date LIMIT 1")
    suspend fun getCompletionForChallengeAndDate(challengeId: Int, date: String): CompletionRecord?
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
