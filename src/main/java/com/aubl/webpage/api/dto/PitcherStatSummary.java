package com.aubl.webpage.api.dto;

import java.math.BigDecimal;

public record PitcherStatSummary(
    Long id,
    Long teamPlayerId,
    Long seasonId,
    String seasonType,
    Integer gamesPlayed,
    Integer gamesStarted,
    BigDecimal inningsPitched,
    Integer wins,
    Integer losses,
    Integer saves,
    BigDecimal era,
    BigDecimal whip,
    BigDecimal kPer9,
    BigDecimal bbPer9
) {
}
