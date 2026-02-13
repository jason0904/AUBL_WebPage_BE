package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.SeasonCreateRequest;
import com.aubl.webpage.api.dto.SeasonSummary;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.service.SeasonService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    public ResponseEntity<IdResponse> createSeason(@RequestBody SeasonCreateRequest request) {
        Season season = seasonService.createSeason(request);
        return ResponseEntity.ok(new IdResponse(season.getId()));
    }

    @GetMapping
    public ResponseEntity<List<SeasonSummary>> getSeasons() {
        List<SeasonSummary> seasons = seasonService.getSeasons().stream()
            .map(season -> new SeasonSummary(season.getId(), season.getYear()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(seasons);
    }
}
