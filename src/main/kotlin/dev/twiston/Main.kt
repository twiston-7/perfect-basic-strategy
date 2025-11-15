package dev.twiston

import dev.twiston.calculator.StrategyCalculator
import dev.twiston.cli.RulesPrompt
import dev.twiston.model.OutputMode
import dev.twiston.output.JsonExporter
import dev.twiston.output.StrategyTable

fun main() {
    val (ruleSets, outputModeStr) = RulesPrompt.promptRulesBatchWithOutput()
    println("Generating strategies for ${ruleSets.size} rule sets...\n")

    // Map from string input to your enum
    val outputMode = when(outputModeStr) {
        "0" -> OutputMode.ONLY_SIMPLE
        "1" -> OutputMode.ONLY_DETAILED
        "*" -> OutputMode.BOTH
        else -> OutputMode.BOTH
    }

    var count = 0
    val total = ruleSets.size

    for (rules in ruleSets) {
        println("Calculating strategy: ${rules.toFilename()} (${++count}/$total)")

        val calculator = StrategyCalculator(rules)
        val detailedMap = calculator.calculateOptimalStrategy()
        val strategyTable = StrategyTable.from(rules, detailedMap)
        val filename = rules.toFilename()

        JsonExporter.exportWithMode(strategyTable, filename, outputMode)

        println("Done\n")
    }

    println("All computations finished.")
}