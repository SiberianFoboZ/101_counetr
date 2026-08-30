package com.counter.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleSerializerTest {

    @Test
    fun `round trip any nominal and base value actions`() {
        val form = RuleSerializer.Form(
            nominal = RuleSerializer.NominalForm.AnyNominal,
            suit = RuleSerializer.SuitForm.ANY,
            condition = null,
            then = RuleSerializer.ActionForm.BaseValue,
            elseAction = RuleSerializer.ActionForm.BaseValue,
        )
        val json = RuleSerializer.encode(form)
        val decoded = RuleSerializer.decode(json)
        assertEquals(form, decoded)
    }

    @Test
    fun `round trip with eq nominal suit and condition`() {
        val form = RuleSerializer.Form(
            nominal = RuleSerializer.NominalForm.Eq(4),
            suit = RuleSerializer.SuitForm.SPADES,
            condition = RuleSerializer.ConditionForm(
                op = RuleSerializer.OpForm.GE,
                left = RuleSerializer.OperandForm.CARD_COUNT,
                rightValue = 2,
            ),
            then = RuleSerializer.ActionForm.Const(50),
            elseAction = RuleSerializer.ActionForm.Const(4),
        )
        val json = RuleSerializer.encode(form)
        val decoded = RuleSerializer.decode(json)
        assertEquals(form, decoded)
    }

    @Test
    fun `round trip with setting action`() {
        val form = RuleSerializer.Form(
            nominal = RuleSerializer.NominalForm.AnyNominal,
            suit = RuleSerializer.SuitForm.NON_SPADES,
            condition = null,
            then = RuleSerializer.ActionForm.Setting("queen_value"),
            elseAction = RuleSerializer.ActionForm.Setting("queen_value"),
        )
        val json = RuleSerializer.encode(form)
        val decoded = RuleSerializer.decode(json)
        assertEquals(form, decoded)
    }

    @Test
    fun `decode parses real seed json`() {
        val seed = """
            {
              "version": 2,
              "match": {
                "nominal": {"op":"==","value":4},
                "suit": "spades",
                "condition": {"op":">=","left":"card_count","right":{"type":"const","value":2}}
              },
              "then": {"type":"const","value":50},
              "else": {"type":"const","value":4}
            }
        """.trimIndent()
        val form = RuleSerializer.decode(seed)
        assertNotNull(form)
        assertEquals(RuleSerializer.NominalForm.Eq(4), form!!.nominal)
        assertEquals(RuleSerializer.SuitForm.SPADES, form.suit)
        assertEquals(2, form.condition!!.rightValue)
    }

    @Test
    fun `decode returns null on garbage`() {
        assertNull(RuleSerializer.decode("not a json"))
    }

    @Test
    fun `final adjustment rule round trip with subtract total`() {
        val form = RuleSerializer.Form(
            kind = RuleSerializer.Kind.FINAL_ADJUSTMENT,
            condition = RuleSerializer.ConditionForm(
                op = RuleSerializer.OpForm.GE,
                left = RuleSerializer.OperandForm.TOTAL_SCORE,
                rightValue = 101,
            ),
            then = RuleSerializer.ActionForm.SubtractTotal,
            elseAction = RuleSerializer.ActionForm.SubtractTotal,
        )
        val json = RuleSerializer.encode(form)
        val decoded = RuleSerializer.decode(json)
        assertEquals(form, decoded)
    }

    @Test
    fun `decode parses final adjustment json`() {
        val raw = """
            {
              "version": 2,
              "kind": "final_adjustment",
              "match": {
                "nominal": {"op":"any"},
                "suit": "any",
                "condition": {"op":">=","left":"total_score","right":{"type":"const","value":101}}
              },
              "then": {"type":"subtract_total"},
              "else": {"type":"const","value":0}
            }
        """.trimIndent()
        val form = RuleSerializer.decode(raw)
        assertNotNull(form)
        assertEquals(RuleSerializer.Kind.FINAL_ADJUSTMENT, form!!.kind)
        assertEquals(RuleSerializer.OperandForm.TOTAL_SCORE, form.condition?.left)
        assertEquals(101, form.condition?.rightValue)
        assertEquals(RuleSerializer.ActionForm.SubtractTotal, form.then)
    }

    @Test
    fun `decode defaults to per_card when kind missing`() {
        // Старые правила без поля kind — должны парситься как PER_CARD.
        val raw = """
            {
              "version": 2,
              "match": {
                "nominal": {"op":"any"},
                "suit": "any"
              },
              "then": {"type":"base_value"},
              "else": {"type":"base_value"}
            }
        """.trimIndent()
        val form = RuleSerializer.decode(raw)
        assertNotNull(form)
        assertEquals(RuleSerializer.Kind.PER_CARD, form!!.kind)
    }
}