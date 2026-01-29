package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    java.util.Optional<Game> findBySeasonIdAndGameDateAndHomeTeamIdAndAwayTeamId(
        Long seasonId,
        java.time.LocalDate gameDate,
        Long homeTeamId,
        Long awayTeamId
    );
}
