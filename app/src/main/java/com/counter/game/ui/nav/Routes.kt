package com.counter.game.ui.nav

sealed class Routes(val path: String) {
    data object Home : Routes("home")
    data object Players : Routes("players")
    data object Settings : Routes("settings")
    data object NewGame : Routes("new_game")

    data object Game : Routes("game/{gameId}") {
        fun build(gameId: Long) = "game/$gameId"
        const val ARG = "gameId"
    }

    data object RoundInput : Routes("game/{gameId}/round") {
        fun build(gameId: Long) = "game/$gameId/round"
        const val ARG = "gameId"
    }

    data object History : Routes("game/{gameId}/history") {
        fun build(gameId: Long) = "game/$gameId/history"
        const val ARG = "gameId"
    }
}