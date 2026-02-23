package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.TeamSeasonResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamSeasonResultRepository extends JpaRepository<TeamSeasonResult, Long> {

    Optional<TeamSeasonResult> findByTeamIdAndSeasonId(Long teamId, Long seasonId);

    /** 특정 팀의 모든 시즌 결과 (season fetch join) */
    @Query("SELECT r FROM TeamSeasonResult r JOIN FETCH r.season s WHERE r.team.id = :teamId ORDER BY s.year ASC")
    List<TeamSeasonResult> findByTeamIdWithSeason(@Param("teamId") Long teamId);

    @Query("SELECT r FROM TeamSeasonResult r JOIN FETCH r.team t JOIN FETCH r.season s WHERE s.id = :seasonId")
    List<TeamSeasonResult> findBySeasonIdWithTeam(@Param("seasonId") Long seasonId);
}

