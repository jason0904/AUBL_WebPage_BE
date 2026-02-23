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
        int rank,
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        Integer jerseyNumber,
        Long seasonId,
        Integer gamesPlayed,
        Integer plateAppearance,
        Integer atBats,
        Integer hits,
        Integer homeRuns,
        Integer runsBattedIn,
        Integer stolenBases,
        Integer walks,
        Integer strikeouts,
        BigDecimal battingAverage,
        BigDecimal onBasePct,
        BigDecimal sluggingPct,
        BigDecimal ops,
        String partCode,
        String group,
        String scope,
        String seasonType,
        String regulation
    ) {
    }

    public record PitcherRecord(
        int rank,
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        Integer jerseyNumber,
        Long seasonId,
        Integer gamesPlayed,
        BigDecimal inningsPitched,
        Integer wins,
        Integer losses,
        Integer saves,
        Integer strikeouts,
        Integer walksAllowed,
        BigDecimal era,
        BigDecimal whip,
        String partCode,
        String group,
        String scope,
        String seasonType,
        String regulation
    ) {
    }
}
