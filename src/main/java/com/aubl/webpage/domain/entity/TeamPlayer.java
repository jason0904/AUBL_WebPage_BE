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
    name = "TEAM_PLAYER",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_player_team_season",
            columnNames = {"player_id", "team_id", "season_id"}
        )
    },
    indexes = {
        @Index(name = "idx_team_player_season", columnList = "season_id"),
        @Index(name = "idx_team_player_team", columnList = "team_id"),
        @Index(name = "idx_team_player_player", columnList = "player_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TeamPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tp_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;
}
