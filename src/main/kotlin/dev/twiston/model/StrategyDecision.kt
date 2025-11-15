package dev.twiston.model

data class StrategyDecision(
    val action: Action,
    val expectedValue: Double,
    val allEVs: Map<Action, Double>
)
