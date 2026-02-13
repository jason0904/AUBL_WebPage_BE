package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.TeamCreateRequest;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
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
}
