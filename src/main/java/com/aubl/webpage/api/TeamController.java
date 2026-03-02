package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.TeamCreateRequest;
import com.aubl.webpage.api.dto.TeamSummary;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.service.TeamService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/api/teams")
    public ResponseEntity<List<TeamSummary>> getTeams() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    @PostMapping("/api/teams")
    public ResponseEntity<IdResponse> createTeam(@RequestBody TeamCreateRequest request) {
        Team team = teamService.createTeam(request);
        return ResponseEntity.ok(new IdResponse(team.getId()));
    }

    @PatchMapping("/api/admin/teams/{teamId}/active")
    public ResponseEntity<Void> updateTeamActive(
            @PathVariable Long teamId,
            @RequestParam boolean active) {
        teamService.updateTeamActive(teamId, active);
        return ResponseEntity.noContent().build();
    }
}
