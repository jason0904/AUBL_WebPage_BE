package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PitcherGameLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherGameLogRepository extends JpaRepository<PitcherGameLog, Long> {
    java.util.List<PitcherGameLog> findByPlayerId(Long playerId);

    java.util.List<PitcherGameLog> findByPlayerIdAndGameId(Long playerId, Long gameId);
}
