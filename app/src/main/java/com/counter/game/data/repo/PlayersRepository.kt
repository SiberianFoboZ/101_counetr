package com.counter.game.data.repo

import androidx.room.withTransaction
import com.counter.game.data.dao.PlayerDao
import com.counter.game.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

class PlayersRepository(private val dao: PlayerDao) {
    fun observeActive(): Flow<List<PlayerEntity>> = dao.observeActive()
    suspend fun listActive(): List<PlayerEntity> = dao.listActive()
    suspend fun countActive(): Int = dao.countActive()

    suspend fun add(name: String): Long {
        val trimmed = name.trim().ifEmpty { defaultName() }
        return dao.insert(PlayerEntity(name = trimmed))
    }

    suspend fun rename(id: Long, newName: String) {
        val current = dao.byId(id) ?: return
        val trimmed = newName.trim().ifEmpty { current.name }
        dao.update(current.copy(name = trimmed))
    }

    suspend fun archive(id: Long) {
        dao.archive(id)
    }

    private suspend fun defaultName(): String {
        val max = dao.maxDefaultNumber()
        return "Игрок ${max + 1}"
    }
}