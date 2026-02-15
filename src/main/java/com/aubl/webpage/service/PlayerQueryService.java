package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.PlayerProfileResponse;
import com.aubl.webpage.api.dto.PlayerSearchResult;
import com.aubl.webpage.api.dto.RosterItemResponse;
import com.aubl.webpage.api.dto.RosterResponse;
import com.aubl.webpage.domain.entity.Player;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.domain.repository.BatterStatsRepository;
import com.aubl.webpage.domain.repository.PitcherStatsRepository;
import com.aubl.webpage.domain.repository.PlayerRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerQueryService {

    private final TeamPlayerRepository teamPlayerRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerRepository playerRepository;
    private final BatterStatsRepository batterStatsRepository;
    private final PitcherStatsRepository pitcherStatsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlayerQueryService(
        TeamPlayerRepository teamPlayerRepository,
        SeasonRepository seasonRepository,
        PlayerRepository playerRepository,
        BatterStatsRepository batterStatsRepository,
        PitcherStatsRepository pitcherStatsRepository
    ) {
        this.teamPlayerRepository = teamPlayerRepository;
        this.seasonRepository = seasonRepository;
        this.playerRepository = playerRepository;
        this.batterStatsRepository = batterStatsRepository;
        this.pitcherStatsRepository = pitcherStatsRepository;
    }

    @Transactional(readOnly = true)
    public RosterResponse getRoster(Long seasonId, Long teamId, String q, Integer limit, String cursor) {
        validateSeasonExists(seasonId);
        Long cursorId = decodeCursor(cursor);
        String normalized = normalizeName(q);
        int pageSize = normalizeLimit(limit, 50, 500);

        List<TeamPlayer> teamPlayers = teamPlayerRepository.findRoster(
            seasonId,
            teamId,
            cursorId,
            normalized,
            PageRequest.of(0, pageSize)
        );
        long total = teamPlayerRepository.countRoster(seasonId, teamId, normalized);

        List<RosterItemResponse> items = teamPlayers.stream()
            .map(tp -> new RosterItemResponse(
                tp.getSeason().getId(),
                tp.getTeam().getId(),
                tp.getTeam().getTeamName(),
                tp.getTeam().getTeamCode(),
                tp.getId(),
                tp.getPlayer().getId(),
                tp.getPlayer().getPlayerName(),
                tp.getJerseyNumber() == null ? null : tp.getJerseyNumber().toString(),
                batterStatsRepository.existsByTeamPlayerIdAndSeasonId(tp.getId(), tp.getSeason().getId()),
                pitcherStatsRepository.existsByTeamPlayerIdAndSeasonId(tp.getId(), tp.getSeason().getId())
            ))
            .toList();

        boolean hasNext = items.size() == pageSize && !items.isEmpty();
        String nextCursor = hasNext ? encodeCursor(items.get(items.size() - 1).teamPlayerId()) : null;

        return new RosterResponse(items, nextCursor, hasNext, total);
    }

    @Transactional(readOnly = true)
    public PlayerProfileResponse getProfile(Long playerId, Long seasonId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "player not found"));

        TeamPlayer tp;
        if (seasonId != null) {
            validateSeasonExists(seasonId);
            tp = teamPlayerRepository.findByPlayerIdAndSeasonId(playerId, seasonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "season assignment not found"));
        } else {
            tp = teamPlayerRepository.findFirstByPlayerIdOrderBySeasonYearDesc(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team assignment not found"));
        }

        return new PlayerProfileResponse(
            player.getId(),
            player.getPlayerName(),
            tp.getSeason().getId(),
            tp.getTeam().getId(),
            tp.getTeam().getTeamName(),
            tp.getTeam().getTeamCode(),
            tp.getJerseyNumber()
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerSearchResult> searchPlayers(Long seasonId, String q, Long teamId, Integer limit) {
        validateSeasonExists(seasonId);
        String normalized = normalizeName(q);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q is required");
        }
        int pageSize = normalizeLimit(limit, 20, 50);

        List<TeamPlayer> result = teamPlayerRepository.searchPlayers(
            seasonId,
            teamId,
            normalized,
            PageRequest.of(0, pageSize)
        );

        return result.stream()
            .map(tp -> new PlayerSearchResult(
                tp.getPlayer().getId(),
                tp.getPlayer().getPlayerName(),
                tp.getTeam().getId(),
                tp.getTeam().getTeamName(),
                tp.getJerseyNumber(),
                tp.getSeason().getId()
            ))
            .toList();
    }

    private void validateSeasonExists(Long seasonId) {
        if (seasonId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seasonId is required");
        }
        seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "season not found"));
    }

    private String normalizeName(String q) {
        if (q == null) {
            return null;
        }
        String stripped = q.replace(" ", "").toLowerCase();
        return stripped.isBlank() ? null : stripped;
    }

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        int value = limit == null ? defaultValue : limit;
        if (value < 1) {
            value = defaultValue;
        }
        return Math.min(value, maxValue);
    }

    private Long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursor);
            Map<?, ?> map = objectMapper.readValue(decoded, Map.class);
            Object tpId = map.get("tpId");
            if (tpId instanceof Number number) {
                return number.longValue();
            }
            throw new IllegalArgumentException("invalid cursor");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cursor");
        }
    }

    private String encodeCursor(Long tpId) {
        try {
            byte[] payload = objectMapper.writeValueAsString(Map.of("tpId", tpId)).getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            return null;
        }
    }
}

