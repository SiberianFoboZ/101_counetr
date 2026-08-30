package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_definitions",
    indices = [Index(value = ["code"], unique = true)],
)
data class CardDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val label: String,
    @ColumnInfo(name = "base_value") val baseValue: Int,
    @ColumnInfo(name = "is_face") val isFace: Boolean = false,
)