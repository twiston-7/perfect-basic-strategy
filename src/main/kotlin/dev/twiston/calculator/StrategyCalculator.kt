package dev.twiston.calculator

import dev.twiston.model.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class StrategyCalculator(private val rules: GameRules) {
    private val dealerEngine = DealerProbabilityEngine(rules)
    private val evaluator = PlayerActionEvaluator(rules, dealerEngine)

    private val uniqueUpcards = Card.uniqueRanks()
    private val uniquePairRanks = Card.uniqueRanks()

    fun calculateOptimalStrategy(): Map<String, StrategyDecision> {
        val results = ConcurrentHashMap<String, StrategyDecision>()

        runBlocking {
            val jobs = mutableListOf<Job>()

            for (dealerUpcard in uniqueUpcards) {
                for (playerTotal in 4..21) {
                    jobs += launch(Dispatchers.Default) {
                        calculateHardHand(playerTotal, dealerUpcard, results)
                    }
                }

                for (softValue in 13..21) {
                    jobs += launch(Dispatchers.Default) {
                        calculateSoftHand(softValue, dealerUpcard, results)
                    }
                }

                for (pairRank in uniquePairRanks) {
                    jobs += launch(Dispatchers.Default) {
                        calculatePair(pairRank, dealerUpcard, results)
                    }
                }
            }
            jobs.forEach { it.join() }
        }
        return results
    }

    private fun calculateHardHand(total: Int, dealerUpcard: Rank, results: MutableMap<String, StrategyDecision>) {
        val deck = DeckComposition(rules.numDecks)
        deck.remove(dealerUpcard)
        val hand = createHardHand(total, deck) ?: return
        val decision = evaluateBestAction(hand, dealerUpcard, deck, canSplit = false)
        results["HARD_${total}_${dealerUpcard.toLabel()}"] = decision
        hand.cards.forEach { deck.add(it) }
    }

    private fun calculateSoftHand(softTotal: Int, dealerUpcard: Rank, results: MutableMap<String, StrategyDecision>) {
        val deck = DeckComposition(rules.numDecks)
        deck.remove(dealerUpcard)
        val otherCardValue = softTotal - 11
        if (otherCardValue !in 2..10) return
        val ace = Rank.ACE
        val otherCard = Rank.values().find { it.value == otherCardValue } ?: return
        if (deck.getCount(ace) == 0 || deck.getCount(otherCard) == 0) return
        deck.remove(ace)
        deck.remove(otherCard)
        val hand = Hand.fromRanks(ace, otherCard)
        val decision = evaluateBestAction(hand, dealerUpcard, deck, canSplit = false)
        results["SOFT_${softTotal}_${dealerUpcard.toLabel()}"] = decision
        deck.add(ace)
        deck.add(otherCard)
    }

    private fun calculatePair(pairRank: Rank, dealerUpcard: Rank, results: MutableMap<String, StrategyDecision>) {
        val deck = DeckComposition(rules.numDecks)
        deck.remove(dealerUpcard)
        if (deck.getCount(pairRank) < 2) return
        deck.remove(pairRank)
        deck.remove(pairRank)
        val hand = Hand.fromRanks(pairRank, pairRank)
        val decision = evaluateBestAction(hand, dealerUpcard, deck, canSplit = true)
        results["PAIR_${pairRank.toLabel()}_${dealerUpcard.toLabel()}"] = decision
        deck.add(pairRank)
        deck.add(pairRank)
    }

    private fun evaluateBestAction(
        hand: Hand,
        dealerUpcard: Rank,
        deck: DeckComposition,
        canSplit: Boolean
    ): StrategyDecision {
        val evMap = mutableMapOf<Action, Double>()
        val deckCopy = deck.copy()
        evMap[Action.STAND] = evaluator.evaluateStand(hand, dealerUpcard, deckCopy)
        evMap[Action.HIT] = evaluator.evaluateHit(hand, dealerUpcard, deckCopy)
        if (hand.cards.size == 2) {
            if (rules.canDouble(hand.hardTotal) || (hand.isSoft && rules.canDouble(hand.bestTotal()))) {
                evMap[Action.DOUBLE] = evaluator.evaluateDouble(hand, dealerUpcard, deckCopy)
            }
            if (canSplit && hand.isPair) {
                evMap[Action.SPLIT] = evaluator.evaluateSplit(hand, dealerUpcard, deckCopy, rules.maxSplits)
            }
            when (rules.surrenderType) {
                SurrenderType.EARLY -> evMap[Action.SURRENDER] = -0.5
                SurrenderType.LATE -> {
                    if (rules.dealerPeeksBlackjack) {
                        if (!(dealerUpcard.isAce() || dealerUpcard.isTen())) {
                            evMap[Action.SURRENDER] = -0.5
                        }
                    } else {
                        evMap[Action.SURRENDER] = -0.5
                    }
                }
                SurrenderType.NONE -> {}
            }
        }
        val best = evMap.maxByOrNull { it.value } ?: throw IllegalStateException("No actions evaluated")
        return StrategyDecision(best.key, best.value, evMap)
    }

    private fun createHardHand(total: Int, deck: DeckComposition): Hand? {
        if (total < 4 || total > 21) return null

        for (firstValue in 2..10) {
            val secondValue = total - firstValue
            if (secondValue !in 2..10) continue
            if (firstValue == 1 || secondValue == 1) continue

            val firstRank = Rank.values().find { it.value == firstValue && it != Rank.ACE }
            val secondRank = Rank.values().find { it.value == secondValue && it != Rank.ACE }
            if (firstRank == null || secondRank == null) continue

            if (deck.getCount(firstRank) > 0 && deck.getCount(secondRank) > 0) {
                deck.remove(firstRank)
                deck.remove(secondRank)
                return Hand.fromRanks(firstRank, secondRank)
            }
        }
        return null
    }
}
