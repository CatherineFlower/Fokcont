package ru.fokcont.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.fokcont.app.data.db.dao.DistractionEventDao
import ru.fokcont.app.data.db.dao.NoteDao
import ru.fokcont.app.data.db.dao.SessionDao
import ru.fokcont.app.data.db.dao.UserDao
import ru.fokcont.app.data.db.entity.DistractionEventEntity
import ru.fokcont.app.data.db.entity.NoteEntity
import ru.fokcont.app.data.db.entity.SessionEntity
import ru.fokcont.app.data.db.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        NoteEntity::class,
        DistractionEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao
    abstract fun distractionEventDao(): DistractionEventDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "context_switch_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
