package com.frankint.battleship.api.mapper;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.PlayerDTO;
import com.frankint.battleship.api.dto.ShipDTO;
import com.frankint.battleship.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameMapper {

    public GameDTO toDTO(Game game, String requestingPlayerId) {
        Player self;
        Player opponent;

        // Determine who is requesting
        if (game.getPlayer1().getId().equals(requestingPlayerId)) {
            self = game.getPlayer1();
            opponent = game.getPlayer2();
        } else {
            self = game.getPlayer2();
            opponent = game.getPlayer1();
        }

        return new GameDTO(
                game.getId(),
                game.getState(),
                game.getCurrentTurnPlayerId(),
                game.getWinnerId(),
                toPlayerDTO(self, true, game.getState()),     // Self: Always show everything
                toPlayerDTO(opponent, false, game.getState()) // Opponent: Show based on rules
        );
    }

    private PlayerDTO toPlayerDTO(Player player, boolean isSelf, GameState state) {
        if (player == null) return null;

        List<ShipDTO> visibleShips;
        List<ShipDTO> sunkShips;

        if (isSelf) {
            // I can always see my own ships
            visibleShips = mapShips(player.getBoard().getShips());
            sunkShips = Collections.emptyList(); // Redundant for self
        } else {
            // LOGIC FOR OPPONENT VIEW
            if (state == GameState.FINISHED) {
                // Issue 22: Game Over -> Show Everything
                visibleShips = mapShips(player.getBoard().getShips());
                sunkShips = Collections.emptyList();
            } else {
                // Active Game -> Hide healthy ships
                visibleShips = Collections.emptyList();

                // Issue 21: Reveal Sunk Ships
                sunkShips = player.getBoard().getShips().stream()
                        .filter(Ship::isSunk)
                        .map(this::toShipDTO)
                        .toList();
            }
        }

        return new PlayerDTO(
                player.getId(),
                visibleShips,
                sunkShips,
                player.getBoard().getHitShots(),
                player.getBoard().getMissedShots()
        );
    }

    private List<ShipDTO> mapShips(List<Ship> ships) {
        return ships.stream().map(this::toShipDTO).toList();
    }

    private ShipDTO toShipDTO(Ship ship) {
        return new ShipDTO(ship.getId(), ship.getSize(), ship.isSunk(), ship.getCoordinates());
    }
}