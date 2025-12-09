package com.frankint.battleship.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frankint.battleship.api.dto.PlaceShipRequest;
import com.frankint.battleship.domain.model.Coordinate;
import com.frankint.battleship.domain.model.Orientation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateGameSuccessfully() throws Exception {
        // Authenticate as "player-1" using the .with(user(...)) post-processor
        mockMvc.perform(post("/api/games")
                        .with(user("player-1"))) // <--- SIMULATES LOGIN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("WAITING_FOR_PLAYER"))
                .andExpect(jsonPath("$.self.playerId").value("player-1"));
    }

    @Test
    void shouldJoinGameSuccessfully() throws Exception {
        // 1. Player 1 creates game
        String response = mockMvc.perform(post("/api/games")
                        .with(user("p1")))
                .andReturn().getResponse().getContentAsString();

        String gameId = objectMapper.readTree(response).get("gameId").asText();

        // 2. Player 2 joins game (Notice we switch users!)
        mockMvc.perform(post("/api/games/" + gameId + "/join")
                        .with(user("p2"))) // <--- DIFFERENT USER
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SETUP"))
                .andExpect(jsonPath("$.opponent.playerId").value("p1"));
    }

    @Test
    void shouldPlaceShipSuccessfully() throws Exception {
        // 1. Setup Game (Create + Join)
        String gameId = createAndJoinGame("p1", "p2");

        // 2. Prepare Request (Carrier)
        PlaceShipRequest request = new PlaceShipRequest(
                "Carrier",
                new Coordinate(0, 0),
                Orientation.HORIZONTAL
        );

        // 3. P1 Places Ship
        mockMvc.perform(post("/api/games/" + gameId + "/place")
                        .with(user("p1")) // <--- Authenticated as P1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.self.ships[0].id").value("Carrier"));
    }

    @Test
    void shouldGetGameHistory() throws Exception {
        // 1. Create a game as "history-user"
        mockMvc.perform(post("/api/games")
                .with(user("history-user")));

        // 2. Fetch history
        mockMvc.perform(get("/api/games")
                        .with(user("history-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Should have 1 game
                .andExpect(jsonPath("$[0].self.playerId").value("history-user"));
    }

    @Test
    void shouldFailIfUnauthenticated() throws Exception {
        // Try to create game without .with(user(...))
        mockMvc.perform(post("/api/games"))
                .andExpect(status().isUnauthorized()); // Expect 401
    }

    // Helper
    private String createAndJoinGame(String p1, String p2) throws Exception {
        String response = mockMvc.perform(post("/api/games")
                        .with(user(p1)))
                .andReturn().getResponse().getContentAsString();

        String gameId = objectMapper.readTree(response).get("gameId").asText();

        mockMvc.perform(post("/api/games/" + gameId + "/join")
                .with(user(p2)));

        return gameId;
    }
}