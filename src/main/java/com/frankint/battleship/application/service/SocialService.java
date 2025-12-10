package com.frankint.battleship.application.service;

import com.frankint.battleship.infrastructure.persistence.entity.FriendEntity;
import com.frankint.battleship.infrastructure.persistence.jpa.JpaFriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final JpaFriendRepository friendRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- FRIEND LOGIC ---
    @Transactional
    public void addFriend(String user, String friendId) {
        // Prevent self-add and duplicates
        if (user.equals(friendId)) throw new IllegalArgumentException("Cannot add yourself");

        if (!friendRepository.existsByUserAndFriend(user, friendId)) {
            FriendEntity f = new FriendEntity();
            f.setUser(user);
            f.setFriend(friendId);
            friendRepository.save(f);
        }
    }

    public List<String> getFriends(String user) {
        return friendRepository.findAllByUser(user).stream()
                .map(FriendEntity::getFriend)
                .toList();
    }

    @Transactional
    public void removeFriend(String user, String friendId) {
        friendRepository.deleteByUserAndFriend(user, friendId);
    }

    // --- INVITE LOGIC ---
    public void sendInvite(String sender, String targetUser, String gameId) {
        // Send a real-time message to the target user
        Notification notification = new Notification(
                "CHALLENGE",
                sender + " has challenged you to a game!",
                gameId,
                sender
        );

        // Topic: /topic/user/{targetUser}/notifications
        messagingTemplate.convertAndSend("/topic/user/" + targetUser + "/notifications", notification);
    }

    public void notifyDecline(String challenger, String decliner) {
        Notification n = new Notification(
                "DECLINED",
                decliner + " declined your challenge.",
                null,
                decliner
        );
        messagingTemplate.convertAndSend("/topic/user/" + challenger + "/notifications", n);
    }

    public record Notification(String type, String message, String gameId, String sender) {}
}