package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.api.dto.SeasonResponse;
import com.aubl.webpage.api.dto.SeasonTeamResponse;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public SeasonService(SeasonRepository seasonRepository, TeamPlayerRepository teamPlayerRepository) {
        this.seasonRepository = seasonRepository;
        this.teamPlayerRepository = teamPlayerRepository;
    }

    @Transactional
    public Season createSeason(SeasonCreateRequest request) {
        Season season = new Season();
        season.setYear(request.year());
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> getSeasons() {
        return seasonRepository.findAllByOrderByYearAsc().stream()
            .map(s -> new SeasonResponse(s.getId(), s.getYear()))
            .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<SeasonTeamResponse> getSeasonTeams(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found"));
        java.util.Map<Long, SeasonTeamResponse> byTeam = new java.util.LinkedHashMap<>();
        teamPlayerRepository.findDistinctBySeasonId(season.getId()).forEach(tp -> {
            byTeam.putIfAbsent(tp.getTeam().getId(), new SeasonTeamResponse(
                season.getId(),
                tp.getTeam().getId(),
                tp.getTeam().getTeamName(),
                tp.getTeam().getTeamCode()
            ));
        });
        return byTeam.values().stream().toList();
    }
}
