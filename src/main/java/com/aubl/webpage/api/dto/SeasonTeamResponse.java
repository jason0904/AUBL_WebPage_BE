package com.aubl.webpage.api.dto;

public record SeasonTeamResponse(
    Long seasonId,
    Long teamId,
    String teamName,
    String teamCode
) {
}

