package com.frankint.battleship.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameTest {
    @Test
    void testGameFlow() {
        // 1. Setup
        Player p1 = new Player("p1", new Board(10, 10));
        Player p2 = new Player("p2", new Board(10, 10));
        Game game = new Game(p1);

        // 2. Join
        game.join(p2);
        assertEquals(GameState.ACTIVE, game.getState());

        // 3. P1 fires (Valid)
        assertDoesNotThrow(() -> game.fire("p1", new Coordinate(0, 0)));

        // 4. P1 fires again (Invalid - turn switched)
        assertThrows(IllegalArgumentException.class, () ->
                game.fire("p1", new Coordinate(0, 1))
        );

        // 5. P2 fires (Valid)
        assertDoesNotThrow(() -> game.fire("p2", new Coordinate(0, 0)));
    }
    @Test
    void testJoin_GameAlreadyStarted() {
        // 1. Setup
        Player p1 = new Player("p1", new Board(10, 10));
        Player p2 = new Player("p2", new Board(10, 10));
        Player p3 = new Player("p2", new Board(10, 10));
        Game game = new Game(p1);

        // 2. Join
        game.join(p2);
        assertEquals(GameState.ACTIVE, game.getState());
        assertThrows(IllegalStateException.class, () ->game.join(p3));
    }
    @Test
    void testValidateMove_GameNotStarted() {
        Player p1 = new Player("p1", new Board(10, 10));
        Game game = new Game(p1);
        assertThrows(IllegalStateException.class, () ->game.fire("p1", new Coordinate(0, 0)));
    }
}
