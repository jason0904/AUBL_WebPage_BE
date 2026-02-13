package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherStatsRepository extends JpaRepository<PitcherStats, Long> {
    java.util.List<PitcherStats> findByTeamPlayerPlayerId(Long playerId);

    java.util.List<PitcherStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    java.util.Optional<PitcherStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(
        Long teamPlayerId,
        Long seasonId
    );

    Page<PitcherStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, Pageable pageable);

    java.util.List<PitcherStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, Sort sort);
}
