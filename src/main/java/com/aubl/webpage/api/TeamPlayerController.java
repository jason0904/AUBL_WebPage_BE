package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.TeamPlayerCreateRequest;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.service.TeamPlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
