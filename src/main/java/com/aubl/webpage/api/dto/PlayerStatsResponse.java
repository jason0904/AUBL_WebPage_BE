package com.aubl.webpage.api.dto;

import java.util.List;

public record PlayerStatsResponse(
    List<BatterStatSummary> batterStats,
    List<PitcherStatSummary> pitcherStats
) {
}
