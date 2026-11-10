package com.instachat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A per-conversation color theme. Colors are stored as ARGB longs so that
 * alpha variants survive the round trip through SQLite.
 */
@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sentColorStart: Long,
    val sentColorEnd: Long,
    val receivedColor: Long,
    val background: Long
)
