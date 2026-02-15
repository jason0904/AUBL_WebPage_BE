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

        BatterRecord topBatter = getTopBatters(seasonId, 1, "ops").stream().findFirst().orElse(null);
        PitcherRecord topPitcher = getTopPitchers(seasonId, 1, "era").stream().findFirst().orElse(null);

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
        requireSeason(seasonId);
        List<BatterStats> statsList = batterStatsRepository.findBySeasonIdWithTeamPlayer(seasonId);
        Comparator<BatterStats> comparator = resolveBatterComparator(sortKey);

        List<BatterStats> filtered = new ArrayList<>();
        for (BatterStats stats : statsList) {
            if (!isValidBatter(stats)) {
                continue; // skip null metrics/associations
            }
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (limit > 0 && filtered.size() > limit) {
            filtered = filtered.subList(0, limit);
        }

        List<BatterRecord> ranked = new ArrayList<>();
        for (BatterStats stats : filtered) {
            ranked.add(toBatterRecord(stats, ranked.size() + 1));
        }
        return ranked;
    }

    public List<PitcherRecord> getTopPitchers(Long seasonId, int limit, String sortKey) {
        requireSeason(seasonId);
        List<PitcherStats> statsList = pitcherStatsRepository.findBySeasonIdWithTeamPlayer(seasonId);
        Comparator<PitcherStats> comparator = resolvePitcherComparator(sortKey);

        List<PitcherStats> filtered = new ArrayList<>();
        for (PitcherStats stats : statsList) {
            if (!isValidPitcher(stats)) {
                continue; // skip null metrics/associations
            }
            filtered.add(stats);
        }
        filtered.sort(comparator);
        if (limit > 0 && filtered.size() > limit) {
            filtered = filtered.subList(0, limit);
        }

        List<PitcherRecord> ranked = new ArrayList<>();
        for (PitcherStats stats : filtered) {
            ranked.add(toPitcherRecord(stats, ranked.size() + 1));
        }
        return ranked;
    }

    private void requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
    }

    private Comparator<BatterStats> resolveBatterComparator(String sortKey) {
        String key = sortKey == null ? "battingAverage" : sortKey;
        return switch (key) {
            case "hits" -> Comparator.comparing(BatterStats::getHits, Comparator.nullsLast(Integer::compareTo)).reversed();
            case "homeRuns" -> Comparator.comparing(BatterStats::getHomeRuns, Comparator.nullsLast(Integer::compareTo)).reversed();
            case "rbi" -> Comparator.comparing(BatterStats::getRunsBattedIn, Comparator.nullsLast(Integer::compareTo)).reversed();
            case "ops" -> Comparator.comparing(BatterStats::getOps, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            case "sluggingPct" -> Comparator.comparing(BatterStats::getSluggingPct, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            case "onBasePct" -> Comparator.comparing(BatterStats::getOnBasePct, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            case "battingAverage" -> Comparator.comparing(BatterStats::getBattingAverage, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid batter sort");
        };
    }

    private Comparator<PitcherStats> resolvePitcherComparator(String sortKey) {
        String key = sortKey == null ? "era" : sortKey;
        return switch (key) {
            case "era" -> Comparator.comparing(PitcherStats::getEra, Comparator.nullsLast(BigDecimal::compareTo)); // ascending
            case "whip" -> Comparator.comparing(PitcherStats::getWhip, Comparator.nullsLast(BigDecimal::compareTo)); // ascending
            case "strikeouts" -> Comparator.comparing(PitcherStats::getStrikeouts, Comparator.nullsLast(Integer::compareTo)).reversed();
            case "wins" -> Comparator.comparing(PitcherStats::getWins, Comparator.nullsLast(Integer::compareTo)).reversed();
            case "saves" -> Comparator.comparing(PitcherStats::getSaves, Comparator.nullsLast(Integer::compareTo)).reversed();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid pitcher sort");
        };
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

    private BatterRecord toBatterRecord(BatterStats stats, int rank) {
        TeamPlayer tp = stats.getTeamPlayer();
        return new BatterRecord(
            rank,
            tp.getPlayer().getId(),
            tp.getPlayer().getPlayerName(),
            tp.getTeam().getId(),
            tp.getTeam().getTeamName(),
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
            stats.getOps()
        );
    }

    private PitcherRecord toPitcherRecord(PitcherStats stats, int rank) {
        TeamPlayer tp = stats.getTeamPlayer();
        return new PitcherRecord(
            rank,
            tp.getPlayer().getId(),
            tp.getPlayer().getPlayerName(),
            tp.getTeam().getId(),
            tp.getTeam().getTeamName(),
            stats.getSeason().getId(),
            stats.getGamesPlayed(),
            stats.getInningsPitched(),
            stats.getWins(),
            stats.getLosses(),
            stats.getSaves(),
            stats.getStrikeouts(),
            stats.getWalksAllowed(),
            stats.getEra(),
            stats.getWhip()
        );
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
