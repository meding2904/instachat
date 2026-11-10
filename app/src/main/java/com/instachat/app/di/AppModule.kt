package com.instachat.app.di

import android.content.Context
import androidx.room.Room
import com.instachat.app.data.local.InstaChatDatabase
import com.instachat.app.data.local.dao.ContactDao
import com.instachat.app.data.local.dao.MessageDao
import com.instachat.app.data.local.dao.ScriptDao
import com.instachat.app.data.local.dao.ThemeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InstaChatDatabase =
        Room.databaseBuilder(context, InstaChatDatabase::class.java, InstaChatDatabase.NAME)
            .addMigrations(*InstaChatDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideContactDao(database: InstaChatDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideMessageDao(database: InstaChatDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideThemeDao(database: InstaChatDatabase): ThemeDao = database.themeDao()

    @Provides
    fun provideScriptDao(database: InstaChatDatabase): ScriptDao = database.scriptDao()
}
