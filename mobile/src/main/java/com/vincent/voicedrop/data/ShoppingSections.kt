package com.vincent.voicedrop.data

/**
 * Eén sectie in het Boodschappen-scherm. [group] = het winkelschap; `null` = de "Afgerond"-sectie
 * (afgevinkte items), die altijd onderaan komt.
 */
data class ShoppingSection(val group: ShoppingGroup?, val items: List<Memo>)

/**
 * Groepeert boodschappen-memo's in secties: niet-afgevinkte items per winkelschap in [order]
 * (lege secties weggelaten, items zonder groep onder [ShoppingGroup.OVERIG]), gevolgd door één
 * "Afgerond"-sectie met de afgevinkte items. Binnen een schap nieuwste eerst (op `timestamp`);
 * afgevinkte items op laatst afgevinkt eerst (op `checkedAt`).
 */
fun buildShoppingSections(memos: List<Memo>, order: List<ShoppingGroup>): List<ShoppingSection> {
    val (checked, active) = memos.partition { it.checkedAt != null }

    val byGroup = active.groupBy { ShoppingGroup.fromName(it.shoppingGroup) ?: ShoppingGroup.OVERIG }
    val sections = order.mapNotNull { group ->
        val items = byGroup[group]?.sortedByDescending { it.timestamp } ?: return@mapNotNull null
        ShoppingSection(group, items)
    }.toMutableList()

    if (checked.isNotEmpty()) {
        sections += ShoppingSection(null, checked.sortedByDescending { it.checkedAt ?: 0L })
    }
    return sections
}
