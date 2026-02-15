package com.aubl.webpage.api.dto;

public record PlayerSearchResult(
    Long playerId,
    String playerName,
    Long teamId,
    String teamName,
    Integer jerseyNumber,
    Long seasonId
) {
}

