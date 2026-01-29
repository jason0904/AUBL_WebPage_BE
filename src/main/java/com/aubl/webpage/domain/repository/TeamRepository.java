package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    java.util.Optional<Team> findByTeamCode(String teamCode);
}
