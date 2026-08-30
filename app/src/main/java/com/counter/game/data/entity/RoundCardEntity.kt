package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "round_cards",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CardDefinitionEntity::class,
            parentColumns = ["code"],
            childColumns = ["card_code"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("round_entry_id"),
        Index("card_code"),
    ],
)
data class RoundCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "round_entry_id") val roundEntryId: Long,
    @ColumnInfo(name = "card_code") val cardCode: String,
    val count: Int,
)