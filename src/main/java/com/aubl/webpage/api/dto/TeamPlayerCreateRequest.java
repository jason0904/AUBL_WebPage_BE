package com.aubl.webpage.api.dto;

public record TeamPlayerCreateRequest(
    Long teamId,
    Long playerId,
    Long seasonId,
    Integer jerseyNumber
) {
}
