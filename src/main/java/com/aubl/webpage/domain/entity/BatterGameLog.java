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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "BATTER_GAME_LOG",
    indexes = {
        @Index(name = "idx_batter_log_game", columnList = "game_idx"),
        @Index(name = "idx_batter_log_player", columnList = "player_idx"),
        @Index(name = "idx_batter_log_stat", columnList = "batter_stat_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class BatterGameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batter_gl_id")
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
    @JoinColumn(name = "batter_stat_id", nullable = false)
    private BatterStats batterStats;

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "player_position", length = 10)
    private String playerPosition;

    @Column(name = "player_bats")
    private Integer playerBats;

    @Column(name = "player_throws")
    private Integer playerThrows;

    @Column(name = "at_bats")
    private Integer atBats;

    @Column(name = "runs")
    private Integer runs;

    @Column(name = "hits")
    private Integer hits;

    @Column(name = "rbi")
    private Integer rbi;

    @Column(name = "walks")
    private Integer walks;

    @Column(name = "strikeouts")
    private Integer strikeouts;
}
