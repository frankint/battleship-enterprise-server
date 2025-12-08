package com.frankint.battleship.api.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors") // Sends to the specific user who caused the error
    public String handleIllegalArgument(IllegalArgumentException e) {
        return "Error: " + e.getMessage();
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public String handleIllegalState(IllegalStateException e) {
        return "Error: " + e.getMessage();
    }
}