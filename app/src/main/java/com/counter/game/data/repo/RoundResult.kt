package com.counter.game.data.repo

import com.counter.game.data.dao.GamePlayerWithScore

data class RoundResult(
    val gameId: Long,
    val roundNumber: Int,
    val scores: List<GamePlayerWithScore>,
    val winnerPlayerId: Long?,
    val thresholdScore: Int,
)