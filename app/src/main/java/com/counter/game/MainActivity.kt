package com.counter.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.counter.game.ui.game.GameScreen
import com.counter.game.ui.history.HistoryScreen
import com.counter.game.ui.home.HomeScreen
import com.counter.game.ui.nav.Routes
import com.counter.game.ui.newgame.NewGameScreen
import com.counter.game.ui.player.PlayerDetailScreen
import com.counter.game.ui.players.PlayersScreen
import com.counter.game.ui.round.RoundInputScreen
import com.counter.game.ui.rules.RuleEditScreen
import com.counter.game.ui.rules.RulesScreen
import com.counter.game.ui.settings.SettingsScreen
import com.counter.game.ui.statistics.StatisticsScreen
import com.counter.game.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CounterApp).container
        val fontScaleFlow = container.settingsRepository.observe()
        setContent {
            val settings by fontScaleFlow.collectAsState(initial = null)
            val scale = settings?.fontScale ?: 1.0f
            MyApplicationTheme(fontScale = scale) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CounterNavHost(container = container)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CounterNavHost(container: com.counter.game.AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Home.path) {
        composable(Routes.Home.path) {
            HomeScreen(
                container = container,
                onContinue = { gameId ->
                    navController.navigate(Routes.Game.build(gameId)) {
                        popUpTo(Routes.Home.path)
                    }
                },
                onNewGame = { navController.navigate(Routes.NewGame.path) },
                onPlayers = { navController.navigate(Routes.Players.path) },
                onStatistics = { navController.navigate(Routes.Statistics.path) },
                onSettings = { navController.navigate(Routes.Settings.path) },
            )
        }
        composable(Routes.Players.path) {
            PlayersScreen(container = container, onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings.path) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenRules = { navController.navigate(Routes.Rules.path) },
            )
        }
        composable(Routes.Rules.path) {
            RulesScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onEditRule = { id -> navController.navigate(Routes.RuleEdit.build(id)) },
            )
        }
        composable(
            route = Routes.RuleEdit.path,
            arguments = listOf(navArgument(Routes.RuleEdit.ARG) {
                type = NavType.LongType
                defaultValue = -1L
            }),
        ) { entry ->
            val raw = entry.arguments?.getLong(Routes.RuleEdit.ARG) ?: -1L
            val id: Long? = if (raw <= 0L) null else raw
            RuleEditScreen(
                container = container,
                ruleId = id,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.NewGame.path) {
            NewGameScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onGameStarted = { gameId ->
                    navController.navigate(Routes.Game.build(gameId)) {
                        popUpTo(Routes.Home.path)
                    }
                },
            )
        }
        composable(
            route = Routes.Game.path,
            arguments = listOf(navArgument(Routes.Game.ARG) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.Game.ARG) ?: 0L
            GameScreen(
                container = container,
                gameId = id,
                onBack = { navController.popBackStack(Routes.Home.path, inclusive = false) },
                onRound = { gid -> navController.navigate(Routes.RoundInput.build(gid)) },
                onHistory = { gid -> navController.navigate(Routes.History.build(gid)) },
            )
        }
        composable(
            route = Routes.RoundInput.path,
            arguments = listOf(navArgument(Routes.RoundInput.ARG) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.RoundInput.ARG) ?: 0L
            RoundInputScreen(
                container = container,
                gameId = id,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.History.path,
            arguments = listOf(navArgument(Routes.History.ARG) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.History.ARG) ?: 0L
            HistoryScreen(container = container, gameId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.Statistics.path) {
            StatisticsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenGame = { id -> navController.navigate(Routes.History.build(id)) },
                onOpenPlayer = { id -> navController.navigate(Routes.PlayerDetail.build(id)) },
                onResumeGame = { id ->
                    // Продолжить игру: тот же сценарий, что и «Продолжить» с главного.
                    navController.navigate(Routes.Game.build(id)) {
                        popUpTo(Routes.Home.path)
                    }
                },
            )
        }
        composable(
            route = Routes.PlayerDetail.path,
            arguments = listOf(navArgument(Routes.PlayerDetail.ARG) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.PlayerDetail.ARG) ?: 0L
            PlayerDetailScreen(
                container = container,
                playerId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}