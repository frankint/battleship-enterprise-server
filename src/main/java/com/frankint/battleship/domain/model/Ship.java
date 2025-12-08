package com.frankint.battleship.domain.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Ship {
    private String id;
    private int size;
    private int health;
    private boolean sunk;
    private List<Coordinate> coordinates;

    public Ship(String id, int size, List<Coordinate> coordinates) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id is null or empty");
        if (size < 0) throw new IllegalArgumentException("size is negative");
        if (coordinates == null || coordinates.isEmpty()) throw new IllegalArgumentException("coordinates is null or empty");
        if (coordinates.size() != size) throw new IllegalArgumentException("coordinates size doesn't match");
        this.id = id;
        this.size = size;
        this.health = size;
        this.coordinates = new ArrayList<>(coordinates);
        this.sunk = false;
    }

    private Ship() {
        this.coordinates = new ArrayList<>();
    }

    public boolean isHit(Coordinate shot) {
        return coordinates.contains(shot);
    }

    public void takeDamage() {
        if (health > 0) health--;
        if (health == 0) sunk = true;
    }
}
