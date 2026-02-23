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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "TEAM_SEASON_RESULT",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_team_season_result",
        columnNames = {"team_id", "season_id"}
    ),
    indexes = {
        @Index(name = "idx_team_season_result_season", columnList = "season_id"),
        @Index(name = "idx_team_season_result_team",   columnList = "team_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TeamSeasonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /**
     * 환산 기준 경기 수 (기본 4경기).
     * 실제 치른 경기 수가 이 값보다 적으면 승점을 비례 환산.
     * 환산 승점 = (승×3 + 무×1) × (prelim_games_standard / 실제경기수)
     */
    @Column(name = "prelim_games_standard", nullable = false)
    private int prelimGamesStandard = 4;

    @Column(name = "note", length = 200)
    private String note;
}

