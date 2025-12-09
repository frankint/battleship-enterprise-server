package com.frankint.battleship.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    private String username; // Simple ID for now
    private String password; // This will be the BCrypt Hash, not plain text
}