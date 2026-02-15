package com.aubl.webpage.api.dto;

import java.math.BigDecimal;

public record BatterStatSummary(
    Long id,
    Long teamPlayerId,
    Long seasonId,
    String seasonType,
    Integer gamesPlayed,
    Integer plateAppearance,
    Integer atBats,
    Integer hits,
    Integer homeRuns,
    BigDecimal battingAverage,
    BigDecimal onBasePct,
    BigDecimal sluggingPct,
    BigDecimal ops,
    Integer jerseyNumber
) {
}
