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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "BATTER_STATS",
    indexes = {
        @Index(name = "idx_batter_stats_season", columnList = "season_id, season_type"),
        @Index(name = "idx_batter_stats_tp", columnList = "tp_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class BatterStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batter_stat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tp_id", nullable = false)
    private TeamPlayer teamPlayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "season_type", length = 20)
    private String seasonType;

    @Column(name = "games_played")
    private Integer gamesPlayed;

    @Column(name = "plate_appearance")
    private Integer plateAppearance;

    @Column(name = "at_bats")
    private Integer atBats;

    @Column(name = "hits")
    private Integer hits;

    @Column(name = "doubles")
    private Integer doubles;

    @Column(name = "triples")
    private Integer triples;

    @Column(name = "home_runs")
    private Integer homeRuns;

    @Column(name = "runs_batted_in")
    private Integer runsBattedIn;

    @Column(name = "runs_scored")
    private Integer runsScored;

    @Column(name = "stolen_bases")
    private Integer stolenBases;

    @Column(name = "caught_stealing")
    private Integer caughtStealing;

    @Column(name = "walks")
    private Integer walks;

    @Column(name = "strikeouts")
    private Integer strikeouts;

    @Column(name = "hit_by_pitch")
    private Integer hitByPitch;

    @Column(name = "sacrifice_hits")
    private Integer sacrificeHits;

    @Column(name = "sacrifice_flies")
    private Integer sacrificeFlies;

    @Column(name = "batting_average", precision = 5, scale = 3)
    private BigDecimal battingAverage;

    @Column(name = "on_base_pct", precision = 5, scale = 3)
    private BigDecimal onBasePct;

    @Column(name = "slugging_pct", precision = 5, scale = 3)
    private BigDecimal sluggingPct;

    @Column(name = "ops", precision = 5, scale = 3)
    private BigDecimal ops;
}
