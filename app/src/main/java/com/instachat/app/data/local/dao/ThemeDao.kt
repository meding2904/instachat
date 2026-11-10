package com.instachat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.instachat.app.data.local.entity.ThemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {

    @Query("SELECT * FROM themes")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes")
    suspend fun getAll(): List<ThemeEntity>

    @Query("SELECT * FROM themes WHERE id = :id")
    fun observeById(id: String): Flow<ThemeEntity?>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getById(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(themes: List<ThemeEntity>)

    @Query("SELECT COUNT(*) FROM themes")
    suspend fun count(): Int
}
