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
        ensurePlayerExists(playerId);
        ensureSeasonExists(seasonId);
        List<BatterStats> batterStats = seasonId == null
            ? batterStatsRepository.findByTeamPlayerPlayerId(playerId)
            : batterStatsRepository.findByTeamPlayerPlayerIdAndSeasonId(playerId, seasonId);

        List<PitcherStats> pitcherStats = seasonId == null
            ? pitcherStatsRepository.findByTeamPlayerPlayerId(playerId)
            : pitcherStatsRepository.findByTeamPlayerPlayerIdAndSeasonId(playerId, seasonId);

        return new PlayerStatsResponse(
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
            stats.getOps()
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
            stats.getBbPer9()
        );
    }

    private BatterGameLogSummary toBatterGameLogSummary(BatterGameLog log) {
        Long playerId = log.getPlayer() == null ? null : log.getPlayer().getId();
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
            log.getStrikeouts()
        );
    }

    private PitcherGameLogSummary toPitcherGameLogSummary(PitcherGameLog log) {
        Long playerId = log.getPlayer() == null ? null : log.getPlayer().getId();
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
            log.getStrikeouts()
        );
    }
}
