package com.aubl.webpage.api.dto;

import java.time.LocalDate;

public record GameCreateRequest(
    Long seasonId,
    LocalDate gameDate,
    Integer gameNumber,
    Long homeTeamId,
    Long awayTeamId,
    Integer homeScore,
    Integer awayScore,
    String gameType,
    String csvFilePath
) {
}
