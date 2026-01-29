package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.PlayerGameLogsResponse;
import com.aubl.webpage.api.dto.PlayerStatsResponse;
import com.aubl.webpage.service.PlayerRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerRecordController {

    private final PlayerRecordService playerRecordService;

    public PlayerRecordController(PlayerRecordService playerRecordService) {
        this.playerRecordService = playerRecordService;
    }

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(
        @PathVariable Long playerId,
        @RequestParam(required = false) Long seasonId
    ) {
        return ResponseEntity.ok(playerRecordService.getPlayerStats(playerId, seasonId));
    }

    @GetMapping("/{playerId}/game-logs")
    public ResponseEntity<PlayerGameLogsResponse> getPlayerGameLogs(
        @PathVariable Long playerId,
        @RequestParam(required = false) Long gameId
    ) {
        return ResponseEntity.ok(playerRecordService.getPlayerGameLogs(playerId, gameId));
    }
}
