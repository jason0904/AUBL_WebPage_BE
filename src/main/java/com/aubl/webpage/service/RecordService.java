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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public RecordService(
        BatterStatsRepository batterStatsRepository,
        PitcherStatsRepository pitcherStatsRepository,
        GameRepository gameRepository
    ) {
        this.batterStatsRepository = batterStatsRepository;
        this.pitcherStatsRepository = pitcherStatsRepository;
        this.gameRepository = gameRepository;
    }

    public OverviewResponse getOverview(Long seasonId) {
        requireSeason(seasonId);
        List<Game> games = gameRepository.findWithTeamsBySeasonId(seasonId);
        int totalGames = (int) games.stream()
            .filter(g -> g.getHomeScore() != null && g.getAwayScore() != null)
            .count();

        int totalTeams = (int) games.stream()
            .flatMap(g -> List.of(g.getHomeTeam(), g.getAwayTeam()).stream())
            .map(Team::getId)
            .distinct()
            .count();

        BatterRecord topBatter = getTopBatters(new RankingQuery(seasonId, 1, "ops", null, null, null, null, null, null))
            .stream().findFirst().orElse(null);
        PitcherRecord topPitcher = getTopPitchers(new RankingQuery(seasonId, 1, "era", null, null, null, null, null, null))
            .stream().findFirst().orElse(null);

        return new OverviewResponse(seasonId, totalGames, totalTeams, topBatter, topPitcher);
    }

    public List<TeamRecord> getTeamRecords(Long seasonId) {
        requireSeason(seasonId);
        List<Game> games = gameRepository.findWithTeamsBySeasonId(seasonId);
        Map<Long, TeamRecordAccumulator> standings = new HashMap<>();

        for (Game game : games) {
            if (game.getHomeScore() == null || game.getAwayScore() == null) {
                continue;
            }
            accumulate(standings, game.getHomeTeam(), game.getHomeScore(), game.getAwayTeam(), game.getAwayScore());
            accumulate(standings, game.getAwayTeam(), game.getAwayScore(), game.getHomeTeam(), game.getHomeScore());
        }

        return standings.values().stream()
            .sorted(Comparator
                .comparing(TeamRecordAccumulator::winPct).reversed()
                .thenComparing(TeamRecordAccumulator::wins).reversed())
            .map(TeamRecordAccumulator::toDto)
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
        List<BatterStats> statsList = batterStatsRepository.findBySeasonIdWithTeamPlayer(query.seasonId());
        Comparator<BatterStats> comparator = resolveBatterComparator(query.sortKey(), query.sortOrder());
        List<BatterStats> filtered = new ArrayList<>();
        for (BatterStats stats : statsList) {
            if (!isValidBatter(stats)) {
                continue;
            }
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (query.limit() > 0 && filtered.size() > query.limit()) {
            filtered = filtered.subList(0, query.limit());
        }
        List<BatterRecord> ranked = new ArrayList<>();
        for (BatterStats stats : filtered) {
            ranked.add(toBatterRecord(stats, ranked.size() + 1, query));
        }
        return ranked;
    }

    public List<PitcherRecord> getTopPitchers(RankingQuery query) {
        requireSeason(query.seasonId());
        List<PitcherStats> statsList = pitcherStatsRepository.findBySeasonIdWithTeamPlayer(query.seasonId());
        Comparator<PitcherStats> comparator = resolvePitcherComparator(query.sortKey(), query.sortOrder());
        List<PitcherStats> filtered = new ArrayList<>();
        for (PitcherStats stats : statsList) {
            if (!isValidPitcher(stats)) {
                continue;
            }
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (query.limit() > 0 && filtered.size() > query.limit()) {
            filtered = filtered.subList(0, query.limit());
        }
        List<PitcherRecord> ranked = new ArrayList<>();
        for (PitcherStats stats : filtered) {
            ranked.add(toPitcherRecord(stats, ranked.size() + 1, query));
        }
        return ranked;
    }

    private void requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
    }

    private Comparator<BatterStats> resolveBatterComparator(String sortKey, String sortOrder) {
        String key = sortKey == null ? "battingAverage" : sortKey;
        Comparator<BatterStats> base = switch (key) {
            case "hits" -> Comparator.comparing(BatterStats::getHits, Comparator.nullsLast(Integer::compareTo));
            case "homeRuns" -> Comparator.comparing(BatterStats::getHomeRuns, Comparator.nullsLast(Integer::compareTo));
            case "rbi" -> Comparator.comparing(BatterStats::getRunsBattedIn, Comparator.nullsLast(Integer::compareTo));
            case "ops" -> Comparator.comparing(BatterStats::getOps, Comparator.nullsLast(BigDecimal::compareTo));
            case "sluggingPct" -> Comparator.comparing(BatterStats::getSluggingPct, Comparator.nullsLast(BigDecimal::compareTo));
            case "onBasePct" -> Comparator.comparing(BatterStats::getOnBasePct, Comparator.nullsLast(BigDecimal::compareTo));
            case "battingAverage" -> Comparator.comparing(BatterStats::getBattingAverage, Comparator.nullsLast(BigDecimal::compareTo));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid batter sort");
        };
        return applyOrder(base, sortOrder, key.equals("battingAverage") || key.equals("ops") || key.equals("sluggingPct") || key.equals("onBasePct") ? "desc" : null);
    }

    private Comparator<PitcherStats> resolvePitcherComparator(String sortKey, String sortOrder) {
        String key = sortKey == null ? "era" : sortKey;
        Comparator<PitcherStats> base = switch (key) {
            case "era" -> Comparator.comparing(PitcherStats::getEra, Comparator.nullsLast(BigDecimal::compareTo));
            case "whip" -> Comparator.comparing(PitcherStats::getWhip, Comparator.nullsLast(BigDecimal::compareTo));
            case "strikeouts" -> Comparator.comparing(PitcherStats::getStrikeouts, Comparator.nullsLast(Integer::compareTo));
            case "wins" -> Comparator.comparing(PitcherStats::getWins, Comparator.nullsLast(Integer::compareTo));
            case "saves" -> Comparator.comparing(PitcherStats::getSaves, Comparator.nullsLast(Integer::compareTo));
            case "inningsPitched" -> Comparator.comparing(PitcherStats::getInningsPitched, Comparator.nullsLast(BigDecimal::compareTo));
            case "walksAllowed" -> Comparator.comparing(PitcherStats::getWalksAllowed, Comparator.nullsLast(Integer::compareTo));
            case "gamesPlayed" -> Comparator.comparing(PitcherStats::getGamesPlayed, Comparator.nullsLast(Integer::compareTo));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid pitcher sort");
        };
        // ERA/WHIP는 기본 asc, 나머지는 기본 desc
        String defaultOrder = (key.equals("era") || key.equals("whip")) ? "asc" : "desc";
        return applyOrder(base, sortOrder, defaultOrder);
    }

    private <T> Comparator<T> applyOrder(Comparator<T> base, String sortOrder, String defaultOrder) {
        String order = (sortOrder == null || sortOrder.isBlank()) ? defaultOrder : sortOrder;
        if (order == null || order.equalsIgnoreCase("desc")) {
            return base.reversed();
        }
        if (order.equalsIgnoreCase("asc")) {
            return base;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid sortOrder");
    }

    private BatterRecord toBatterRecord(BatterStats stats, int rank, RankingQuery query) {
        TeamPlayer tp = stats.getTeamPlayer();
        String partCode = query.partCode() != null ? query.partCode() : tp.getPartCode();
        String group = mapGroup(partCode, query.group());
        String scope = query.scope();
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

    private PitcherRecord toPitcherRecord(PitcherStats stats, int rank, RankingQuery query) {
        TeamPlayer tp = stats.getTeamPlayer();
        String partCode = query.partCode() != null ? query.partCode() : tp.getPartCode();
        String group = mapGroup(partCode, query.group());
        String scope = query.scope();
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

    private String mapGroup(String partCode, String requestedGroup) {
        if (requestedGroup != null && !requestedGroup.isBlank()) {
            return requestedGroup;
        }
        if (partCode == null) {
            return null;
        }
        return switch (partCode) {
            case "1" -> "A";
            case "2" -> "B";
            case "3" -> "C";
            case "4" -> "D";
            case "5" -> "E";
            case "6" -> "F";
            case "7" -> "G";
            case "8" -> "H";
            default -> null;
        };
    }

    public record RankingQuery(
        Long seasonId,
        int limit,
        String sortKey,
        String sortOrder,
        String scope,
        String group,
        String partCode,
        String playoffDivision,
        String regulation
    ) {
    }

    private boolean isValidBatter(BatterStats stats) {
        if (stats == null || stats.getTeamPlayer() == null || stats.getTeamPlayer().getPlayer() == null || stats.getTeamPlayer().getTeam() == null) {
            return false;
        }
        return stats.getBattingAverage() != null
            && stats.getHits() != null
            && stats.getHomeRuns() != null
            && stats.getRunsBattedIn() != null;
    }

    private boolean isValidPitcher(PitcherStats stats) {
        if (stats == null || stats.getTeamPlayer() == null || stats.getTeamPlayer().getPlayer() == null || stats.getTeamPlayer().getTeam() == null) {
            return false;
        }
        return stats.getEra() != null
            && stats.getInningsPitched() != null;
    }

    private void accumulate(
        Map<Long, TeamRecordAccumulator> map,
        Team team,
        Integer teamScore,
        Team opponent,
        Integer opponentScore
    ) {
        if (team == null || teamScore == null || opponentScore == null) {
            return;
        }
        TeamRecordAccumulator acc = map.computeIfAbsent(team.getId(), id -> new TeamRecordAccumulator(team));
        acc.games += 1;
        if (teamScore > opponentScore) {
            acc.wins += 1;
        } else if (teamScore.equals(opponentScore)) {
            acc.ties += 1;
        } else {
            acc.losses += 1;
        }
    }

    private static class TeamRecordAccumulator {
        private final Team team;
        private int games;
        private int wins;
        private int losses;
        private int ties;

        TeamRecordAccumulator(Team team) {
            this.team = team;
        }

        BigDecimal winPct() {
            if (games == 0) {
                return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(wins + (ties * 0.5))
                .divide(BigDecimal.valueOf(games), 3, RoundingMode.HALF_UP);
        }

        int wins() {
            return wins;
        }

        TeamRecord toDto() {
            return new TeamRecord(
                team.getId(),
                team.getTeamName(),
                wins,
                losses,
                ties,
                winPct()
            );
        }
    }
}
