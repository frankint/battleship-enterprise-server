package com.frankint.battleship.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    @Test
    void testPlaceShip_OutOfBounds_RightEdge() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(9, 9);

        assertThrows(IllegalArgumentException.class, () -> board.placeShip("Destroyer", 2, c, Orientation.HORIZONTAL)); // Out of bounds
    }

    @Test
    void testPlaceShip_OutOfBounds_BottomEdge() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(9, 9);

        assertThrows(IllegalArgumentException.class, () -> board.placeShip("Destroyer", 2, c, Orientation.VERTICAL)); // Out of bounds
    }

    @Test
    void testPlaceShip_SuccessfulPlacement() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(8, 9); // valid second coordinate

        assertDoesNotThrow(() -> board.placeShip("Destroyer", 2, c, Orientation.HORIZONTAL)); // Valid placement
    }

    @Test
    void testPlaceShip_Overlap() {
        Board board = new Board(10, 10);
        Coordinate c1 = new Coordinate(8, 9);
        Coordinate c2 = new Coordinate(9, 8);

        board.placeShip("Destroyer", 2, c1, Orientation.HORIZONTAL);
        assertThrows(IllegalArgumentException.class, () -> board.placeShip("Destroyer1", 2, c2, Orientation.VERTICAL)); // Overlap with existing ship
    }

    @Test
    void testInvalidBoardSize_HeightZero() {
        assertThrows(IllegalArgumentException.class, () -> new Board(10, 0));
    }

    @Test
    void testInvalidBoardSize_HeightNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Board(10, -10));
    }

    @Test
    void testInvalidBoardSize_WidthZero() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 10));
    }

    @Test
    void testInvalidBoardSize_WidthNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Board(-10, 10));
    }

    @Test
    void testFireShot_Miss() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(5, 5); // no ship placed

        ShotResult result = board.fireShot(c);

        assertEquals(ShotResult.MISS, result);
    }

    @Test
    void testFireShot_Hit_NotSunk() {
        Board board = new Board(10, 10);
        Coordinate c1 = new Coordinate(0, 0);
        board.placeShip("Destroyer", 2, c1, Orientation.VERTICAL);

        ShotResult result = board.fireShot(c1);

        assertEquals(ShotResult.HIT, result);
    }

    @Test
    void testFireShot_Sunk() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(0, 0);
        board.placeShip("Destroyer", 1, c, Orientation.HORIZONTAL);

        ShotResult result = board.fireShot(c);

        assertEquals(ShotResult.SUNK, result);
    }

    @Test
    void testFireShot_RepeatedShotSameCoordinateHit() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(0, 0);
        board.placeShip("Destroyer", 1, c, Orientation.HORIZONTAL);

        board.fireShot(c); // first shot sinks ship
        ShotResult result = board.fireShot(c); // second should NOT sink again

        assertEquals(ShotResult.DUPLICATE, result);
    }

    @Test
    void testFireShot_RepeatedShotSameCoordinateMiss() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(0, 0);
        Coordinate c0 = new Coordinate(1, 0);
        board.placeShip("Destroyer", 1, c0, Orientation.HORIZONTAL);

        board.fireShot(c); // first shot sinks ship
        ShotResult result = board.fireShot(c); // second should NOT sink again

        assertEquals(ShotResult.DUPLICATE, result);
    }

    @Test
    void testFireShot_OutOfBoundsBoth() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(10, 10); // out of bounds

        assertThrows(IllegalArgumentException.class, () -> board.fireShot(c));
    }

    @Test
    void testFireShot_OutOfBoundsX() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(10, 9); // out of bounds

        assertThrows(IllegalArgumentException.class, () -> board.fireShot(c));
    }

    @Test
    void testFireShot_OutOfBoundsY() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(9, 10); // out of bounds

        assertThrows(IllegalArgumentException.class, () -> board.fireShot(c));
    }

    @Test
    void testFireShot_AtSunkShip() {
        Board board = new Board(10, 10);
        Coordinate c = new Coordinate(0, 0);
        board.placeShip("ship", 1, c, Orientation.HORIZONTAL);

        board.fireShot(c);      // sinks the ship
        ShotResult result = board.fireShot(c); // second shot

        assertEquals(ShotResult.DUPLICATE, result);
    }

    @Test
    void testFireShot_MultipleShipsCorrectHit() {
        Board board = new Board(10, 10);

        board.placeShip("Submarine", 1, new Coordinate(1, 1), Orientation.HORIZONTAL);
        board.placeShip("Destroyer", 2, new Coordinate(3, 3), Orientation.VERTICAL);

        ShotResult result1 = board.fireShot(new Coordinate(3, 3)); // hit destroyer
        ShotResult result2 = board.fireShot(new Coordinate(1, 1)); // hit submarine

        assertEquals(ShotResult.HIT, result1);
        assertEquals(ShotResult.SUNK, result2); // size 1 ship
    }


}
