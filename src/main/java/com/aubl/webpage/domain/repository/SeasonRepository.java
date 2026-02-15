package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    java.util.Optional<Season> findByYear(Integer year);
    java.util.List<Season> findAllByOrderByYearAsc();
}
