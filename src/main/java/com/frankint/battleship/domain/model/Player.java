package com.frankint.battleship.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Player {
    private final String id;
    private final Board board;

    public boolean hasLost() {
        return board.allShipsSunk();
    }
}
