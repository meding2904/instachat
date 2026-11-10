package com.instachat.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val username: String,
    val bio: String = "",
    val avatarPath: String? = null,
    val themeId: String = "aurora",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val autoScriptEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val typingSimulationEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val activeNow: Boolean = true
)
