package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("game_id"),
        Index(value = ["game_id", "round_number"], unique = true),
    ],
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "round_number") val roundNumber: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)