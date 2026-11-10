package com.instachat.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.instachat.app.data.local.InstaChatDatabase
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.Sender
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationAndDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrateFromV1_preservesRows_andAddsSimulationFieldsAndIndex() = runBlocking {
        createLegacyDatabase()

        val db = Room.databaseBuilder(context, InstaChatDatabase::class.java, dbName)
            .addMigrations(*InstaChatDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        val contact = db.contactDao().getById(1L)
        assertEquals("Ava", contact?.name)
        assertFalse(contact!!.autoScriptEnabled)
        assertTrue(contact.typingSimulationEnabled)
        assertTrue(contact.activeNow)

        val messages = db.messageDao().getForContact(1L)
        assertEquals(1, messages.size)
        assertFalse(messages.first().scripted)
        assertEquals(MessageStatus.SEEN, messages.first().status)

        // New columns are writable after the migration.
        db.messageDao().insert(
            MessageEntity(
                contactId = 1L,
                sender = Sender.FRIEND,
                type = MessageType.TEXT,
                text = "scripted hello",
                scripted = true
            )
        )
        assertEquals(2, db.messageDao().getForContact(1L).size)
        assertTrue(db.messageDao().getForContact(1L).last().scripted)

        db.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_messages_contactId_status'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("index_messages_contactId_status", cursor.getString(0))
            }

        val theme = db.themeDao().getById("aurora")
        assertEquals("Default", theme?.name)
        assertEquals(0xFFC42BC1L, theme?.sentColorStart)
        assertEquals(0xFF000000L, theme?.background)

        val scripts = db.scriptDao().getForContact(1L)
        assertEquals(1, scripts.size)
        assertEquals(1500L, scripts.first().typingMs)

        assertEquals(4, db.openHelper.readableDatabase.version)
        db.close()
    }

    @Test
    fun inboxProjection_ordersByLatestMessage_andCountsUnread() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InstaChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val now = System.currentTimeMillis()
        val first = db.contactDao().insert(
            com.instachat.app.data.local.entity.ContactEntity(
                name = "Older",
                username = "older",
                createdAt = now - 10_000
            )
        )
        val second = db.contactDao().insert(
            com.instachat.app.data.local.entity.ContactEntity(
                name = "Newer",
                username = "newer",
                createdAt = now - 20_000
            )
        )
        db.messageDao().insert(
            MessageEntity(
                contactId = first,
                sender = Sender.FRIEND,
                text = "old message",
                timestamp = now - 5_000,
                status = MessageStatus.SEEN
            )
        )
        db.messageDao().insert(
            MessageEntity(
                contactId = second,
                sender = Sender.FRIEND,
                text = "new message",
                timestamp = now,
                status = MessageStatus.DELIVERED
            )
        )

        val inbox = db.contactDao().observeInbox().first()
        assertEquals(2, inbox.size)
        assertEquals("Newer", inbox[0].contact.name)
        assertEquals("new message", inbox[0].lastText)
        assertEquals(1, inbox[0].unreadCount)
        assertEquals("Older", inbox[1].contact.name)
        assertEquals(0, inbox[1].unreadCount)

        db.messageDao().markFriendMessagesSeen(second)
        assertEquals(0, db.contactDao().observeInbox().first()[0].unreadCount)

        db.close()
    }

    private fun createLegacyDatabase() {
        val legacy = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `contacts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                `bio` TEXT NOT NULL,
                `avatarPath` TEXT,
                `themeId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `themes` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `sentColorStart` INTEGER NOT NULL,
                `sentColorEnd` INTEGER NOT NULL,
                `receivedColor` INTEGER NOT NULL,
                `background` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `messages` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contactId` INTEGER NOT NULL,
                `sender` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `text` TEXT,
                `mediaPath` TEXT,
                `durationMs` INTEGER,
                `timestamp` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `reaction` TEXT,
                FOREIGN KEY(`contactId`) REFERENCES `contacts`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_messages_contactId` ON `messages` (`contactId`)"
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_messages_contactId_timestamp` ON `messages` (`contactId`, `timestamp`)"
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scripts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contactId` INTEGER NOT NULL,
                `orderIndex` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                `delayMs` INTEGER NOT NULL,
                FOREIGN KEY(`contactId`) REFERENCES `contacts`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_scripts_contactId_orderIndex` ON `scripts` (`contactId`, `orderIndex`)"
        )

        legacy.execSQL(
            "INSERT INTO contacts (id, name, username, bio, themeId, createdAt) VALUES (1, 'Ava', 'ava', 'bio', 'aurora', 1000)"
        )
        legacy.execSQL(
            "INSERT INTO messages (contactId, sender, type, text, timestamp, status) VALUES (1, 'FRIEND', 'TEXT', 'legacy hello', 2000, 'SEEN')"
        )
        legacy.execSQL(
            "INSERT INTO scripts (contactId, orderIndex, text, delayMs) VALUES (1, 0, 'legacy script', 1000)"
        )
        legacy.execSQL(
            "INSERT INTO themes (id, name, sentColorStart, sentColorEnd, receivedColor, background) VALUES ('aurora', 'Aurora', 1, 2, 3, 4)"
        )
        legacy.version = 1
        legacy.close()
    }
}
