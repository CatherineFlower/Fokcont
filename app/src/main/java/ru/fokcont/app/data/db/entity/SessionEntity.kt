package ru.fokcont.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "start_time")
    val startTime: Long = 0L,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "duration_sec")
    val durationSec: Long = 0L,

    @ColumnInfo(name = "switch_count")
    val switchCount: Int = 0,

    @ColumnInfo(name = "note")
    val note: String = ""
)