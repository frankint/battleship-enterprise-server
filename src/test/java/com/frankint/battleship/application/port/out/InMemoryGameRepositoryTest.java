package com.frankint.battleship.application.port.out;

import com.frankint.battleship.application.adapter.persistence.InMemoryGameRepository;
import com.frankint.battleship.domain.model.Board;
import com.frankint.battleship.domain.model.Game;
import com.frankint.battleship.domain.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryGameRepositoryTest {
    @Test
    public void testBasic() {
        InMemoryGameRepository repository = new InMemoryGameRepository();
        Game game = new Game(new Player("p1", new Board(10, 10)));
        assertEquals(game, repository.save(game));
        assertEquals(game, repository.findById(game.getId()).get());
        repository.delete(game.getId());
        assertTrue(repository.findById(game.getId()).isEmpty());
    }
}
