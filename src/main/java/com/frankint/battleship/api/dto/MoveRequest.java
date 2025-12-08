package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.Coordinate;

public record MoveRequest(Coordinate target) {
}
