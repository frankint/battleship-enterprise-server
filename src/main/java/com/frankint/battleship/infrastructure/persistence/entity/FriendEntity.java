package com.frankint.battleship.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "friends", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "friend_id"}) // Prevent duplicates
})
@Data
public class FriendEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String user;   // The person who added the friend

    @Column(name = "friend_id", nullable = false)
    private String friend; // The target username
}