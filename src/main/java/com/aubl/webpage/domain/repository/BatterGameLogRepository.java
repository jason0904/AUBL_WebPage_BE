package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.BatterGameLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterGameLogRepository extends JpaRepository<BatterGameLog, Long> {
    @EntityGraph(attributePaths = {"batterStats", "batterStats.teamPlayer"})
    java.util.List<BatterGameLog> findByPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"batterStats", "batterStats.teamPlayer"})
    java.util.List<BatterGameLog> findByPlayerIdAndGameId(Long playerId, Long gameId);
}
