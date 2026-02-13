package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.TeamCreateRequest;
import com.aubl.webpage.api.dto.TeamSummary;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.service.TeamService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/teams", "/api/team"})
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createTeam(@RequestBody TeamCreateRequest request) {
        Team team = teamService.createTeam(request);
        return ResponseEntity.ok(new IdResponse(team.getId()));
    }

    @GetMapping
    public ResponseEntity<List<TeamSummary>> getTeams() {
        List<TeamSummary> teams = teamService.getTeams().stream()
            .map(team -> new TeamSummary(team.getId(), team.getTeamName(), team.getTeamCode()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(teams);
    }
}
