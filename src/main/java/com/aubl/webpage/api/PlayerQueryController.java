package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.PlayerProfileResponse;
import com.aubl.webpage.api.dto.PlayerSearchResult;
import com.aubl.webpage.api.dto.RosterResponse;
import com.aubl.webpage.service.PlayerQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerQueryController {

    private final PlayerQueryService playerQueryService;

    public PlayerQueryController(PlayerQueryService playerQueryService) {
        this.playerQueryService = playerQueryService;
    }

    @GetMapping("/roster")
    public ResponseEntity<RosterResponse> getRoster(
        @RequestParam Long seasonId,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(playerQueryService.getRoster(seasonId, teamId, q, limit, cursor));
    }

    @GetMapping("/{playerId}/profile")
    public ResponseEntity<PlayerProfileResponse> getProfile(
        @PathVariable Long playerId,
        @RequestParam(required = false) Long seasonId
    ) {
        return ResponseEntity.ok(playerQueryService.getProfile(playerId, seasonId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchResult>> search(
        @RequestParam Long seasonId,
        @RequestParam String q,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(playerQueryService.searchPlayers(seasonId, q, teamId, limit));
    }
}

