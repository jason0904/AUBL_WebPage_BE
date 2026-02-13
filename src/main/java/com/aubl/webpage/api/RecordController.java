package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.RecordDtos.BatterRecord;
import com.aubl.webpage.api.dto.RecordDtos.OverviewResponse;
import com.aubl.webpage.api.dto.RecordDtos.PitcherRecord;
import com.aubl.webpage.api.dto.RecordDtos.TeamRecord;
import com.aubl.webpage.service.RecordService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview(@RequestParam(required = false) Long seasonId) {
        return ResponseEntity.ok(recordService.getOverview(seasonId));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamRecord>> getTeams(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(required = false) String division
    ) {
        // division currently unused; reserved for future filtering
        return ResponseEntity.ok(recordService.getTeamRecords(seasonId));
    }

    @GetMapping("/batters")
    public ResponseEntity<List<BatterRecord>> getBatters(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(recordService.getTopBatters(seasonId, limit, sort));
    }

    @GetMapping("/pitchers")
    public ResponseEntity<List<PitcherRecord>> getPitchers(
        @RequestParam(required = false) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(recordService.getTopPitchers(seasonId, limit, sort));
    }
}
