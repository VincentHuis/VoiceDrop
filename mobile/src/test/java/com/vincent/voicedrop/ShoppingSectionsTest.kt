package com.vincent.voicedrop

import com.vincent.voicedrop.data.Memo
import com.vincent.voicedrop.data.ShoppingGroup
import com.vincent.voicedrop.data.buildShoppingSections
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingSectionsTest {

    private fun memo(id: String, group: ShoppingGroup?, ts: Long, checkedAt: Long? = null) =
        Memo(id = id, text = id, timestamp = ts, shoppingGroup = group?.name, checkedAt = checkedAt)

    private val defaultOrder = ShoppingGroup.entries.toList()

    @Test
    fun groupsInGivenOrderAndHidesEmpty() {
        val memos = listOf(
            memo("melk", ShoppingGroup.ZUIVEL_KOELING, 1),
            memo("appel", ShoppingGroup.GROENTE_FRUIT, 2)
        )
        val order = listOf(ShoppingGroup.ZUIVEL_KOELING, ShoppingGroup.GROENTE_FRUIT) +
            ShoppingGroup.entries.filter { it != ShoppingGroup.ZUIVEL_KOELING && it != ShoppingGroup.GROENTE_FRUIT }
        val sections = buildShoppingSections(memos, order)

        assertEquals(2, sections.size)
        assertEquals(ShoppingGroup.ZUIVEL_KOELING, sections[0].group)
        assertEquals(ShoppingGroup.GROENTE_FRUIT, sections[1].group)
    }

    @Test
    fun nullGroupGoesToOverig() {
        val memos = listOf(memo("wraps", null, 1))
        val sections = buildShoppingSections(memos, defaultOrder)
        assertEquals(1, sections.size)
        assertEquals(ShoppingGroup.OVERIG, sections[0].group)
    }

    @Test
    fun checkedItemsGoToDoneSectionAtBottom() {
        val memos = listOf(
            memo("melk", ShoppingGroup.ZUIVEL_KOELING, 2),
            memo("appel", ShoppingGroup.GROENTE_FRUIT, 1, checkedAt = 99)
        )
        val sections = buildShoppingSections(memos, defaultOrder)
        val done = sections.last()
        assertEquals(null, done.group)
        assertEquals(1, done.items.size)
        assertEquals("appel", done.items[0].id)
        assertEquals(ShoppingGroup.ZUIVEL_KOELING, sections[0].group)
        assertEquals(listOf("melk"), sections[0].items.map { it.id })
    }

    @Test
    fun itemsWithinGroupSortedNewestFirst() {
        val memos = listOf(
            memo("oud", ShoppingGroup.GROENTE_FRUIT, 1),
            memo("nieuw", ShoppingGroup.GROENTE_FRUIT, 5)
        )
        val sections = buildShoppingSections(memos, defaultOrder)
        assertEquals(listOf("nieuw", "oud"), sections[0].items.map { it.id })
    }

    @Test
    fun emptyInputGivesNoSections() {
        assertEquals(emptyList<Any>(), buildShoppingSections(emptyList(), defaultOrder))
    }
}
