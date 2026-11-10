package com.instachat.app.data.local.dao

import androidx.room.Embedded
import com.instachat.app.data.local.entity.ContactEntity

/** Projection used by the inbox: contact + a cheap preview of the latest message. */
data class InboxRow(
    @Embedded val contact: ContactEntity,
    val lastText: String?,
    val lastType: String?,
    val lastTimestamp: Long?,
    val lastSender: String?,
    val unreadCount: Int
)
