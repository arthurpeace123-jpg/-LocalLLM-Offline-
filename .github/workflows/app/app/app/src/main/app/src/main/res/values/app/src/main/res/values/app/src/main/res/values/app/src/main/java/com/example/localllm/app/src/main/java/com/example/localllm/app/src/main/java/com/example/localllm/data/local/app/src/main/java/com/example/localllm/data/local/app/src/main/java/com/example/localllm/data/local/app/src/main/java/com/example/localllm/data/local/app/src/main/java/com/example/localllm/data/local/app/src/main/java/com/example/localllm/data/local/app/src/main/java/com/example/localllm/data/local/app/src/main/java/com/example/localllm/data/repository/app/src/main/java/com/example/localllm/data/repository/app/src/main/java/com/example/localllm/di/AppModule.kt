package com.example.localllm.di

import android.content.Context
import androidx.room.Room
import com.example.localllm.data.local.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "localllm.db"
        ).build()
    }

    @Provides
    fun provideModelDao(db: AppDatabase) = db.modelDao()

    @Provides
    fun provideChatDao(db: AppDatabase) = db.chatDao()
}
