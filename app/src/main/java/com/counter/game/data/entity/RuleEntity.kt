package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rules",
    foreignKeys = [
        ForeignKey(
            entity = CardDefinitionEntity::class,
            parentColumns = ["code"],
            childColumns = ["applies_to_card"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("applies_to_card"),
        Index("priority"),
    ],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "applies_to_card") val appliesToCard: String,
    val priority: Int,
    val enabled: Boolean = true,
    @ColumnInfo(name = "definition_json") val definitionJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)