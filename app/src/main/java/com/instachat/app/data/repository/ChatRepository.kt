package com.instachat.app.data.repository

import androidx.room.withTransaction
import com.instachat.app.data.local.InstaChatDatabase
import com.instachat.app.data.local.dao.ContactDao
import com.instachat.app.data.local.dao.MessageDao
import com.instachat.app.data.local.dao.ScriptDao
import com.instachat.app.data.local.dao.ThemeDao
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.ScriptEntity
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.data.local.entity.ThemeEntity
import com.instachat.app.data.media.MediaStorage
import com.instachat.app.data.model.InboxItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ChatRepository @Inject constructor(
    private val db: InstaChatDatabase,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val themeDao: ThemeDao,
    private val scriptDao: ScriptDao,
    private val mediaStorage: MediaStorage
) {
    val themes: Flow<List<ThemeEntity>> = themeDao.observeAll()

    fun observeInbox(): Flow<List<InboxItem>> = contactDao.observeInbox().map { rows ->
        rows.map { row ->
            val type = row.lastType?.let { runCatching { MessageType.valueOf(it) }.getOrNull() }
            val sender = row.lastSender?.let { runCatching { Sender.valueOf(it) }.getOrNull() }
            InboxItem(
                contact = row.contact,
                preview = when (type) {
                    MessageType.IMAGE -> row.lastText?.takeIf { it.isNotBlank() } ?: "Photo"
                    MessageType.VOICE -> "Voice message"
                    MessageType.TEXT -> row.lastText.orEmpty()
                    null -> null
                },
                previewType = type,
                previewSender = sender,
                timestamp = row.lastTimestamp,
                unreadCount = row.unreadCount
            )
        }
    }

    fun observeContacts(): Flow<List<ContactEntity>> = contactDao.observeAll()

    fun observeContact(id: Long): Flow<ContactEntity?> = contactDao.observeById(id)

    fun observeMessages(contactId: Long): Flow<List<MessageEntity>> =
        messageDao.observeForContact(contactId)

    fun observeThemes(): Flow<List<ThemeEntity>> = themeDao.observeAll()

    fun observeTheme(id: String): Flow<ThemeEntity?> = themeDao.observeById(id)

    fun observeScripts(contactId: Long): Flow<List<ScriptEntity>> =
        scriptDao.observeForContact(contactId)

    suspend fun getContact(id: Long): ContactEntity? = contactDao.getById(id)

    suspend fun getContacts(): List<ContactEntity> = contactDao.getAll()

    suspend fun getMessages(contactId: Long): List<MessageEntity> =
        messageDao.getForContact(contactId)

    suspend fun getThemes(): List<ThemeEntity> = themeDao.getAll()

    suspend fun getTheme(id: String): ThemeEntity? = themeDao.getById(id)

    suspend fun getScripts(contactId: Long): List<ScriptEntity> =
        scriptDao.getForContact(contactId)

    suspend fun insertContact(
        name: String,
        username: String,
        bio: String,
        avatarPath: String?,
        themeId: String,
        activeNow: Boolean = true
    ): Long = contactDao.insert(
        ContactEntity(
            name = name.trim(),
            username = username.trim().removePrefix("@"),
            bio = bio.trim(),
            avatarPath = avatarPath,
            themeId = themeId,
            activeNow = activeNow
        )
    )

    suspend fun updateContact(updated: ContactEntity) {
        val old = contactDao.getById(updated.id) ?: return
        contactDao.update(updated)
        if (old.avatarPath != updated.avatarPath) {
            mediaStorage.deletePath(old.avatarPath)
        }
    }

    suspend fun deleteContact(id: Long) {
        val contact = contactDao.getById(id) ?: return
        val messages = messageDao.getForContact(id)
        contactDao.delete(contact)
        mediaStorage.deletePath(contact.avatarPath)
        messages.forEach { if (it.mediaPath != null) mediaStorage.deletePath(it.mediaPath) }
    }

    suspend fun setTheme(contactId: Long, themeId: String) {
        contactDao.updateTheme(contactId, themeId)
    }

    suspend fun setAutoScript(contactId: Long, enabled: Boolean) {
        contactDao.updateAutoScript(contactId, enabled)
    }

    suspend fun setTypingSimulation(contactId: Long, enabled: Boolean) {
        contactDao.updateTypingSimulation(contactId, enabled)
    }

    suspend fun sendText(
        contactId: Long,
        sender: Sender,
        text: String,
        timestamp: Long = System.currentTimeMillis(),
        scripted: Boolean = false
    ): Long? {
        if (text.isBlank()) return null
        if (contactDao.getById(contactId) == null) return null
        return messageDao.insert(
            MessageEntity(
                contactId = contactId,
                sender = sender,
                type = MessageType.TEXT,
                text = text.trim(),
                timestamp = timestamp,
                status = if (sender == Sender.ME) MessageStatus.SENT else MessageStatus.DELIVERED,
                scripted = scripted
            )
        )
    }

    suspend fun sendImage(
        contactId: Long,
        sender: Sender,
        path: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long? {
        if (path.isBlank()) return null
        if (contactDao.getById(contactId) == null) return null
        return messageDao.insert(
            MessageEntity(
                contactId = contactId,
                sender = sender,
                type = MessageType.IMAGE,
                mediaPath = path,
                timestamp = timestamp,
                status = if (sender == Sender.ME) MessageStatus.SENT else MessageStatus.DELIVERED
            )
        )
    }

    suspend fun sendVoice(
        contactId: Long,
        sender: Sender,
        path: String,
        durationMs: Long,
        timestamp: Long = System.currentTimeMillis()
    ): Long? {
        if (path.isBlank()) return null
        if (contactDao.getById(contactId) == null) return null
        return messageDao.insert(
            MessageEntity(
                contactId = contactId,
                sender = sender,
                type = MessageType.VOICE,
                mediaPath = path,
                durationMs = durationMs,
                timestamp = timestamp,
                status = if (sender == Sender.ME) MessageStatus.SENT else MessageStatus.DELIVERED
            )
        )
    }

    suspend fun markConversationSeen(contactId: Long) {
        messageDao.markFriendMessagesSeen(contactId)
    }

    suspend fun markMessageStatus(messageId: Long, status: MessageStatus) {
        messageDao.updateStatus(messageId, status)
    }

    suspend fun toggleReaction(messageId: Long, emoji: String): String? {
        val message = messageDao.getById(messageId) ?: return null
        val next = if (message.reaction == emoji) null else emoji
        messageDao.updateReaction(messageId, next)
        return next
    }

    suspend fun updateTimestamp(messageId: Long, timestamp: Long) {
        messageDao.updateTimestamp(messageId, timestamp)
    }

    suspend fun deleteMessage(messageId: Long) {
        val message = messageDao.getById(messageId) ?: return
        messageDao.deleteById(messageId)
        mediaStorage.deletePath(message.mediaPath)
    }

    suspend fun clearChat(contactId: Long) {
        val messages = messageDao.getForContact(contactId)
        messageDao.deleteForContact(contactId)
        messages.forEach { if (it.mediaPath != null) mediaStorage.deletePath(it.mediaPath) }
    }

    suspend fun addScript(
        contactId: Long,
        text: String,
        delayMs: Long,
        typingMs: Long
    ): Long {
        val order = scriptDao.nextOrderIndex(contactId)
        return scriptDao.insert(
            ScriptEntity(
                contactId = contactId,
                orderIndex = order,
                text = text.trim(),
                delayMs = delayMs.coerceIn(200L, 30_000L),
                typingMs = typingMs.coerceIn(300L, 30_000L)
            )
        )
    }

    suspend fun updateScript(id: Long, text: String, delayMs: Long, typingMs: Long) {
        scriptDao.updateLine(
            id,
            text.trim(),
            delayMs.coerceIn(200L, 30_000L),
            typingMs.coerceIn(300L, 30_000L)
        )
    }

    suspend fun deleteScript(id: Long, contactId: Long) {
        val scripts = scriptDao.getForContact(contactId)
        val script = scripts.firstOrNull { it.id == id } ?: return
        db.withTransaction {
            scriptDao.delete(script)
            scriptDao.getForContact(contactId).forEachIndexed { index, line ->
                if (line.orderIndex != index) scriptDao.updateOrder(line.id, index)
            }
        }
    }

    suspend fun moveScript(contactId: Long, scriptId: Long, direction: Int) {
        val scripts = scriptDao.getForContact(contactId)
        val index = scripts.indexOfFirst { it.id == scriptId }
        val target = index + direction
        if (index < 0 || target !in scripts.indices) return
        db.withTransaction {
            scriptDao.updateOrder(scripts[index].id, scripts[target].orderIndex)
            scriptDao.updateOrder(scripts[target].id, scripts[index].orderIndex)
        }
    }

    suspend fun deleteAllContacts() {
        val contacts = contactDao.getAll()
        val messages = mutableListOf<MessageEntity>()
        contacts.forEach { messages += messageDao.getForContact(it.id) }
        contactDao.deleteAll()
        contacts.forEach { mediaStorage.deletePath(it.avatarPath) }
        messages.forEach { mediaStorage.deletePath(it.mediaPath) }
    }
}
