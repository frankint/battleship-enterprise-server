package com.frankint.battleship.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Game {
    private final String id;
    private final Player player1;
    private Player player2; // Nullable until someone joins
    private String currentTurnPlayerId;
    private GameState state;
    private String winnerId;

    // Constructor for a new game
    public Game(Player player1) {
        this.id = UUID.randomUUID().toString();
        this.player1 = player1;
        this.currentTurnPlayerId = player1.getId(); // Player 1 starts
        this.state = GameState.WAITING_FOR_PLAYER;
    }

    public static Game reconstitute(String id, Player p1, Player p2, String turn, GameState state, String winner) {
        Game game = new Game(p1);
        // We use reflection or direct assignment to force the state
        game.player2 = p2;
        game.currentTurnPlayerId = turn;
        game.state = state;
        game.winnerId = winner;

        // Overwrite the random ID generated in constructor with the real one from DB
        try {
            var idField = Game.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(game, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstitute game", e);
        }
        return game;
    }

    public void join(Player player2) {
        if (state != GameState.WAITING_FOR_PLAYER) {
            throw new IllegalStateException("Game is already full or finished");
        }
        this.player2 = player2;
        this.state = GameState.ACTIVE;
    }

    public ShotResult fire(String playerId, Coordinate target) {
        validateMove(playerId);

        // Determine opponent
        Player opponent = playerId.equals(player1.getId()) ? player2 : player1;

        // Fire at opponent's board
        ShotResult result = opponent.getBoard().fireShot(target);

        // Check Win Condition
        if (result == ShotResult.SUNK && opponent.hasLost()) {
            state = GameState.FINISHED;
            winnerId = playerId;
        }

        // Switch Turn (Simple Rule: Turn always switches)
        switchTurn();

        return result;
    }

    private void validateMove(String playerId) {
        if (state != GameState.ACTIVE) {
            throw new IllegalStateException("Game is not active");
        }
        if (!playerId.equals(currentTurnPlayerId)) {
            throw new IllegalArgumentException("It is not your turn!");
        }
    }

    private void switchTurn() {
        if (state == GameState.ACTIVE) {
            currentTurnPlayerId = currentTurnPlayerId.equals(player1.getId())
                    ? player2.getId()
                    : player1.getId();
        }
    }
}
