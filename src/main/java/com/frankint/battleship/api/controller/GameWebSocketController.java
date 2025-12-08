package com.frankint.battleship.api.controller;

import com.frankint.battleship.api.dto.GameDTO;
import com.frankint.battleship.api.dto.MoveRequest;
import com.frankint.battleship.api.mapper.GameMapper;
import com.frankint.battleship.application.service.GameService;
import com.frankint.battleship.domain.model.Game;
import com.frankint.battleship.domain.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j // Adds logging capability
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final GameMapper gameMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/move")
    public void makeMove(
            @DestinationVariable String gameId,
            @Header("playerId") String playerId,
            @Payload MoveRequest request) {

        try {
            log.info("Player {} making move in game {} at {}", playerId, gameId, request.target());

            // 1. Execute the Logic (Validation happens here)
            Game game = gameService.makeMove(gameId, playerId, request.target());

            // 2. Broadcast the NEW state to BOTH players (Customized for each!)
            sendGameStateToPlayer(game, game.getPlayer1());
            if (game.getPlayer2() != null) {
                sendGameStateToPlayer(game, game.getPlayer2());
            }

        } catch (RuntimeException e) {
            log.warn("Invalid move by player {}: {}", playerId, e.getMessage());

            // 3. Send Error ONLY to the player who made the mistake
            // Topic: /topic/game/{gameId}/{playerId}/error
            String errorTopic = "/topic/game/" + gameId + "/" + playerId + "/error";
            messagingTemplate.convertAndSend(errorTopic, new ErrorResponse(e.getMessage()));
        }
    }

    private void sendGameStateToPlayer(Game game, Player player) {
        // Generate the view specifically for this player (hiding enemy ships)
        GameDTO gameDTO = gameMapper.toDTO(game, player.getId());

        // Push to their private topic: /topic/game/{gameId}/{playerId}
        String destination = "/topic/game/" + game.getId() + "/" + player.getId();
        messagingTemplate.convertAndSend(destination, gameDTO);
    }

    // Simple record for sending JSON errors
    record ErrorResponse(String message) {}
}