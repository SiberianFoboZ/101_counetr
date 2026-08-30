package com.counter.game.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.counter.game.AppContainer
import com.counter.game.ui.game.GameViewModel
import com.counter.game.ui.history.HistoryViewModel
import com.counter.game.ui.home.HomeViewModel
import com.counter.game.ui.newgame.NewGameViewModel
import com.counter.game.ui.players.PlayersViewModel
import com.counter.game.ui.round.RoundInputViewModel
import com.counter.game.ui.rules.RulesViewModel
import com.counter.game.ui.settings.SettingsViewModel
import com.counter.game.ui.statistics.StatisticsViewModel

fun viewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { HomeViewModel(container) }
    initializer { PlayersViewModel(container) }
    initializer { SettingsViewModel(container) }
    initializer { NewGameViewModel(container) }
    initializer { GameViewModel(container) }
    initializer { RoundInputViewModel(container) }
    initializer { HistoryViewModel(container) }
    initializer { RulesViewModel(container) }
    initializer { StatisticsViewModel(container) }
}