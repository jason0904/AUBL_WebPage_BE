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
    name = "PITCHER_GAME_LOG",
    indexes = {
        @Index(name = "idx_pitcher_log_game", columnList = "game_idx"),
        @Index(name = "idx_pitcher_log_player", columnList = "player_idx"),
        @Index(name = "idx_pitcher_log_stat", columnList = "pitcher_stat_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class PitcherGameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pitcher_gl_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_idx", nullable = false)
    private Game game;

    @Column(name = "team_side", length = 10)
    private String teamSide;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_idx", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_idx")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pitcher_stat_id", nullable = false)
    private PitcherStats pitcherStats;

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "player_position", length = 10)
    private String playerPosition;

    @Column(name = "player_bats")
    private Integer playerBats;

    @Column(name = "player_throws")
    private Integer playerThrows;

    @Column(name = "innings_pitched", precision = 5, scale = 2)
    private BigDecimal inningsPitched;

    @Column(name = "hits_allowed")
    private Integer hitsAllowed;

    @Column(name = "runs_allowed")
    private Integer runsAllowed;

    @Column(name = "earned_runs")
    private Integer earnedRuns;

    @Column(name = "walks")
    private Integer walks;

    @Column(name = "strikeouts")
    private Integer strikeouts;
}
