package com.flowboard.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return fallback("auth-service");
    }

    @RequestMapping("/fallback/workspace")
    public ResponseEntity<Map<String, Object>> workspaceFallback() {
        return fallback("workspace-service");
    }

    @RequestMapping("/fallback/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        return fallback("payment-service");
    }

    @RequestMapping("/fallback/board")
    public ResponseEntity<Map<String, Object>> boardFallback() {
        return fallback("board-service");
    }

    @RequestMapping("/fallback/list")
    public ResponseEntity<Map<String, Object>> listFallback() {
        return fallback("list-service");
    }

    @RequestMapping("/fallback/comment")
    public ResponseEntity<Map<String, Object>> commentFallback() {
        return fallback("comment-service");
    }

    @RequestMapping("/fallback/label")
    public ResponseEntity<Map<String, Object>> labelFallback() {
        return fallback("label-service");
    }

    @RequestMapping("/fallback/card")
    public ResponseEntity<Map<String, Object>> cardFallback() {
        return fallback("card-service");
    }

    @RequestMapping("/fallback/notification")
    public ResponseEntity<Map<String, Object>> notificationFallback() {
        return fallback("notification-service");
    }

    private ResponseEntity<Map<String, Object>> fallback(String serviceName) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "service", serviceName,
                "message", serviceName + " is temporarily unavailable. Please try again shortly."
        ));
    }
}
