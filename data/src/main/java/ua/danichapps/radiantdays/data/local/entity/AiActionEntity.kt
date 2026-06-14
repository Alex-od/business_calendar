package ua.danichapps.radiantdays.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_actions")
data class AiActionEntity(
    @PrimaryKey
    @ColumnInfo(name = "guid")
    val guid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "prompt")
    val prompt: String,

    @ColumnInfo(name = "is_visible", defaultValue = "1")
    val isVisible: Boolean = true,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_built_in", defaultValue = "0")
    val isBuiltIn: Boolean = false,
)
