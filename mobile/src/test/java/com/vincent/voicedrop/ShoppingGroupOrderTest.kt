package com.vincent.voicedrop

import com.vincent.voicedrop.data.ShoppingGroup
import com.vincent.voicedrop.data.ShoppingGroupOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingGroupOrderTest {
    @Test
    fun emptyGivesEnumOrder() {
        assertEquals(ShoppingGroup.entries.toList(), ShoppingGroupOrder.parse(null))
        assertEquals(ShoppingGroup.entries.toList(), ShoppingGroupOrder.parse(""))
    }

    @Test
    fun storedOrderIsRespected() {
        val stored = "ZUIVEL_KOELING,GROENTE_FRUIT"
        val result = ShoppingGroupOrder.parse(stored)
        assertEquals(ShoppingGroup.ZUIVEL_KOELING, result[0])
        assertEquals(ShoppingGroup.GROENTE_FRUIT, result[1])
        assertEquals(ShoppingGroup.entries.size, result.size)
        assertEquals(ShoppingGroup.entries.toSet(), result.toSet())
    }

    @Test
    fun unknownAndDuplicatesIgnored() {
        val result = ShoppingGroupOrder.parse("ONZIN,GROENTE_FRUIT,GROENTE_FRUIT")
        assertEquals(ShoppingGroup.GROENTE_FRUIT, result[0])
        assertEquals(ShoppingGroup.entries.size, result.size)
        assertEquals(ShoppingGroup.entries.toSet(), result.toSet())
    }
}
