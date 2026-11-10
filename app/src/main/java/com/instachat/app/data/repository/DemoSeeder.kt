package com.instachat.app.data.repository

import android.content.Context
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
import com.instachat.app.data.media.AvatarStyle
import com.instachat.app.data.media.MediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Seeds the two demo friends and their conversations. The generator is
 * deterministic and uses no third-party imagery.
 */
@Singleton
class DemoSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: InstaChatDatabase,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val themeDao: ThemeDao,
    private val scriptDao: ScriptDao,
    private val mediaStorage: MediaStorage
) {
    private val mutex = Mutex()
    private val seedPrefs = context.getSharedPreferences("demo_seed_state", Context.MODE_PRIVATE)
    private val seededKey = "demo_seeded_once"

    suspend fun seedIfNeeded() = mutex.withLock {
        seedThemesIfNeeded()
        if (!seedPrefs.getBoolean(seededKey, false)) {
            if (contactDao.count() == 0) {
                seedConversations()
            }
            seedPrefs.edit().putBoolean(seededKey, true).apply()
        }
    }

    suspend fun resetDemoData() = mutex.withLock {
        contactDao.deleteAll()
        mediaStorage.clearAllMedia()
        seedThemesIfNeeded()
        seedConversations()
        seedPrefs.edit().putBoolean(seededKey, true).apply()
    }

    private suspend fun seedThemesIfNeeded() {
        if (themeDao.count() > 0) return
        themeDao.upsertAll(SeedThemes.all)
    }

    private suspend fun seedConversations() {
        val now = System.currentTimeMillis()
        val avaAvatar = mediaStorage.generateSeedAvatar(AvatarStyle.BOB)
        val leoAvatar = mediaStorage.generateSeedAvatar(AvatarStyle.CURLS)
        val rooftop = mediaStorage.generateSeedImage()

        val avaId = contactDao.insert(
            ContactEntity(
                name = "Ava Mitchell",
                username = "ava.mitchell",
                bio = "Golden hour & film rolls",
                avatarPath = avaAvatar,
                themeId = "aurora",
                createdAt = now - days(30)
            )
        )
        val leoId = contactDao.insert(
            ContactEntity(
                name = "Leo Park",
                username = "leopark",
                bio = "Studio session all night",
                avatarPath = leoAvatar,
                themeId = "aurora",
                createdAt = now - days(18)
            )
        )

        val pendingMessages = mutableListOf<MessageEntity>()

        fun msg(
            contactId: Long,
            sender: Sender,
            text: String? = null,
            type: MessageType = MessageType.TEXT,
            mediaPath: String? = null,
            timestamp: Long,
            status: MessageStatus,
            reaction: String? = null
        ) {
            pendingMessages += MessageEntity(
                contactId = contactId,
                sender = sender,
                type = type,
                text = text,
                mediaPath = mediaPath,
                timestamp = timestamp,
                status = status,
                reaction = reaction
            )
        }

        // Ava Mitchell conversation — includes joined multi-message groups on both sides
        msg(
            avaId, Sender.ME,
            "are we still on for the rooftop tonight?",
            timestamp = now - hours(27) - minutes(50), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            type = MessageType.IMAGE,
            mediaPath = rooftop,
            timestamp = now - hours(27) - minutes(45),
            status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "look at the sky right now",
            timestamp = now - hours(27) - minutes(44), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "i already saved us a table",
            timestamp = now - hours(27) - minutes(43), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.ME,
            "ok that view is unreal",
            timestamp = now - hours(27) - minutes(40), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.ME,
            "leaving in 10 mins",
            timestamp = now - hours(27) - minutes(39), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "Wait really??",
            timestamp = now - minutes(50), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "Don't forget the film camera",
            timestamp = now - minutes(49), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "I'm only asking once",
            timestamp = now - minutes(48), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.FRIEND,
            "And grab iced coffee on the way",
            timestamp = now - minutes(47), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.ME,
            "already in my bag",
            timestamp = now - minutes(42), status = MessageStatus.SEEN
        )
        msg(
            avaId, Sender.ME,
            "i'll bring dessert too 🍰",
            timestamp = now - minutes(41), status = MessageStatus.SEEN
        )

        // Leo Park conversation — includes joined multi-message groups on both sides
        msg(
            leoId, Sender.ME,
            "did you finish the poster?",
            timestamp = now - hours(28), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.FRIEND,
            "almost",
            timestamp = now - hours(27) - minutes(56), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.FRIEND,
            "fighting with the type layout rn",
            timestamp = now - hours(27) - minutes(55), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.FRIEND,
            "send help",
            timestamp = now - hours(27) - minutes(54), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.ME,
            "the spacing looks clean though",
            timestamp = now - hours(27) - minutes(40), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.ME,
            "just dial up the contrast",
            timestamp = now - hours(27) - minutes(39), status = MessageStatus.SEEN
        )
        msg(
            leoId, Sender.FRIEND,
            "wait that actually fixed it",
            timestamp = now - minutes(12), status = MessageStatus.DELIVERED
        )
        msg(
            leoId, Sender.FRIEND,
            "we're getting food after this",
            timestamp = now - minutes(11), status = MessageStatus.DELIVERED
        )
        msg(
            leoId, Sender.FRIEND,
            "i'm buying",
            timestamp = now - minutes(10), status = MessageStatus.DELIVERED
        )

        pendingMessages.forEach { messageDao.insert(it) }

        scriptDao.insert(
            ScriptEntity(
                contactId = avaId,
                orderIndex = 0,
                text = "wait really?? tell me everything",
                delayMs = 400,
                typingMs = 1500
            )
        )
        scriptDao.insert(
            ScriptEntity(
                contactId = avaId,
                orderIndex = 1,
                text = "ok now i'm invested",
                delayMs = 350,
                typingMs = 1900
            )
        )
        scriptDao.insert(
            ScriptEntity(
                contactId = avaId,
                orderIndex = 2,
                text = "give me five minutes, i'll be there",
                delayMs = 400,
                typingMs = 2200
            )
        )
        scriptDao.insert(
            ScriptEntity(
                contactId = leoId,
                orderIndex = 0,
                text = "haha okay that's fair",
                delayMs = 300,
                typingMs = 1200
            )
        )
        scriptDao.insert(
            ScriptEntity(
                contactId = leoId,
                orderIndex = 1,
                text = "send me the link",
                delayMs = 350,
                typingMs = 1700
            )
        )
    }

    private fun days(value: Long): Long = value * 24L * 60L * 60L * 1000L
    private fun hours(value: Long): Long = value * 60L * 60L * 1000L
    private fun minutes(value: Long): Long = value * 60L * 60L * 1000L
}

object SeedThemes {
    val all = listOf(
        ThemeEntity(
            id = "aurora",
            name = "Default",
            sentColorStart = 0xFFC42BC1,
            sentColorEnd = 0xFF5B4DFF,
            receivedColor = 0xFF262626,
            background = 0xFF000000
        ),
        ThemeEntity(
            id = "sunset",
            name = "Sunset",
            sentColorStart = 0xFFFF9F43,
            sentColorEnd = 0xFFFF4D8D,
            receivedColor = 0xFF262626,
            background = 0xFF000000
        ),
        ThemeEntity(
            id = "forest",
            name = "Ocean",
            sentColorStart = 0xFF16C8B8,
            sentColorEnd = 0xFF2F8CFF,
            receivedColor = 0xFF262626,
            background = 0xFF000000
        ),
        ThemeEntity(
            id = "midnight",
            name = "Midnight",
            sentColorStart = 0xFF5B4DFF,
            sentColorEnd = 0xFF8B5CFF,
            receivedColor = 0xFF262626,
            background = 0xFF000000
        )
    )
}
