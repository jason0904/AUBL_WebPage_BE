package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Game;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySeasonIdAndGameDateAndHomeTeamIdAndAwayTeamId(
        Long seasonId,
        LocalDate gameDate,
        Long homeTeamId,
        Long awayTeamId
    );

    List<Game> findBySeasonId(Long seasonId);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    List<Game> findWithTeamsBySeasonId(Long seasonId);

    /**
     * 포스트시즌 경기 전체 조회 (tier 무관)
     */
    @Query("SELECT g FROM Game g "
        + "JOIN FETCH g.season s "
        + "JOIN FETCH g.homeTeam ht "
        + "JOIN FETCH g.awayTeam at "
        + "WHERE s.id = :seasonId AND g.gameType LIKE '%포스트%' "
        + "ORDER BY g.playoffTier ASC, g.playoffRound ASC, g.gameDate ASC")
    List<Game> findPlayoffGamesBySeasonId(@Param("seasonId") Long seasonId);

    /**
     * 포스트시즌 경기 — tier 필터 (EUTTEUM | BEOGEUM)
     */
    @Query("SELECT g FROM Game g "
        + "JOIN FETCH g.season s "
        + "JOIN FETCH g.homeTeam ht "
        + "JOIN FETCH g.awayTeam at "
        + "WHERE s.id = :seasonId AND g.playoffTier = :tier "
        + "ORDER BY g.playoffRound ASC, g.gameDate ASC")
    List<Game> findPlayoffGamesBySeasonIdAndTier(
        @Param("seasonId") Long seasonId,
        @Param("tier") String tier
    );

    /**
     * 특정 시즌의 정규시즌(예선) 경기 목록 — homeTeam, awayTeam fetch join
     */
    @Query("SELECT g FROM Game g "
        + "JOIN FETCH g.season s "
        + "JOIN FETCH g.homeTeam ht "
        + "JOIN FETCH g.awayTeam at "
        + "WHERE s.id = :seasonId AND g.gameType = '정규시즌' "
        + "AND g.homeScore IS NOT NULL AND g.awayScore IS NOT NULL "
        + "ORDER BY g.gameDate ASC")
    List<Game> findRegularSeasonGames(@Param("seasonId") Long seasonId);

    /**
     * 연도 범위의 정규시즌 경기 (파워랭킹 rebuild용)
     */
    @Query("SELECT g FROM Game g "
        + "JOIN FETCH g.season s "
        + "JOIN FETCH g.homeTeam ht "
        + "JOIN FETCH g.awayTeam at "
        + "WHERE s.year >= :fromYear AND s.year <= :toYear "
        + "AND g.gameType = '정규시즌' "
        + "AND g.homeScore IS NOT NULL AND g.awayScore IS NOT NULL")
    List<Game> findRegularSeasonGamesByYearRange(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    /**
     * 연도 범위의 포스트시즌 경기 (파워랭킹 rebuild용)
     */
    @Query("SELECT g FROM Game g "
        + "JOIN FETCH g.season s "
        + "JOIN FETCH g.homeTeam ht "
        + "JOIN FETCH g.awayTeam at "
        + "WHERE s.year >= :fromYear AND s.year <= :toYear "
        + "AND g.gameType LIKE '%포스트%' "
        + "AND g.homeScore IS NOT NULL AND g.awayScore IS NOT NULL "
        + "AND g.playoffRound IS NOT NULL")
    List<Game> findPlayoffGamesByYearRange(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );
}
