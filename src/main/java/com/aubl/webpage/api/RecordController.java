package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.RecordDtos.OverviewResponse;
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
    public ResponseEntity<OverviewResponse> getOverview(@RequestParam Long seasonId) {
        return ResponseEntity.ok(recordService.getOverview(seasonId));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamRecord>> getTeamRecords(
        @RequestParam Long seasonId,
        @RequestParam(required = false) String division
    ) {
        return ResponseEntity.ok(recordService.getTeamRecords(seasonId));
    }
}

