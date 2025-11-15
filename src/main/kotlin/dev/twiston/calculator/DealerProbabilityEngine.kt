package dev.twiston.calculator

import dev.twiston.model.GameRules
import dev.twiston.model.Hand
import dev.twiston.model.Rank
import java.util.concurrent.ConcurrentHashMap

data class DealerOutcome(
    val bust: Double,
    val seventeen: Double,
    val eighteen: Double,
    val nineteen: Double,
    val twenty: Double,
    val twentyOne: Double,
    val blackjack: Double
)

class DealerProbabilityEngine(private val rules: GameRules) {
    private val cache = ConcurrentHashMap<String, DealerOutcome>()

    fun getDealerOutcome(upcard: Rank, deck: DeckComposition): DealerOutcome {
        val key = "${upcard.ordinal}_${deck.hashKey()}"
        return cache.getOrPut(key) { calculateDealerOutcome(upcard, deck) }
    }

    private fun calculateDealerOutcome(upcard: Rank, deck: DeckComposition): DealerOutcome {
        val deckCopy = deck.copy()
        deckCopy.remove(upcard)

        var bust = 0.0
        var seventeen = 0.0
        var eighteen = 0.0
        var nineteen = 0.0
        var twenty = 0.0
        var twentyOne = 0.0
        var blackjack = 0.0

        for (holeCard in Rank.values()) {
            if (deckCopy.getCount(holeCard) == 0) continue

            val probability = deckCopy.probabilityOf(holeCard)
            deckCopy.remove(holeCard)

            val hand = Hand.fromRanks(upcard, holeCard)

            if (hand.isBlackjack) {
                blackjack += probability
            } else {
                val outcome = dealerPlays(hand, deckCopy)
                bust += probability * outcome.bust
                seventeen += probability * outcome.seventeen
                eighteen += probability * outcome.eighteen
                nineteen += probability * outcome.nineteen
                twenty += probability * outcome.twenty
                twentyOne += probability * outcome.twentyOne
            }

            deckCopy.add(holeCard)
        }

        return DealerOutcome(bust, seventeen, eighteen, nineteen, twenty, twentyOne, blackjack)
    }

    private fun dealerPlays(hand: Hand, deck: DeckComposition): DealerOutcome {
        val total = hand.bestTotal()

        if (hand.isBusted) {
            return DealerOutcome(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val mustHit = total < 17 || (total == 17 && hand.isSoft && rules.dealerHitsSoft17)

        if (!mustHit) {
            return when (total) {
                17 -> DealerOutcome(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                18 -> DealerOutcome(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0)
                19 -> DealerOutcome(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)
                20 -> DealerOutcome(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                21 -> DealerOutcome(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
                else -> DealerOutcome(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            }
        }

        var bust = 0.0
        var seventeen = 0.0
        var eighteen = 0.0
        var nineteen = 0.0
        var twenty = 0.0
        var twentyOne = 0.0

        for (nextCard in Rank.values()) {
            if (deck.getCount(nextCard) == 0) continue

            val probability = deck.probabilityOf(nextCard)
            deck.remove(nextCard)

            val newHand = hand.addCard(nextCard)
            val outcome = dealerPlays(newHand, deck)

            bust += probability * outcome.bust
            seventeen += probability * outcome.seventeen
            eighteen += probability * outcome.eighteen
            nineteen += probability * outcome.nineteen
            twenty += probability * outcome.twenty
            twentyOne += probability * outcome.twentyOne

            deck.add(nextCard)
        }

        return DealerOutcome(bust, seventeen, eighteen, nineteen, twenty, twentyOne, 0.0)
    }
}
