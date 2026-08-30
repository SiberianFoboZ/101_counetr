package com.counter.game.data.repo

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.counter.game.data.db.AppDatabase
import com.counter.game.data.db.DatabaseSeeder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamesRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: GamesRepository

    @Before
    fun setUp() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Сразу сидим, минуя callback (in-memory создаётся без onCreate с нашей логикой).
        val handle = db.openHelper.writableDatabase
        DatabaseSeeder.seed(handle as SupportSQLiteDatabase)
        // Создаём репозитории.
        val settings = SettingsRepository(db.settingsDao())
        val players = PlayersRepository(db.playerDao())
        val cards = CardDefinitionsRepository(db.cardDefinitionDao())
        val rules = RulesRepository(db.ruleDao())
        repo = GamesRepository(
            db = db,
            gameDao = db.gameDao(),
            gamePlayerDao = db.gamePlayerDao(),
            roundDao = db.roundDao(),
            roundEntryDao = db.roundEntryDao(),
            rulesRepo = rules,
            cardRepo = cards,
            settingsRepo = settings,
        )
        // 12 игроков для теста лимитов.
        repeat(12) { players.add("Игрок ${it + 1}") }
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun rejectsFewerThan2() {
        val ids = db.playerDao().listActive().take(1).map { it.id }
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.startGame(NewGameInput(thresholdScore = 101, selectedPlayerIds = ids)) }
        }
        assertTrue(ex.message!!.contains("2"))
    }

    @Test
    fun rejectsMoreThan10() {
        val ids = db.playerDao().listActive().take(11).map { it.id }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.startGame(NewGameInput(thresholdScore = 101, selectedPlayerIds = ids)) }
        }
    }

    @Test
    fun startGameSucceedsFor2to10() {
        val ids = db.playerDao().listActive().take(10).map { it.id }
        val gameId = runBlocking { repo.startGame(NewGameInput(thresholdScore = 101, selectedPlayerIds = ids)) }
        assertTrue(gameId > 0)
        assertEquals(10, db.gamePlayerDao().listByGame(gameId).size)
    }

    @Test
    fun pauseChangesStatus() {
        val ids = db.playerDao().listActive().take(3).map { it.id }
        val gameId = runBlocking { repo.startGame(NewGameInput(thresholdScore = 101, selectedPlayerIds = ids)) }
        runBlocking { repo.pause(gameId) }
        assertEquals("PAUSED", db.gameDao().byId(gameId)?.status)
    }
}