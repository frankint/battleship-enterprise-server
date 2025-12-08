package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.Coordinate;

import java.util.List;

public record ShipDTO(
        String id,
        int size,
        boolean sunk,
        List<Coordinate> coordinates // Only sent to owner
) {
}
