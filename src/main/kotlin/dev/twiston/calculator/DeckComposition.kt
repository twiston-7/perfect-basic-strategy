package dev.twiston.calculator

import dev.twiston.model.Rank

class DeckComposition(private val numDecks: Int) {
    private val counts: IntArray = IntArray(13) { numDecks * 4 }

    fun remove(rank: Rank) {
        counts[rank.ordinal]--
    }

    fun add(rank: Rank) {
        counts[rank.ordinal]++
    }

    fun getCount(rank: Rank): Int = counts[rank.ordinal]

    fun totalCards(): Int = counts.sum()

    fun copy(): DeckComposition {
        val copy = DeckComposition(numDecks)
        counts.copyInto(copy.counts)
        return copy
    }

    fun probabilityOf(rank: Rank): Double {
        val total = totalCards()
        return if (total > 0) getCount(rank).toDouble() / total else 0.0
    }

    fun hashKey(): Int {
        var hash = 0
        for (i in counts.indices) {
            hash = hash * 31 + counts[i]
        }
        return hash
    }
}
