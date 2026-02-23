package com.aubl.webpage.service;

import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.PowerRanking;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.entity.TeamSeasonResult;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.PowerRankingRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamRepository;
import com.aubl.webpage.domain.repository.TeamSeasonResultRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PowerRankingService {

    // 연도별 가중치
    private static final BigDecimal W1 = new BigDecimal("0.3");
    private static final BigDecimal W2 = new BigDecimal("0.6");
    private static final BigDecimal W3 = new BigDecimal("1.0");

    // 본선 라운드별 점수
    private static final Map<String, Integer> ROUND_POINTS = Map.of(
        "FINAL",         25,   // 우승 (준우승은 별도 처리)
        "SEMI_FINAL",    15,
        "QUARTER_FINAL", 10,
        "ROUND_OF_16",    5
    );

    private final GameRepository gameRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final TeamSeasonResultRepository teamSeasonResultRepository;
    private final PowerRankingRepository powerRankingRepository;

    public PowerRankingService(
        GameRepository gameRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository,
        TeamSeasonResultRepository teamSeasonResultRepository,
        PowerRankingRepository powerRankingRepository
    ) {
        this.gameRepository = gameRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.teamSeasonResultRepository = teamSeasonResultRepository;
        this.powerRankingRepository = powerRankingRepository;
    }

    // ── power-ranking ──────────────────────────────────────────────────────

    /**
     * 특정 rankingYear의 파워랭킹 목록 조회.
     * POWER_RANKING 캐시(최신 버전)에서 조회. 캐시 없으면 빈 배열 반환.
     *
     * @param rankingYear 기준 연도 (필수)
     * @param limit       상위 N개 제한 (0 = 전체)
     */
    @Transactional(readOnly = true)
    public List<PowerRankingRow> getPowerRanking(Integer rankingYear, int limit) {
        if (rankingYear == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rankingYear is required");
        }

        // 최신 calc_version 조회
        int latestVersion = powerRankingRepository.findMaxCalcVersion(rankingYear).orElse(0);
        if (latestVersion == 0) {
            return List.of(); // 캐시 없음 → 빈 배열
        }

        List<PowerRanking> rows = powerRankingRepository.findByYearAndVersion(rankingYear, latestVersion);

        // limit 적용
        if (limit > 0 && rows.size() > limit) {
            rows = rows.subList(0, limit);
        }

        // rank 부여 (weightedScore DESC 기준, 이미 쿼리에서 정렬됨)
        List<PowerRankingRow> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            PowerRanking pr = rows.get(i);
            result.add(new PowerRankingRow(
                i + 1,
                pr.getTeam().getId(),
                pr.getTeam().getTeamName(),
                pr.getWeightedScore(),
                pr.getY1Score(),
                pr.getY2Score(),
                pr.getY3Score(),
                parseWindowYears(pr.getWindowYears()),
                pr.getCalcVersion()
            ));
        }
        return result;
    }

    /** windowYears JSON 문자열 "[2021,2022,2023]" → List<Integer> */
    private List<Integer> parseWindowYears(String windowYears) {
        if (windowYears == null || windowYears.isBlank()) return List.of();
        try {
            String stripped = windowYears.replaceAll("[\\[\\]\\s]", "");
            if (stripped.isEmpty()) return List.of();
            return Arrays.stream(stripped.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    // ── season-scores ──────────────────────────────────────────────────────

    /**
     * 특정 팀의 연도별 파워랭킹 원점수 조회.
     * POWER_RANKING 캐시가 있으면 캐시에서, 없으면 실시간 계산.
     */
    @Transactional(readOnly = true)
    public List<SeasonScoreRow> getSeasonScores(Long teamId, Integer fromYear, Integer toYear) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
        }
        teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Team not found: " + teamId));

        // 캐시 조회
        List<PowerRanking> cached = powerRankingRepository.findByTeamIdAndYearRange(teamId, fromYear, toYear);

        // 캐시가 있으면 캐시 기반 변환
        if (!cached.isEmpty()) {
            return cached.stream()
                .map(pr -> new SeasonScoreRow(
                    pr.getRankingYear(),
                    pr.getY3Score(),          // 해당 연도 원점수
                    pr.getY3Score(),          // normalized = y3 (단일 연도 조회 시)
                    BigDecimal.ZERO,          // finalsPoints는 y3에 포함됨
                    pr.getY3Score()
                ))
                .toList();
        }

        // 캐시 없으면 실시간 계산
        return computeSeasonScores(teamId, fromYear, toYear);
    }

    private List<SeasonScoreRow> computeSeasonScores(Long teamId, Integer fromYear, Integer toYear) {
        int from = fromYear != null ? fromYear : 2017;
        int to   = toYear   != null ? toYear   : LocalDateTime.now().getYear();

        List<SeasonScoreRow> rows = new ArrayList<>();
        for (int year = from; year <= to; year++) {
            Optional<Season> seasonOpt = seasonRepository.findByYear(year);
            if (seasonOpt.isEmpty()) continue;
            Season season = seasonOpt.get();

            // 예선 원점수
            BigDecimal prelimRaw = calcPrelimRaw(teamId, season);
            // 환산 기준 경기수
            int standard = teamSeasonResultRepository
                .findByTeamIdAndSeasonId(teamId, season.getId())
                .map(TeamSeasonResult::getPrelimGamesStandard)
                .orElse(4);
            // 실제 경기수
            int played = countPrelimGames(teamId, season.getId());
            BigDecimal prelimNormalized = played > 0 && played < standard
                ? prelimRaw.multiply(BigDecimal.valueOf(standard))
                    .divide(BigDecimal.valueOf(played), 3, RoundingMode.HALF_UP)
                : prelimRaw;

            // 본선 점수
            BigDecimal finalsPoints = BigDecimal.valueOf(calcFinalsPoints(teamId, season.getId()));

            BigDecimal total = prelimNormalized.add(finalsPoints).setScale(3, RoundingMode.HALF_UP);

            rows.add(new SeasonScoreRow(year, prelimRaw, prelimNormalized, finalsPoints, total));
        }
        return rows;
    }

    // ── rebuild ────────────────────────────────────────────────────────────

    /**
     * 파워랭킹 집계 재계산 및 POWER_RANKING 테이블 갱신.
     * rankingYear 는 fromYear~toYear 범위의 각 연도에 대해 독립적으로 계산.
     * (각 rankingYear = 그 해 + 직전 2년의 가중 합산)
     */
    @Transactional
    public RebuildResult rebuild(Integer fromYear, Integer toYear) {
        int from = fromYear != null ? fromYear : 2017;
        int to   = toYear   != null ? toYear   : LocalDateTime.now().getYear();

        if (from > to) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "fromYear must be <= toYear");
        }

        String runId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();

        for (int rankingYear = from; rankingYear <= to; rankingYear++) {
            rebuildForYear(rankingYear);
        }

        return new RebuildResult(runId, startedAt, "COMPLETED");
    }

    private void rebuildForYear(int rankingYear) {
        // 3개년 윈도우: y1(가중치 0.3), y2(0.6), rankingYear(1.0)
        int y1 = rankingYear - 2;
        int y2 = rankingYear - 1;

        // 다음 버전 번호
        int nextVersion = powerRankingRepository.findMaxCalcVersion(rankingYear)
            .map(v -> v + 1).orElse(1);

        // 3개년 내 모든 팀 수집
        Map<Long, TeamScoreAccumulator> accMap = new HashMap<>();

        for (int year : new int[]{y1, y2, rankingYear}) {
            Optional<Season> seasonOpt = seasonRepository.findByYear(year);
            if (seasonOpt.isEmpty()) continue;
            Season season = seasonOpt.get();

            List<Game> regularGames = gameRepository.findRegularSeasonGames(season.getId());
            for (Game g : regularGames) {
                accMap.computeIfAbsent(g.getHomeTeam().getId(),
                    id -> new TeamScoreAccumulator(g.getHomeTeam()));
                accMap.computeIfAbsent(g.getAwayTeam().getId(),
                    id -> new TeamScoreAccumulator(g.getAwayTeam()));
            }

            for (TeamScoreAccumulator acc : accMap.values()) {
                BigDecimal prelim = calcPrelimRaw(acc.team.getId(), season);
                int standard = teamSeasonResultRepository
                    .findByTeamIdAndSeasonId(acc.team.getId(), season.getId())
                    .map(TeamSeasonResult::getPrelimGamesStandard).orElse(4);
                int played = countPrelimGames(acc.team.getId(), season.getId());
                BigDecimal prelimNorm = played > 0 && played < standard
                    ? prelim.multiply(BigDecimal.valueOf(standard))
                        .divide(BigDecimal.valueOf(played), 3, RoundingMode.HALF_UP)
                    : prelim;
                BigDecimal finals = BigDecimal.valueOf(
                    calcFinalsPoints(acc.team.getId(), season.getId()));
                BigDecimal yearScore = prelimNorm.add(finals).setScale(3, RoundingMode.HALF_UP);

                if (year == y1) acc.y1 = yearScore;
                else if (year == y2) acc.y2 = yearScore;
                else acc.y3 = yearScore;
            }
        }

        // PowerRanking 저장
        List<String> windowList = buildWindowYears(y1, y2, rankingYear);
        String windowJson = "[" + String.join(",", windowList) + "]";

        List<PowerRanking> toSave = new ArrayList<>();
        for (TeamScoreAccumulator acc : accMap.values()) {
            BigDecimal weighted = acc.y1.multiply(W1)
                .add(acc.y2.multiply(W2))
                .add(acc.y3.multiply(W3))
                .setScale(3, RoundingMode.HALF_UP);

            PowerRanking pr = new PowerRanking();
            pr.setRankingYear(rankingYear);
            pr.setTeam(acc.team);
            pr.setY1Score(acc.y1);
            pr.setY2Score(acc.y2);
            pr.setY3Score(acc.y3);
            pr.setWeightedScore(weighted);
            pr.setWindowYears(windowJson);
            pr.setCalcVersion(nextVersion);
            pr.setCalculatedAt(LocalDateTime.now());
            toSave.add(pr);
        }
        powerRankingRepository.saveAll(toSave);
    }

    // ── 계산 헬퍼 ──────────────────────────────────────────────────────────

    /** 예선 원점수 = 승×3 + 무×1 */
    private BigDecimal calcPrelimRaw(Long teamId, Season season) {
        List<Game> games = gameRepository.findRegularSeasonGames(season.getId());
        int wins = 0, draws = 0;
        for (Game g : games) {
            boolean isHome = g.getHomeTeam().getId().equals(teamId);
            boolean isAway = g.getAwayTeam().getId().equals(teamId);
            if (!isHome && !isAway) continue;
            int myScore  = isHome ? g.getHomeScore() : g.getAwayScore();
            int oppScore = isHome ? g.getAwayScore()  : g.getHomeScore();
            if (myScore > oppScore) wins++;
            else if (myScore == oppScore) draws++;
        }
        return BigDecimal.valueOf(wins * 3L + draws).setScale(3, RoundingMode.HALF_UP);
    }

    /** 실제 치른 예선 경기 수 */
    private int countPrelimGames(Long teamId, Long seasonId) {
        List<Game> games = gameRepository.findRegularSeasonGames(seasonId);
        return (int) games.stream()
            .filter(g -> g.getHomeTeam().getId().equals(teamId)
                || g.getAwayTeam().getId().equals(teamId))
            .count();
    }

    /**
     * 본선 점수.
     * FINAL 경기 승자 = 25점, 패자 = 20점
     * 그 외 라운드 = 해당 라운드 점수 (더 이상 진출 못한 라운드)
     */
    private int calcFinalsPoints(Long teamId, Long seasonId) {
        List<Game> playoffs = gameRepository.findPlayoffGamesBySeasonId(seasonId);
        if (playoffs.isEmpty()) return 0;

        // 가장 높은 라운드 진출 여부 확인
        String bestRound = null;
        boolean wonFinal = false;

        for (Game g : playoffs) {
            boolean isHome = g.getHomeTeam().getId().equals(teamId);
            boolean isAway = g.getAwayTeam().getId().equals(teamId);
            if (!isHome && !isAway) continue;

            String round = g.getPlayoffRound();
            if (round == null) continue;

            // 더 높은 라운드면 갱신
            if (bestRound == null || roundOrder(round) > roundOrder(bestRound)) {
                bestRound = round;
            }
            // FINAL 경기 승자 체크
            if ("FINAL".equalsIgnoreCase(round)
                && g.getHomeScore() != null && g.getAwayScore() != null) {
                boolean iWon = isHome
                    ? g.getHomeScore() > g.getAwayScore()
                    : g.getAwayScore() > g.getHomeScore();
                if (iWon) wonFinal = true;
            }
        }

        if (bestRound == null) return 0;
        if ("FINAL".equalsIgnoreCase(bestRound)) {
            return wonFinal ? 25 : 20;
        }
        return ROUND_POINTS.getOrDefault(bestRound.toUpperCase(), 0);
    }

    private int roundOrder(String round) {
        if (round == null) return 0;
        return switch (round.toUpperCase()) {
            case "ROUND_OF_16"   -> 1;
            case "QUARTER_FINAL" -> 2;
            case "SEMI_FINAL"    -> 3;
            case "FINAL"         -> 4;
            default -> 0;
        };
    }

    private List<String> buildWindowYears(int y1, int y2, int y3) {
        List<String> list = new ArrayList<>();
        if (seasonRepository.findByYear(y1).isPresent()) list.add(String.valueOf(y1));
        if (seasonRepository.findByYear(y2).isPresent()) list.add(String.valueOf(y2));
        if (seasonRepository.findByYear(y3).isPresent()) list.add(String.valueOf(y3));
        return list;
    }

    // ── 내부 accumulator ──────────────────────────────────────────────────

    private static class TeamScoreAccumulator {
        final Team team;
        BigDecimal y1 = BigDecimal.ZERO;
        BigDecimal y2 = BigDecimal.ZERO;
        BigDecimal y3 = BigDecimal.ZERO;
        TeamScoreAccumulator(Team team) { this.team = team; }
    }

    // ── 응답 레코드 ───────────────────────────────────────────────────────

    public record PowerRankingRow(
        int rank,
        Long teamId,
        String teamName,
        BigDecimal weightedScore,
        BigDecimal y1Score,
        BigDecimal y2Score,
        BigDecimal y3Score,
        List<Integer> windowYears,
        Integer calcVersion
    ) {}

    public record SeasonScoreRow(
        Integer seasonYear,
        BigDecimal prelimRaw,
        BigDecimal prelimNormalized,
        BigDecimal finalsPoints,
        BigDecimal total
    ) {}

    public record RebuildResult(
        String runId,
        LocalDateTime startedAt,
        String status
    ) {}
}

