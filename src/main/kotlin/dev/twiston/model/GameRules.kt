package dev.twiston.model

import kotlinx.serialization.Serializable

enum class SurrenderType { NONE, EARLY, LATE }
enum class DoubleRestriction { ANY, NINE_TEN_ELEVEN, TEN_ELEVEN_ONLY }

@Serializable
data class GameRules(
    val numDecks: Int,
    val dealerHitsSoft17: Boolean,
    val doubleAfterSplit: Boolean,
    val surrenderType: SurrenderType,
    val doubleRestriction: DoubleRestriction,
    val dealerPeeksBlackjack: Boolean,
    val resplitAces: Boolean,
    val maxSplits: Int
) {
    fun toFilename(detailed: Boolean = false, shortKeys: Boolean = false): String = buildString {
        val suffix = if (detailed) "_detailed.json" else ".json"
        // start all filenames with strategy_ for clarity
        append("strategy_")
        append("${numDecks}${if (shortKeys) "d" else "deck"}")
        append(if (dealerHitsSoft17) (if (shortKeys) "H17" else "_H17") else (if (shortKeys) "S17" else "_S17"))
        if (doubleAfterSplit) append(if (shortKeys) "DAS" else "_DAS")
        // Always reflect SurrenderType
        append(
            when (surrenderType) {
                SurrenderType.NONE -> if (shortKeys) "NS" else "_NOSURR"
                SurrenderType.EARLY -> if (shortKeys) "ES" else "_ES"
                SurrenderType.LATE -> if (shortKeys) "LS" else "_LS"
            }
        )
        // Always reflect DoubleRestriction
        append(
            when (doubleRestriction) {
                DoubleRestriction.ANY -> if (shortKeys) "DA" else "_DANY"
                DoubleRestriction.NINE_TEN_ELEVEN -> if (shortKeys) "D911" else "_D911"
                DoubleRestriction.TEN_ELEVEN_ONLY -> if (shortKeys) "D1011" else "_D1011"
            }
        )
        append(if (dealerPeeksBlackjack) (if (shortKeys) "P" else "_PEEK") else (if (shortKeys) "NP" else "_NOPEEK"))
        append(if (resplitAces) (if (shortKeys) "RA" else "_RSA") else (if (shortKeys) "NRA" else "_NORSA"))
        append(if (maxSplits != 3) (if (shortKeys) "MS$maxSplits" else "_MAXSPLITS$maxSplits") else "")
        append(suffix)
    }


    fun canDouble(total: Int): Boolean = when (doubleRestriction) {
        DoubleRestriction.ANY -> true
        DoubleRestriction.NINE_TEN_ELEVEN -> total in 9..11
        DoubleRestriction.TEN_ELEVEN_ONLY -> total in 10..11
    }
}
