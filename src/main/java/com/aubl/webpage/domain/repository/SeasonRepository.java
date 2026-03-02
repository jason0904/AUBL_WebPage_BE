package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByYear(Integer year);
    List<Season> findAllByOrderByYearAsc();
}
