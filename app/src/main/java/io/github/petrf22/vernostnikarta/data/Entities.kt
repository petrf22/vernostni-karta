package io.github.petrf22.vernostnikarta.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cards", indices = [Index(value = ["uuid"], unique = true)])
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity across export/import. */
    val uuid: String,
    val name: String,
    val note: String = "",
    val code: String,
    /** Name of a [io.github.petrf22.vernostnikarta.barcode.CodeFormat]. */
    val format: String,
    /** ARGB. */
    val color: Int,
    val position: Int = 0,
    val createdAt: Long = 0,
)

/** One place where a card was shown. Only stored when the user enabled location. */
@Entity(
    tableName = "card_usage",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId")],
)
data class UsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)
