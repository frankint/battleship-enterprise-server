package com.frankint.battleship.api.controller;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.PlaceShipRequest;
import com.frankint.battleship.api.mapper.GameMapper;
import com.frankint.battleship.application.service.GameService;
import com.frankint.battleship.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final GameMapper gameMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. Create Game - No body needed, ID comes from Session
    @PostMapping
    public ResponseEntity<GameDTO> createGame(@AuthenticationPrincipal UserDetails user) {
        Game game = gameService.createGame(user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameMapper.toDTO(game, user.getUsername()));
    }

    // 2. Join Game - ID comes from Path, User from Session
    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameDTO> joinGame(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails user) {

        Game game = gameService.joinGame(gameId, user.getUsername());
        GameDTO p2Dto = gameMapper.toDTO(game, user.getUsername());

        // Notify Player 1 that the game has started!
        // We must reconstruct the DTO from P1's perspective
        if (game.getPlayer1() != null) {
            String p1Username = game.getPlayer1().getId();

            // Only send if P1 is actually a different person (not testing vs self)
            if (!p1Username.equals(user.getUsername())) {
                GameDTO p1Dto = gameMapper.toDTO(game, p1Username);

                // Broadcast to P1's specific topic
                messagingTemplate.convertAndSend(
                        "/topic/game/" + gameId + "/" + p1Username,
                        p1Dto
                );
            }
        }

        return ResponseEntity.ok(p2Dto);
    }

    // 3. Place Ship - User from Session
    @PostMapping("/{gameId}/place")
    public ResponseEntity<GameDTO> placeShip(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody PlaceShipRequest request) {

        Game game = gameService.placeShip(
                gameId,
                user.getUsername(),
                request.shipType(),
                request.start(),
                request.orientation()
        );

        return ResponseEntity.ok(gameMapper.toDTO(game, user.getUsername()));
    }

    // 4. Game History
    @GetMapping
    public ResponseEntity<List<GameDTO>> getMyGames(@AuthenticationPrincipal UserDetails user) {
        List<Game> games = gameService.getPlayerHistory(user.getUsername());

        // Convert all to DTOs
        List<GameDTO> dtos = games.stream()
                .map(g -> gameMapper.toDTO(g, user.getUsername()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{gameId}/hide")
    public ResponseEntity<Void> hideGame(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails user) {

        gameService.hideGame(gameId, user.getUsername());
        return ResponseEntity.ok().build();
    }
}