package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherStats;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitcherStatsRepository extends JpaRepository<PitcherStats, Long> {

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<PitcherStats> findByTeamPlayerPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<PitcherStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    Optional<PitcherStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(Long teamPlayerId, Long seasonId);


    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    List<PitcherStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, Sort sort);

    @Query("SELECT ps FROM PitcherStats ps "
        + "JOIN FETCH ps.teamPlayer tp "
        + "JOIN FETCH tp.player p "
        + "JOIN FETCH tp.team t "
        + "JOIN FETCH ps.season s "
        + "WHERE s.id = :seasonId AND ps.seasonType IS NULL")
    List<PitcherStats> findBySeasonIdWithTeamPlayer(@Param("seasonId") Long seasonId);

    /**
     * scope/partCode/seasonType 동적 필터 쿼리.
     */
    @Query("SELECT ps FROM PitcherStats ps "
        + "JOIN FETCH ps.teamPlayer tp "
        + "JOIN FETCH tp.player p "
        + "JOIN FETCH tp.team t "
        + "JOIN FETCH ps.season s "
        + "WHERE s.id = :seasonId "
        + "AND (:scopeType IS NULL "
        + "  OR (:scopeType = 'LEAGUE' AND ps.seasonType IS NULL) "
        + "  OR (:scopeType = 'PLAYOFF' AND ps.seasonType IS NOT NULL)) "
        + "AND (:partCode IS NULL OR tp.partCode = :partCode) "
        + "AND (:seasonType IS NULL OR ps.seasonType = :seasonType)")
    List<PitcherStats> findBySeasonIdFiltered(
        @Param("seasonId") Long seasonId,
        @Param("scopeType") String scopeType,
        @Param("partCode") String partCode,
        @Param("seasonType") String seasonType
    );

    boolean existsByTeamPlayerIdAndSeasonId(Long teamPlayerId, Long seasonId);
}
