package io.github.petrf22.vernostnikarta.data

import androidx.room.withTransaction
import io.github.petrf22.vernostnikarta.backup.BackupCard
import io.github.petrf22.vernostnikarta.backup.BackupCodec
import io.github.petrf22.vernostnikarta.backup.BackupUsage
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import io.github.petrf22.vernostnikarta.ui.DEFAULT_CARD_COLOR
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CardRepository(private val db: AppDatabase) {
    private val dao = db.cardDao()

    val cards: Flow<List<CardEntity>> = dao.observeCards()
    val usages: Flow<List<UsageEntity>> = dao.observeUsages()

    fun observeCard(id: Long): Flow<CardEntity?> = dao.observeCard(id)

    suspend fun getCard(id: Long): CardEntity? = dao.getCard(id)

    /** Inserts a new card at the end of the list (id == 0) or updates an existing one. */
    suspend fun save(card: CardEntity): Long = db.withTransaction {
        if (card.id == 0L) {
            dao.insertCard(card.copy(position = (dao.maxPosition() ?: -1) + 1))
        } else {
            dao.updateCard(card)
            card.id
        }
    }

    suspend fun delete(id: Long) = db.withTransaction {
        dao.deleteUsagesFor(id)
        dao.deleteCard(id)
    }

    /** Stores the order of the list; [ids] are card ids from top to bottom. */
    suspend fun reorder(ids: List<Long>) = db.withTransaction {
        ids.forEachIndexed { index, id -> dao.setPosition(id, index) }
    }

    suspend fun addUsage(cardId: Long, latitude: Double, longitude: Double, timestamp: Long) =
        db.withTransaction {
            if (dao.getCard(cardId) == null) return@withTransaction
            dao.insertUsage(UsageEntity(cardId = cardId, latitude = latitude, longitude = longitude, timestamp = timestamp))
            dao.trimUsages(cardId, MAX_USAGES_PER_CARD)
        }

    suspend fun clearUsages() = dao.deleteAllUsages()

    suspend fun exportCards(): List<BackupCard> = db.withTransaction {
        val usages = dao.getUsages().groupBy { it.cardId }
        dao.getCards().map { card ->
            BackupCard(
                uuid = card.uuid,
                name = card.name,
                note = card.note,
                code = card.code,
                barcodeFormat = card.format,
                color = BackupCodec.colorToHex(card.color),
                position = card.position,
                createdAt = card.createdAt,
                usages = usages[card.id].orEmpty().map { BackupUsage(it.latitude, it.longitude, it.timestamp) },
            )
        }
    }

    /**
     * @param replace true = delete everything first; false = merge by [BackupCard.uuid]
     *   (a card that already exists is updated, new ones are appended).
     * @return number of imported cards
     */
    suspend fun importCards(cards: List<BackupCard>, replace: Boolean): Int = db.withTransaction {
        if (replace) {
            dao.deleteAllUsages()
            dao.deleteAllCards()
        }
        var nextPosition = (dao.maxPosition() ?: -1) + 1
        for (item in cards.sortedBy { it.position }) {
            val format = (CodeFormat.fromName(item.barcodeFormat) ?: CodeFormat.QR_CODE).name
            val color = BackupCodec.hexToColor(item.color) ?: DEFAULT_CARD_COLOR
            val existing = if (item.uuid.isBlank()) null else dao.getCardByUuid(item.uuid)
            val cardId = if (existing != null) {
                dao.updateCard(
                    existing.copy(name = item.name, note = item.note, code = item.code, format = format, color = color),
                )
                existing.id
            } else {
                dao.insertCard(
                    CardEntity(
                        uuid = item.uuid.ifBlank { UUID.randomUUID().toString() },
                        name = item.name,
                        note = item.note,
                        code = item.code,
                        format = format,
                        color = color,
                        position = nextPosition++,
                        createdAt = item.createdAt,
                    ),
                )
            }
            val known = dao.getUsagesFor(cardId).map { it.timestamp }.toSet()
            item.usages.filter { it.timestamp !in known }.forEach {
                dao.insertUsage(
                    UsageEntity(cardId = cardId, latitude = it.latitude, longitude = it.longitude, timestamp = it.timestamp),
                )
            }
            dao.trimUsages(cardId, MAX_USAGES_PER_CARD)
        }
        cards.size
    }

    companion object {
        const val MAX_USAGES_PER_CARD = 50
    }
}
