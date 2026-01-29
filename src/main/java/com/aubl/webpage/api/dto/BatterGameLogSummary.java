package com.aubl.webpage.api.dto;

public record BatterGameLogSummary(
    Long id,
    Long gameId,
    Long teamId,
    String teamSide,
    Long playerId,
    String playerName,
    String playerPosition,
    Integer atBats,
    Integer runs,
    Integer hits,
    Integer rbi,
    Integer walks,
    Integer strikeouts
) {
}
