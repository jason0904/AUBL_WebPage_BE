package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.api.dto.SeasonResponse;
import com.aubl.webpage.api.dto.SeasonTeamResponse;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.service.SeasonService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    public ResponseEntity<List<SeasonResponse>> getSeasons() {
        return ResponseEntity.ok(seasonService.getSeasons());
    }

    @PostMapping
    public ResponseEntity<IdResponse> createSeason(@RequestBody SeasonCreateRequest request) {
        Season season = seasonService.createSeason(request);
        return ResponseEntity.ok(new IdResponse(season.getId()));
    }

    @GetMapping("/{seasonId}/teams")
    public ResponseEntity<List<SeasonTeamResponse>> getSeasonTeams(@PathVariable Long seasonId) {
        return ResponseEntity.ok(seasonService.getSeasonTeams(seasonId));
    }
}
