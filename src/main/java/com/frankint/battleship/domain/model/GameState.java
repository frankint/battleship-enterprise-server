package com.frankint.battleship.domain.model;

public enum GameState {
    WAITING_FOR_PLAYER, // Only 1 player
    SETUP,              // 2 players, placing ships
    ACTIVE,             // 2 players, ships placed, shooting allowed
    FINISHED            // Game over
}
