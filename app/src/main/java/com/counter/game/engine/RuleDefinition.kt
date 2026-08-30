package com.counter.game.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * JSON-модель для `definition_json` правила (ТЗ, раздел 2.4).
 *
 * ```
 * {
 *   "version": 2,
 *   "match": {
 *     "nominal":   { "op": "==", "value": 4 } | { "op": "any" } | ...,
 *     "suit":      "spades" | "non_spades" | "any" | ...,
 *     "condition": { "op": ">=", "left": "card_count", "right": { "type": "const", "value": 2 } } | null
 *   },
 *   "then": { "type": "const" | "base_value" | "setting", ... },
 *   "else": { "type": "const" | "base_value" | "setting", ... }
 * }
 * ```
 */
object RuleDefinition {
    const val VERSION = 2

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): Rule {
        val obj = json.parseToJsonElement(raw).jsonObject
        val version = (obj["version"] as? JsonPrimitive)?.intOrNull ?: VERSION
        require(version == VERSION) { "Unsupported rule version: $version" }
        val match = (obj["match"] as JsonObject).toMatch()
        val then = (obj["then"] as JsonObject).toAction()
        val els = (obj["else"] as JsonObject).toAction()
        return Rule(match, then, els)
    }

    fun parseOrNull(raw: String): Rule? = try {
        parse(raw)
    } catch (_: Throwable) {
        null
    }

    data class Rule(
        val match: Match,
        val then: Action,
        val elseAction: Action,
    )

    data class Match(
        val nominal: NominalPredicate,
        val suit: SuitPredicate,
        val condition: Condition?,
    )

    sealed interface NominalPredicate {
        data object Any : NominalPredicate
        data class Eq(val value: Int) : NominalPredicate
        data class Ne(val value: Int) : NominalPredicate
        data class In(val values: List<Int>) : NominalPredicate
    }

    enum class SuitPredicate { ANY, SPADES, HEARTS, DIAMONDS, CLUBS, RED, BLACK, NON_SPADES }

    sealed interface Operand {
        data class Const(val value: Int) : Operand
        data object CardCount : Operand
        data object RoundDeltaSoFar : Operand
        data object DistinctCardCodes : Operand
    }

    enum class Op { EQ, NE, LT, LE, GT, GE }

    data class Condition(
        val op: Op,
        val left: Operand,
        val right: Operand,
    )

    sealed interface Action {
        data class Const(val value: Int) : Action
        data object BaseValue : Action
        data class Setting(val key: String) : Action
    }

    private fun JsonObject.toMatch(): Match {
        val nominal = (this["nominal"] as JsonObject).toNominal()
        val suit = (this["suit"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.let { runCatching { SuitPredicate.valueOf(it.uppercase()) }.getOrNull() }
            ?: SuitPredicate.ANY
        val condition = (this["condition"] as? JsonObject)?.toCondition()
        return Match(nominal, suit, condition)
    }

    private fun JsonObject.toNominal(): NominalPredicate {
        val op = (this["op"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return NominalPredicate.Any
        return when (op.lowercase()) {
            "any" -> NominalPredicate.Any
            "==" -> NominalPredicate.Eq((this["value"] as JsonPrimitive).int())
            "!=" -> NominalPredicate.Ne((this["value"] as JsonPrimitive).int())
            "in" -> NominalPredicate.In(
                (this["values"] as JsonArray).map { (it as JsonPrimitive).int() },
            )
            else -> NominalPredicate.Any
        }
    }

    private fun JsonObject.toCondition(): Condition {
        val opStr = (this["op"] as JsonPrimitive).content
        val op = runCatching { Op.valueOf(opStr.uppercase()) }.getOrElse {
            throw IllegalArgumentException("Unknown condition op: $opStr")
        }
        val left = this["left"]!!.toOperand()
        val right = this["right"]!!.toOperand()
        return Condition(op, left, right)
    }

    private fun kotlinx.serialization.json.JsonElement.toOperand(): Operand = when (this) {
        is JsonPrimitive -> if (this.isString) {
            when (content) {
                "card_count" -> Operand.CardCount
                "round_delta_so_far" -> Operand.RoundDeltaSoFar
                "distinct_card_codes" -> Operand.DistinctCardCodes
                else -> throw IllegalArgumentException("Unknown operand: $content")
            }
        } else Operand.Const(int())

        is JsonObject -> {
            val type = (this["type"] as JsonPrimitive).content
            when (type) {
                "const" -> Operand.Const((this["value"] as JsonPrimitive).int())
                else -> throw IllegalArgumentException("Unknown operand type: $type")
            }
        }

        else -> throw IllegalArgumentException("Unsupported operand shape")
    }

    private fun JsonObject.toAction(): Action {
        val type = (this["type"] as JsonPrimitive).content
        return when (type) {
            "const" -> Action.Const((this["value"] as JsonPrimitive).int())
            "base_value" -> Action.BaseValue
            "setting" -> Action.Setting((this["key"] as JsonPrimitive).content)
            else -> throw IllegalArgumentException("Unknown action type: $type")
        }
    }

    private fun JsonPrimitive.int(): Int = intOrNull
        ?: doubleOrNull?.toInt()
        ?: throw IllegalArgumentException("Expected integer, got: $content")
}