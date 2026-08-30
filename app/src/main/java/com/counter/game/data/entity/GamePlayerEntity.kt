package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_players",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("game_id"),
        Index("player_id"),
        Index(value = ["game_id", "player_id"], unique = true),
    ],
)
data class GamePlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "seat_order") val seatOrder: Int,
)