package com.counter.game.engine

import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.data.entity.RuleEntity
import com.counter.game.data.entity.SettingsEntity
import com.counter.game.data.entity.RoundCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEngineTest {

    private val cardDefs: Map<String, CardDefinitionEntity> = listOf(
        CardDefinitionEntity(code = "6", label = "6", baseValue = 6, isFace = false),
        CardDefinitionEntity(code = "9", label = "9", baseValue = 0, isFace = false),
        CardDefinitionEntity(code = "J", label = "Валет", baseValue = 2, isFace = true),
        CardDefinitionEntity(code = "A", label = "Туз", baseValue = 11, isFace = true),
        CardDefinitionEntity(code = "Q_hearts", label = "Дама ♥", baseValue = 3, isFace = true),
        CardDefinitionEntity(code = "Q_diamonds", label = "Дама ♦", baseValue = 3, isFace = true),
        CardDefinitionEntity(code = "Q_clubs", label = "Дама ♣", baseValue = 3, isFace = true),
        CardDefinitionEntity(code = "Q_spades", label = "Дама ♠", baseValue = 3, isFace = true),
        CardDefinitionEntity(code = "K_hearts", label = "Король ♥", baseValue = 4, isFace = true),
        CardDefinitionEntity(code = "K_diamonds", label = "Король ♦", baseValue = 4, isFace = true),
        CardDefinitionEntity(code = "K_clubs", label = "Король ♣", baseValue = 4, isFace = true),
        CardDefinitionEntity(code = "K_spades", label = "Король ♠", baseValue = 4, isFace = true),
    ).associateBy { it.code }

    private val settings = SettingsEntity(queenValue = 3, queenSpadesValue = 25, kingValue = 4)

    private fun rule(name: String, code: String, priority: Int, json: String) = RuleEntity(
        name = name,
        appliesToCard = code,
        priority = priority,
        enabled = true,
        definitionJson = json,
    )

    @Test
    fun `king of spades with single card costs 4`() {
        val rules = listOf(
            rule("Король пик", "K_spades", 100, """
                { "version": 2,
                  "match": { "nominal": {"op":"==","value":4}, "suit":"spades",
                             "condition": {"op":">=","left":"card_count","right":{"type":"const","value":2}} },
                  "then": {"type":"const","value":50},
                  "else": {"type":"const","value":4} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("K_spades" to 1))
        assertEquals(4, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `king of spades with 3 cards costs 50`() {
        val rules = listOf(
            rule("Король пик", "K_spades", 100, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"spades",
                             "condition": {"op":">=","left":"card_count","right":{"type":"const","value":2}} },
                  "then": {"type":"const","value":50},
                  "else": {"type":"const","value":4} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("K_spades" to 3))
        assertEquals(50, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `queen of spades always 25`() {
        val rules = listOf(
            rule("Дама пик", "Q_spades", 90, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"spades" },
                  "then": {"type":"const","value":25},
                  "else": {"type":"const","value":25} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("Q_spades" to 2))
        assertEquals(25, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `queen of hearts uses queenValue`() {
        val rules = listOf(
            rule("Дама", "Q_hearts", 50, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"non_spades" },
                  "then": {"type":"setting","key":"queen_value"},
                  "else": {"type":"setting","key":"queen_value"} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("Q_hearts" to 1))
        assertEquals(3, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `basic card 9 contributes 0`() {
        val rules = listOf(
            rule("Базовая: 9", "9", 10, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"any" },
                  "then": {"type":"base_value"},
                  "else": {"type":"base_value"} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("9" to 5))
        assertEquals(0, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `two aces give 22`() {
        val rules = listOf(
            rule("Базовая: Туз", "A", 10, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"any" },
                  "then": {"type":"base_value"},
                  "else": {"type":"base_value"} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf("A" to 2))
        assertEquals(22, RuleEngine().compute(rules, ctx))
    }

    @Test
    fun `empty hand contributes 0`() {
        val rules = listOf(
            rule("Базовая: 6", "6", 10, """
                { "version": 2,
                  "match": { "nominal": {"op":"any"}, "suit":"any" },
                  "then": {"type":"base_value"},
                  "else": {"type":"base_value"} }
            """.trimIndent()),
        )
        val ctx = RuleContext(settings = settings, cardDefs = cardDefs, hand = handOf())
        assertEquals(0, RuleEngine().compute(rules, ctx))
    }

    private fun handOf(vararg pairs: Pair<String, Int>): RoundHand {
        return RoundHand(
            cards = pairs.map { (code, count) ->
                RoundCardEntity(roundEntryId = 1L, cardCode = code, count = count)
            },
        )
    }

    @Test
    fun `final adjustment subtracts total when total reaches 101`() {
        // Per-card правило: 1 шестёрка → +6.
        val perCard = rule("Базовая: 6", "6", 10, """
            { "version": 2,
              "match": { "nominal": {"op":"==","value":6}, "suit":"any" },
              "then": {"type":"const","value":6},
              "else": {"type":"const","value":0} }
        """.trimIndent())
        // Final-adjustment: если total >= 101 — вычесть total.
        val finalRule = rule(
            name = "Обнуление при 101",
            code = "_final",
            priority = 200,
            json = """
                { "version": 2,
                  "kind": "final_adjustment",
                  "match": { "nominal": {"op":"any"}, "suit":"any",
                             "condition": {"op":">=","left":"total_score","right":{"type":"const","value":101}} },
                  "then": {"type":"subtract_total"},
                  "else": {"type":"const","value":0} }
            """.trimIndent(),
        )
        // totalScoreBefore = 100, после +6 от per-card должно быть 106, финал: -106.
        val ctx = RuleContext(
            settings = settings,
            cardDefs = cardDefs,
            hand = handOf("6" to 1),
            totalScoreBeforeRound = 100,
        )
        val delta = RuleEngine().compute(listOf(perCard, finalRule), ctx)
        assertEquals(6 + (-106), delta) // = -100
    }

    @Test
    fun `final adjustment does nothing when total stays below 101`() {
        val finalRule = rule(
            name = "Обнуление при 101",
            code = "_final",
            priority = 200,
            json = """
                { "version": 2,
                  "kind": "final_adjustment",
                  "match": { "nominal": {"op":"any"}, "suit":"any",
                             "condition": {"op":">=","left":"total_score","right":{"type":"const","value":101}} },
                  "then": {"type":"subtract_total"},
                  "else": {"type":"const","value":0} }
            """.trimIndent(),
        )
        val ctx = RuleContext(
            settings = settings,
            cardDefs = cardDefs,
            hand = handOf(),
            totalScoreBeforeRound = 50,
        )
        assertEquals(0, RuleEngine().compute(listOf(finalRule), ctx))
    }
}