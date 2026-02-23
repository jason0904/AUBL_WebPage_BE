package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.RecordDtos.BatterRecord;
import com.aubl.webpage.api.dto.RecordDtos.PitcherRecord;
import com.aubl.webpage.service.RecordService;
import com.aubl.webpage.service.RecordService.RankingQuery;
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
        @RequestParam(required = true) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String partCode,
        @RequestParam(required = false) String playoffDivision,
        @RequestParam(required = false) String division,
        @RequestParam(required = false) String regulation
    ) {
        String resolvedPlayoffDivision = playoffDivision != null ? playoffDivision : division;
        RankingQuery query = new RankingQuery(seasonId, limit, sort, sortOrder, scope, group, partCode, resolvedPlayoffDivision, regulation);
        return ResponseEntity.ok(recordService.getTopBatters(query));
    }

    @GetMapping("/pitchers")
    public ResponseEntity<List<PitcherRecord>> getPitcherRankings(
        @RequestParam(required = true) Long seasonId,
        @RequestParam(defaultValue = "0") int limit,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String partCode,
        @RequestParam(required = false) String playoffDivision,
        @RequestParam(required = false) String division,
        @RequestParam(required = false) String regulation
    ) {
        String resolvedPlayoffDivision = playoffDivision != null ? playoffDivision : division;
        RankingQuery query = new RankingQuery(seasonId, limit, sort, sortOrder, scope, group, partCode, resolvedPlayoffDivision, regulation);
        return ResponseEntity.ok(recordService.getTopPitchers(query));
    }
}
