package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
