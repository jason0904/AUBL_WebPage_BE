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
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
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
        List<Game> games = gameRepository.findBySeasonId(seasonId);
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
        List<Game> games = gameRepository.findBySeasonId(seasonId);
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
        Sort sort = resolveBatterSort(sortKey);
        if (limit <= 0) {
            return batterStatsRepository.findBySeasonIdAndSeasonTypeIsNull(seasonId, sort).stream()
                .map(this::toBatterRecord)
                .toList();
        }
        int size = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, sort);
        return batterStatsRepository.findBySeasonIdAndSeasonTypeIsNull(seasonId, pageable)
            .map(this::toBatterRecord)
            .getContent();
    }

    public List<PitcherRecord> getTopPitchers(Long seasonId, int limit, String sortKey) {
        requireSeason(seasonId);
        Sort sort = resolvePitcherSort(sortKey);
        if (limit <= 0) {
            return pitcherStatsRepository.findBySeasonIdAndSeasonTypeIsNull(seasonId, sort).stream()
                .map(this::toPitcherRecord)
                .toList();
        }
        int size = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, size, sort);
        return pitcherStatsRepository.findBySeasonIdAndSeasonTypeIsNull(seasonId, pageable)
            .map(this::toPitcherRecord)
            .getContent();
    }

    private void requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
    }

    private Sort resolveBatterSort(String sortKey) {
        String key = sortKey == null ? "battingAverage" : sortKey;
        return switch (key) {
            case "hits" -> Sort.by(Sort.Direction.DESC, "hits");
            case "homeRuns" -> Sort.by(Sort.Direction.DESC, "homeRuns");
            case "rbi" -> Sort.by(Sort.Direction.DESC, "runsBattedIn");
            case "ops" -> Sort.by(Sort.Direction.DESC, "ops");
            case "sluggingPct" -> Sort.by(Sort.Direction.DESC, "sluggingPct");
            case "onBasePct" -> Sort.by(Sort.Direction.DESC, "onBasePct");
            case "battingAverage" -> Sort.by(Sort.Direction.DESC, "battingAverage");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid batter sort");
        };
    }

    private Sort resolvePitcherSort(String sortKey) {
        String key = sortKey == null ? "era" : sortKey;
        return switch (key) {
            case "era" -> Sort.by(Sort.Direction.ASC, "era");
            case "whip" -> Sort.by(Sort.Direction.ASC, "whip");
            case "strikeouts" -> Sort.by(Sort.Direction.DESC, "strikeouts");
            case "wins" -> Sort.by(Sort.Direction.DESC, "wins");
            case "saves" -> Sort.by(Sort.Direction.DESC, "saves");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid pitcher sort");
        };
    }

    private BatterRecord toBatterRecord(BatterStats stats) {
        TeamPlayer tp = stats.getTeamPlayer();
        return new BatterRecord(
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
            stats.getBattingAverage(),
            stats.getOnBasePct(),
            stats.getSluggingPct(),
            stats.getOps()
        );
    }

    private PitcherRecord toPitcherRecord(PitcherStats stats) {
        TeamPlayer tp = stats.getTeamPlayer();
        return new PitcherRecord(
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
