package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.BatterStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterStatsRepository extends JpaRepository<BatterStats, Long> {
    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<BatterStats> findByTeamPlayerPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<BatterStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    java.util.Optional<BatterStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(
        Long teamPlayerId,
        Long seasonId
    );

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    org.springframework.data.domain.Page<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"teamPlayer", "teamPlayer.player", "teamPlayer.team", "season"})
    java.util.List<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Sort sort);
}
