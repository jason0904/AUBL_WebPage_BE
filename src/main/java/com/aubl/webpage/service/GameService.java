package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.GameCreateRequest;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;

    public GameService(
        GameRepository gameRepository,
        SeasonRepository seasonRepository,
        TeamRepository teamRepository
    ) {
        this.gameRepository = gameRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Game createGame(GameCreateRequest request) {
        Game game = new Game();
        game.setSeason(resolveSeason(request.seasonId()));
        game.setGameDate(request.gameDate());
        game.setGameNumber(request.gameNumber());
        game.setHomeTeam(resolveTeam(request.homeTeamId(), "home team"));
        game.setAwayTeam(resolveTeam(request.awayTeamId(), "away team"));
        game.setHomeScore(request.homeScore());
        game.setAwayScore(request.awayScore());
        game.setGameType(request.gameType());
        game.setCsvFilePath(request.csvFilePath());
        return gameRepository.save(game);
    }

    private Season resolveSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found"));
    }

    private Team resolveTeam(Long teamId, String label) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " id is required");
        }
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, label + " not found"));
    }
}
