package com.frankint.battleship.application.service;

import com.frankint.battleship.application.port.out.GameRepository;
import com.frankint.battleship.domain.exception.GameNotFoundException;
import com.frankint.battleship.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    /**
     * Starts a new game with the initial player.
     * Note: In a real app, we would look up the Player details from a User DB.
     */
    public Game createGame(String playerId) {
        Board emptyBoard = new Board(10, 10); // Standard 10x10 board
        Player player1 = new Player(playerId, emptyBoard);

        Game game = new Game(player1);

        return gameRepository.save(game);
    }

    /**
     * Joins an existing game.
     */
    public Game joinGame(String gameId, String playerId) {
        Game game = getGameOrThrow(gameId);

        Board emptyBoard = new Board(10, 10);
        Player player2 = new Player(playerId, emptyBoard);

        game.join(player2);

        return gameRepository.save(game);
    }

    public Game placeShip(String gameId, String playerId, String shipId, int length, Coordinate start, Orientation orientation) {
        Game game = getGameOrThrow(gameId);

        // We need to find the correct player's board
        // Note: You might need to add a helper method in Game.java like getPlayer(id)
        Player player = game.getPlayer1().getId().equals(playerId) ? game.getPlayer1() : game.getPlayer2();

        if (player == null || !player.getId().equals(playerId)) {
            throw new IllegalArgumentException("Player not part of this game");
        }

        // Execute domain logic
        player.getBoard().placeShip(shipId, length, start, orientation);

        return gameRepository.save(game);
    }

    /**
     * Orchestrates the move: Load -> Fire -> Save
     */
    public Game makeMove(String gameId, String playerId, Coordinate target) {
        Game game = getGameOrThrow(gameId);

        // The Domain Model enforces the rules (Turn check, Game Over check, etc)
        game.fire(playerId, target);

        return gameRepository.save(game);
    }

    public Game getGame(String gameId) {
        return getGameOrThrow(gameId);
    }

    // Helper method to DRY (Don't Repeat Yourself)
    private Game getGameOrThrow(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }
}