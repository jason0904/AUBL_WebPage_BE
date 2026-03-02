package com.aubl.webpage.service;

import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.PowerRanking;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.PowerRankingRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamRepository;
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
import java.util.stream.Collectors;
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

    // 으뜸(EUTTEUM) 라운드별 점수 (FINAL은 별도 처리: 우승=25, 준우승=20)
    private static final Map<String, Integer> EUTTEUM_ROUND_POINTS = Map.of(
        "SEMI_FINAL",    15,
        "QUARTER_FINAL", 10,
        "ROUND_OF_16",    5
    );

    private final GameRepository gameRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final PowerRankingRepository powerRankingRepository;

    public PowerRankingService(
        GameRepository gameRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository,
        PowerRankingRepository powerRankingRepository
    ) {
        this.gameRepository = gameRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.powerRankingRepository = powerRankingRepository;
    }

    // ── power-ranking ──────────────────────────────────────────────────────

    /**
     * 특정 rankingYear의 파워랭킹 목록 조회.
     * POWER_RANKING 캐시(최신 버전)에서 조회. 캐시 없으면 빈 배열 반환.
     */
    @Transactional(readOnly = true)
    public List<PowerRankingRow> getPowerRanking(Integer rankingYear, int limit) {
        if (rankingYear == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rankingYear is required");
        }

        int latestVersion = powerRankingRepository.findMaxCalcVersion(rankingYear).orElse(0);
        if (latestVersion == 0) {
            return List.of();
        }

        List<PowerRanking> rows = powerRankingRepository.findByYearAndVersion(rankingYear, latestVersion);

        // 이벤트 팀 제외: 팀명이 숫자 또는 영문자로 시작하는 팀 (TEAM WILSON, 2025 올스타 등)
        rows = rows.stream()
            .filter(pr -> {
                String name = pr.getTeam().getTeamName();
                if (name == null || name.isEmpty()) return true;
                char first = name.charAt(0);
                return !(first < 128 && Character.isLetterOrDigit(first));
            })
            .collect(Collectors.toList());

        if (limit > 0 && rows.size() > limit) {
            rows = rows.subList(0, limit);
        }

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

        List<PowerRanking> cached = powerRankingRepository.findByTeamIdAndYearRange(teamId, fromYear, toYear);

        if (!cached.isEmpty()) {
            return cached.stream()
                .map(pr -> new SeasonScoreRow(
                    pr.getRankingYear(),
                    pr.getY3Score(),
                    pr.getY3Score(),
                    BigDecimal.ZERO,
                    pr.getY3Score()
                ))
                .toList();
        }

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

            // 정규시즌 게임 한 번만 로드
            List<Game> regularGames = gameRepository.findRegularSeasonGames(season.getId());

            // 예선 원점수
            BigDecimal prelimRaw = calcPrelimFromGames(teamId, regularGames);

            // 실제 경기수
            int played = (int) regularGames.stream()
                .filter(g -> g.getHomeTeam().getId().equals(teamId)
                    || g.getAwayTeam().getId().equals(teamId))
                .count();

            // 정규화: 4/6/9경기 포맷만 8경기 기준으로 환산
            BigDecimal prelimNormalized;
            if (played == 4 || played == 6 || played == 9) {
                prelimNormalized = prelimRaw.multiply(BigDecimal.valueOf(8))
                    .divide(BigDecimal.valueOf(played), 3, RoundingMode.HALF_UP);
            } else {
                prelimNormalized = prelimRaw;
            }

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
        // 3개년 윈도우: y1(가중치 0.3), y2(0.6), y3(1.0)
        // 직전 3개 완료 시즌 데이터 사용
        // 예: rankingYear=2026 → y1=2023, y2=2024, y3=2025
        int y3 = rankingYear - 1;
        int y1 = rankingYear - 3;
        int y2 = rankingYear - 2;

        int nextVersion = powerRankingRepository.findMaxCalcVersion(rankingYear)
            .map(v -> v + 1).orElse(1);

        Map<Long, TeamScoreAccumulator> accMap = new HashMap<>();

        for (int year : new int[]{y1, y2, y3}) {
            Optional<Season> seasonOpt = seasonRepository.findByYear(year);
            if (seasonOpt.isEmpty()) continue;
            Season season = seasonOpt.get();

            // 정규시즌 게임 한 번만 로드
            List<Game> regularGames = gameRepository.findRegularSeasonGames(season.getId());

            // 팀별 실제 경기수 집계
            Map<Long, Integer> teamGameCounts = new HashMap<>();
            for (Game g : regularGames) {
                Long hId = g.getHomeTeam().getId();
                Long aId = g.getAwayTeam().getId();
                teamGameCounts.put(hId, teamGameCounts.getOrDefault(hId, 0) + 1);
                teamGameCounts.put(aId, teamGameCounts.getOrDefault(aId, 0) + 1);
            }

            // 팀 수집
            for (Game g : regularGames) {
                accMap.computeIfAbsent(g.getHomeTeam().getId(),
                    id -> new TeamScoreAccumulator(g.getHomeTeam()));
                accMap.computeIfAbsent(g.getAwayTeam().getId(),
                    id -> new TeamScoreAccumulator(g.getAwayTeam()));
            }

            // 포스트시즌 게임 한 번만 로드
            List<Game> playoffGames = gameRepository.findPlayoffGamesBySeasonId(season.getId());

            for (TeamScoreAccumulator acc : accMap.values()) {
                // 예선 원점수 (로드된 게임 목록에서 계산, DB 재조회 없음)
                BigDecimal prelim = calcPrelimFromGames(acc.team.getId(), regularGames);

                // 실제 경기수 (로드된 목록에서 계산)
                int played = teamGameCounts.getOrDefault(acc.team.getId(), 0);

                // 정규화: 4/6/9경기 포맷만 8경기 기준으로 환산
                // (7경기 등 부분 참가 또는 표준 8경기는 원점수 그대로)
                BigDecimal prelimNorm;
                if (played == 4 || played == 6 || played == 9) {
                    prelimNorm = prelim.multiply(BigDecimal.valueOf(8))
                        .divide(BigDecimal.valueOf(played), 3, RoundingMode.HALF_UP);
                } else {
                    prelimNorm = prelim;
                }

                // 본선 점수 (티어 구분, 로드된 목록 사용)
                BigDecimal finals = BigDecimal.valueOf(
                    calcFinalsPointsFromGames(acc.team.getId(), playoffGames));

                BigDecimal yearScore = prelimNorm.add(finals).setScale(3, RoundingMode.HALF_UP);

                if (year == y1) acc.y1 = yearScore;
                else if (year == y2) acc.y2 = yearScore;
                else acc.y3 = yearScore;
            }
        }

        // PowerRanking 저장
        List<String> windowList = buildWindowYears(y1, y2, y3);
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


    /** 예선 원점수 = 승×3 + 무×1 (미리 로드된 게임 목록 사용, DB 재조회 없음) */
    private BigDecimal calcPrelimFromGames(Long teamId, List<Game> games) {
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

    /**
     * 본선 점수 (DB 재조회 버전).
     * 으뜸(EUTTEUM): 우승=25, 준우승=20, 4강=15, 8강=10, 16강=5
     * 버금(BEOGEUM): 우승=10, 준우승=5, 나머지=0
     */
    private int calcFinalsPoints(Long teamId, Long seasonId) {
        List<Game> playoffs = gameRepository.findPlayoffGamesBySeasonId(seasonId);
        return calcFinalsPointsFromGames(teamId, playoffs);
    }

    /**
     * 본선 점수 (미리 로드된 게임 목록 사용, DB 재조회 없음).
     * 으뜸(EUTTEUM): 우승=25, 준우승=20, 4강=15, 8강=10, 16강=5
     * 버금(BEOGEUM): 우승=10, 준우승=5, 나머지=0
     */
    private int calcFinalsPointsFromGames(Long teamId, List<Game> playoffs) {
        if (playoffs.isEmpty()) return 0;

        String bestEutteumRound = null;
        boolean wonEutteumFinal = false;
        String bestBeogumRound = null;
        boolean wonBeogumFinal = false;

        for (Game g : playoffs) {
            boolean isHome = g.getHomeTeam().getId().equals(teamId);
            boolean isAway = g.getAwayTeam().getId().equals(teamId);
            if (!isHome && !isAway) continue;

            String round = g.getPlayoffRound();
            if (round == null) continue;

            String tier = g.getPlayoffTier();
            boolean isBeogum = "BEOGEUM".equalsIgnoreCase(tier);

            boolean iWon = false;
            if ("FINAL".equalsIgnoreCase(round)
                    && g.getHomeScore() != null && g.getAwayScore() != null) {
                iWon = isHome
                    ? g.getHomeScore() > g.getAwayScore()
                    : g.getAwayScore() > g.getHomeScore();
            }

            if (!isBeogum) {
                // 으뜸 브래킷
                if (bestEutteumRound == null || roundOrder(round) > roundOrder(bestEutteumRound)) {
                    bestEutteumRound = round;
                }
                if ("FINAL".equalsIgnoreCase(round) && iWon) wonEutteumFinal = true;
            } else {
                // 버금 브래킷
                if (bestBeogumRound == null || roundOrder(round) > roundOrder(bestBeogumRound)) {
                    bestBeogumRound = round;
                }
                if ("FINAL".equalsIgnoreCase(round) && iWon) wonBeogumFinal = true;
            }
        }

        // 으뜸 점수: 결승=25/20, 4강=15, 8강=10, 16강=5
        int eutteumPoints = 0;
        if (bestEutteumRound != null) {
            if ("FINAL".equalsIgnoreCase(bestEutteumRound)) {
                eutteumPoints = wonEutteumFinal ? 25 : 20;
            } else {
                eutteumPoints = EUTTEUM_ROUND_POINTS.getOrDefault(bestEutteumRound.toUpperCase(), 0);
            }
        }

        // 버금 점수: 결승 진출팀만 부여 (비결승 라운드는 0점)
        int beogumPoints = 0;
        if ("FINAL".equalsIgnoreCase(bestBeogumRound)) {
            beogumPoints = wonBeogumFinal ? 10 : 5;
        }

        return eutteumPoints + beogumPoints;
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
