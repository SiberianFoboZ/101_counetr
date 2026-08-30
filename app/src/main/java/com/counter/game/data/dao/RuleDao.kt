package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.counter.game.data.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority DESC, id ASC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun listEnabled(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE applies_to_card = :code AND enabled = 1")
    suspend fun listEnabledForCard(code: String): List<RuleEntity>

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<RuleEntity>)

    @Update
    suspend fun update(rule: RuleEntity)
}