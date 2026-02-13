package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.BatterGameLog;
import com.aubl.webpage.domain.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterGameLogRepository extends JpaRepository<BatterGameLog, Long> {
    java.util.List<BatterGameLog> findByPlayerId(Long playerId);

    java.util.List<BatterGameLog> findByPlayerIdAndGameId(Long playerId, Long gameId);

    long countByGame(Game game);

    long deleteByGame(Game game);
}
