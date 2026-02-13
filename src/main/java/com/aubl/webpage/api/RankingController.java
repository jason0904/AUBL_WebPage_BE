package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.RecordDtos.BatterRecord;
import com.aubl.webpage.api.dto.RecordDtos.PitcherRecord;
import com.aubl.webpage.service.RecordService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RecordService recordService;

    public RankingController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/batters")
    public ResponseEntity<List<BatterRecord>> getBatterRankings(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(recordService.getTopBatters(seasonId, limit, sort));
    }

    @GetMapping("/pitchers")
    public ResponseEntity<List<PitcherRecord>> getPitcherRankings(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(recordService.getTopPitchers(seasonId, limit, sort));
    }
}
