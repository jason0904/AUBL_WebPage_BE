package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
