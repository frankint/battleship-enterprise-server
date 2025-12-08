package com.frankint.battleship.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frankint.battleship.domain.model.Board;
import com.frankint.battleship.domain.model.Game;
import com.frankint.battleship.domain.model.Player;
import com.frankint.battleship.infrastructure.persistence.entity.GameEntity;
import com.frankint.battleship.infrastructure.persistence.entity.PlayerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameEntityMapper {

    private final ObjectMapper objectMapper;

    public GameEntity toEntity(Game game) {
        GameEntity entity = new GameEntity();
        entity.setId(game.getId());
        entity.setState(game.getState());
        entity.setCurrentTurnPlayerId(game.getCurrentTurnPlayerId());
        entity.setWinnerId(game.getWinnerId());
        entity.setPlayer1(toPlayerEntity(game.getPlayer1()));
        if (game.getPlayer2() != null) {
            entity.setPlayer2(toPlayerEntity(game.getPlayer2()));
        }
        return entity;
    }

    public Game toDomain(GameEntity entity) {
        return Game.reconstitute(
                entity.getId(),
                toPlayerDomain(entity.getPlayer1()),
                entity.getPlayer2() != null ? toPlayerDomain(entity.getPlayer2()) : null,
                entity.getCurrentTurnPlayerId(),
                entity.getState(),
                entity.getWinnerId()
        );
    }

    private PlayerEntity toPlayerEntity(Player player) {
        PlayerEntity entity = new PlayerEntity();
        entity.setId(player.getId());
        try {
            entity.setBoardJson(objectMapper.writeValueAsString(player.getBoard()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing board", e);
        }
        return entity;
    }

    private Player toPlayerDomain(PlayerEntity entity) {
        try {
            Board board = objectMapper.readValue(entity.getBoardJson(), Board.class);
            return new Player(entity.getId(), board);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing board", e);
        }
    }
}