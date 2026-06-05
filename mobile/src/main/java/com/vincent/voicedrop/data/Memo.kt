package com.vincent.voicedrop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class Memo(
    @PrimaryKey val id: String,
    val text: String,
    val timestamp: Long,
    val category: String = Category.OVERIG.name,
    /** Tijdstip waarop een boodschap is afgevinkt; null = niet afgevinkt. */
    val checkedAt: Long? = null,
    /** Tijdstip waarop voor een herinnering een notificatie moet komen; null = geen tijd. */
    val remindAt: Long? = null,
    /** Plek (Place.id als string) waarbij een herinnering moet afgaan; null = geen locatie. */
    val placeId: String? = null,
    /** Tijdstip waarop memo vastgepind is; null = niet vastgepind. */
    val pinnedAt: Long? = null,
    /**
     * Herhalingsregel als "UNIT:interval" (bv. "WEEK:1", "DAY:2", "HOUR:3"); null = eenmalig.
     * [remindAt] is het eerstvolgende moment; bij vuren wordt het volgende moment berekend.
     */
    val recurrence: String? = null
)
