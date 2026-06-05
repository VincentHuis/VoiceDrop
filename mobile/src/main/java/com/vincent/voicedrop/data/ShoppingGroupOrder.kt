package com.vincent.voicedrop.data

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Door de gebruiker instelbare volgorde van de winkelgroepen, bewaard in SharedPreferences. */
object ShoppingGroupOrder {
    private const val KEY = "shopping_group_order"

    /**
     * Zet een opgeslagen comma-string om naar een complete, gededupliceerde lijst van alle
     * [ShoppingGroup]s: opgegeven groepen vooraan (in opgegeven volgorde), ontbrekende groepen
     * achteraan in enum-volgorde. Onbekende namen worden genegeerd. Leeg/null -> enum-volgorde.
     */
    fun parse(stored: String?): List<ShoppingGroup> {
        val ordered = LinkedHashSet<ShoppingGroup>()
        stored?.split(",")
            ?.mapNotNull { ShoppingGroup.fromName(it.trim()) }
            ?.forEach { ordered.add(it) }
        ShoppingGroup.entries.forEach { ordered.add(it) }
        return ordered.toList()
    }

    fun get(prefs: SharedPreferences): List<ShoppingGroup> = parse(prefs.getString(KEY, null))

    fun set(prefs: SharedPreferences, order: List<ShoppingGroup>) {
        prefs.edit().putString(KEY, order.joinToString(",") { it.name }).apply()
    }

    /** Emit de huidige volgorde en daarna bij elke wijziging van de key. */
    fun observe(prefs: SharedPreferences): Flow<List<ShoppingGroup>> = callbackFlow {
        trySend(get(prefs))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
            if (k == KEY) trySend(get(p))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
