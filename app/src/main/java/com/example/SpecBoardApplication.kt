package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.SpecRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpecBoardApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: SpecRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = SpecRepository(database)

        // Seed sample flagship smartphone dataset asynchronously
        applicationScope.launch {
            repository.seedSampleFlagships()
        }
    }

    companion object {
        lateinit var instance: SpecBoardApplication
            private set
    }
}
