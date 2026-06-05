package com.vincent.voicedrop.data

/** Vaste winkelschappen voor de boodschappenlijst, in standaard-winkelvolgorde ([order]). */
enum class ShoppingGroup(val order: Int) {
    GROENTE_FRUIT(0),
    ZUIVEL_KOELING(1),
    BROOD_BAKKERIJ(2),
    VLEES_VIS(3),
    DIEPVRIES(4),
    DRINKEN(5),
    HOUDBAAR(6),
    SLIJTERIJ(7),
    HUISHOUD_DROGIST(8),
    OVERIG(9);

    companion object {
        fun fromName(name: String?): ShoppingGroup? = entries.firstOrNull { it.name == name }
    }
}
