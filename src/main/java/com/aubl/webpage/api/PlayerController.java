package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.PlayerCreateRequest;
import com.aubl.webpage.domain.entity.Player;
import com.aubl.webpage.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createPlayer(@RequestBody PlayerCreateRequest request) {
        Player player = playerService.createPlayer(request);
        return ResponseEntity.ok(new IdResponse(player.getId()));
    }
}
