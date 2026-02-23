package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.BatterStats;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatterStatsRepository extends JpaRepository<BatterStats, Long> {

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<BatterStats> findByTeamPlayerPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<BatterStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    Optional<BatterStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(Long teamPlayerId, Long seasonId);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    Page<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, Pageable pageable);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, Sort sort);

    @Query("SELECT bs FROM BatterStats bs "
        + "JOIN FETCH bs.teamPlayer tp "
        + "JOIN FETCH tp.player p "
        + "JOIN FETCH tp.team t "
        + "JOIN FETCH bs.season s "
        + "WHERE s.id = :seasonId AND bs.seasonType IS NULL")
    List<BatterStats> findBySeasonIdWithTeamPlayer(@Param("seasonId") Long seasonId);

    /**
     * scope/partCode/seasonType 동적 필터 쿼리.
     * - scopeType: null → 전체, "LEAGUE" → seasonType IS NULL, "PLAYOFF" → seasonType IS NOT NULL
     * - partCode: null → 전체, 값 있으면 TEAM_PLAYER.part_code 기준 필터
     * - seasonType: null → 전체, 값 있으면 정확히 일치
     */
    @Query("SELECT bs FROM BatterStats bs "
        + "JOIN FETCH bs.teamPlayer tp "
        + "JOIN FETCH tp.player p "
        + "JOIN FETCH tp.team t "
        + "JOIN FETCH bs.season s "
        + "WHERE s.id = :seasonId "
        + "AND (:scopeType IS NULL "
        + "  OR (:scopeType = 'LEAGUE' AND bs.seasonType IS NULL) "
        + "  OR (:scopeType = 'PLAYOFF' AND bs.seasonType IS NOT NULL)) "
        + "AND (:partCode IS NULL OR tp.partCode = :partCode) "
        + "AND (:seasonType IS NULL OR bs.seasonType = :seasonType)")
    List<BatterStats> findBySeasonIdFiltered(
        @Param("seasonId") Long seasonId,
        @Param("scopeType") String scopeType,
        @Param("partCode") String partCode,
        @Param("seasonType") String seasonType
    );

    boolean existsByTeamPlayerIdAndSeasonId(Long teamPlayerId, Long seasonId);
}
