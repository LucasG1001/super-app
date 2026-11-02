package tech.gomeslab.superapp.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.gomeslab.superapp.core.data.AppDatabase
import tech.gomeslab.superapp.feature.alarm.data.AlarmDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()
}
