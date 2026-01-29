package com.aubl.webpage.api.dto;

public record ImportResult(
    int gamesProcessed,
    int batterLogsInserted,
    int pitcherLogsInserted
) {
}
