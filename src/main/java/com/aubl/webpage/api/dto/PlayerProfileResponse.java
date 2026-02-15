package com.aubl.webpage.api.dto;

public record PlayerProfileResponse(
    Long playerId,
    String playerName,
    Long seasonId,
    Long teamId,
    String teamName,
    String teamCode,
    Integer jerseyNumber
) {
}

