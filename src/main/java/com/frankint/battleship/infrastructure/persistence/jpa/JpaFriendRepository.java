package com.frankint.battleship.infrastructure.persistence.jpa;

import com.frankint.battleship.infrastructure.persistence.entity.FriendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaFriendRepository extends JpaRepository<FriendEntity, String> {
    List<FriendEntity> findAllByUser(String user);
    boolean existsByUserAndFriend(String user, String friend);
    void deleteByUserAndFriend(String user, String friend);
}