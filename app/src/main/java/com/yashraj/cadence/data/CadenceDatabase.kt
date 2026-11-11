package com.yashraj.cadence.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, TaskEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CadenceDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): CadenceDatabase =
            Room.databaseBuilder(context, CadenceDatabase::class.java, "cadence.db").build()
    }
}
