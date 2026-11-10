package com.instachat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC, id ASC")
    fun observeForContact(contactId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC, id ASC")
    suspend fun getForContact(contactId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :id")
    suspend fun updateReaction(id: Long, reaction: String?)

    @Query("UPDATE messages SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MessageStatus)

    @Query("UPDATE messages SET status = 'SEEN' WHERE contactId = :contactId AND sender = 'FRIEND' AND status != 'SEEN'")
    suspend fun markFriendMessagesSeen(contactId: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteForContact(contactId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE contactId = :contactId")
    suspend fun countForContact(contactId: Long): Int
}
