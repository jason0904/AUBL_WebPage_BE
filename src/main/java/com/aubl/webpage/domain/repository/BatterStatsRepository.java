package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.BatterStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterStatsRepository extends JpaRepository<BatterStats, Long> {
    java.util.List<BatterStats> findByTeamPlayerPlayerId(Long playerId);

    java.util.List<BatterStats> findByTeamPlayerPlayerIdAndSeasonId(Long playerId, Long seasonId);

    java.util.Optional<BatterStats> findByTeamPlayerIdAndSeasonIdAndSeasonTypeIsNull(
        Long teamPlayerId,
        Long seasonId
    );

    org.springframework.data.domain.Page<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Pageable pageable);

    java.util.List<BatterStats> findBySeasonIdAndSeasonTypeIsNull(Long seasonId, org.springframework.data.domain.Sort sort);
}
