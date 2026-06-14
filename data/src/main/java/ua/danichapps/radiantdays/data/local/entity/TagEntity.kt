package ua.danichapps.radiantdays.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "guid")
    val guid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "color", defaultValue = "DEFAULT")
    val color: String = "DEFAULT",
)
