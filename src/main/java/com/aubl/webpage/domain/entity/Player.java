package com.aubl.webpage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "PLAYER",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_user_player", columnNames = {"user_id"})
    },
    indexes = {
        @Index(name = "idx_player_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserAccount user;

    @Column(name = "player_name", nullable = false, length = 100)
    private String playerName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "position", length = 20)
    private String position;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "school", length = 200)
    private String school;

    @Column(name = "is_player")
    private Boolean isPlayer;
}
