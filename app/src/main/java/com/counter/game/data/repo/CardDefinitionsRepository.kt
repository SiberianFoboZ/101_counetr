package com.counter.game.data.repo

import com.counter.game.data.dao.CardDefinitionDao
import com.counter.game.data.entity.CardDefinitionEntity

class CardDefinitionsRepository(private val dao: CardDefinitionDao) {
    suspend fun listAll(): List<CardDefinitionEntity> = dao.listAll()
    suspend fun byCode(code: String): CardDefinitionEntity? = dao.byCode(code)
}