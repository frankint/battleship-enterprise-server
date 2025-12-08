package com.frankint.battleship.api.dto;

import com.frankint.battleship.domain.model.GameState;

public record GameDTO(
        String gameId,
        GameState state,
        String currentTurnPlayerId,
        String winnerId,
        PlayerDTO self,     // The player requesting the data
        PlayerDTO opponent  // The enemy (masked)
) {
}
