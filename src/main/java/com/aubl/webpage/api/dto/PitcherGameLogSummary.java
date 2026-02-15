package com.aubl.webpage.api.dto;

import java.math.BigDecimal;

public record PitcherGameLogSummary(
    Long id,
    Long gameId,
    Long teamId,
    String teamSide,
    Long playerId,
    String playerName,
    String playerPosition,
    BigDecimal inningsPitched,
    Integer hitsAllowed,
    Integer runsAllowed,
    Integer earnedRuns,
    Integer walks,
    Integer strikeouts,
    Integer jerseyNumber
) {
}
