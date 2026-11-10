package com.instachat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.instachat.app.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query(
        """
        SELECT c.*,
            (SELECT m.text FROM messages m WHERE m.contactId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS lastText,
            (SELECT m.type FROM messages m WHERE m.contactId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS lastType,
            (SELECT m.timestamp FROM messages m WHERE m.contactId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS lastTimestamp,
            (SELECT m.sender FROM messages m WHERE m.contactId = c.id
                ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS lastSender,
            (SELECT COUNT(*) FROM messages m WHERE m.contactId = c.id
                AND m.sender = 'FRIEND' AND m.status != 'SEEN') AS unreadCount
        FROM contacts c
        ORDER BY COALESCE(
            (SELECT MAX(m.timestamp) FROM messages m WHERE m.contactId = c.id),
            c.createdAt
        ) DESC
        """
    )
    fun observeInbox(): Flow<List<InboxRow>>

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun observeById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("UPDATE contacts SET themeId = :themeId WHERE id = :id")
    suspend fun updateTheme(id: Long, themeId: String)

    @Query("UPDATE contacts SET autoScriptEnabled = :enabled WHERE id = :id")
    suspend fun updateAutoScript(id: Long, enabled: Boolean)

    @Query("UPDATE contacts SET typingSimulationEnabled = :enabled WHERE id = :id")
    suspend fun updateTypingSimulation(id: Long, enabled: Boolean)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
