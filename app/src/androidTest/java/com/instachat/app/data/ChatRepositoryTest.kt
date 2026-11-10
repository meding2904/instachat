package com.instachat.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.instachat.app.data.local.InstaChatDatabase
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.data.media.MediaStorage
import com.instachat.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: InstaChatDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, InstaChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChatRepository(
            db = db,
            contactDao = db.contactDao(),
            messageDao = db.messageDao(),
            themeDao = db.themeDao(),
            scriptDao = db.scriptDao(),
            mediaStorage = MediaStorage(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun messagesScriptsReactionsAndUnread_flowThroughRepository() = runBlocking {
        val contactId = repository.insertContact(
            name = "Test Friend",
            username = "test.friend",
            bio = "bio",
            avatarPath = null,
            themeId = "aurora"
        )

        val friendMessageId = requireNotNull(
            repository.sendText(contactId, Sender.FRIEND, "hello from the friend")
        )
        val myMessageId = requireNotNull(
            repository.sendText(contactId, Sender.ME, "hello from me")
        )

        var messages = repository.getMessages(contactId)
        assertEquals(2, messages.size)
        assertEquals(MessageStatus.DELIVERED, messages.first { it.id == friendMessageId }.status)
        assertEquals(MessageStatus.SENT, messages.first { it.id == myMessageId }.status)

        var inbox = repository.observeInbox().first()
        assertEquals(1, inbox.first().unreadCount)
        assertEquals("hello from me", inbox.first().preview)
        assertEquals(Sender.ME, inbox.first().previewSender)

        repository.markConversationSeen(contactId)
        inbox = repository.observeInbox().first()
        assertEquals(0, inbox.first().unreadCount)

        assertEquals("❤️", repository.toggleReaction(myMessageId, "❤️"))
        assertEquals("❤️", repository.getMessages(contactId).first { it.id == myMessageId }.reaction)
        assertNull(repository.toggleReaction(myMessageId, "❤️"))
        assertNull(repository.getMessages(contactId).first { it.id == myMessageId }.reaction)

        val oldTimestamp = System.currentTimeMillis() - 86_400_000L
        repository.updateTimestamp(myMessageId, oldTimestamp)
        messages = repository.getMessages(contactId)
        assertEquals("hello from me", messages.first().text)
        assertEquals(oldTimestamp, messages.first().timestamp)

        val firstScript = repository.addScript(contactId, "first", 400, 1200)
        val secondScript = repository.addScript(contactId, "second", 300, 800)
        assertEquals(listOf("first", "second"), repository.getScripts(contactId).map { it.text })
        assertEquals(listOf(1200L, 800L), repository.getScripts(contactId).map { it.typingMs })

        repository.moveScript(contactId, secondScript, -1)
        assertEquals(listOf("second", "first"), repository.getScripts(contactId).map { it.text })

        repository.deleteScript(firstScript, contactId)
        assertEquals(listOf("second"), repository.getScripts(contactId).map { it.text })

        repository.clearChat(contactId)
        assertTrue(repository.getMessages(contactId).isEmpty())
        assertTrue(repository.getScripts(contactId).isNotEmpty())
    }
}
