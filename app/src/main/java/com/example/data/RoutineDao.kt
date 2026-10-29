package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY isEnabled DESC, hour ASC, minute ASC, id ASC")
    fun observeAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getEnabledRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Query("DELETE FROM routines")
    suspend fun clearAllRoutines()

    @Query("SELECT * FROM check_in_logs ORDER BY timestamp DESC")
    fun observeAllCheckIns(): Flow<List<CheckInLogEntity>>

    @Query("SELECT * FROM check_in_logs ORDER BY timestamp DESC")
    suspend fun getAllCheckInsOnce(): List<CheckInLogEntity>

    @Query("SELECT * FROM check_in_logs WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun observeCheckInsForDate(dateKey: String): Flow<List<CheckInLogEntity>>

    @Query("SELECT * FROM check_in_logs WHERE routineId = :routineId AND dateKey = :dateKey")
    suspend fun getCheckInsForRoutineOnDate(routineId: Long, dateKey: String): List<CheckInLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(log: CheckInLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(logs: List<CheckInLogEntity>)

    @Query("DELETE FROM check_in_logs WHERE id = :logId")
    suspend fun deleteCheckInById(logId: Long)

    @Query("DELETE FROM check_in_logs")
    suspend fun clearAllCheckIns()
}
