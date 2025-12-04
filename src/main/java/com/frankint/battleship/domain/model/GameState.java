package com.frankint.battleship.domain.model;

public enum GameState {
    WAITING_FOR_PLAYER, // Player 1 created, waiting for Player 2
    ACTIVE,             // Both players joined, game in progress
    FINISHED            // One player sunk all ships
}
