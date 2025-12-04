package com.frankint.battleship.application.port.out;

import com.frankint.battleship.domain.model.Game;

import java.util.Optional;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findById(String gameId);
    void delete(String gameId);
}
