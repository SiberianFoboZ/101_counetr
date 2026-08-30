package com.counter.game.data.repo

import com.counter.game.data.dao.RuleDao
import com.counter.game.data.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

class RulesRepository(private val dao: RuleDao) {
    fun observeAll(): Flow<List<RuleEntity>> = dao.observeAll()
    suspend fun listEnabled(): List<RuleEntity> = dao.listEnabled()
    suspend fun listEnabledForCard(code: String): List<RuleEntity> = dao.listEnabledForCard(code)
}