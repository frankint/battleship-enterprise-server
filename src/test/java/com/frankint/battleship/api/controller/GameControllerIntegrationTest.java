package com.frankint.battleship.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frankint.battleship.api.dto.JoinGameRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Ensures DB is cleaned up after every test
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateGameSuccessfully() throws Exception {
        JoinGameRequest request = new JoinGameRequest("p1-create");

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("WAITING_FOR_PLAYER"))
                .andExpect(jsonPath("$.self.playerId").value("p1-create"));
    }

    @Test
    void shouldJoinGameSuccessfully() throws Exception {
        // 1. Create
        String createResponse = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest("p1-join"))))
                .andReturn().getResponse().getContentAsString();

        String gameId = objectMapper.readTree(createResponse).get("gameId").asText();

        // 2. Join
        mockMvc.perform(post("/api/games/" + gameId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest("p2-join"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SETUP"))
                .andExpect(jsonPath("$.opponent.playerId").value("p1-join"));
    }

    @Test
    void shouldPlaceShipSuccessfully() throws Exception {
        // 1. Setup Game (Create + Join)
        String gameId = createAndJoinGame("p1-place", "p2-place");

        // 2. Prepare Request (Note: No 'size' parameter, using 'shipType')
        PlaceShipRequest request = new PlaceShipRequest(
                "Carrier", // Matches ShipType.CARRIER
                new Coordinate(0, 0),
                Orientation.HORIZONTAL
        );

        // 3. Perform Request
        mockMvc.perform(post("/api/games/" + gameId + "/place")
                        .header("X-Player-ID", "p1-place") // Auth Header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.self.ships[0].id").value("Carrier"))
                .andExpect(jsonPath("$.self.ships[0].size").value(5)); // Server enforces size 5
    }

    // Helper to reduce code duplication in tests
    private String createAndJoinGame(String p1, String p2) throws Exception {
        String response = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest(p1))))
                .andReturn().getResponse().getContentAsString();

        String gameId = objectMapper.readTree(response).get("gameId").asText();

        mockMvc.perform(post("/api/games/" + gameId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinGameRequest(p2))));

        return gameId;
    }
}