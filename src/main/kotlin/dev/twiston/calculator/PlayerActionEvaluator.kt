package dev.twiston.calculator

import dev.twiston.model.*
import java.util.concurrent.ConcurrentHashMap

class PlayerActionEvaluator(
    private val rules: GameRules,
    private val dealerEngine: DealerProbabilityEngine
) {
    private val splitCache = ConcurrentHashMap<String, Double>()

    fun evaluateStand(hand: Hand, dealerUpcard: Rank, deck: DeckComposition): Double {
        if (hand.isBusted) return -1.0

        val dealerOutcome = dealerEngine.getDealerOutcome(dealerUpcard, deck)
        val playerTotal = hand.bestTotal()

        var ev = 0.0
        ev += dealerOutcome.bust * 1.0
        ev += if (playerTotal > 17) dealerOutcome.seventeen * 1.0 else dealerOutcome.seventeen * -1.0
        ev += if (playerTotal > 18) dealerOutcome.eighteen * 1.0 else dealerOutcome.eighteen * -1.0
        ev += if (playerTotal > 19) dealerOutcome.nineteen * 1.0 else dealerOutcome.nineteen * -1.0
        ev += if (playerTotal > 20) dealerOutcome.twenty * 1.0 else dealerOutcome.twenty * -1.0
        ev += dealerOutcome.twentyOne * -1.0
        ev += dealerOutcome.blackjack * -1.0

        return ev
    }

    fun evaluateHit(hand: Hand, dealerUpcard: Rank, deck: DeckComposition): Double {
        if (hand.isBusted) return -1.0

        var ev = 0.0

        for (nextCard in Rank.values()) {
            if (deck.getCount(nextCard) == 0) continue

            val probability = deck.probabilityOf(nextCard)
            deck.remove(nextCard)

            val newHand = hand.addCard(nextCard)

            val outcomeEV = if (newHand.isBusted) {
                -1.0
            } else if (newHand.bestTotal() == 21) {
                evaluateStand(newHand, dealerUpcard, deck)
            } else {
                val standEV = evaluateStand(newHand, dealerUpcard, deck)
                val hitEV = evaluateHit(newHand, dealerUpcard, deck)
                maxOf(standEV, hitEV)
            }

            ev += probability * outcomeEV
            deck.add(nextCard)
        }

        return ev
    }

    fun evaluateDouble(hand: Hand, dealerUpcard: Rank, deck: DeckComposition): Double {
        if (hand.isBusted) return -2.0

        var ev = 0.0

        for (nextCard in Rank.values()) {
            if (deck.getCount(nextCard) == 0) continue

            val probability = deck.probabilityOf(nextCard)
            deck.remove(nextCard)

            val newHand = hand.addCard(nextCard)
            val standEV = evaluateStand(newHand, dealerUpcard, deck)

            ev += probability * standEV * 2.0
            deck.add(nextCard)
        }

        return ev
    }

    fun evaluateSplit(hand: Hand, dealerUpcard: Rank, deck: DeckComposition, splitsRemaining: Int): Double {
        if (!hand.isPair || splitsRemaining <= 0) return Double.NEGATIVE_INFINITY

        val pairRank = hand.cards[0]
        val key = "${pairRank.ordinal}_${dealerUpcard.ordinal}_${splitsRemaining}_${deck.hashKey()}"

        return splitCache.getOrPut(key) {
            calculateSplitEV(pairRank, dealerUpcard, deck, splitsRemaining)
        }
    }

    private fun calculateSplitEV(pairRank: Rank, dealerUpcard: Rank, deck: DeckComposition, splitsRemaining: Int): Double {
        var totalEV = 0.0

        for (nextCard in Rank.values()) {
            if (deck.getCount(nextCard) == 0) continue

            val probability = deck.probabilityOf(nextCard)
            deck.remove(nextCard)

            val newHand = Hand.fromRanks(pairRank, nextCard)

            val handEV = if (newHand.isBlackjack) {
                evaluateStand(newHand, dealerUpcard, deck)
            } else {
                val standEV = evaluateStand(newHand, dealerUpcard, deck)
                val hitEV = evaluateHit(newHand, dealerUpcard, deck)
                var bestEV = maxOf(standEV, hitEV)

                if (rules.doubleAfterSplit && rules.canDouble(newHand.hardTotal)) {
                    val doubleEV = evaluateDouble(newHand, dealerUpcard, deck)
                    bestEV = maxOf(bestEV, doubleEV)
                }

                if (newHand.isPair && splitsRemaining > 1 && (pairRank != Rank.ACE || rules.resplitAces)) {
                    val resplitEV = evaluateSplit(newHand, dealerUpcard, deck, splitsRemaining - 1)
                    bestEV = maxOf(bestEV, resplitEV)
                }

                bestEV
            }

            totalEV += probability * handEV
            deck.add(nextCard)
        }

        return totalEV * 2.0
    }
}
