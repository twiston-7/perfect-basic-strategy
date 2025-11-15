package dev.twiston.model

data class Hand(val cards: List<Rank>) {
    val hardTotal: Int
    val softTotal: Int?
    val isSoft: Boolean
    val isBusted: Boolean
    val isBlackjack: Boolean
    val isPair: Boolean

    init {
        var total = 0
        var aces = 0

        for (card in cards) {
            total += card.value
            if (card.isAce()) aces++
        }

        hardTotal = total

        var softValue: Int? = null
        if (aces > 0 && total + 10 <= 21) {
            softValue = total + 10
        }

        softTotal = softValue
        isSoft = softValue != null
        isBusted = hardTotal > 21
        isBlackjack = cards.size == 2 && softTotal == 21
        isPair = cards.size == 2 && cards[0] == cards[1]
    }

    fun bestTotal(): Int = softTotal ?: hardTotal

    fun addCard(rank: Rank): Hand = Hand(cards + rank)

    companion object {
        fun fromRanks(vararg ranks: Rank) = Hand(ranks.toList())
    }
}
