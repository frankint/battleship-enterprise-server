package com.frankint.battleship.infrastructure.persistence;

import com.frankint.battleship.application.port.out.GameRepository;
import com.frankint.battleship.domain.model.Game;
import com.frankint.battleship.infrastructure.persistence.entity.GameEntity;
import com.frankint.battleship.infrastructure.persistence.mapper.GameEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 1. The Internal JPA Interface
interface JpaGameRepository extends JpaRepository<GameEntity, String> {
    List<GameEntity> findByPlayer1_IdOrPlayer2_Id(String p1Id, String p2Id);
}

// 2. The Public Adapter (The one GameService uses)
@Primary
@Repository
@RequiredArgsConstructor
public class PostgresGameRepository implements GameRepository {

    private final JpaGameRepository jpaRepository;
    private final GameEntityMapper mapper;

    @Override
    public Game save(Game game) {
        GameEntity entity = mapper.toEntity(game);
        GameEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return jpaRepository.findById(gameId).map(mapper::toDomain);
    }

    @Override
    public void delete(String gameId) {
        jpaRepository.deleteById(gameId);
    }
    @Override
    public List<Game> findGamesByPlayer(String playerId) {
        return jpaRepository.findByPlayer1_IdOrPlayer2_Id(playerId, playerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}