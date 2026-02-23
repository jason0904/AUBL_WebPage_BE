package com.aubl.webpage.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordsFilterController {

    @GetMapping("/filter-options")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions(@RequestParam Long seasonId) {
        List<GroupOption> groups = List.of(
            new GroupOption("1", "A", "A조", 1),
            new GroupOption("2", "B", "B조", 2),
            new GroupOption("3", "C", "C조", 3),
            new GroupOption("4", "D", "D조", 4),
            new GroupOption("5", "E", "E조", 5),
            new GroupOption("6", "F", "F조", 6),
            new GroupOption("7", "G", "G조", 7),
            new GroupOption("8", "H", "H조", 8)
        );
        return ResponseEntity.ok(new FilterOptionsResponse(
            seasonId,
            groups,
            List.of("LEAGUE", "PLAYOFF"),
            List.of("EUTTEUM", "BEOGEUM"),
            List.of("IN", "OUT"),
            "IN",
            List.of("battingAverage", "hits", "homeRuns", "rbi", "ops", "sluggingPct", "onBasePct", "gamesPlayed", "plateAppearance"),
            List.of("era", "whip", "strikeouts", "wins", "saves", "inningsPitched", "walksAllowed", "gamesPlayed")
        ));
    }

    public record GroupOption(String partCode, String group, String label, int order) {
    }

    public record FilterOptionsResponse(
        Long seasonId,
        List<GroupOption> groups,
        List<String> scopes,
        List<String> playoffDivisions,
        List<String> regulations,
        String defaultRegulation,
        List<String> batterSortOptions,
        List<String> pitcherSortOptions
    ) {
    }
}

