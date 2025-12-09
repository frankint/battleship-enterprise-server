package com.frankint.battleship.application.service;

import com.frankint.battleship.application.port.out.GameRepository;
import com.frankint.battleship.domain.exception.GameNotFoundException;
import com.frankint.battleship.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    // Logic stays the same, but we ensure the caller (Controller) passes the secure ID
    @Transactional
    public Game createGame(String playerId) {
        Board emptyBoard = new Board(10, 10);
        Player player1 = new Player(playerId, emptyBoard);
        Game game = new Game(player1);
        return gameRepository.save(game);
    }

    @Transactional
    public Game joinGame(String gameId, String playerId) {
        Game game = getGameOrThrow(gameId);

        // Check if player is already in the game (Idempotency)
        if (game.getPlayer1().getId().equals(playerId)) {
            return game; // Re-joining their own game
        }

        // Check if Player 2 is re-joining
        if (game.getPlayer2() != null && game.getPlayer2().getId().equals(playerId)) {
            return game;
        }

        Board emptyBoard = new Board(10, 10);
        Player player2 = new Player(playerId, emptyBoard);
        game.join(player2);

        return gameRepository.save(game);
    }

    @Transactional
    public Game placeShip(String gameId, String playerId, String shipTypeId, Coordinate start, Orientation orientation) {
        Game game = getGameOrThrow(gameId);
        ShipType type = ShipType.fromId(shipTypeId);
        game.placeShip(playerId, type, start, orientation);
        return gameRepository.save(game);
    }

    @Transactional
    public Game makeMove(String gameId, String playerId, Coordinate target) {
        Game game = getGameOrThrow(gameId);

        // The Domain Model enforces the rules (Turn check, Game Over check, etc)
        game.fire(playerId, target);

        return gameRepository.save(game);
    }

    @Transactional
    public List<Game> getPlayerHistory(String playerId) {
        return gameRepository.findGamesByPlayer(playerId);
    }

    @Transactional
    public void hideGame(String gameId, String userId) {
        gameRepository.hideGame(gameId, userId);
    }

    // Helper
    private Game getGameOrThrow(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }
}