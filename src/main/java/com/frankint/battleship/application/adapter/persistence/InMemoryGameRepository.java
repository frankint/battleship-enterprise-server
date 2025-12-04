package com.frankint.battleship.application.adapter.persistence;

import com.frankint.battleship.application.port.out.GameRepository;
import com.frankint.battleship.domain.model.Game;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for storing games in memory.
 * Useful for testing and early development phases.
 */
@Repository
public class InMemoryGameRepository implements GameRepository {

    // ConcurrentHashMap is crucial here for thread safety in a multiplayer environment
    private final Map<String, Game> store = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        store.put(game.getId(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(store.get(gameId));
    }

    @Override
    public void delete(String gameId) {
        store.remove(gameId);
    }
}