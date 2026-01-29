package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.GameCreateRequest;
import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createGame(@RequestBody GameCreateRequest request) {
        Game game = gameService.createGame(request);
        return ResponseEntity.ok(new IdResponse(game.getId()));
    }
}
