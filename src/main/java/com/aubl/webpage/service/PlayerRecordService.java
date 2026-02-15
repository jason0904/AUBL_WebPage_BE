package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.BatterGameLogSummary;
import com.aubl.webpage.api.dto.BatterStatSummary;
import com.aubl.webpage.api.dto.PitcherGameLogSummary;
import com.aubl.webpage.api.dto.PitcherStatSummary;
import com.aubl.webpage.api.dto.PlayerGameLogsResponse;
import com.aubl.webpage.api.dto.PlayerStatsResponse;
import com.aubl.webpage.domain.entity.BatterGameLog;
import com.aubl.webpage.domain.entity.BatterStats;
import com.aubl.webpage.domain.entity.PitcherGameLog;
import com.aubl.webpage.domain.entity.PitcherStats;
import com.aubl.webpage.domain.entity.Player;
import com.aubl.webpage.domain.repository.BatterGameLogRepository;
import com.aubl.webpage.domain.repository.BatterStatsRepository;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.PlayerRepository;
import com.aubl.webpage.domain.repository.PitcherGameLogRepository;
import com.aubl.webpage.domain.repository.PitcherStatsRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerRecordService {

    private final BatterStatsRepository batterStatsRepository;
    private final PitcherStatsRepository pitcherStatsRepository;
    private final BatterGameLogRepository batterGameLogRepository;
    private final PitcherGameLogRepository pitcherGameLogRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;

    public PlayerRecordService(
        BatterStatsRepository batterStatsRepository,
        PitcherStatsRepository pitcherStatsRepository,
        BatterGameLogRepository batterGameLogRepository,
        PitcherGameLogRepository pitcherGameLogRepository,
        PlayerRepository playerRepository,
        SeasonRepository seasonRepository,
        GameRepository gameRepository
    ) {
        this.batterStatsRepository = batterStatsRepository;
        this.pitcherStatsRepository = pitcherStatsRepository;
        this.batterGameLogRepository = batterGameLogRepository;
        this.pitcherGameLogRepository = pitcherGameLogRepository;
        this.playerRepository = playerRepository;
        this.seasonRepository = seasonRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse getPlayerStats(Long playerId, Long seasonId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "player not found"));
        ensureSeasonExists(seasonId);
        List<BatterStats> batterStats = seasonId == null
            ? batterStatsRepository.findByTeamPlayerPlayerId(playerId)
            : batterStatsRepository.findByTeamPlayerPlayerIdAndSeasonId(playerId, seasonId);

        List<PitcherStats> pitcherStats = seasonId == null
            ? pitcherStatsRepository.findByTeamPlayerPlayerId(playerId)
            : pitcherStatsRepository.findByTeamPlayerPlayerIdAndSeasonId(playerId, seasonId);

        String playerName = resolvePlayerName(player, batterStats, pitcherStats);
        String teamName = resolveTeamName(batterStats, pitcherStats);
        Integer jerseyNumber = resolveJerseyNumber(batterStats, pitcherStats);

        return new PlayerStatsResponse(
            playerName,
            teamName,
            jerseyNumber,
            batterStats.stream().map(this::toBatterStatSummary).toList(),
            pitcherStats.stream().map(this::toPitcherStatSummary).toList()
        );
    }

    @Transactional(readOnly = true)
    public PlayerGameLogsResponse getPlayerGameLogs(Long playerId, Long gameId) {
        ensurePlayerExists(playerId);
        ensureGameExists(gameId);
        List<BatterGameLog> batterLogs = gameId == null
            ? batterGameLogRepository.findByPlayerId(playerId)
            : batterGameLogRepository.findByPlayerIdAndGameId(playerId, gameId);

        List<PitcherGameLog> pitcherLogs = gameId == null
            ? pitcherGameLogRepository.findByPlayerId(playerId)
            : pitcherGameLogRepository.findByPlayerIdAndGameId(playerId, gameId);

        return new PlayerGameLogsResponse(
            batterLogs.stream().map(this::toBatterGameLogSummary).toList(),
            pitcherLogs.stream().map(this::toPitcherGameLogSummary).toList()
        );
    }

    private void ensurePlayerExists(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "player not found");
        }
    }

    private void ensureSeasonExists(Long seasonId) {
        if (seasonId == null) {
            return;
        }
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found");
        }
    }

    private void ensureGameExists(Long gameId) {
        if (gameId == null) {
            return;
        }
        if (!gameRepository.existsById(gameId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "game not found");
        }
    }

    private BatterStatSummary toBatterStatSummary(BatterStats stats) {
        return new BatterStatSummary(
            stats.getId(),
            stats.getTeamPlayer().getId(),
            stats.getSeason().getId(),
            stats.getSeasonType(),
            stats.getGamesPlayed(),
            stats.getPlateAppearance(),
            stats.getAtBats(),
            stats.getHits(),
            stats.getHomeRuns(),
            stats.getBattingAverage(),
            stats.getOnBasePct(),
            stats.getSluggingPct(),
            stats.getOps(),
            stats.getTeamPlayer().getJerseyNumber()
        );
    }

    private PitcherStatSummary toPitcherStatSummary(PitcherStats stats) {
        return new PitcherStatSummary(
            stats.getId(),
            stats.getTeamPlayer().getId(),
            stats.getSeason().getId(),
            stats.getSeasonType(),
            stats.getGamesPlayed(),
            stats.getGamesStarted(),
            stats.getInningsPitched(),
            stats.getWins(),
            stats.getLosses(),
            stats.getSaves(),
            stats.getEra(),
            stats.getWhip(),
            stats.getKPer9(),
            stats.getBbPer9(),
            stats.getTeamPlayer().getJerseyNumber()
        );
    }

    private BatterGameLogSummary toBatterGameLogSummary(BatterGameLog log) {
        Long playerId = log.getPlayer() == null ? null : log.getPlayer().getId();
        Integer jerseyNumber = log.getBatterStats() == null || log.getBatterStats().getTeamPlayer() == null
            ? null
            : log.getBatterStats().getTeamPlayer().getJerseyNumber();
        return new BatterGameLogSummary(
            log.getId(),
            log.getGame().getId(),
            log.getTeam().getId(),
            log.getTeamSide(),
            playerId,
            log.getPlayerName(),
            log.getPlayerPosition(),
            log.getAtBats(),
            log.getRuns(),
            log.getHits(),
            log.getRbi(),
            log.getWalks(),
            log.getStrikeouts(),
            jerseyNumber
        );
    }

    private PitcherGameLogSummary toPitcherGameLogSummary(PitcherGameLog log) {
        Long playerId = log.getPlayer() == null ? null : log.getPlayer().getId();
        Integer jerseyNumber = log.getPitcherStats() == null || log.getPitcherStats().getTeamPlayer() == null
            ? null
            : log.getPitcherStats().getTeamPlayer().getJerseyNumber();
        return new PitcherGameLogSummary(
            log.getId(),
            log.getGame().getId(),
            log.getTeam().getId(),
            log.getTeamSide(),
            playerId,
            log.getPlayerName(),
            log.getPlayerPosition(),
            log.getInningsPitched(),
            log.getHitsAllowed(),
            log.getRunsAllowed(),
            log.getEarnedRuns(),
            log.getWalks(),
            log.getStrikeouts(),
            jerseyNumber
        );
    }

    private String resolvePlayerName(Player player, List<BatterStats> batterStats, List<PitcherStats> pitcherStats) {
        if (player != null && player.getPlayerName() != null) {
            return player.getPlayerName();
        }
        if (batterStats != null) {
            for (BatterStats bs : batterStats) {
                if (bs != null && bs.getTeamPlayer() != null && bs.getTeamPlayer().getPlayer() != null) {
                    String name = bs.getTeamPlayer().getPlayer().getPlayerName();
                    if (name != null) {
                        return name;
                    }
                }
            }
        }
        if (pitcherStats != null) {
            for (PitcherStats ps : pitcherStats) {
                if (ps != null && ps.getTeamPlayer() != null && ps.getTeamPlayer().getPlayer() != null) {
                    String name = ps.getTeamPlayer().getPlayer().getPlayerName();
                    if (name != null) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    private String resolveTeamName(List<BatterStats> batterStats, List<PitcherStats> pitcherStats) {
        if (batterStats != null) {
            for (BatterStats bs : batterStats) {
                if (bs != null && bs.getTeamPlayer() != null && bs.getTeamPlayer().getTeam() != null) {
                    return bs.getTeamPlayer().getTeam().getTeamName();
                }
            }
        }
        if (pitcherStats != null) {
            for (PitcherStats ps : pitcherStats) {
                if (ps != null && ps.getTeamPlayer() != null && ps.getTeamPlayer().getTeam() != null) {
                    return ps.getTeamPlayer().getTeam().getTeamName();
                }
            }
        }
        return null;
    }

    private Integer resolveJerseyNumber(List<BatterStats> batterStats, List<PitcherStats> pitcherStats) {
        if (batterStats != null) {
            for (BatterStats bs : batterStats) {
                if (bs != null && bs.getTeamPlayer() != null && bs.getTeamPlayer().getJerseyNumber() != null) {
                    return bs.getTeamPlayer().getJerseyNumber();
                }
            }
        }
        if (pitcherStats != null) {
            for (PitcherStats ps : pitcherStats) {
                if (ps != null && ps.getTeamPlayer() != null && ps.getTeamPlayer().getJerseyNumber() != null) {
                    return ps.getTeamPlayer().getJerseyNumber();
                }
            }
        }
        return null;
    }
}
