package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {
    java.util.Optional<TeamPlayer> findByTeamIdAndSeasonIdAndPlayerPlayerName(
        Long teamId,
        Long seasonId,
        String playerName
    );

    java.util.Optional<TeamPlayer> findByTeamIdAndSeasonIdAndJerseyNumberAndPlayerPlayerName(
        Long teamId,
        Long seasonId,
        Integer jerseyNumber,
        String playerName
    );
}
