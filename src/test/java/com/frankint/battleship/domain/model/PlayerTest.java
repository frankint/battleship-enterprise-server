package com.frankint.battleship.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerTest {
    @Test
    public void testPlayer() {
        Player player = new Player("player", new Board(10, 10));
        assertTrue(player.hasLost());
    }
}
