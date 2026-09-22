package com.alvand.player.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AlvandDatabase =
        Room.databaseBuilder(ctx, AlvandDatabase::class.java, "alvand.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDao(db: AlvandDatabase): LibraryDao = db.libraryDao()
}
