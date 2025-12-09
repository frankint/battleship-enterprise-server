package com.frankint.battleship.infrastructure.persistence.entity;

import com.frankint.battleship.domain.model.GameState;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "games")
@Data
public class GameEntity {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private GameState state;

    private String currentTurnPlayerId;
    private String winnerId;

    @OneToOne(cascade = CascadeType.ALL)
    private PlayerEntity player1;

    @OneToOne(cascade = CascadeType.ALL)
    private PlayerEntity player2;

    @Column(nullable = false)
    private boolean p1Visible = true;

    @Column(nullable = false)
    private boolean p2Visible = true;
}