package com.counter.game.ui.round

/**
 * Лимиты на количество карт в колоде и в одной руке.
 *
 * Правила по умолчанию (без учёта winnerDelta):
 * - Дама пик (Q_spades) — в колоде 1, поэтому макс 1 суммарно по раунду.
 * - Остальные дамы (Q_hearts/diamonds/clubs) — по 3 в колоде каждая, поэтому макс 3 на код.
 * - Общий лимит на номинал «дама» — 4 в колоде, поэтому 4 суммарно по раунду.
 * - Аналогично для королей.
 * - Базовые карты (6, 7, 8, 9, 10, J, A) — по 4 в колоде, поэтому макс 4 суммарно по раунду.
 *
 * Штрафы победителя (winnerDelta):
 * - `-40` — Q_spades заблокирована для остальных игроков (одну даму пик забрал победитель).
 * - `-50` — K_spades заблокирована для остальных.
 * - `-20` — общий пул дам уменьшен на 1 (было 4, стало 3) и индивидуальный лимит не-пик дам = 2 (было 3).
 *
 * Лимиты считаются **по всему раунду** (по всем игрокам), а не только в руке одного.
 */
object CardLimits {
    const val QUEEN_SPADES_MAX = 1
    const val QUEEN_OTHER_MAX = 3
    const val QUEEN_TOTAL_MAX = 4
    const val KING_SPADES_MAX = 1
    const val KING_OTHER_MAX = 3
    const val KING_TOTAL_MAX = 4
    const val BASIC_MAX = 4
}

sealed interface AddCardResult {
    /** Можно добавить. */
    data object Ok : AddCardResult

    /** Превышен индивидуальный лимит кода (например, 2-я дама пик). */
    data class CodeLimitExceeded(val current: Int, val max: Int) : AddCardResult

    /** Превышен общий лимит номинала (например, 5-я дама суммарно). */
    data class NominalLimitExceeded(val current: Int, val max: Int) : AddCardResult

    /**
     * Карта заблокирована из-за дельты победителя в текущем раунде.
     * Например, победитель набрал −40 → Q_spades другим недоступна.
     */
    data class BlockedByWinnerDelta(val winnerDelta: Int, val reason: String) : AddCardResult
}

/**
 * @param allHands все руки раунда: gamePlayerId -> (cardCode -> count)
 * @param gamePlayerId игрок, который пытается добавить карту
 * @param code код добавляемой карты
 * @param winnerDelta дельта победителя в текущем раунде. Если 0 — обычные лимиты.
 */
fun canAddCard(
    allHands: Map<Long, Map<String, Int>>,
    gamePlayerId: Long,
    code: String,
    winnerDelta: Int = 0,
): AddCardResult {
    val perPlayer = allHands[gamePlayerId].orEmpty()
    val current = perPlayer[code] ?: 0
    val globalForCode = sumAcrossPlayers(allHands, code)

    // Штрафы победителя: блокировка конкретных кодов.
    if (winnerDelta == -40 && code == "Q_spades") {
        return AddCardResult.BlockedByWinnerDelta(
            winnerDelta = -40,
            reason = "Победитель набрал −40: дама пик уже разыграна, другим игрокам недоступна.",
        )
    }
    if (winnerDelta == -50 && code == "K_spades") {
        return AddCardResult.BlockedByWinnerDelta(
            winnerDelta = -50,
            reason = "Победитель набрал −50: король пик уже разыгран, другим игрокам недоступен.",
        )
    }

    // Штраф -20 уменьшает пул дам: 1 дама ушла, остальные 3 раскидываются.
    // Индивидуальный лимит не-пик дам становится 2 (было 3).
    val queenTotalMax = if (winnerDelta == -20) 3 else CardLimits.QUEEN_TOTAL_MAX
    val queenOtherMax = if (winnerDelta == -20) 2 else CardLimits.QUEEN_OTHER_MAX

    return when {
        code == "Q_spades" -> checkSpade(
            perPlayerCount = current,
            globalForCode = globalForCode,
            nominalTotal = queenTotalInRound(allHands),
            codeMax = CardLimits.QUEEN_SPADES_MAX,
            nominalMax = queenTotalMax,
        )
        code.startsWith("Q_") -> checkWithNominal(
            codeCurrent = current,
            codeMax = queenOtherMax,
            nominalTotal = queenTotalInRound(allHands),
            nominalMax = queenTotalMax,
        )
        code == "K_spades" -> checkSpade(
            perPlayerCount = current,
            globalForCode = globalForCode,
            nominalTotal = kingTotalInRound(allHands),
            codeMax = CardLimits.KING_SPADES_MAX,
            nominalMax = CardLimits.KING_TOTAL_MAX,
        )
        code.startsWith("K_") -> checkWithNominal(
            codeCurrent = current,
            codeMax = CardLimits.KING_OTHER_MAX,
            nominalTotal = kingTotalInRound(allHands),
            nominalMax = CardLimits.KING_TOTAL_MAX,
        )
        else -> {
            // Базовые карты: индивидуальный и глобальный лимит совпадают (4 в колоде).
            if (current >= CardLimits.BASIC_MAX) {
                AddCardResult.CodeLimitExceeded(current, CardLimits.BASIC_MAX)
            } else if (globalForCode >= CardLimits.BASIC_MAX) {
                AddCardResult.NominalLimitExceeded(globalForCode, CardLimits.BASIC_MAX)
            } else {
                AddCardResult.Ok
            }
        }
    }
}

private fun checkSpade(
    perPlayerCount: Int,
    globalForCode: Int,
    nominalTotal: Int,
    codeMax: Int,
    nominalMax: Int,
): AddCardResult = when {
    perPlayerCount >= codeMax -> AddCardResult.CodeLimitExceeded(perPlayerCount, codeMax)
    globalForCode >= codeMax -> AddCardResult.NominalLimitExceeded(globalForCode, codeMax)
    nominalTotal >= nominalMax -> AddCardResult.NominalLimitExceeded(nominalTotal, nominalMax)
    else -> AddCardResult.Ok
}

private fun checkWithNominal(
    codeCurrent: Int,
    codeMax: Int,
    nominalTotal: Int,
    nominalMax: Int,
): AddCardResult = when {
    codeCurrent >= codeMax -> AddCardResult.CodeLimitExceeded(codeCurrent, codeMax)
    nominalTotal >= nominalMax -> AddCardResult.NominalLimitExceeded(nominalTotal, nominalMax)
    else -> AddCardResult.Ok
}

private fun sumAcrossPlayers(allHands: Map<Long, Map<String, Int>>, code: String): Int =
    allHands.values.sumOf { it[code] ?: 0 }

private fun queenTotalInRound(allHands: Map<Long, Map<String, Int>>): Int =
    allHands.values.sumOf { hand -> hand.entries.filter { it.key.startsWith("Q_") }.sumOf { it.value } }

private fun kingTotalInRound(allHands: Map<Long, Map<String, Int>>): Int =
    allHands.values.sumOf { hand -> hand.entries.filter { it.key.startsWith("K_") }.sumOf { it.value } }
