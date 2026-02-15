package com.aubl.webpage.api.dto;

import java.util.List;

public record PlayerStatsResponse(
    String playerName,
    String teamName,
    Integer jerseyNumber,
    List<BatterStatSummary> batterStats,
    List<PitcherStatSummary> pitcherStats
) {
}
