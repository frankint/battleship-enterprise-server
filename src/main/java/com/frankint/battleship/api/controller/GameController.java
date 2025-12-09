package com.frankint.battleship.api.controller;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.JoinGameRequest;
import com.frankint.battleship.api.dto.PlaceShipRequest;
import com.frankint.battleship.api.mapper.GameMapper;
import com.frankint.battleship.application.service.GameService;
import com.frankint.battleship.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final GameMapper gameMapper;

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody JoinGameRequest request) {
        // We reuse JoinGameRequest since it just contains a playerId
        Game game = gameService.createGame(request.playerId());

        // Return 201 Created
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameMapper.toDTO(game, request.playerId()));
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameDTO> joinGame(
            @PathVariable String gameId,
            @RequestBody JoinGameRequest request) {

        Game game = gameService.joinGame(gameId, request.playerId());

        return ResponseEntity.ok(gameMapper.toDTO(game, request.playerId()));
    }

    // NOTE: This endpoint is strictly for ship placement via REST.
    @PostMapping("/{gameId}/place")
    public ResponseEntity<GameDTO> placeShip(
            @PathVariable String gameId,
            @RequestHeader("X-Player-ID") String playerId,
            @RequestBody PlaceShipRequest request) {

        Game game = gameService.placeShip(
                gameId,
                playerId,
                request.shipType(),
                request.start(),
                request.orientation()
        );

        return ResponseEntity.ok(gameMapper.toDTO(game, playerId));
    }
}