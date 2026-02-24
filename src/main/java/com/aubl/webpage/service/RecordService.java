package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.RecordDtos.BatterRecord;
import com.aubl.webpage.api.dto.RecordDtos.OverviewResponse;
import com.aubl.webpage.api.dto.RecordDtos.PitcherRecord;
import com.aubl.webpage.api.dto.RecordDtos.TeamRecord;
import com.aubl.webpage.domain.entity.BatterStats;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.PitcherStats;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.domain.repository.BatterStatsRepository;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.PitcherStatsRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RecordService {

    private final BatterStatsRepository batterStatsRepository;
    private final PitcherStatsRepository pitcherStatsRepository;
    private final GameRepository gameRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public RecordService(
        BatterStatsRepository batterStatsRepository,
        PitcherStatsRepository pitcherStatsRepository,
        GameRepository gameRepository,
        TeamPlayerRepository teamPlayerRepository
    ) {
        this.batterStatsRepository = batterStatsRepository;
        this.pitcherStatsRepository = pitcherStatsRepository;
        this.gameRepository = gameRepository;
        this.teamPlayerRepository = teamPlayerRepository;
    }

    public OverviewResponse getOverview(Long seasonId) {
        return getOverview(seasonId, null, null, null, null);
    }

    public OverviewResponse getOverview(Long seasonId, String scope, String group, String partCode, String playoffDivision) {
        requireSeason(seasonId);
        List<Game> games = gameRepository.findWithTeamsBySeasonId(seasonId);

        // scope 필터 적용
        String resolvedScope = resolveScope(scope, playoffDivision);
        List<Game> filteredGames = games.stream()
            .filter(g -> g.getHomeScore() != null && g.getAwayScore() != null)
            .filter(g -> matchesScope(g.getGameType(), resolvedScope))
            .toList();

        // group/partCode 필터용 partCode 해소
        String resolvedPartCode = resolvePartCode(group, partCode);

        int totalGames = filteredGames.size();
        int totalTeams = (int) filteredGames.stream()
            .flatMap(g -> List.of(g.getHomeTeam(), g.getAwayTeam()).stream())
            .map(Team::getId)
            .distinct()
            .count();

        RankingQuery topQuery = new RankingQuery(seasonId, 1, "ops", null,
            resolvedScope, group, resolvedPartCode, playoffDivision, "IN");
        BatterRecord topBatter = getTopBatters(topQuery).stream().findFirst().orElse(null);

        RankingQuery topPitcherQuery = new RankingQuery(seasonId, 1, "era", null,
            resolvedScope, group, resolvedPartCode, playoffDivision, "IN");
        PitcherRecord topPitcher = getTopPitchers(topPitcherQuery).stream().findFirst().orElse(null);

        return new OverviewResponse(seasonId, totalGames, totalTeams, topBatter, topPitcher);
    }

    public List<TeamRecord> getTeamRecords(Long seasonId) {
        return getTeamRecords(seasonId, null, null, null, null);
    }

    public List<TeamRecord> getTeamRecords(Long seasonId, String scope, String group, String partCode, String playoffDivision) {
        requireSeason(seasonId);
        List<Game> games = gameRepository.findWithTeamsBySeasonId(seasonId);
        String resolvedScope = resolveScope(scope, playoffDivision);
        String resolvedPartCode = resolvePartCode(group, partCode);

        // 시즌별 팀→partCode 매핑 (AC3: partCode 기반 필터, AC4: 메타 필드)
        Map<Long, String> teamPartCodeMap = teamPlayerRepository
            .findTeamPartCodesBySeasonId(seasonId)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (String) row[1],
                (a, b) -> a  // 동일 팀에 중복 partCode 있으면 첫 번째 사용
            ));

        Map<Long, TeamRecordAccumulator> standings = new HashMap<>();
        for (Game game : games) {
            if (game.getHomeScore() == null || game.getAwayScore() == null) continue;
            if (!matchesScope(game.getGameType(), resolvedScope)) continue;

            Long homeId = game.getHomeTeam().getId();
            Long awayId = game.getAwayTeam().getId();

            // AC3: partCode 필터 적용 — 해당 조(partCode)에 속한 팀의 경기만 집계
            if (resolvedPartCode != null) {
                boolean homeMatch = resolvedPartCode.equals(teamPartCodeMap.get(homeId));
                boolean awayMatch = resolvedPartCode.equals(teamPartCodeMap.get(awayId));
                if (!homeMatch && !awayMatch) continue;
            }

            String tier = game.getPlayoffTier();
            accumulate(standings, game.getHomeTeam(), game.getHomeScore(), game.getAwayTeam(), game.getAwayScore(), tier);
            accumulate(standings, game.getAwayTeam(), game.getAwayScore(), game.getHomeTeam(), game.getHomeScore(), tier);
        }

        // resolvedScope → 응답 scope 문자열
        String scopeLabel = resolvedScope != null ? resolvedScope
            : (playoffDivision != null && !playoffDivision.equalsIgnoreCase("ALL") ? "PLAYOFF" : "LEAGUE");

        return standings.values().stream()
            .sorted(Comparator
                .comparing(TeamRecordAccumulator::winPct).reversed()
                .thenComparing(TeamRecordAccumulator::wins).reversed())
            .map(acc -> {
                String pc    = teamPartCodeMap.get(acc.team.getId());
                String grp   = mapGroup(pc);
                return acc.toDto(pc, grp, scopeLabel);
            })
            .toList();
    }

    public List<BatterRecord> getTopBatters(Long seasonId, int limit, String sortKey) {
        return getTopBatters(new RankingQuery(seasonId, limit, sortKey, null, null, null, null, null, null));
    }

    public List<PitcherRecord> getTopPitchers(Long seasonId, int limit, String sortKey) {
        return getTopPitchers(new RankingQuery(seasonId, limit, sortKey, null, null, null, null, null, null));
    }

    public List<BatterRecord> getTopBatters(RankingQuery query) {
        requireSeason(query.seasonId());

        // scope/partCode/seasonType 해소
        String resolvedScope    = resolveScope(query.scope(), query.playoffDivision());
        String resolvedPartCode = resolvePartCode(query.group(), query.partCode());
        String resolvedSeasonType = resolveSeasonType(query.playoffDivision());

        List<BatterStats> statsList = batterStatsRepository.findBySeasonIdFiltered(
            query.seasonId(),
            resolvedScope,
            resolvedPartCode,
            resolvedSeasonType
        );

        // regulation 필터 (IN: valid 데이터만, OUT: 유효하지 않은 데이터, ALL: 전체)
        String regulation = query.regulation();
        Comparator<BatterStats> comparator = resolveBatterComparator(query.sortKey(), query.sortOrder());
        List<BatterStats> filtered = new ArrayList<>();
        for (BatterStats stats : statsList) {
            boolean valid = isValidBatter(stats);
            if ("OUT".equalsIgnoreCase(regulation) && valid) continue;
            if (!"OUT".equalsIgnoreCase(regulation) && !"ALL".equalsIgnoreCase(regulation) && !valid) continue;
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (query.limit() > 0 && filtered.size() > query.limit()) {
            filtered = filtered.subList(0, query.limit());
        }
        List<BatterRecord> ranked = new ArrayList<>();
        for (BatterStats stats : filtered) {
            ranked.add(toBatterRecord(stats, ranked.size() + 1, query, resolvedPartCode, resolvedScope));
        }
        return ranked;
    }

    public List<PitcherRecord> getTopPitchers(RankingQuery query) {
        requireSeason(query.seasonId());

        String resolvedScope      = resolveScope(query.scope(), query.playoffDivision());
        String resolvedPartCode   = resolvePartCode(query.group(), query.partCode());
        String resolvedSeasonType = resolveSeasonType(query.playoffDivision());

        List<PitcherStats> statsList = pitcherStatsRepository.findBySeasonIdFiltered(
            query.seasonId(),
            resolvedScope,
            resolvedPartCode,
            resolvedSeasonType
        );

        String regulation = query.regulation();
        Comparator<PitcherStats> comparator = resolvePitcherComparator(query.sortKey(), query.sortOrder());
        List<PitcherStats> filtered = new ArrayList<>();
        for (PitcherStats stats : statsList) {
            boolean valid = isValidPitcher(stats);
            if ("OUT".equalsIgnoreCase(regulation) && valid) continue;
            if (!"OUT".equalsIgnoreCase(regulation) && !"ALL".equalsIgnoreCase(regulation) && !valid) continue;
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (query.limit() > 0 && filtered.size() > query.limit()) {
            filtered = filtered.subList(0, query.limit());
        }
        List<PitcherRecord> ranked = new ArrayList<>();
        for (PitcherStats stats : filtered) {
            ranked.add(toPitcherRecord(stats, ranked.size() + 1, query, resolvedPartCode, resolvedScope));
        }
        return ranked;
    }

    // ── 필터 해소 헬퍼 ──────────────────────────────────────────────────────

    /**
     * scope 최종값 해소.
     * playoffDivision 이 ALL/null 이 아니면 PLAYOFF 로 강제.
     */
    private String resolveScope(String scope, String playoffDivision) {
        if (playoffDivision != null && !playoffDivision.isBlank()
            && !playoffDivision.equalsIgnoreCase("ALL")) {
            return "PLAYOFF";
        }
        if (scope == null || scope.isBlank() || scope.equalsIgnoreCase("ALL")) {
            return null; // null = 전체
        }
        String upper = scope.toUpperCase();
        if (!upper.equals("LEAGUE") && !upper.equals("PLAYOFF")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid scope. Allowed: ALL, LEAGUE, PLAYOFF");
        }
        return upper;
    }

    /**
     * group 우선, 없으면 partCode 사용.
     * group(A~H) → partCode(1~8) 역매핑.
     */
    private String resolvePartCode(String group, String partCode) {
        if (group != null && !group.isBlank() && !group.equalsIgnoreCase("ALL")) {
            return switch (group.toUpperCase()) {
                case "A" -> "1"; case "B" -> "2"; case "C" -> "3"; case "D" -> "4";
                case "E" -> "5"; case "F" -> "6"; case "G" -> "7"; case "H" -> "8";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid group. Allowed: ALL, A~H");
            };
        }
        if (partCode != null && !partCode.isBlank() && !partCode.equalsIgnoreCase("ALL")) {
            return partCode;
        }
        return null; // null = 전체
    }

    /**
     * playoffDivision → DB seasonType 값 변환.
     * EUTTEUM/BEOGEUM 이면 해당 값, ALL/null 이면 null(전체).
     */
    private String resolveSeasonType(String playoffDivision) {
        if (playoffDivision == null || playoffDivision.isBlank()
            || playoffDivision.equalsIgnoreCase("ALL")) {
            return null;
        }
        return playoffDivision.toUpperCase();
    }

    /**
     * 게임 gameType 이 resolvedScope 에 해당하는지 판별.
     * null scope = 전체 허용.
     */
    private boolean matchesScope(String gameType, String resolvedScope) {
        if (resolvedScope == null) return true;
        if ("LEAGUE".equals(resolvedScope)) {
            return gameType != null && gameType.contains("정규");
        }
        if ("PLAYOFF".equals(resolvedScope)) {
            return gameType != null && gameType.contains("포스트");
        }
        return true;
    }

    // ── 기존 헬퍼 ───────────────────────────────────────────────────────────

    private void requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
    }

    private Comparator<BatterStats> resolveBatterComparator(String sortKey, String sortOrder) {
        String key = sortKey == null ? "battingAverage" : sortKey;
        Comparator<BatterStats> base = switch (key) {
            case "hits"          -> Comparator.comparing(BatterStats::getHits,           Comparator.nullsLast(Integer::compareTo));
            case "homeRuns"      -> Comparator.comparing(BatterStats::getHomeRuns,        Comparator.nullsLast(Integer::compareTo));
            case "rbi"           -> Comparator.comparing(BatterStats::getRunsBattedIn,    Comparator.nullsLast(Integer::compareTo));
            case "ops"           -> Comparator.comparing(BatterStats::getOps,             Comparator.nullsLast(BigDecimal::compareTo));
            case "sluggingPct"   -> Comparator.comparing(BatterStats::getSluggingPct,     Comparator.nullsLast(BigDecimal::compareTo));
            case "onBasePct"     -> Comparator.comparing(BatterStats::getOnBasePct,       Comparator.nullsLast(BigDecimal::compareTo));
            case "battingAverage"-> Comparator.comparing(BatterStats::getBattingAverage,  Comparator.nullsLast(BigDecimal::compareTo));
            case "gamesPlayed"   -> Comparator.comparing(BatterStats::getGamesPlayed,     Comparator.nullsLast(Integer::compareTo));
            case "plateAppearance"->Comparator.comparing(BatterStats::getPlateAppearance, Comparator.nullsLast(Integer::compareTo));
            case "stolenBases"   -> Comparator.comparing(BatterStats::getStolenBases,     Comparator.nullsLast(Integer::compareTo));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid batter sort: " + key);
        };
        boolean defaultDesc = !key.equals("gamesPlayed") && !key.equals("plateAppearance");
        return applyOrder(base, sortOrder, defaultDesc ? "desc" : "desc");
    }

    private Comparator<PitcherStats> resolvePitcherComparator(String sortKey, String sortOrder) {
        String key = sortKey == null ? "era" : sortKey;
        Comparator<PitcherStats> base = switch (key) {
            case "era"          -> Comparator.comparing(PitcherStats::getEra,           Comparator.nullsLast(BigDecimal::compareTo));
            case "whip"         -> Comparator.comparing(PitcherStats::getWhip,          Comparator.nullsLast(BigDecimal::compareTo));
            case "strikeouts"   -> Comparator.comparing(PitcherStats::getStrikeouts,    Comparator.nullsLast(Integer::compareTo));
            case "wins"         -> Comparator.comparing(PitcherStats::getWins,          Comparator.nullsLast(Integer::compareTo));
            case "saves"        -> Comparator.comparing(PitcherStats::getSaves,         Comparator.nullsLast(Integer::compareTo));
            case "inningsPitched"->Comparator.comparing(PitcherStats::getInningsPitched,Comparator.nullsLast(BigDecimal::compareTo));
            case "walksAllowed" -> Comparator.comparing(PitcherStats::getWalksAllowed,  Comparator.nullsLast(Integer::compareTo));
            case "gamesPlayed"  -> Comparator.comparing(PitcherStats::getGamesPlayed,   Comparator.nullsLast(Integer::compareTo));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid pitcher sort: " + key);
        };
        String defaultOrder = (key.equals("era") || key.equals("whip")) ? "asc" : "desc";
        return applyOrder(base, sortOrder, defaultOrder);
    }

    private <T> Comparator<T> applyOrder(Comparator<T> base, String sortOrder, String defaultOrder) {
        String order = (sortOrder == null || sortOrder.isBlank()) ? defaultOrder : sortOrder;
        if (order == null || order.equalsIgnoreCase("desc")) return base.reversed();
        if (order.equalsIgnoreCase("asc"))  return base;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid sortOrder: " + sortOrder);
    }

    private BatterRecord toBatterRecord(BatterStats stats, int rank, RankingQuery query,
        String resolvedPartCode, String resolvedScope) {
        TeamPlayer tp = stats.getTeamPlayer();
        // DB의 part_code 우선, 필터 partCode는 쿼리 조건이므로 응답은 항상 DB 원본 사용
        String partCode = tp.getPartCode();
        String group    = mapGroup(partCode);
        String scope    = resolvedScope != null ? resolvedScope
            : (stats.getSeasonType() == null ? "LEAGUE" : "PLAYOFF");
        String regulation = query.regulation();
        return new BatterRecord(
            rank,
            tp.getPlayer().getId(),
            tp.getPlayer().getPlayerName(),
            tp.getTeam().getId(),
            tp.getTeam().getTeamName(),
            tp.getJerseyNumber(),
            stats.getSeason().getId(),
            stats.getGamesPlayed(),
            stats.getPlateAppearance(),
            stats.getAtBats(),
            stats.getHits(),
            stats.getHomeRuns(),
            stats.getRunsBattedIn(),
            stats.getStolenBases(),
            stats.getWalks(),
            stats.getStrikeouts(),
            stats.getBattingAverage(),
            stats.getOnBasePct(),
            stats.getSluggingPct(),
            stats.getOps(),
            partCode,
            group,
            scope,
            stats.getSeasonType(),
            regulation
        );
    }

    private PitcherRecord toPitcherRecord(PitcherStats stats, int rank, RankingQuery query,
        String resolvedPartCode, String resolvedScope) {
        TeamPlayer tp = stats.getTeamPlayer();
        String partCode = tp.getPartCode();
        String group    = mapGroup(partCode);
        String scope    = resolvedScope != null ? resolvedScope
            : (stats.getSeasonType() == null ? "LEAGUE" : "PLAYOFF");
        String regulation = query.regulation();
        return new PitcherRecord(
            rank,
            tp.getPlayer().getId(),
            tp.getPlayer().getPlayerName(),
            tp.getTeam().getId(),
            tp.getTeam().getTeamName(),
            tp.getJerseyNumber(),
            stats.getSeason().getId(),
            stats.getGamesPlayed(),
            stats.getInningsPitched(),
            stats.getWins(),
            stats.getLosses(),
            stats.getSaves(),
            stats.getStrikeouts(),
            stats.getWalksAllowed(),
            stats.getEra(),
            stats.getWhip(),
            partCode,
            group,
            scope,
            stats.getSeasonType(),
            regulation
        );
    }

    private String mapGroup(String partCode) {
        if (partCode == null) return null;
        return switch (partCode) {
            case "1" -> "A"; case "2" -> "B"; case "3" -> "C"; case "4" -> "D";
            case "5" -> "E"; case "6" -> "F"; case "7" -> "G"; case "8" -> "H";
            default -> null;
        };
    }

    public record RankingQuery(
        Long seasonId, int limit, String sortKey, String sortOrder,
        String scope, String group, String partCode, String playoffDivision, String regulation
    ) {}

    private boolean isValidBatter(BatterStats stats) {
        if (stats == null || stats.getTeamPlayer() == null
            || stats.getTeamPlayer().getPlayer() == null
            || stats.getTeamPlayer().getTeam() == null) return false;
        return stats.getBattingAverage() != null
            && stats.getHits() != null
            && stats.getHomeRuns() != null
            && stats.getRunsBattedIn() != null;
    }

    private boolean isValidPitcher(PitcherStats stats) {
        if (stats == null || stats.getTeamPlayer() == null
            || stats.getTeamPlayer().getPlayer() == null
            || stats.getTeamPlayer().getTeam() == null) return false;
        return stats.getEra() != null && stats.getInningsPitched() != null;
    }

    private void accumulate(Map<Long, TeamRecordAccumulator> map, Team team,
        Integer teamScore, Team opponent, Integer opponentScore, String playoffTier) {
        if (team == null || teamScore == null || opponentScore == null) return;
        TeamRecordAccumulator acc = map.computeIfAbsent(team.getId(), id -> new TeamRecordAccumulator(team));
        acc.games += 1;
        if (teamScore > opponentScore)           acc.wins++;
        else if (teamScore.equals(opponentScore)) acc.ties++;
        else                                      acc.losses++;
        if (playoffTier != null && acc.playoffTier == null) {
            acc.playoffTier = playoffTier;
        }
    }

    private static class TeamRecordAccumulator {
        private final Team team;
        private int games, wins, losses, ties;
        private String playoffTier;
        TeamRecordAccumulator(Team team) { this.team = team; }
        BigDecimal winPct() {
            if (games == 0) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(wins + (ties * 0.5))
                .divide(BigDecimal.valueOf(games), 3, RoundingMode.HALF_UP);
        }
        int wins() { return wins; }
        /** 기본 (메타 없음, 하위 호환) */
        TeamRecord toDto() {
            return new TeamRecord(team.getId(), team.getTeamName(), wins, losses, ties, winPct());
        }
        /** AC4: partCode/group/scope 포함 */
        TeamRecord toDto(String partCode, String group, String scope) {
            return new TeamRecord(team.getId(), team.getTeamName(), wins, losses, ties, winPct(),
                partCode, group, scope, playoffTier);
        }
    }
}
