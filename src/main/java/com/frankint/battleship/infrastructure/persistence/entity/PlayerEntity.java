package com.frankint.battleship.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "players")
@Data
public class PlayerEntity {
    @Id
    private String id;

    @Lob // Large Object
    @Column(columnDefinition = "TEXT")
    private String boardJson; // We store the board as a JSON string
}