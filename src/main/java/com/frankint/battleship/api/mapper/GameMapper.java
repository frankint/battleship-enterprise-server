package com.frankint.battleship.api.mapper;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.PlayerDTO;
import com.frankint.battleship.api.dto.ShipDTO;
import com.frankint.battleship.domain.model.Board;
import com.frankint.battleship.domain.model.Game;
import com.frankint.battleship.domain.model.Player;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameMapper {

    public GameDTO toDTO(Game game, String viewerId) {
        // Identify who is who
        Player player1 = game.getPlayer1();
        Player player2 = game.getPlayer2();

        Player viewer;
        Player opponent;

        if (player1.getId().equals(viewerId)) {
            viewer = player1;
            opponent = player2;
        } else if (player2 != null && player2.getId().equals(viewerId)) {
            viewer = player2;
            opponent = player1;
        } else {
            throw new IllegalArgumentException("Viewer is not a player in this game");
        }

        return new GameDTO(
                game.getId(),
                game.getState(),
                game.getCurrentTurnPlayerId(),
                game.getWinnerId(),
                toPlayerDTO(viewer, true),   // Show everything for self
                opponent != null ? toPlayerDTO(opponent, false) : null // Hide ships for opponent
        );
    }

    private PlayerDTO toPlayerDTO(Player player, boolean isSelf) {
        return new PlayerDTO(
                player.getId(),
                isSelf ? toShipDTOs(player.getBoard()) : Collections.emptyList(), // HIDE SHIPS IF NOT SELF
                player.getBoard().getHitShots(),
                player.getBoard().getMissedShots()
        );
    }

    private List<ShipDTO> toShipDTOs(Board board) {
        // We need to add getShips() to Board
        return board.getShips().stream()
                .map(ship -> new ShipDTO(
                        ship.getId(),
                        ship.getSize(),
                        ship.isSunk(),
                        ship.getCoordinates()
                ))
                .collect(Collectors.toList());
    }
}