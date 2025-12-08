package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.Coordinate;
import com.frankint.battleship.domain.model.Orientation;

public record PlaceShipRequest(
        String shipId,
        int size,
        Coordinate start,
        Orientation orientation
) {
}