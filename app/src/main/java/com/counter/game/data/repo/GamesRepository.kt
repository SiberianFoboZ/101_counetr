package com.counter.game.data.repo

import androidx.room.withTransaction
import com.counter.game.data.dao.GameDao
import com.counter.game.data.dao.GamePlayerDao
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.dao.RoundDao
import com.counter.game.data.dao.RoundEntryDao
import com.counter.game.data.db.AppDatabase
import com.counter.game.data.entity.GameEntity
import com.counter.game.data.entity.GamePlayerEntity
import com.counter.game.data.entity.GameStatus
import com.counter.game.data.entity.RoundCardEntity
import com.counter.game.data.entity.RoundEntryEntity
import com.counter.game.data.entity.RoundEntity
import com.counter.game.engine.RuleContext
import com.counter.game.engine.RuleEngine
import com.counter.game.engine.RoundHand
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class NewGameInput(
    val thresholdScore: Int,
    val selectedPlayerIds: List<Long>,
    val displayNames: Map<Long, String> = emptyMap(),
)

data class RoundInput(
    val gameId: Long,
    val winnerGamePlayerId: Long?,
    val winnerDelta: Int = 0,
    val hands: Map<Long, Map<String, Int>> = emptyMap(),
    /**
     * Ручной режим ввода: дельта для каждого проигравшего (по gamePlayerId).
     * Если задано — для проигравших движок правил не вызывается, `round_cards` не пишутся,
     * `delta_score` берётся напрямую из карты. Для победителя по-прежнему применяется
     * `winnerDelta`. В БД пометка `mode=manual` пишется в `raw_input_json`.
     */
    val manualDelta: Map<Long, Int>? = null,
)

class GamesRepository(
    private val db: AppDatabase,
    private val gameDao: GameDao,
    private val gamePlayerDao: GamePlayerDao,
    private val roundDao: RoundDao,
    private val roundEntryDao: RoundEntryDao,
    private val rulesRepo: RulesRepository,
    private val cardRepo: CardDefinitionsRepository,
    private val settingsRepo: SettingsRepository,
) {
    private val engine = RuleEngine()

    suspend fun startGame(input: NewGameInput): Long {
        require(input.selectedPlayerIds.size in 2..10) {
            "Game must have between 2 and 10 players, got ${input.selectedPlayerIds.size}"
        }
        return db.withTransaction {
            val gameId = gameDao.insert(
                GameEntity(
                    status = GameStatus.IN_PROGRESS.name,
                    thresholdScore = input.thresholdScore,
                ),
            )
            val players = input.selectedPlayerIds.mapIndexed { idx, pid ->
                GamePlayerEntity(
                    gameId = gameId,
                    playerId = pid,
                    displayName = input.displayNames[pid].orEmpty(),
                    seatOrder = idx,
                )
            }
            gamePlayerDao.insertAll(players)
            gameId
        }
    }

    suspend fun pause(gameId: Long) {
        gameDao.setStatus(gameId, GameStatus.PAUSED.name)
    }

    suspend fun resume(gameId: Long) {
        gameDao.setStatus(gameId, GameStatus.IN_PROGRESS.name)
    }

    suspend fun finish(gameId: Long) {
        gameDao.finish(
            id = gameId,
            finishedAt = System.currentTimeMillis(),
            winnerId = null,
        )
    }

    suspend fun latestPausedId(): Long? = gameDao.latestByStatus(GameStatus.PAUSED.name)?.id

    suspend fun saveRound(input: RoundInput): RoundResult = db.withTransaction {
        val settings = settingsRepo.get()
        val cardDefs = cardRepo.listAll().associateBy { it.code }
        val enabledRules = rulesRepo.listEnabled()

        // Парсим правила ОДИН РАЗ для всего раунда, чтобы не парсить 12×N раз.
        val parsedRules = enabledRules.mapNotNull { rule ->
            val def = com.counter.game.engine.RuleDefinition.parseOrNull(rule.definitionJson)
                ?: return@mapNotNull null
            rule to def
        }

        val roundNumber = roundDao.maxRoundNumber(input.gameId) + 1
        val roundId = roundDao.insert(
            RoundEntity(gameId = input.gameId, roundNumber = roundNumber),
        )
        val gamePlayers = gamePlayerDao.listByGame(input.gameId)
        val winnerId = input.winnerGamePlayerId

        val manual = input.manualDelta
        val isManual = manual != null

        for (gp in gamePlayers) {
            val isWinner = gp.id == winnerId
            val hand: Map<String, Int> = input.hands[gp.id].orEmpty()
            val rawJson = when {
                isManual -> encodeManual(isWinner, manual!![gp.id], input.winnerDelta)
                else -> encodeHand(hand)
            }
            val entryId = roundEntryDao.insertEntry(
                RoundEntryEntity(
                    roundId = roundId,
                    gamePlayerId = gp.id,
                    rawInputJson = rawJson,
                ),
            )

            val delta: Int = when {
                isWinner -> input.winnerDelta
                isManual -> manual!![gp.id] ?: 0
                else -> {
                    val cards = hand
                        .filter { it.value > 0 }
                        .map { (code, count) -> RoundCardEntity(roundEntryId = entryId, cardCode = code, count = count) }
                    if (cards.isNotEmpty()) roundEntryDao.insertCards(cards)
                    val ctx = RuleContext(
                        settings = settings,
                        cardDefs = cardDefs,
                        hand = RoundHand(cards = cards),
                        totalScoreBeforeRound = gamePlayerDao.totalScore(input.gameId, gp.id),
                    )
                    engine.computeWith(parsedRules, ctx)
                }
            }
            roundEntryDao.updateDelta(entryId, delta)
        }

        // Пересчёт итогов и проверка победителя.
        val scores = gamePlayers.map { gp ->
            GamePlayerWithScore(
                id = gp.id,
                playerId = gp.playerId,
                displayName = gp.displayName,
                seatOrder = gp.seatOrder,
                totalScore = gamePlayerDao.totalScore(input.gameId, gp.id),
            )
        }

        val winnerPlayerId = finishIfNeeded(input.gameId, scores, settings.thresholdScore)

        RoundResult(
            gameId = input.gameId,
            roundNumber = roundNumber,
            scores = scores,
            winnerPlayerId = winnerPlayerId,
            thresholdScore = settings.thresholdScore,
        )
    }

    private suspend fun finishIfNeeded(
        gameId: Long,
        scores: List<GamePlayerWithScore>,
        threshold: Int,
    ): Long? {
        val alive = scores.filter { it.totalScore <= threshold }
        if (scores.size > 1 && alive.size == 1) {
            val winner = alive.single()
            gameDao.finish(
                id = gameId,
                finishedAt = System.currentTimeMillis(),
                winnerId = winner.playerId,
            )
            return winner.playerId
        }
        return null
    }

    private fun encodeHand(hand: Map<String, Int>): String = buildJsonObject {
        for ((code, count) in hand) {
            put(code, count)
        }
    }.toString()

    private fun encodeManual(isWinner: Boolean, manualDelta: Int?, winnerDelta: Int): String =
        buildJsonObject {
            put("mode", "manual")
            if (isWinner) {
                put("delta", winnerDelta)
            } else {
                put("delta", manualDelta ?: 0)
            }
        }.toString()
}