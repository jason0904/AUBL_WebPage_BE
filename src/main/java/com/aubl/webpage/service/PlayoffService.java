package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.PlayoffDtos;
import com.aubl.webpage.api.dto.PlayoffDtos.PlayoffGameRow;
import com.aubl.webpage.api.dto.PlayoffDtos.PlayoffTeamRow;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PlayoffService {

    private final GameRepository gameRepository;
    private final SeasonRepository seasonRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public PlayoffService(
        GameRepository gameRepository,
        SeasonRepository seasonRepository,
        TeamPlayerRepository teamPlayerRepository
    ) {
        this.gameRepository = gameRepository;
        this.seasonRepository = seasonRepository;
        this.teamPlayerRepository = teamPlayerRepository;
    }

    // ── 경기 목록 ──────────────────────────────────────────────

    /**
     * 포스트시즌 경기 목록 조회
     * tier: ALL(기본) | EUTTEUM | BEOGEUM
     */
    public List<PlayoffGameRow> getPlayoffGames(Long seasonId, String tier) {
        validate(seasonId);
        List<Game> games = resolveGames(seasonId, tier);
        return games.stream()
            .map(g -> new PlayoffGameRow(
                g.getSeason().getId(),
                g.getSeason().getYear(),
                g.getId(),
                g.getGameDate(),
                g.getGameType(),
                g.getPlayoffTier(),
                g.getPlayoffRound(),
                g.getHomeTeam().getId(),
                g.getHomeTeam().getTeamName(),
                g.getHomeScore(),
                g.getAwayTeam().getId(),
                g.getAwayTeam().getTeamName(),
                g.getAwayScore(),
                "PLAYOFF"
            ))
            .toList();
    }

    // ── 팀 집계 ────────────────────────────────────────────────

    /**
     * 포스트시즌 팀별 집계
     * tier: ALL(기본) | EUTTEUM | BEOGEUM
     */
    public List<PlayoffTeamRow> getPlayoffTeams(Long seasonId, String tier) {
        validate(seasonId);
        List<Game> games = resolveGames(seasonId, tier);

        // 팀별 누적
        Map<Long, Accumulator> map = new HashMap<>();
        for (Game game : games) {
            if (game.getHomeScore() == null || game.getAwayScore() == null) continue;
            boolean homeWon = game.getHomeScore() > game.getAwayScore();
            accumulate(map, game.getHomeTeam(), game.getHomeScore(), game.getAwayScore(),
                game.getPlayoffTier(), game.getPlayoffRound(), homeWon);
            accumulate(map, game.getAwayTeam(), game.getAwayScore(), game.getHomeScore(),
                game.getPlayoffTier(), game.getPlayoffRound(), !homeWon);
        }

        // part_code 조회
        Map<Long, String> partCodeMap = resolvePartCodes(seasonId, new ArrayList<>(map.keySet()));
        Integer seasonYear = games.isEmpty() ? null : games.get(0).getSeason().getYear();

        return map.values().stream()
            .sorted(Comparator
                .comparing((Accumulator a) -> PlayoffDtos.roundOrder(a.bestRound))
                .reversed()
                .thenComparingInt((Accumulator a) -> a.wins - a.losses)
                .reversed())
            .map(acc -> {
                String partCode = partCodeMap.getOrDefault(acc.team.getId(), null);
                return new PlayoffTeamRow(
                    seasonId,
                    seasonYear,
                    acc.team.getId(),
                    acc.team.getTeamName(),
                    acc.playoffTier,
                    acc.bestRound,
                    acc.wins,
                    acc.losses,
                    acc.runsScored,
                    acc.runsAllowed,
                    "PLAYOFF",
                    partCode,
                    PlayoffDtos.toGroup(partCode)
                );
            })
            .toList();
    }

    // ── private helpers ────────────────────────────────────────

    private List<Game> resolveGames(Long seasonId, String tier) {
        if (tier == null || tier.isBlank() || tier.equalsIgnoreCase("ALL")) {
            return gameRepository.findPlayoffGamesBySeasonId(seasonId);
        }
        String normalized = tier.toUpperCase();
        if (!normalized.equals("EUTTEUM") && !normalized.equals("BEOGEUM")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid tier. Allowed: ALL, EUTTEUM, BEOGEUM");
        }
        return gameRepository.findPlayoffGamesBySeasonIdAndTier(seasonId, normalized);
    }

    private void validate(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Season not found: " + seasonId));
    }

    private void accumulate(
        Map<Long, Accumulator> map, Team team,
        int scored, int allowed,
        String tier, String round, boolean won
    ) {
        Accumulator acc = map.computeIfAbsent(team.getId(), id -> new Accumulator(team));
        acc.runsScored += scored;
        acc.runsAllowed += allowed;
        if (won) acc.wins++; else acc.losses++;
        // tier: ALL 조회 시 여러 tier가 섞일 수 있으므로 null 처리
        if (acc.playoffTier == null) acc.playoffTier = tier;
        else if (!acc.playoffTier.equals(tier)) acc.playoffTier = "MIXED";
        // 가장 높은(나중) 라운드 갱신
        if (PlayoffDtos.roundOrder(round) > PlayoffDtos.roundOrder(acc.bestRound)) {
            acc.bestRound = round;
        }
    }

    private Map<Long, String> resolvePartCodes(Long seasonId, List<Long> teamIds) {
        Map<Long, String> result = new HashMap<>();
        if (teamIds.isEmpty()) return result;
        teamPlayerRepository.findDistinctBySeasonId(seasonId).forEach(tp -> {
            Long tid = tp.getTeam().getId();
            if (teamIds.contains(tid) && !result.containsKey(tid)) {
                result.put(tid, tp.getPartCode());
            }
        });
        return result;
    }

    private static class Accumulator {
        final Team team;
        String playoffTier;
        String bestRound;
        int wins, losses, runsScored, runsAllowed;
        Accumulator(Team team) { this.team = team; }
    }
}

