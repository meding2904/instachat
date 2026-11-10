package com.instachat.app.data.model

import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.Sender

/** A contact plus the bits the inbox needs, assembled by the repository. */
data class InboxItem(
    val contact: ContactEntity,
    val preview: String?,
    val previewType: MessageType?,
    val previewSender: Sender?,
    val timestamp: Long?,
    val unreadCount: Int
)
