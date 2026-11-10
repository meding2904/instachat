package com.instachat.app

import android.app.Application
import com.instachat.app.data.repository.DemoSeeder
import com.instachat.app.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class InstaChatApp : Application() {

    @Inject
    lateinit var seeder: DemoSeeder

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { seeder.seedIfNeeded() }
        }
    }
}
