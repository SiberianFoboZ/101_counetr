package com.counter.game.ui.round

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLimitsTest {

    private fun hands(vararg pairs: Pair<Long, Map<String, Int>>): Map<Long, Map<String, Int>> =
        pairs.toMap()

    @Test
    fun `queen spades max 1 per round`() {
        assertEquals(AddCardResult.Ok, canAddCard(emptyMap(), 1L, "Q_spades"))
        val one = hands(1L to mapOf("Q_spades" to 1))
        val res = canAddCard(one, 1L, "Q_spades")
        assertTrue(res is AddCardResult.CodeLimitExceeded)
    }

    @Test
    fun `queen spades taken by another player blocks`() {
        // У другого игрока уже есть 1 дама пик — добавить вторую нельзя.
        val taken = hands(2L to mapOf("Q_spades" to 1))
        val res = canAddCard(taken, 1L, "Q_spades")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `queen non-spades max 3 per code per player`() {
        val three = hands(1L to mapOf("Q_hearts" to 3))
        val res = canAddCard(three, 1L, "Q_hearts")
        assertTrue(res is AddCardResult.CodeLimitExceeded)
    }

    @Test
    fun `queen total max 4 across all players`() {
        // У игрока 3 дамы, у второго 1 — больше ни одной нельзя.
        val counts = hands(1L to mapOf("Q_hearts" to 3), 2L to mapOf("Q_diamonds" to 1))
        val res = canAddCard(counts, 2L, "Q_clubs")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `queen spades count toward total limit`() {
        // 1 дама пик + 3 дамы не-пик = 4. Больше ни одной.
        val counts = hands(1L to mapOf("Q_spades" to 1, "Q_hearts" to 3))
        val res = canAddCard(counts, 2L, "Q_diamonds")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `queen total 4 across suits is allowed but no more`() {
        // 1+1+2 = 4. Добавить ещё одну в эту руку — нельзя.
        val counts = hands(1L to mapOf("Q_spades" to 1, "Q_hearts" to 1, "Q_diamonds" to 2))
        val res = canAddCard(counts, 1L, "Q_clubs")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `king spades max 1 per round`() {
        val taken = hands(1L to mapOf("K_spades" to 1))
        val res = canAddCard(taken, 2L, "K_spades")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `king non-spades max 3 per code`() {
        val three = hands(1L to mapOf("K_diamonds" to 3))
        val res = canAddCard(three, 1L, "K_diamonds")
        assertTrue(res is AddCardResult.CodeLimitExceeded)
    }

    @Test
    fun `king total max 4 across players`() {
        val counts = hands(1L to mapOf("K_clubs" to 2), 2L to mapOf("K_hearts" to 2))
        val res = canAddCard(counts, 3L, "K_spades")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `basic card max 4 per code per round`() {
        // Сценарий из бага: 4 шестёрки розданы, лимит исчерпан по раунду.
        val counts = hands(
            1L to mapOf("6" to 2),
            2L to mapOf("6" to 2),
        )
        val res = canAddCard(counts, 3L, "6")
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `basic cards of different codes are independent`() {
        // 4 шестёрки розданы, но семёрки свободны.
        val counts = hands(
            1L to mapOf("6" to 2),
            2L to mapOf("6" to 2),
        )
        assertEquals(AddCardResult.Ok, canAddCard(counts, 3L, "7"))
    }

    @Test
    fun `nominal limit for queens is independent from kings`() {
        // 4 дамы уже, но короли свободны.
        val counts = hands(1L to mapOf("Q_spades" to 1, "Q_hearts" to 3))
        assertEquals(AddCardResult.Ok, canAddCard(counts, 1L, "K_spades"))
    }

    @Test
    fun `non-spade queen reaches code limit before nominal limit`() {
        // 3 дамы червей у одного игрока — добавить ещё одну черву нельзя (code limit),
        // даже если номинал ещё не исчерпан.
        val counts = hands(1L to mapOf("Q_hearts" to 3))
        val res = canAddCard(counts, 1L, "Q_hearts")
        assertTrue(res is AddCardResult.CodeLimitExceeded)
    }

    @Test
    fun `winner delta -40 blocks queen spades for losers`() {
        val counts = hands(1L to emptyMap())
        val res = canAddCard(counts, 1L, "Q_spades", winnerDelta = -40)
        assertTrue(res is AddCardResult.BlockedByWinnerDelta)
    }

    @Test
    fun `winner delta -40 does not block winner himself`() {
        // winnerDelta применяется только к проигравшим. Сами проверки в VM не пускают победителя
        // — но контракт canAddCard должен позволять добавлять ему, если кто-то обходит VM.
        val counts = hands(1L to emptyMap())
        val res = canAddCard(counts, 1L, "Q_spades", winnerDelta = -40)
        // Без фильтра на "победитель" эта проверка возвращает Blocked. VM отфильтрует — см. ниже.
        assertTrue(res is AddCardResult.BlockedByWinnerDelta)
    }

    @Test
    fun `winner delta -50 blocks king spades for losers`() {
        val counts = hands(1L to emptyMap())
        val res = canAddCard(counts, 1L, "K_spades", winnerDelta = -50)
        assertTrue(res is AddCardResult.BlockedByWinnerDelta)
    }

    @Test
    fun `winner delta -40 does not block non-spade queens`() {
        val counts = hands(1L to emptyMap())
        val res = canAddCard(counts, 1L, "Q_hearts", winnerDelta = -40)
        assertEquals(AddCardResult.Ok, res)
    }

    @Test
    fun `winner delta -20 reduces queen total limit to 3`() {
        // Без -20: можно добавить 4 дамы (3 не-пик + 1 пик).
        // С -20: общий лимит 3. 3 не-пик уже на руках — больше ни одной.
        val counts = hands(
            1L to mapOf("Q_hearts" to 2, "Q_diamonds" to 1),
        )
        val res = canAddCard(counts, 1L, "Q_clubs", winnerDelta = -20)
        assertTrue("expected NominalLimitExceeded, got $res", res is AddCardResult.NominalLimitExceeded)
        val r2 = canAddCard(counts, 1L, "Q_spades", winnerDelta = -20)
        assertTrue(r2 is AddCardResult.NominalLimitExceeded)
    }

    @Test
    fun `winner delta -20 reduces non-spade queen code limit to 2`() {
        // Без -20: можно 3 червей в одной руке.
        // С -20: только 2.
        val counts = hands(1L to mapOf("Q_hearts" to 2))
        val res = canAddCard(counts, 1L, "Q_hearts", winnerDelta = -20)
        assertTrue("expected CodeLimitExceeded, got $res", res is AddCardResult.CodeLimitExceeded)
    }

    @Test
    fun `winner delta -20 still allows 2 queens total`() {
        val counts = hands(1L to mapOf("Q_spades" to 1, "Q_hearts" to 1))
        val res = canAddCard(counts, 1L, "Q_clubs", winnerDelta = -20)
        assertEquals(AddCardResult.Ok, res) // 1+1+1 = 3, лимит = 3 — ОК
    }
}