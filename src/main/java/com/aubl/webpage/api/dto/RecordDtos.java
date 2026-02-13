package com.aubl.webpage.api.dto;

import java.math.BigDecimal;

public class RecordDtos {

    public record OverviewResponse(
        Long seasonId,
        int totalGames,
        int totalTeams,
        BatterRecord topBatter,
        PitcherRecord topPitcher
    ) {
    }

    public record TeamRecord(
        Long teamId,
        String teamName,
        int wins,
        int losses,
        int ties,
        BigDecimal winPct
    ) {
    }

    public record BatterRecord(
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        Long seasonId,
        Integer gamesPlayed,
        Integer plateAppearance,
        Integer atBats,
        Integer hits,
        Integer homeRuns,
        Integer runsBattedIn,
        BigDecimal battingAverage,
        BigDecimal onBasePct,
        BigDecimal sluggingPct,
        BigDecimal ops
    ) {
    }

    public record PitcherRecord(
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        Long seasonId,
        Integer gamesPlayed,
        BigDecimal inningsPitched,
        Integer wins,
        Integer losses,
        Integer saves,
        Integer strikeouts,
        BigDecimal era,
        BigDecimal whip
    ) {
    }
}
