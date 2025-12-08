package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.Coordinate;

import java.util.List;

public record PlayerDTO(
        String playerId,
        List<ShipDTO> ships,      // Only populated if "self"
        List<Coordinate> hits,    // Shots fired by opponent that hit ships
        List<Coordinate> misses   // Shots fired by opponent that missed
) {
}
