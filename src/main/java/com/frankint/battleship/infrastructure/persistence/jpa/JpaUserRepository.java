package com.frankint.battleship.infrastructure.persistence.jpa;

import com.frankint.battleship.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<UserEntity, String> {
}
