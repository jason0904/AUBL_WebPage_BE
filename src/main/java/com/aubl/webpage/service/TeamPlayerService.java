package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.TeamPlayerCreateRequest;
import com.aubl.webpage.api.dto.TeamPlayerSummary;
import com.aubl.webpage.domain.entity.Player;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.domain.repository.PlayerRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import com.aubl.webpage.domain.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TeamPlayerService {

    private final TeamPlayerRepository teamPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;

    public TeamPlayerService(
        TeamPlayerRepository teamPlayerRepository,
        TeamRepository teamRepository,
        PlayerRepository playerRepository,
        SeasonRepository seasonRepository
    ) {
        this.teamPlayerRepository = teamPlayerRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public TeamPlayer createTeamPlayer(TeamPlayerCreateRequest request) {
        TeamPlayer teamPlayer = new TeamPlayer();
        teamPlayer.setTeam(resolveTeam(request.teamId()));
        teamPlayer.setPlayer(resolvePlayer(request.playerId()));
        teamPlayer.setSeason(resolveSeason(request.seasonId()));
        teamPlayer.setJerseyNumber(request.jerseyNumber());
        return teamPlayerRepository.save(teamPlayer);
    }

    private Team resolveTeam(Long teamId) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId is required");
        }
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found"));
    }

    private Player resolvePlayer(Long playerId) {
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId is required");
        }
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "player not found"));
    }

    private Season resolveSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found"));
    }

    @Transactional(readOnly = true)
    public List<TeamPlayerSummary> getTeamPlayers(Long teamId, Long seasonId) {
        if (teamId == null || seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "teamId and seasonId are required");
        }
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found");
        }
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found");
        }
        return teamPlayerRepository.findByTeamIdAndSeasonId(teamId, seasonId).stream()
            .map(tp -> new TeamPlayerSummary(
                tp.getId(),
                tp.getTeam().getId(),
                tp.getTeam().getTeamName(),
                tp.getSeason().getId(),
                tp.getPlayer().getId(),
                tp.getPlayer().getPlayerName(),
                tp.getJerseyNumber(),
                tp.getPlayer().getPosition()
            ))
            .toList();
    }
}
