package com.counter.game.data.repo

import com.counter.game.data.dao.SettingsDao
import com.counter.game.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dao: SettingsDao) {
    fun observe(): Flow<SettingsEntity?> = dao.observe()
    suspend fun get(): SettingsEntity = dao.get() ?: SettingsEntity()

    suspend fun update(
        queenValue: Int,
        queenSpadesValue: Int,
        kingValue: Int,
        thresholdScore: Int,
        fontScale: Float,
    ) {
        dao.upsert(
            SettingsEntity(
                queenValue = queenValue,
                queenSpadesValue = queenSpadesValue,
                kingValue = kingValue,
                thresholdScore = thresholdScore,
                fontScale = fontScale,
            ),
        )
    }
}