package com.frankint.battleship.api.controller;

import com.frankint.battleship.application.service.GameService;
import com.frankint.battleship.application.service.SocialService;
import com.frankint.battleship.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final GameService gameService; // Needed to create the game for the invite

    @GetMapping("/friends")
    public List<String> getFriends(@AuthenticationPrincipal UserDetails user) {
        return socialService.getFriends(user.getUsername());
    }

    @PostMapping("/friends")
    public void addFriend(@AuthenticationPrincipal UserDetails user, @RequestBody Map<String, String> body) {
        socialService.addFriend(user.getUsername(), body.get("username"));
    }

    @PostMapping("/invite")
    public ResponseEntity<String> sendInvite(@AuthenticationPrincipal UserDetails user, @RequestBody Map<String, String> body) {
        String opponent = body.get("username");

        // 1. Create a new game
        Game game = gameService.createGame(user.getUsername());

        // 2. Notify the opponent
        socialService.sendInvite(user.getUsername(), opponent, game.getId());

        return ResponseEntity.ok(game.getId());
    }
    // 1. Remove Friend
    @DeleteMapping("/friends/{username}")
    public void removeFriend(@AuthenticationPrincipal UserDetails user, @PathVariable String username) {
        socialService.removeFriend(user.getUsername(), username);
    }

    // 2. Decline Invite
    @PostMapping("/invite/decline")
    public void declineInvite(@AuthenticationPrincipal UserDetails user, @RequestBody Map<String, String> body) {
        String gameId = body.get("gameId");
        String challenger = body.get("challenger");

        // 1. Notify the challenger (so they get alerted and kicked to lobby)
        socialService.notifyDecline(challenger, user.getUsername());

        // 2. Delete the game entirely.
        // It will disappear from the database and history immediately.
        gameService.deleteGame(gameId);
    }
}