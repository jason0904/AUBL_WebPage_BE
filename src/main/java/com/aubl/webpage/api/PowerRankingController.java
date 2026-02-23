package com.aubl.webpage.api;

import com.aubl.webpage.service.PowerRankingService;
import com.aubl.webpage.service.PowerRankingService.PowerRankingRow;
import com.aubl.webpage.service.PowerRankingService.RebuildResult;
import com.aubl.webpage.service.PowerRankingService.SeasonScoreRow;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PowerRankingController {

    private final PowerRankingService powerRankingService;

    public PowerRankingController(PowerRankingService powerRankingService) {
        this.powerRankingService = powerRankingService;
    }

    @GetMapping("/api/records/power-ranking")
    public ResponseEntity<List<PowerRankingRow>> getPowerRanking(
        @RequestParam Integer rankingYear,
        @RequestParam(required = false, defaultValue = "0") int limit
    ) {
        return ResponseEntity.ok(
            powerRankingService.getPowerRanking(rankingYear, limit)
        );
    }

    @GetMapping("/api/records/power-ranking/season-scores")
    public ResponseEntity<List<SeasonScoreRow>> getSeasonScores(
        @RequestParam Long teamId,
        @RequestParam(required = false) Integer fromYear,
        @RequestParam(required = false) Integer toYear
    ) {
        return ResponseEntity.ok(
            powerRankingService.getSeasonScores(teamId, fromYear, toYear)
        );
    }

    @PostMapping("/api/admin/records/power-ranking/rebuild")
    public ResponseEntity<RebuildResult> rebuild(
        @RequestParam(required = false) Integer fromYear,
        @RequestParam(required = false) Integer toYear
    ) {
        return ResponseEntity.ok(
            powerRankingService.rebuild(fromYear, toYear)
        );
    }
}

