package com.frankint.battleship.api.mapper;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.ShipDTO;
import com.frankint.battleship.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameMapperTest {

    private final GameMapper mapper = new GameMapper();

    @Test
    void testToDTO_HidesOpponentShips() {
        Board b1 = new Board(10, 10);
        b1.placeShip("s1", 3, new Coordinate(0, 0), Orientation.HORIZONTAL);
        Player p1 = new Player("p1", b1);

        Board b2 = new Board(10, 10);
        b2.placeShip("s2", 3, new Coordinate(5, 5), Orientation.HORIZONTAL); // Hidden
        Player p2 = new Player("p2", b2);

        Game game = new Game(p1);
        game.join(p2);

        GameDTO dto = mapper.toDTO(game, "p1");

        assertEquals("p1", dto.self().playerId());
        assertFalse(dto.self().ships().isEmpty());

        assertEquals("p2", dto.opponent().playerId());
        assertTrue(dto.opponent().ships().isEmpty());
    }

    @Test
    void testToDTO_HidesOpponentShips_WhenViewerIsPlayer2() {
        Board b1 = new Board(10, 10);
        b1.placeShip("s1", 4, new Coordinate(1, 1), Orientation.VERTICAL);
        Player p1 = new Player("p1", b1);

        Board b2 = new Board(10, 10);
        b2.placeShip("s2", 4, new Coordinate(7, 2), Orientation.VERTICAL);
        Player p2 = new Player("p2", b2);

        Game game = new Game(p1);
        game.join(p2);

        GameDTO dto = mapper.toDTO(game, "p2");

        assertFalse(dto.self().ships().isEmpty());
        assertTrue(dto.opponent().ships().isEmpty());
    }

    @Test
    void testToDTO_CopiesHitAndMissedShots() {
        Board b1 = new Board(10, 10);
        b1.placeShip("s1", 3, new Coordinate(0, 0), Orientation.HORIZONTAL);

        // Fire shots at P1's board
        b1.fireShot(new Coordinate(0, 0)); // hit
        b1.fireShot(new Coordinate(9, 9)); // miss

        Player p1 = new Player("p1", b1);
        Game game = new Game(p1);

        GameDTO dto = mapper.toDTO(game, "p1");

        assertEquals(1, dto.self().hits().size());
        assertEquals(1, dto.self().misses().size());
    }

    @Test
    void testToDTO_OpponentHitMissShownEvenIfShipsHidden() {
        Board b1 = new Board(10, 10);
        b1.placeShip("s1", 3, new Coordinate(0, 0), Orientation.HORIZONTAL);
        Player p1 = new Player("p1", b1);

        Board b2 = new Board(10, 10);
        b2.placeShip("s2", 3, new Coordinate(5, 5), Orientation.HORIZONTAL);
        b2.fireShot(new Coordinate(5, 5)); // hit
        b2.fireShot(new Coordinate(0, 9)); // miss
        Player p2 = new Player("p2", b2);

        Game game = new Game(p1);
        game.join(p2);

        GameDTO dto = mapper.toDTO(game, "p1");

        assertEquals(1, dto.opponent().hits().size());
        assertEquals(1, dto.opponent().misses().size());
        assertTrue(dto.opponent().ships().isEmpty());
    }

    @Test
    void testToDTO_SunkShipStateIncludedForSelf() {
        Board b1 = new Board(10, 10);
        b1.placeShip("s1", 2, new Coordinate(0, 0), Orientation.HORIZONTAL);

        // sink the ship
        b1.fireShot(new Coordinate(0, 0));
        b1.fireShot(new Coordinate(1, 0));

        Player p1 = new Player("p1", b1);

        Game game = new Game(p1);

        GameDTO dto = mapper.toDTO(game, "p1");
        ShipDTO shipDTO = dto.self().ships().get(0);

        assertTrue(shipDTO.sunk(), "Ship should be sunk");
    }

    @Test
    void testToDTO_CopiesMetadata() {
        Player p1 = new Player("p1", new Board(10, 10));
        Player p2 = new Player("p2", new Board(10, 10));

        Game game = new Game(p1);
        game.join(p2);

        GameDTO dto = mapper.toDTO(game, "p1");

        assertEquals(GameState.SETUP, dto.state());
        assertEquals("p1", dto.currentTurnPlayerId());
        assertEquals(null, dto.winnerId());
    }

    @Test
    void testToDTO_ThrowsIfViewerNotInGame() {
        Player p1 = new Player("p1", new Board(10, 10));
        Game game = new Game(p1);

        assertThrows(IllegalArgumentException.class, () ->
                mapper.toDTO(game, "stranger")
        );
    }

    @Test
    void testToDTO_OnePlayerGameOpponentIsNull() {
        Player p1 = new Player("p1", new Board(10, 10));
        Game game = new Game(p1);

        GameDTO dto = mapper.toDTO(game, "p1");

        assertNotNull(dto.self());
        assertNull(dto.opponent());
    }
}
