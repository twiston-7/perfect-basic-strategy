package dev.twiston.cli

import dev.twiston.model.*

object RulesPrompt {
    private val allNumDecks = (1..8).map { it.toString() }
    private val allBool = listOf("0", "1")
    private val allSurrender = listOf("0", "1", "2")
    private val allDoubleRestrictions = listOf("0", "1", "2")
    private val allMaxSplits = (1..4).map { it.toString() }
    private val outputModeOptions = listOf("0", "1", "*") // 0=simple,1=detailed,*=both

    fun promptList(message: String, validOptions: List<String>): List<String> {
        while (true) {
            println("$message (options: ${validOptions.joinToString(", ")}, * for all):")
            print("> ")
            val input = readLine()?.trim() ?: ""
            if (input == "*") return validOptions
            if (validOptions.contains(input)) return listOf(input)
            println("Invalid input.")
        }
    }

    fun promptOutputMode(): List<String> {
        while (true) {
            println("\nOutput mode options:")
            println("0 = Only simple (short) output")
            println("1 = Only detailed output")
            println("* = Both outputs")
            print("Select output mode (0,1,*): ")
            val input = readLine()?.trim() ?: ""
            if (input == "*" || input == "0" || input == "1") return listOf(input)
            println("Invalid input.")
        }
    }

    fun promptRulesBatchWithOutput(): Pair<List<GameRules>, String> {
        val decksInput = promptList("Number of decks", allNumDecks)
        val dealerHitsInput = promptList("Dealer hits soft 17? (0=no, 1=yes)", allBool)
        val dasInput = promptList("Double after split allowed? (0=no, 1=yes)", allBool)
        val surrenderInput = promptList("Surrender type (0=none, 1=late, 2=early)", allSurrender)
        val doubleRestrictionInput = promptList("Double restrictions (0=any,1=9-11,2=10-11)", allDoubleRestrictions)
        val dealerPeekInput = promptList("Dealer peeks for blackjack? (0=no,1=yes)", allBool)
        val resplitAcesInput = promptList("Resplit aces allowed? (0=no,1=yes)", allBool)
        val maxSplitsInput = promptList("Max splits (1-4)", allMaxSplits)
        val outputModeInput = promptOutputMode().first()  // since returns list

        val results = mutableListOf<GameRules>()

        for (numDecks in decksInput) {
            for (dealerHits in dealerHitsInput) {
                for (das in dasInput) {
                    for (surrender in surrenderInput) {
                        for (doubleRestrict in doubleRestrictionInput) {
                            for (dealerPeek in dealerPeekInput) {
                                for (resplitAces in resplitAcesInput) {
                                    for (maxSplits in maxSplitsInput) {
                                        results.add(
                                            GameRules(
                                                numDecks = numDecks.toInt(),
                                                dealerHitsSoft17 = dealerHits == "1",
                                                doubleAfterSplit = das == "1",
                                                surrenderType = when (surrender) {
                                                    "0" -> SurrenderType.NONE
                                                    "1" -> SurrenderType.LATE
                                                    "2" -> SurrenderType.EARLY
                                                    else -> error("Invalid surrender type")
                                                },
                                                doubleRestriction = when (doubleRestrict) {
                                                    "0" -> DoubleRestriction.ANY
                                                    "1" -> DoubleRestriction.NINE_TEN_ELEVEN
                                                    "2" -> DoubleRestriction.TEN_ELEVEN_ONLY
                                                    else -> error("Invalid double restriction")
                                                },
                                                dealerPeeksBlackjack = dealerPeek == "1",
                                                resplitAces = resplitAces == "1",
                                                maxSplits = maxSplits.toInt()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Pair(results, outputModeInput)
    }
}
