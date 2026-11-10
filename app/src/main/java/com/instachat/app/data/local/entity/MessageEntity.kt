package com.instachat.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("contactId"),
        Index(value = ["contactId", "timestamp"]),
        Index(value = ["contactId", "status"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val sender: Sender,
    val type: MessageType = MessageType.TEXT,
    val text: String? = null,
    val mediaPath: String? = null,
    val durationMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val reaction: String? = null,
    @ColumnInfo(defaultValue = "0")
    val scripted: Boolean = false
)
