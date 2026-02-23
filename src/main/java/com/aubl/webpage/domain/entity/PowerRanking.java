package com.aubl.webpage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "POWER_RANKING",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_power_ranking",
        columnNames = {"ranking_year", "team_id", "calc_version"}
    ),
    indexes = {
        @Index(name = "idx_power_ranking_year", columnList = "ranking_year, calc_version"),
        @Index(name = "idx_power_ranking_team", columnList = "team_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class PowerRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long id;

    /** 랭킹 기준 연도 (3개년 중 가장 최근 연도). 예: 2024 */
    @Column(name = "ranking_year", nullable = false)
    private Integer rankingYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** rankingYear-2 성적 원점수 (가중치 0.3) */
    @Column(name = "y1_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal y1Score = BigDecimal.ZERO;

    /** rankingYear-1 성적 원점수 (가중치 0.6) */
    @Column(name = "y2_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal y2Score = BigDecimal.ZERO;

    /** rankingYear 성적 원점수 (가중치 1.0) */
    @Column(name = "y3_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal y3Score = BigDecimal.ZERO;

    /** 최종 가중 합산: y1×0.3 + y2×0.6 + y3×1.0 */
    @Column(name = "weighted_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal weightedScore = BigDecimal.ZERO;

    /** 반영된 연도 목록 JSON 배열 문자열. 예: "[2021,2022,2023]" */
    @Column(name = "window_years", length = 50)
    private String windowYears;

    /** rebuild 회차 (동일 ranking_year 내 최신값 = 가장 큰 번호) */
    @Column(name = "calc_version", nullable = false)
    private Integer calcVersion = 1;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}

