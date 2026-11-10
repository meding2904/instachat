package com.instachat.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.instachat.app.data.local.dao.ContactDao
import com.instachat.app.data.local.dao.MessageDao
import com.instachat.app.data.local.dao.ScriptDao
import com.instachat.app.data.local.dao.ThemeDao
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.ScriptEntity
import com.instachat.app.data.local.entity.ThemeEntity

@Database(
    entities = [
        ContactEntity::class,
        MessageEntity::class,
        ThemeEntity::class,
        ScriptEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InstaChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun themeDao(): ThemeDao
    abstract fun scriptDao(): ScriptDao

    companion object {
        const val NAME = "instachat.db"

        /**
         * v2 adds the simulation controls that are edited in the script screen.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contacts ADD COLUMN autoScriptEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE contacts ADD COLUMN typingSimulationEnabled INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE messages ADD COLUMN scripted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * v3 adds an index used by the unread-dot query on the inbox.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_messages_contactId_status " +
                        "ON messages (contactId, status)"
                )
            }
        }

        /**
         * v4 aligns the built-in palettes with the design system and adds the
         * per-contact activity switch and per-script typing duration.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contacts ADD COLUMN activeNow INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE scripts ADD COLUMN typingMs INTEGER NOT NULL DEFAULT 1500"
                )
                db.execSQL(
                    "UPDATE themes SET name = 'Default', " +
                        "sentColorStart = 0xFFC42BC1, sentColorEnd = 0xFF6C4DFF, " +
                        "receivedColor = 0xFF262626, background = 0xFF000000 " +
                        "WHERE id = 'aurora'"
                )
                db.execSQL(
                    "UPDATE themes SET name = 'Sunset', " +
                        "sentColorStart = 0xFFFF9F43, sentColorEnd = 0xFFFF4D8D, " +
                        "receivedColor = 0xFF262626, background = 0xFF000000 " +
                        "WHERE id = 'sunset'"
                )
                db.execSQL(
                    "UPDATE themes SET name = 'Ocean', " +
                        "sentColorStart = 0xFF16C8B8, sentColorEnd = 0xFF2F8CFF, " +
                        "receivedColor = 0xFF262626, background = 0xFF000000 " +
                        "WHERE id = 'forest'"
                )
                db.execSQL(
                    "UPDATE themes SET name = 'Midnight', " +
                        "sentColorStart = 0xFF5B4DFF, sentColorEnd = 0xFF8B5CFF, " +
                        "receivedColor = 0xFF262626, background = 0xFF000000 " +
                        "WHERE id = 'midnight'"
                )
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
