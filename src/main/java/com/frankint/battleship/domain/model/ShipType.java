package com.frankint.battleship.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipType {
    CARRIER("Carrier", 5),
    BATTLESHIP("Battleship", 4),
    CRUISER("Cruiser", 3),
    SUBMARINE("Submarine", 3),
    DESTROYER("Destroyer", 2);

    private final String id;
    private final int size;

    public static ShipType fromId(String id) {
        for (ShipType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ship type: " + id);
    }
}
