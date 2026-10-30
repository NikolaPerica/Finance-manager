package com.example.financemanager.di

import android.content.Context
import com.example.financemanager.data.AppDatabase
import com.example.financemanager.data.FinanceRepository
import com.example.financemanager.data.RoomFinanceRepository
import com.example.financemanager.security.AppLock
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** One database, repository and lock for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase = AppDatabase.create(context)

    @Provides
    @Singleton
    fun repository(database: AppDatabase): FinanceRepository = RoomFinanceRepository(database)

    /** Lives as long as the process, so rotating the screen doesn't lock the app. */
    @Provides
    @Singleton
    fun appLock(): AppLock = AppLock()
}

/** For code Hilt can't inject into: the widget, the reminder alarm and tests. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun repository(): FinanceRepository
    fun appLock(): AppLock
}

fun Context.appEntryPoint(): AppEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
