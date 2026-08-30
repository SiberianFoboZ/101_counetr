package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "round_entries",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GamePlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_player_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("round_id"),
        Index("game_player_id"),
        Index(value = ["round_id", "game_player_id"], unique = true),
    ],
)
data class RoundEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "round_id") val roundId: Long,
    @ColumnInfo(name = "game_player_id") val gamePlayerId: Long,
    @ColumnInfo(name = "delta_score") val deltaScore: Int = 0,
    @ColumnInfo(name = "raw_input_json") val rawInputJson: String? = null,
)