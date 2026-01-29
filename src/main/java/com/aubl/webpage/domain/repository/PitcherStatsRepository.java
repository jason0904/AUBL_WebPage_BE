package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherStatsRepository extends JpaRepository<PitcherStats, Long> {
    java.util.List<PitcherStats> findByTeamPlayerPlayerId(Long playerId);

    java.util.List<PitcherStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    java.util.Optional<PitcherStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(
        Long teamPlayerId,
        Long seasonId
    );
}
