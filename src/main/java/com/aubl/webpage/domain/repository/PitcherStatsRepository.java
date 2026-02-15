package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitcherStatsRepository extends JpaRepository<PitcherStats, Long> {
    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<PitcherStats> findByTeamPlayerPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<PitcherStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    java.util.Optional<PitcherStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(
        Long teamPlayerId,
        Long seasonId
    );

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    org.springframework.data.domain.Page<PitcherStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<PitcherStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Sort sort);

    @Query("select ps from PitcherStats ps " +
        "join fetch ps.teamPlayer tp " +
        "join fetch tp.player p " +
        "join fetch tp.team t " +
        "join fetch ps.season s " +
        "where s.id = :seasonId and ps.seasonType is null")
    java.util.List<PitcherStats> findBySeasonIdWithTeamPlayer(@Param("seasonId") Long seasonId);

    boolean existsByTeamPlayerIdAndSeasonId(Long teamPlayerId, Long seasonId);
}
