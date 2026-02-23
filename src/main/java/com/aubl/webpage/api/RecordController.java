package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.PlayoffDtos.PlayoffGameRow;
import com.aubl.webpage.api.dto.PlayoffDtos.PlayoffTeamRow;
import com.aubl.webpage.api.dto.RecordDtos.OverviewResponse;
import com.aubl.webpage.api.dto.RecordDtos.TeamRecord;
import com.aubl.webpage.service.PlayoffService;
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
    private final PlayoffService playoffService;

    public RecordController(RecordService recordService, PlayoffService playoffService) {
        this.recordService = recordService;
        this.playoffService = playoffService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview(
        @RequestParam Long seasonId,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String partCode,
        @RequestParam(required = false) String playoffDivision,
        @RequestParam(required = false) String division
    ) {
        String resolvedDiv = playoffDivision != null ? playoffDivision : division;
        return ResponseEntity.ok(recordService.getOverview(seasonId, scope, group, partCode, resolvedDiv));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamRecord>> getTeamRecords(
        @RequestParam Long seasonId,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String partCode,
        @RequestParam(required = false) String playoffDivision,
        @RequestParam(required = false) String division
    ) {
        String resolvedDiv = playoffDivision != null ? playoffDivision : division;
        return ResponseEntity.ok(recordService.getTeamRecords(seasonId, scope, group, partCode, resolvedDiv));
    }

    /**
     * GET /api/records/playoffs?seasonId=&view=games|teams&tier=ALL|EUTTEUM|BEOGEUM
     *
     * view=games  (기본) → 포스트시즌 경기 목록
     * view=teams       → 포스트시즌 참가팀 승/패 집계
     * tier=ALL    (기본) → 으뜸+버금 전체
     * tier=EUTTEUM      → 으뜸만
     * tier=BEOGEUM      → 버금만
     */
    @GetMapping("/playoffs")
    public ResponseEntity<?> getPlayoffs(
        @RequestParam Long seasonId,
        @RequestParam(required = false, defaultValue = "games") String view,
        @RequestParam(required = false, defaultValue = "ALL") String tier
    ) {
        if ("teams".equalsIgnoreCase(view)) {
            return ResponseEntity.ok(playoffService.getPlayoffTeams(seasonId, tier));
        }
        return ResponseEntity.ok(playoffService.getPlayoffGames(seasonId, tier));
    }
}


