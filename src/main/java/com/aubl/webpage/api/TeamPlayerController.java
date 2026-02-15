package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.TeamPlayerCreateRequest;
import com.aubl.webpage.api.dto.TeamPlayerSummary;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.service.TeamPlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/team-players")
public class TeamPlayerController {

    private final TeamPlayerService teamPlayerService;

    public TeamPlayerController(TeamPlayerService teamPlayerService) {
        this.teamPlayerService = teamPlayerService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createTeamPlayer(@RequestBody TeamPlayerCreateRequest request) {
        TeamPlayer teamPlayer = teamPlayerService.createTeamPlayer(request);
        return ResponseEntity.ok(new IdResponse(teamPlayer.getId()));
    }

    @GetMapping
    public ResponseEntity<List<TeamPlayerSummary>> getTeamPlayers(
        @RequestParam Long teamId,
        @RequestParam Long seasonId
    ) {
        return ResponseEntity.ok(teamPlayerService.getTeamPlayers(teamId, seasonId));
    }
}
