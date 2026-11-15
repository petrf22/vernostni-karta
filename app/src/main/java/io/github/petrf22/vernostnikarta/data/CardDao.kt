package io.github.petrf22.vernostnikarta.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY position, id")
    fun observeCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY position, id")
    suspend fun getCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observeCard(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCard(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE uuid = :uuid")
    suspend fun getCardByUuid(uuid: String): CardEntity?

    @Query("SELECT MAX(position) FROM cards")
    suspend fun maxPosition(): Int?

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("UPDATE cards SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: Long)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("SELECT * FROM card_usage")
    fun observeUsages(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM card_usage ORDER BY timestamp")
    suspend fun getUsages(): List<UsageEntity>

    @Query("SELECT * FROM card_usage WHERE cardId = :cardId")
    suspend fun getUsagesFor(cardId: Long): List<UsageEntity>

    @Insert
    suspend fun insertUsage(usage: UsageEntity)

    /** Keeps only the [keep] newest records of a card. */
    @Query(
        "DELETE FROM card_usage WHERE cardId = :cardId AND id NOT IN " +
            "(SELECT id FROM card_usage WHERE cardId = :cardId ORDER BY timestamp DESC LIMIT :keep)",
    )
    suspend fun trimUsages(cardId: Long, keep: Int)

    @Query("DELETE FROM card_usage WHERE cardId = :cardId")
    suspend fun deleteUsagesFor(cardId: Long)

    @Query("DELETE FROM card_usage")
    suspend fun deleteAllUsages()
}
