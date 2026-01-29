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
    name = "PITCHER_STATS",
    indexes = {
        @Index(name = "idx_pitcher_stats_season", columnList = "season_id, season_type"),
        @Index(name = "idx_pitcher_stats_tp", columnList = "tp_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class PitcherStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pitcher_stat_id")
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

    @Column(name = "games_started")
    private Integer gamesStarted;

    @Column(name = "complete_games")
    private Integer completeGames;

    @Column(name = "shutouts")
    private Integer shutouts;

    @Column(name = "innings_pitched", precision = 5, scale = 2)
    private BigDecimal inningsPitched;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "losses")
    private Integer losses;

    @Column(name = "saves")
    private Integer saves;

    @Column(name = "holds")
    private Integer holds;

    @Column(name = "hits_allowed")
    private Integer hitsAllowed;

    @Column(name = "runs_allowed")
    private Integer runsAllowed;

    @Column(name = "earned_runs")
    private Integer earnedRuns;

    @Column(name = "home_runs_allow")
    private Integer homeRunsAllow;

    @Column(name = "walks_allowed")
    private Integer walksAllowed;

    @Column(name = "strikeouts")
    private Integer strikeouts;

    @Column(name = "hit_batters")
    private Integer hitBatters;

    @Column(name = "wild_pitches")
    private Integer wildPitches;

    @Column(name = "balks")
    private Integer balks;

    @Column(name = "era", precision = 5, scale = 2)
    private BigDecimal era;

    @Column(name = "whip", precision = 5, scale = 3)
    private BigDecimal whip;

    @Column(name = "k_per_9", precision = 5, scale = 2)
    private BigDecimal kPer9;

    @Column(name = "bb_per_9", precision = 5, scale = 2)
    private BigDecimal bbPer9;
}
