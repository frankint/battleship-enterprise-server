package com.frankint.battleship.domain.model;

import lombok.Getter;

import java.util.*;

@Getter
public class Board {
    private int width;
    private int height;
    private List<Ship> ships;
    private List<Coordinate> missedShots;
    private List<Coordinate> hitShots;
    private Set<Coordinate> occupied;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Board width and height must be positive");
        this.width = width;
        this.height = height;
        this.ships = new ArrayList<>();
        this.missedShots = new ArrayList<>();
        this.hitShots = new ArrayList<>();
        this.occupied = new HashSet<>();
    }

    private Board() {
        this.width = 10;
        this.height = 10;
        this.ships = new ArrayList<>();
        this.missedShots = new ArrayList<>();
        this.hitShots = new ArrayList<>();
        this.occupied = new HashSet<>();
    }

    /**
     * Attempts to place a ship.
     * Throws IllegalArgumentException if placement is invalid.
     */
    public void placeShip(String shipId, int length, Coordinate start, Orientation orientation) {
        List<Coordinate> coordinates = calculateCoordinates(start, length, orientation);

        canPlaceShip(coordinates);

        Ship newShip = new Ship(shipId, length, coordinates);
        ships.add(newShip);
        occupied.addAll(coordinates);
    }

    private List<Coordinate> calculateCoordinates(Coordinate start, int length, Orientation orientation) {
        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int x = start.x() + (orientation == Orientation.HORIZONTAL ? i : 0);
            int y = start.y() + (orientation == Orientation.VERTICAL ? i : 0);
            coords.add(new Coordinate(x, y));
        }
        return coords;
    }

    public ShotResult fireShot(Coordinate coordinate) {
        // Check bounds
        if (coordinate.x() >= width || coordinate.y() >= height) {
            throw new IllegalArgumentException("Shot out of bounds");
        }

        // Check if already shot here
        if (missedShots.contains(coordinate) || hitShots.contains(coordinate)) return ShotResult.DUPLICATE;

        // Check if we hit a ship
        Optional<Ship> hitShip = ships.stream()
                .filter(s -> s.isHit(coordinate))
                .findFirst();

        if (hitShip.isPresent()) {
            Ship ship = hitShip.get();
            ship.takeDamage();
            hitShots.add(coordinate);
            return ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
        }

        // It's a miss
        missedShots.add(coordinate);
        return ShotResult.MISS;
    }

    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    private void canPlaceShip(List<Coordinate> coordinates) {
        for (Coordinate c : coordinates) {
            if (c.x() >= width || c.y() >= height) {
                throw new IllegalArgumentException("Ship extends off the board at " + c);
            }
        }
        for (Coordinate c : coordinates) {
            if (occupied.contains(c)) {
                throw new IllegalArgumentException("Ship overlaps with an existing ship at " + c);
            }
        }
    }
}
