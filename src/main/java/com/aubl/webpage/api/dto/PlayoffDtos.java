package com.aubl.webpage.api.dto;

import java.time.LocalDate;

public class PlayoffDtos {

    /**
     * partCode(1~8) -> group(A~H) 매핑
     */
    public static String toGroup(String partCode) {
        if (partCode == null) return null;
        return switch (partCode) {
            case "1" -> "A"; case "2" -> "B"; case "3" -> "C"; case "4" -> "D";
            case "5" -> "E"; case "6" -> "F"; case "7" -> "G"; case "8" -> "H";
            default -> null;
        };
    }

    /**
     * playoff_round → 라운드 순서 (낮을수록 이른 라운드)
     * 파워랭킹 본선 점수 계산에 사용
     *   FINAL        → 우승(25) or 준우승(20)
     *   SEMI_FINAL   → 4강(15)
     *   QUARTER_FINAL→ 8강(10)
     *   ROUND_OF_16  → 16강(5)
     *   null/기타     → 예선 탈락(0)
     */
    public static int toFinalsPoints(String playoffRound, boolean isWinner) {
        if (playoffRound == null) return 0;
        return switch (playoffRound.toUpperCase()) {
            case "FINAL"         -> isWinner ? 25 : 20;
            case "SEMI_FINAL"    -> 15;
            case "QUARTER_FINAL" -> 10;
            case "ROUND_OF_16"   -> 5;
            default -> 0;
        };
    }

    /**
     * 라운드 정렬 순서 (16강 → 결승 순)
     */
    public static int roundOrder(String round) {
        if (round == null) return 99;
        return switch (round.toUpperCase()) {
            case "ROUND_OF_16"   -> 1;
            case "QUARTER_FINAL" -> 2;
            case "SEMI_FINAL"    -> 3;
            case "FINAL"         -> 4;
            default -> 99;
        };
    }

    // ── 응답 DTO ──────────────────────────────────────────────

    /**
     * view=games : 포스트시즌 경기 1건
     */
    public record PlayoffGameRow(
        Long seasonId,
        Integer seasonYear,
        Long gameId,
        LocalDate gameDate,
        String gameType,
        String playoffTier,    // EUTTEUM | BEOGEUM | null
        String playoffRound,   // ROUND_OF_16 | QUARTER_FINAL | SEMI_FINAL | FINAL | null
        Long homeTeamId,
        String homeTeamName,
        Integer homeScore,
        Long awayTeamId,
        String awayTeamName,
        Integer awayScore,
        String scope           // 항상 "PLAYOFF"
    ) {}

    /**
     * view=teams : 포스트시즌 팀별 집계
     * bestRound = 해당 팀이 진출한 가장 높은 라운드
     */
    public record PlayoffTeamRow(
        Long seasonId,
        Integer seasonYear,
        Long teamId,
        String teamName,
        String playoffTier,    // EUTTEUM | BEOGEUM | null (tier 필터 시 값 고정)
        String bestRound,      // 해당 팀의 최고 진출 라운드
        int wins,
        int losses,
        int runsScored,
        int runsAllowed,
        String scope,          // 항상 "PLAYOFF"
        String partCode,
        String group
    ) {}
}

