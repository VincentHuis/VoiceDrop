package com.vincent.voicedrop

import com.vincent.voicedrop.data.ShoppingGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingGroupTest {
    @Test
    fun fromNameKnown() {
        assertEquals(ShoppingGroup.GROENTE_FRUIT, ShoppingGroup.fromName("GROENTE_FRUIT"))
        assertEquals(ShoppingGroup.HUISHOUD_DROGIST, ShoppingGroup.fromName("HUISHOUD_DROGIST"))
        assertEquals(ShoppingGroup.OVERIG, ShoppingGroup.fromName("OVERIG"))
    }

    @Test
    fun fromNameUnknownOrNull() {
        assertNull(ShoppingGroup.fromName(null))
        assertNull(ShoppingGroup.fromName(""))
        assertNull(ShoppingGroup.fromName("ONZIN"))
    }

    @Test
    fun enumOrderIsStoreWalkthrough() {
        assertEquals(0, ShoppingGroup.GROENTE_FRUIT.order)
        assertEquals(9, ShoppingGroup.OVERIG.order)
        assertEquals(ShoppingGroup.entries.size, 10)
    }
}
