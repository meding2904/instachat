package com.instachat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.instachat.app.data.local.entity.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts WHERE contactId = :contactId ORDER BY orderIndex ASC, id ASC")
    fun observeForContact(contactId: Long): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE contactId = :contactId ORDER BY orderIndex ASC, id ASC")
    suspend fun getForContact(contactId: Long): List<ScriptEntity>

    @Insert
    suspend fun insert(script: ScriptEntity): Long

    @Update
    suspend fun update(script: ScriptEntity)

    @Delete
    suspend fun delete(script: ScriptEntity)

    @Query("UPDATE scripts SET text = :text, delayMs = :delayMs, typingMs = :typingMs WHERE id = :id")
    suspend fun updateLine(id: Long, text: String, delayMs: Long, typingMs: Long)

    @Query("UPDATE scripts SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrder(id: Long, orderIndex: Int)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM scripts WHERE contactId = :contactId")
    suspend fun nextOrderIndex(contactId: Long): Int

    @Query("SELECT COUNT(*) FROM scripts WHERE contactId = :contactId")
    suspend fun countForContact(contactId: Long): Int

    @Query("DELETE FROM scripts WHERE contactId = :contactId")
    suspend fun deleteForContact(contactId: Long)
}
