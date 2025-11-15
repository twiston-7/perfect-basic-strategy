package dev.twiston.model

enum class Rank(val value: Int) {
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(10),
    QUEEN(10),
    KING(10);

    fun isAce() = this == ACE
    fun isTen() = value == 10

    fun toLabel(): String = when (this) {
        ACE -> "A"
        TWO -> "2"
        THREE -> "3"
        FOUR -> "4"
        FIVE -> "5"
        SIX -> "6"
        SEVEN -> "7"
        EIGHT -> "8"
        NINE -> "9"
        TEN, JACK, QUEEN, KING -> "10"
    }
}

data class Card(val rank: Rank) {
    val value: Int get() = rank.value

    companion object {
        fun allRanks() = Rank.values().toList()
        fun uniqueRanks() = listOf(
            Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE,
            Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN
        )
    }
}
