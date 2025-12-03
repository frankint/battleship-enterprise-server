package com.frankint.battleship.domain.model;

import lombok.Getter;

import java.util.*;

@Getter
public class Board {
    private final int width;
    private final int height;
    private final List<Ship> ships;
    private final List<Coordinate> missedShots;
    private final List<Coordinate> hitShots;
    private final Set<Coordinate> occupied;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Board width and height must be positive");
        this.width = width;
        this.height = height;
        this.ships = new ArrayList<>();
        this.missedShots = new ArrayList<>();
        this.hitShots = new ArrayList<>();
        this.occupied = new HashSet<>();
    }

    public boolean placeShip(Ship ship) {
        if(!canPlaceShip(ship)) return false;
        ships.add(ship);
        occupied.addAll(ship.getCoordinates());
        return true;
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
    private boolean canPlaceShip(Ship ship) {
        for (Coordinate c : ship.getCoordinates()) {
            if (c.x() >= width || c.y() >= height) {
                return false; // Out of bounds
            }
        }
        for (Coordinate c : ship.getCoordinates()) {
            if (occupied.contains(c)) {
                return false; // Already occupied
            }
        }
        return true;
    }
}
