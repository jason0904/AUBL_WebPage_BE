package com.aubl.webpage.api.dto;

import java.util.List;

public record PlayerGameLogsResponse(
    List<BatterGameLogSummary> batterLogs,
    List<PitcherGameLogSummary> pitcherLogs
) {
}
