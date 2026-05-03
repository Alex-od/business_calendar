package ua.danichapps.radiantdays.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    @ColumnInfo(name = "guid")
    val guid: String,

    @ColumnInfo(name = "name")
    val name: String,
)
