package com.aubl.webpage.api.dto;

public record TeamPlayerSummary(
    Long teamPlayerId,
    Long teamId,
    String teamName,
    Long seasonId,
    Long playerId,
    String playerName,
    Integer jerseyNumber,
    String position
) {
}

