package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GameStatus { IN_PROGRESS, PAUSED, FINISHED }

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: String = GameStatus.IN_PROGRESS.name,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    @ColumnInfo(name = "winner_player_id") val winnerPlayerId: Long? = null,
    @ColumnInfo(name = "threshold_score") val thresholdScore: Int = 101,
)