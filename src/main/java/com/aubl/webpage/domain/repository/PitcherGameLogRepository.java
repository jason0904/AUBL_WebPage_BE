package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherGameLog;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherGameLogRepository extends JpaRepository<PitcherGameLog, Long> {
    @EntityGraph(attributePaths = {"pitcherStats", "pitcherStats.teamPlayer"})
    List<PitcherGameLog> findByPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"pitcherStats", "pitcherStats.teamPlayer"})
    List<PitcherGameLog> findByPlayerIdAndGameId(Long playerId, Long gameId);
}
