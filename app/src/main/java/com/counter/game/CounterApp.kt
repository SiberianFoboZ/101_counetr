package com.counter.game

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.counter.game.data.db.AppDatabase
import com.counter.game.data.db.DatabaseSeeder
import com.counter.game.data.repo.CardDefinitionsRepository
import com.counter.game.data.repo.GamesRepository
import com.counter.game.data.repo.PlayersRepository
import com.counter.game.data.repo.RulesRepository
import com.counter.game.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CounterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.build(this)
        container.seedIfNeeded()
    }
}

class AppContainer private constructor(
    val database: AppDatabase,
    val playersRepository: PlayersRepository,
    val settingsRepository: SettingsRepository,
    val rulesRepository: RulesRepository,
    val gamesRepository: GamesRepository,
    val cardDefinitionsRepository: CardDefinitionsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfNeeded() {
        scope.launch {
            val cards = database.cardDefinitionDao().count()
            val rules = database.ruleDao().count()
            if (cards > 0 && rules > 0) return@launch
            val handle = database.openHelper.writableDatabase as SupportSQLiteDatabase
            DatabaseSeeder.seed(handle)
        }
    }

    companion object {
        fun build(app: CounterApp): AppContainer {
            val db = Room.databaseBuilder(
                app.applicationContext,
                AppDatabase::class.java,
                "101_counter.db",
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build()

            val settingsRepo = SettingsRepository(db.settingsDao())
            val playersRepo = PlayersRepository(db.playerDao())
            val cardRepo = CardDefinitionsRepository(db.cardDefinitionDao())
            val rulesRepo = RulesRepository(db.ruleDao())
            val gamesRepo = GamesRepository(
                db = db,
                gameDao = db.gameDao(),
                gamePlayerDao = db.gamePlayerDao(),
                roundDao = db.roundDao(),
                roundEntryDao = db.roundEntryDao(),
                rulesRepo = rulesRepo,
                cardRepo = cardRepo,
                settingsRepo = settingsRepo,
            )

            return AppContainer(
                database = db,
                playersRepository = playersRepo,
                settingsRepository = settingsRepo,
                rulesRepository = rulesRepo,
                gamesRepository = gamesRepo,
                cardDefinitionsRepository = cardRepo,
            )
        }
    }
}