package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.ImportResult;
import com.aubl.webpage.domain.entity.BatterGameLog;
import com.aubl.webpage.domain.entity.BatterStats;
import com.aubl.webpage.domain.entity.Game;
import com.aubl.webpage.domain.entity.PitcherGameLog;
import com.aubl.webpage.domain.entity.PitcherStats;
import com.aubl.webpage.domain.entity.Season;
import com.aubl.webpage.domain.entity.Team;
import com.aubl.webpage.domain.entity.TeamPlayer;
import com.aubl.webpage.domain.repository.BatterGameLogRepository;
import com.aubl.webpage.domain.repository.BatterStatsRepository;
import com.aubl.webpage.domain.repository.GameRepository;
import com.aubl.webpage.domain.repository.PlayerRepository;
import com.aubl.webpage.domain.repository.PitcherGameLogRepository;
import com.aubl.webpage.domain.repository.PitcherStatsRepository;
import com.aubl.webpage.domain.repository.SeasonRepository;
import com.aubl.webpage.domain.repository.TeamPlayerRepository;
import com.aubl.webpage.domain.repository.TeamRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentReference;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FirestoreImportService {

    private final Firestore firestore;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final BatterStatsRepository batterStatsRepository;
    private final PitcherStatsRepository pitcherStatsRepository;
    private final BatterGameLogRepository batterGameLogRepository;
    private final PitcherGameLogRepository pitcherGameLogRepository;
    private final PlayerRepository playerRepository;

    public FirestoreImportService(
        Firestore firestore,
        TeamRepository teamRepository,
        SeasonRepository seasonRepository,
        GameRepository gameRepository,
        TeamPlayerRepository teamPlayerRepository,
        BatterStatsRepository batterStatsRepository,
        PitcherStatsRepository pitcherStatsRepository,
        BatterGameLogRepository batterGameLogRepository,
        PitcherGameLogRepository pitcherGameLogRepository,
        PlayerRepository playerRepository
    ) {
        this.firestore = firestore;
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
        this.gameRepository = gameRepository;
        this.teamPlayerRepository = teamPlayerRepository;
        this.batterStatsRepository = batterStatsRepository;
        this.pitcherStatsRepository = pitcherStatsRepository;
        this.batterGameLogRepository = batterGameLogRepository;
        this.pitcherGameLogRepository = pitcherGameLogRepository;
        this.playerRepository = playerRepository;
    }

    public ImportResult importCompletedMatches() {
        List<? extends DocumentSnapshot> matches = fetchCompletedMatches();
        int batterInserted = 0;
        int pitcherInserted = 0;

        for (DocumentSnapshot match : matches) {
            ImportCounts counts = importMatch(match);
            batterInserted += counts.batterLogs();
            pitcherInserted += counts.pitcherLogs();
        }

        return new ImportResult(matches.size(), batterInserted, pitcherInserted);
    }

    public ImportResult importMatchById(String matchId) {
        DocumentSnapshot match = fetchMatchById(matchId);
        if (!"completed".equalsIgnoreCase(asString(match.get("status")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "match is not completed");
        }
        ImportCounts counts = importMatch(match);
        return new ImportResult(1, counts.batterLogs(), counts.pitcherLogs());
    }

    private List<? extends DocumentSnapshot> fetchCompletedMatches() {
        ApiFuture<QuerySnapshot> future = firestore.collection("matches")
            .whereEqualTo("status", "completed")
            .get();
        try {
            return future.get().getDocuments();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore query interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Firestore query failed", ex);
        }
    }

    private DocumentSnapshot fetchMatchById(String matchId) {
        DocumentReference ref = firestore.collection("matches").document(matchId);
        try {
            DocumentSnapshot snapshot = ref.get().get();
            if (!snapshot.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found");
            }
            return snapshot;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore query interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Firestore query failed", ex);
        }
    }

    @Transactional
    public ImportCounts importMatch(DocumentSnapshot match) {
        Team homeTeam = resolveTeam(match.getString("homeTeamId"));
        Team awayTeam = resolveTeam(match.getString("awayTeamId"));
        LocalDate gameDate = resolveGameDate(match);
        Season season = resolveSeason(gameDate);
        Game game = resolveGame(season, gameDate, homeTeam, awayTeam);

        applyGameResult(game, match);
        boolean applyStats = !deleteExistingLogs(game);

        Map<String, Object> postGame = readMap(match.get("postGame"));
        if (postGame == null) {
            return new ImportCounts(0, 0);
        }

        List<BatterGameLog> batterLogs = new ArrayList<>();
        List<PitcherGameLog> pitcherLogs = new ArrayList<>();

        Map<String, Object> batters = readMap(postGame.get("batters"));
        appendBatterLogs(batterLogs, batters, "home", homeTeam, season, game, applyStats);
        appendBatterLogs(batterLogs, batters, "away", awayTeam, season, game, applyStats);

        Map<String, Object> pitchers = readMap(postGame.get("pitchers"));
        appendPitcherLogs(pitcherLogs, pitchers, "home", homeTeam, season, game, applyStats);
        appendPitcherLogs(pitcherLogs, pitchers, "away", awayTeam, season, game, applyStats);

        batterGameLogRepository.saveAll(batterLogs);
        pitcherGameLogRepository.saveAll(pitcherLogs);

        return new ImportCounts(batterLogs.size(), pitcherLogs.size());
    }

    private boolean deleteExistingLogs(Game game) {
        long batterCount = batterGameLogRepository.countByGame(game);
        long pitcherCount = pitcherGameLogRepository.countByGame(game);
        if (batterCount > 0) {
            batterGameLogRepository.deleteByGame(game);
        }
        if (pitcherCount > 0) {
            pitcherGameLogRepository.deleteByGame(game);
        }
        return batterCount + pitcherCount > 0;
    }

    private void applyGameResult(Game game, DocumentSnapshot match) {
        Integer homeScore = asInteger(match.get("homeScore"));
        Integer awayScore = asInteger(match.get("awayScore"));
        if (homeScore != null) {
            game.setHomeScore(homeScore);
        }
        if (awayScore != null) {
            game.setAwayScore(awayScore);
        }
        gameRepository.save(game);
    }

    private void appendBatterLogs(
        List<BatterGameLog> logs,
        Map<String, Object> batters,
        String side,
        Team team,
        Season season,
        Game game,
        boolean applyStats
    ) {
        List<Map<String, Object>> entries = readList(batters, side);
        if (entries == null) {
            return;
        }
        for (Map<String, Object> entry : entries) {
            PlayerKey playerKey = parsePlayerKey(asString(entry.get("name")));
            TeamPlayer teamPlayer = resolveTeamPlayer(team, season, playerKey);
            BatterStats batterStats = resolveBatterStats(teamPlayer, season);

            BatterGameLog log = new BatterGameLog();
            log.setGame(game);
            log.setTeamSide(side);
            log.setTeam(team);
            log.setPlayer(teamPlayer.getPlayer());
            log.setBatterStats(batterStats);
            log.setPlayerName(playerKey.name());
            log.setPlayerPosition(asString(entry.get("pos")));
            Integer atBats = asInteger(entry.get("ab"));
            Integer plateAppearance = asInteger(entry.get("pa"));
            log.setPlayerBats(defaultIfNull(plateAppearance));
            log.setAtBats(defaultIfNull(atBats));
            log.setRuns(defaultIfNull(asInteger(entry.get("sb"))));
            log.setHits(defaultIfNull(asInteger(entry.get("h"))));
            log.setRbi(defaultIfNull(asInteger(entry.get("rbi"))));
            log.setWalks(defaultIfNull(asInteger(entry.get("bb"))));
            log.setStrikeouts(defaultIfNull(asInteger(entry.get("so"))));
            logs.add(log);

            if (applyStats) {
                applyBatterStats(batterStats, entry);
            }
        }
    }

    private void appendPitcherLogs(
        List<PitcherGameLog> logs,
        Map<String, Object> pitchers,
        String side,
        Team team,
        Season season,
        Game game,
        boolean applyStats
    ) {
        List<Map<String, Object>> entries = readList(pitchers, side);
        if (entries == null) {
            return;
        }
        for (Map<String, Object> entry : entries) {
            PlayerKey playerKey = parsePlayerKey(asString(entry.get("name")));
            TeamPlayer teamPlayer = resolveTeamPlayer(team, season, playerKey);
            PitcherStats pitcherStats = resolvePitcherStats(teamPlayer, season);

            PitcherGameLog log = new PitcherGameLog();
            log.setGame(game);
            log.setTeamSide(side);
            log.setTeam(team);
            log.setPlayer(teamPlayer.getPlayer());
            log.setPitcherStats(pitcherStats);
            log.setPlayerName(playerKey.name());
            log.setInningsPitched(asBigDecimal(entry.get("ip")));
            log.setHitsAllowed(asInteger(entry.get("h")));
            log.setRunsAllowed(asInteger(entry.get("r")));
            log.setEarnedRuns(asInteger(entry.get("er")));
            log.setWalks(asInteger(entry.get("bb")));
            log.setStrikeouts(asInteger(entry.get("so")));
            log.setPlayerBats(asInteger(entry.get("bf")));
            log.setPlayerThrows(asInteger(entry.get("pitches")));
            logs.add(log);

            if (applyStats) {
                applyPitcherStats(pitcherStats, entry);
            }
        }
    }

    private Team resolveTeam(String teamCode) {
        if (teamCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "team code missing");
        }
        return teamRepository.findByTeamCode(teamCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found: " + teamCode));
    }

    private Season resolveSeason(LocalDate gameDate) {
        int year = gameDate.getYear();
        return seasonRepository.findByYear(year)
            .orElseGet(() -> {
                Season season = new Season();
                season.setYear(year);
                return seasonRepository.save(season);
            });
    }

    private Game resolveGame(Season season, LocalDate gameDate, Team homeTeam, Team awayTeam) {
        return gameRepository.findBySeasonIdAndGameDateAndHomeTeamIdAndAwayTeamId(
                season.getId(),
                gameDate,
                homeTeam.getId(),
                awayTeam.getId()
            )
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "game not found"));
    }

    private TeamPlayer resolveTeamPlayer(Team team, Season season, PlayerKey playerKey) {
        if (playerKey.name() == null || playerKey.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "player name missing");
        }
        if (playerKey.jerseyNumber() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "jersey number missing for player: " + playerKey.name()
            );
        }
        return teamPlayerRepository.findByTeamIdAndSeasonIdAndJerseyNumberAndPlayerPlayerName(
                team.getId(),
                season.getId(),
                playerKey.jerseyNumber(),
                playerKey.name()
            )
            .orElseGet(() -> createTeamPlayer(team, season, playerKey));
    }

    private TeamPlayer createTeamPlayer(Team team, Season season, PlayerKey playerKey) {
        com.aubl.webpage.domain.entity.Player player = new com.aubl.webpage.domain.entity.Player();
        player.setPlayerName(playerKey.name());
        player = playerRepository.save(player);

        TeamPlayer teamPlayer = new TeamPlayer();
        teamPlayer.setTeam(team);
        teamPlayer.setSeason(season);
        teamPlayer.setPlayer(player);
        teamPlayer.setJerseyNumber(playerKey.jerseyNumber());
        return teamPlayerRepository.save(teamPlayer);
    }

    private BatterStats resolveBatterStats(TeamPlayer teamPlayer, Season season) {
        Optional<BatterStats> existing = batterStatsRepository
            .findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(teamPlayer.getId(), season.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        BatterStats stats = new BatterStats();
        stats.setTeamPlayer(teamPlayer);
        stats.setSeason(season);
        return batterStatsRepository.save(stats);
    }

    private PitcherStats resolvePitcherStats(TeamPlayer teamPlayer, Season season) {
        Optional<PitcherStats> existing = pitcherStatsRepository
            .findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(teamPlayer.getId(), season.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        PitcherStats stats = new PitcherStats();
        stats.setTeamPlayer(teamPlayer);
        stats.setSeason(season);
        return pitcherStatsRepository.save(stats);
    }

    private void applyBatterStats(BatterStats stats, Map<String, Object> entry) {
        ensureSeasonType(stats);
        stats.setGamesPlayed(increment(stats.getGamesPlayed(), 1));
        Integer atBats = asInteger(entry.get("ab"));
        Integer plateAppearance = asInteger(entry.get("pa"));
        stats.setAtBats(increment(stats.getAtBats(), atBats));
        stats.setPlateAppearance(increment(stats.getPlateAppearance(), plateAppearance != null ? plateAppearance : atBats));
        stats.setHits(increment(stats.getHits(), asInteger(entry.get("h"))));
        stats.setDoubles(increment(stats.getDoubles(), asInteger(entry.get("doubles"))));
        stats.setTriples(increment(stats.getTriples(), asInteger(entry.get("triples"))));
        stats.setHomeRuns(increment(stats.getHomeRuns(), asInteger(entry.get("hr"))));
        stats.setRunsScored(increment(stats.getRunsScored(), asInteger(entry.get("r"))));
        stats.setRunsBattedIn(increment(stats.getRunsBattedIn(), asInteger(entry.get("rbi"))));
        stats.setStolenBases(increment(stats.getStolenBases(), asInteger(entry.get("sb"))));
        stats.setWalks(increment(stats.getWalks(), asInteger(entry.get("bb"))));
        stats.setStrikeouts(increment(stats.getStrikeouts(), asInteger(entry.get("so"))));
        stats.setHitByPitch(increment(stats.getHitByPitch(), asInteger(entry.get("hbp"))));
        stats.setSacrificeHits(increment(stats.getSacrificeHits(), asInteger(entry.get("sac"))));
        updateBatterRates(stats);
        batterStatsRepository.save(stats);
    }

    private void applyPitcherStats(PitcherStats stats, Map<String, Object> entry) {
        ensureSeasonType(stats);
        stats.setGamesPlayed(increment(stats.getGamesPlayed(), 1));
        stats.setInningsPitched(addInnings(stats.getInningsPitched(), asBigDecimal(entry.get("ip"))));
        stats.setHitsAllowed(increment(stats.getHitsAllowed(), asInteger(entry.get("h"))));
        stats.setRunsAllowed(increment(stats.getRunsAllowed(), asInteger(entry.get("r"))));
        stats.setEarnedRuns(increment(stats.getEarnedRuns(), asInteger(entry.get("er"))));
        stats.setWalksAllowed(increment(stats.getWalksAllowed(), asInteger(entry.get("bb"))));
        stats.setStrikeouts(increment(stats.getStrikeouts(), asInteger(entry.get("so"))));
        stats.setHomeRunsAllow(increment(stats.getHomeRunsAllow(), asInteger(entry.get("hr"))));
        stats.setWildPitches(increment(stats.getWildPitches(), asInteger(entry.get("wp"))));
        stats.setHitBatters(increment(stats.getHitBatters(), asInteger(entry.get("hbp"))));
        applyPitcherResult(stats, asString(entry.get("result")));
        updatePitcherRates(stats);
        pitcherStatsRepository.save(stats);
    }

    private LocalDate resolveGameDate(DocumentSnapshot match) {
        String startTime = match.getString("startTime");
        if (startTime != null && !startTime.isBlank()) {
            Instant instant = Instant.parse(startTime);
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        }
        String id = match.getId();
        if (id != null && id.length() >= 8) {
            String date = id.substring(0, 8);
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(4, 6));
            int day = Integer.parseInt(date.substring(6, 8));
            return LocalDate.of(year, month, day);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "game date missing");
    }

    private Map<String, Object> readMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private List<Map<String, Object>> readList(Map<String, Object> parent, String key) {
        if (parent == null) {
            return null;
        }
        Object value = parent.get(key);
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private PlayerKey parsePlayerKey(String rawName) {
        if (rawName == null) {
            return new PlayerKey(null, null);
        }
        String trimmed = rawName.trim();
        int parenIndex = trimmed.lastIndexOf('(');
        if (parenIndex > 0 && trimmed.endsWith(")")) {
            String name = trimmed.substring(0, parenIndex).trim();
            String numberText = trimmed.substring(parenIndex + 1, trimmed.length() - 1).trim();
            Integer number = parseInteger(numberText);
            return new PlayerKey(name, number);
        }
        return new PlayerKey(trimmed, null);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer increment(Integer current, Integer delta) {
        int base = current == null ? 0 : current;
        int add = delta == null ? 0 : delta;
        return base + add;
    }

    private BigDecimal addInnings(BigDecimal current, BigDecimal delta) {
        if (delta == null) {
            return current == null ? BigDecimal.ZERO : current;
        }
        int currentOuts = inningsToOuts(current == null ? BigDecimal.ZERO : current);
        int deltaOuts = inningsToOuts(delta);
        return outsToInnings(currentOuts + deltaOuts);
    }

    private int inningsToOuts(BigDecimal innings) {
        int whole = innings.intValue();
        BigDecimal fraction = innings.subtract(BigDecimal.valueOf(whole)).setScale(1, java.math.RoundingMode.HALF_UP);
        int outs = whole * 3;
        if (fraction.compareTo(BigDecimal.valueOf(0.1)) == 0) {
            return outs + 1;
        }
        if (fraction.compareTo(BigDecimal.valueOf(0.2)) == 0) {
            return outs + 2;
        }
        return outs;
    }

    private BigDecimal outsToInnings(int outs) {
        int whole = outs / 3;
        int rem = outs % 3;
        if (rem == 0) {
            return BigDecimal.valueOf(whole);
        }
        if (rem == 1) {
            return BigDecimal.valueOf(whole).add(BigDecimal.valueOf(0.1));
        }
        return BigDecimal.valueOf(whole).add(BigDecimal.valueOf(0.2));
    }

    private void applyPitcherResult(PitcherStats stats, String result) {
        if (result == null) {
            return;
        }
        switch (result) {
            case "승" -> stats.setWins(increment(stats.getWins(), 1));
            case "패" -> stats.setLosses(increment(stats.getLosses(), 1));
            case "세" -> stats.setSaves(increment(stats.getSaves(), 1));
            case "홀" -> stats.setHolds(increment(stats.getHolds(), 1));
            default -> {
            }
        }
    }

    private void updateBatterRates(BatterStats stats) {
        int atBats = stats.getAtBats() == null ? 0 : stats.getAtBats();
        int hits = stats.getHits() == null ? 0 : stats.getHits();
        int plateAppearance = stats.getPlateAppearance() == null ? 0 : stats.getPlateAppearance();
        int doubles = stats.getDoubles() == null ? 0 : stats.getDoubles();
        int triples = stats.getTriples() == null ? 0 : stats.getTriples();
        int homeRuns = stats.getHomeRuns() == null ? 0 : stats.getHomeRuns();
        int walks = stats.getWalks() == null ? 0 : stats.getWalks();
        int hitByPitch = stats.getHitByPitch() == null ? 0 : stats.getHitByPitch();
        int singles = Math.max(0, hits - doubles - triples - homeRuns);

        BigDecimal battingAverage = safeDivide(hits, atBats, 3);
        BigDecimal onBasePct = safeDivide(hits + walks + hitByPitch, plateAppearance, 3);
        int totalBases = singles + (2 * doubles) + (3 * triples) + (4 * homeRuns);
        BigDecimal sluggingPct = safeDivide(totalBases, atBats, 3);

        stats.setBattingAverage(defaultDecimal(battingAverage));
        stats.setOnBasePct(defaultDecimal(onBasePct));
        stats.setSluggingPct(defaultDecimal(sluggingPct));
        stats.setOps(defaultDecimal(onBasePct).add(defaultDecimal(sluggingPct)));
    }

    private void updatePitcherRates(PitcherStats stats) {
        BigDecimal inningsPitched = stats.getInningsPitched();
        if (inningsPitched == null || inningsPitched.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        int outs = inningsToOuts(inningsPitched);
        BigDecimal innings = BigDecimal.valueOf(outs)
            .divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);

        int earnedRuns = stats.getEarnedRuns() == null ? 0 : stats.getEarnedRuns();
        int walks = stats.getWalksAllowed() == null ? 0 : stats.getWalksAllowed();
        int hits = stats.getHitsAllowed() == null ? 0 : stats.getHitsAllowed();

        BigDecimal era = BigDecimal.valueOf(earnedRuns)
            .multiply(BigDecimal.valueOf(9))
            .divide(innings, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal whip = BigDecimal.valueOf(walks + hits)
            .divide(innings, 3, java.math.RoundingMode.HALF_UP);

        stats.setEra(era);
        stats.setWhip(whip);
    }

    private BigDecimal safeDivide(int numerator, int denominator, int scale) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
            .divide(BigDecimal.valueOf(denominator), scale, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(3, java.math.RoundingMode.HALF_UP);
        }
        return value;
    }

    private void ensureSeasonType(BatterStats stats) {
        if (stats.getSeasonType() == null || stats.getSeasonType().isBlank()) {
            stats.setSeasonType("정규시즌");
        }
    }

    private void ensureSeasonType(PitcherStats stats) {
        if (stats.getSeasonType() == null || stats.getSeasonType().isBlank()) {
            stats.setSeasonType("정규시즌");
        }
    }

    private Integer defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    public record ImportCounts(int batterLogs, int pitcherLogs) {
    }

    private record PlayerKey(String name, Integer jerseyNumber) {
    }
}
