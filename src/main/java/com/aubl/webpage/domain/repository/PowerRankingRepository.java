package com.aubl.webpage.domain.repository;

import com.aubl.webpage.domain.entity.PowerRanking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PowerRankingRepository extends JpaRepository<PowerRanking, Long> {

    /** 특정 rankingYear의 최신 calc_version */
    @Query("SELECT MAX(pr.calcVersion) FROM PowerRanking pr WHERE pr.rankingYear = :year")
    Optional<Integer> findMaxCalcVersion(@Param("year") Integer year);

    /** 특정 rankingYear + 최신 버전 전체 조회 (team fetch join, weightedScore 내림차순) */
    @Query("SELECT pr FROM PowerRanking pr JOIN FETCH pr.team t "
        + "WHERE pr.rankingYear = :year AND pr.calcVersion = :version "
        + "ORDER BY pr.weightedScore DESC")
    List<PowerRanking> findByYearAndVersion(
        @Param("year") Integer year,
        @Param("version") Integer version
    );

    /** 특정 팀의 rankingYear 범위 조회 (최신 버전만) */
    @Query("SELECT pr FROM PowerRanking pr "
        + "WHERE pr.team.id = :teamId "
        + "AND (:fromYear IS NULL OR pr.rankingYear >= :fromYear) "
        + "AND (:toYear IS NULL OR pr.rankingYear <= :toYear) "
        + "AND pr.calcVersion = ("
        + "  SELECT MAX(pr2.calcVersion) FROM PowerRanking pr2 WHERE pr2.rankingYear = pr.rankingYear"
        + ") "
        + "ORDER BY pr.rankingYear ASC")
    List<PowerRanking> findByTeamIdAndYearRange(
        @Param("teamId") Long teamId,
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    /** rebuild 전 기존 데이터 삭제 (특정 year + version) */
    @Modifying
    @Query("DELETE FROM PowerRanking pr WHERE pr.rankingYear = :year AND pr.calcVersion = :version")
    void deleteByYearAndVersion(@Param("year") Integer year, @Param("version") Integer version);
}

