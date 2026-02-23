package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.RecordDtos.TeamRecord;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import com.aubl.webpage.service.RecordService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/records")
public class RecordsFilterController {

    private final TeamPlayerRepository teamPlayerRepository;
    private final RecordService recordService;

    public RecordsFilterController(TeamPlayerRepository teamPlayerRepository, RecordService recordService) {
        this.teamPlayerRepository = teamPlayerRepository;
        this.recordService = recordService;
    }

    /**
     * GET /api/records/filter-options?seasonId=
     * groups는 DB part_code 기반으로만 반환 (팀명순 금지, partCode ASC 고정)
     */
    @GetMapping("/filter-options")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions(@RequestParam Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        // DB에서 실제 사용된 part_code만 조회 (partCode ASC 정렬)
        List<String> partCodes = teamPlayerRepository.findDistinctPartCodesBySeasonId(seasonId);

        List<GroupOption> groups = partCodes.stream()
            .map(pc -> {
                String g = toGroup(pc);
                int order = Integer.parseInt(pc);
                return new GroupOption(pc, g, g + "조", order);
            })
            .toList();

        return ResponseEntity.ok(new FilterOptionsResponse(
            seasonId,
            groups,
            List.of("LEAGUE", "PLAYOFF"),
            List.of("EUTTEUM", "BEOGEUM"),
            List.of("IN", "OUT"),
            "IN",
            List.of("battingAverage", "hits", "homeRuns", "rbi", "ops",
                "sluggingPct", "onBasePct", "gamesPlayed", "plateAppearance"),
            List.of("era", "whip", "strikeouts", "wins", "saves",
                "inningsPitched", "walksAllowed", "gamesPlayed")
        ));
    }

    /**
     * GET /api/standings?seasonId=&scope=&group=&partCode=&playoffDivision=&division=
     * 팀 순위 (RecordService.getTeamRecords 위임)
     */
    @GetMapping("/standings")
    public ResponseEntity<List<TeamRecord>> getStandings(
        @RequestParam Long seasonId,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String partCode,
        @RequestParam(required = false) String playoffDivision,
        @RequestParam(required = false) String division
    ) {
        String resolvedDiv = playoffDivision != null ? playoffDivision : division;
        return ResponseEntity.ok(
            recordService.getTeamRecords(seasonId, scope, group, partCode, resolvedDiv)
        );
    }

    private String toGroup(String partCode) {
        return switch (partCode) {
            case "1" -> "A"; case "2" -> "B"; case "3" -> "C"; case "4" -> "D";
            case "5" -> "E"; case "6" -> "F"; case "7" -> "G"; case "8" -> "H";
            default -> partCode;
        };
    }

    public record GroupOption(String partCode, String group, String label, int order) {}

    public record FilterOptionsResponse(
        Long seasonId,
        List<GroupOption> groups,
        List<String> scopes,
        List<String> playoffDivisions,
        List<String> regulations,
        String defaultRegulation,
        List<String> batterSortOptions,
        List<String> pitcherSortOptions
    ) {}
}

