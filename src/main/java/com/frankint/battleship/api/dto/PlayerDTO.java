package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.Coordinate;

import java.util.List;

public record PlayerDTO(
        String playerId,
        List<ShipDTO> ships,      // My ships (or All ships if game over)
        List<ShipDTO> sunkShips,  // Opponent's sunk ships (for immediate reveal)
        List<Coordinate> hits,
        List<Coordinate> misses
) {
}
