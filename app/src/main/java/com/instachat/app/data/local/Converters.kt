package com.instachat.app.data.local

import androidx.room.TypeConverter
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.Sender

class Converters {
    @TypeConverter
    fun fromSender(sender: Sender): String = sender.name

    @TypeConverter
    fun toSender(value: String): Sender = Sender.valueOf(value)

    @TypeConverter
    fun fromMessageType(type: MessageType): String = type.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
