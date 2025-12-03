package com.frankint.battleship.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {
    @Test
    void testValidShipCreation() {
        List<Coordinate> coords = List.of(new Coordinate(0, 0), new Coordinate(0, 1));
        Ship ship = new Ship("Destroyer", 2, coords);

        assertEquals("Destroyer", ship.getId());
        assertEquals(2, ship.getSize());
        assertEquals(2, ship.getHealth());
        assertFalse(ship.isSunk());
        assertEquals(coords, ship.getCoordinates());
    }

    @Test
    void testIdNullThrowsException() {
        List<Coordinate> coords = List.of(new Coordinate(0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new Ship(null, 1, coords));
    }

    @Test
    void testIdEmptyThrowsException() {
        List<Coordinate> coords = List.of(new Coordinate(0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new Ship("", 1, coords));
    }

    @Test
    void testNegativeSizeThrowsException() {
        List<Coordinate> coords = List.of(new Coordinate(0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new Ship("Test", -1, coords));
    }

    @Test
    void testCoordinatesNullThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ship("Test", 1, null));
    }

    @Test
    void testCoordinatesEmptyThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ship("Test", 1, List.of()));
    }

    @Test
    void testCoordinateSizeMismatchThrowsException() {
        List<Coordinate> coords = List.of(new Coordinate(0, 0)); // size is 1

        assertThrows(IllegalArgumentException.class,
                () -> new Ship("Destroyer", 2, coords));
    }

    @Test
    void testIsHit_ReturnsTrueWhenShotMatchesCoordinate() {
        Coordinate c = new Coordinate(0, 0);
        Ship ship = new Ship("Sub", 1, List.of(c));

        assertTrue(ship.isHit(new Coordinate(0, 0)));
    }

    @Test
    void testIsHit_ReturnsFalseWhenShotMisses() {
        Ship ship = new Ship("Sub", 1, List.of(new Coordinate(0, 0)));

        assertFalse(ship.isHit(new Coordinate(1, 1)));
    }

    @Test
    void testTakeDamage_ReducesHealth() {
        Ship ship = new Ship("Destroyer", 3,
                List.of(new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(0, 2)));

        ship.takeDamage();

        assertEquals(2, ship.getHealth());
        assertFalse(ship.isSunk());
    }

    @Test
    void testTakeDamage_SinksWhenHealthReachesZero() {
        Ship ship = new Ship("Sub", 1, List.of(new Coordinate(0, 0)));

        ship.takeDamage();

        assertEquals(0, ship.getHealth());
        assertTrue(ship.isSunk());
    }

    @Test
    void testTakeDamage_DoesNotGoBelowZero() {
        Ship ship = new Ship("Sub", 1, List.of(new Coordinate(0, 0)));

        ship.takeDamage(); // health 0, sunk true
        ship.takeDamage(); // should not change anything

        assertEquals(0, ship.getHealth());
        assertTrue(ship.isSunk());
    }

    @Test
    void testMultipleHitsSinkShipCorrectly() {
        Ship ship = new Ship("Destroyer", 2,
                List.of(new Coordinate(1, 1), new Coordinate(1, 2)));

        ship.takeDamage(); // health 1
        assertFalse(ship.isSunk());

        ship.takeDamage(); // health 0
        assertTrue(ship.isSunk());
    }
}
