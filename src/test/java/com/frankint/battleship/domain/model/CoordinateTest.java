package com.frankint.battleship.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinateTest {

    @Test
    void testValidCoordinate() {
        Coordinate c = new Coordinate(5, 7);
        assertEquals(5, c.x());
        assertEquals(7, c.y());
    }

    @Test
    void testZeroCoordinateIsValid() {
        Coordinate c = new Coordinate(0, 0);
        assertEquals(0, c.x());
        assertEquals(0, c.y());
    }

    @Test
    void testNegativeXThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinate(-1, 5));
    }

    @Test
    void testNegativeYThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinate(3, -2));
    }

    @Test
    void testBothNegativeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinate(-4, -4));
    }

    @Test
    void testLargeCoordinateValuesAreAllowed() {
        Coordinate c = new Coordinate(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, c.x());
        assertEquals(Integer.MAX_VALUE, c.y());
    }
}

