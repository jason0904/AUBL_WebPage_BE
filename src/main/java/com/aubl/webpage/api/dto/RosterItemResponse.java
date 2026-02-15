package com.aubl.webpage.api.dto;

public record RosterItemResponse(
    Long seasonId,
    Long teamId,
    String teamName,
    String teamCode,
    Long teamPlayerId,
    Long playerId,
    String playerName,
    String jerseyNumber,
    boolean hasBatterStats,
    boolean hasPitcherStats
) {
}

