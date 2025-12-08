package com.frankint.battleship.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frankint.battleship.api.dto.JoinGameRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // Loads the full application context
@AutoConfigureMockMvc
@Transactional
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // To convert Objects to JSON

    @Test
    void shouldCreateGameSuccessfully() throws Exception {
        JoinGameRequest request = new JoinGameRequest("player-1");

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("WAITING_FOR_PLAYER"))
                .andExpect(jsonPath("$.self.playerId").value("player-1"));
    }

    @Test
    void shouldJoinGameSuccessfully() throws Exception {
        // 1. Create a game first
        String createResponse = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest("player-1"))))
                .andReturn().getResponse().getContentAsString();

        // Extract ID (Simple string manipulation for test speed)
        String gameId = objectMapper.readTree(createResponse).get("gameId").asText();

        // 2. Join it
        mockMvc.perform(post("/api/games/" + gameId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGameRequest("player-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.opponent.playerId").value("player-1"));
    }
}