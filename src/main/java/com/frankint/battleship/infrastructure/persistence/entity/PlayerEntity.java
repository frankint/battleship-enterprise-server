package com.frankint.battleship.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "players")
@Data
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Generate a random ID for this "Seat"
    private String id;

    private String userId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String boardJson;
}